package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A minimal Chrome DevTools protocol client for the parity suites that must hold a state a measurement of a resting page cannot: it launches a headless
 * Chrome that is stated to be a device that can hover, navigates a page, forces pseudo-classes on real elements ({@code CSS.forcePseudoState}) and evaluates
 * a script. It was written for, and extracted unchanged from, {@code ButtonStateParityTest}; it adds nothing to the shared parity harness and changes no
 * normalization. Every failure to start or load is an infrastructure failure, never a parity result.
 */
final class DevToolsSession implements AutoCloseable {
    private final Process process;
    private final Path profile;
    private final WebSocket socket;
    private final AtomicInteger ids = new AtomicInteger();
    private final Map<Integer, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<String> loads = new LinkedBlockingQueue<>();

    DevToolsSession(Path chrome) throws Exception {
        profile = Files.createTempDirectory("ashlar-state-chrome");
        List<String> command = new ArrayList<>(List.of(chrome.toString(), "--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
                "--user-data-dir=" + profile, "--remote-debugging-port=0", "--remote-allow-origins=*", "--hide-scrollbars",
                "--blink-settings=primaryHoverType=2,availableHoverTypes=2", "about:blank"));
        if (System.getenv("CI") != null) {
            command.add("--no-sandbox");
        }
        process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        Path portFile = profile.resolve("DevToolsActivePort");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!Files.isRegularFile(portFile) || Files.readAllLines(portFile).size() < 2) {
            if (System.nanoTime() > deadline || !process.isAlive()) {
                throw new IllegalStateException("Chrome did not publish its DevTools port - infrastructure failure, not a parity result");
            }
            Thread.sleep(50);
        }
        int port = Integer.parseInt(Files.readAllLines(portFile).get(0).trim());
        HttpClient http = HttpClient.newHttpClient();
        String ws = null;
        for (int attempt = 0; attempt < 100 && ws == null; attempt++) {
            String list = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/json/list")).build(), HttpResponse.BodyHandlers.ofString()).body();
            Matcher m = Pattern.compile("\"type\":\\s*\"page\"[^}]*?\"webSocketDebuggerUrl\":\\s*\"([^\"]+)\"|\"webSocketDebuggerUrl\":\\s*\"([^\"]+)\"[^}]*?\"type\":\\s*\"page\"").matcher(list);
            if (m.find()) {
                ws = m.group(1) != null ? m.group(1) : m.group(2);
            } else {
                Thread.sleep(100);
            }
        }
        if (ws == null) {
            throw new IllegalStateException("Chrome has no page target - infrastructure failure, not a parity result");
        }
        socket = http.newWebSocketBuilder().buildAsync(URI.create(ws), new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                buffer.append(data);
                if (last) {
                    String message = buffer.toString();
                    buffer.setLength(0);
                    Matcher id = Pattern.compile("^\\{\"id\":(\\d+)").matcher(message);
                    if (id.find()) {
                        CompletableFuture<String> future = pending.remove(Integer.parseInt(id.group(1)));
                        if (future != null) {
                            future.complete(message);
                        }
                    } else if (message.contains("\"method\":\"Page.loadEventFired\"")) {
                        loads.add(message);
                    }
                }
                webSocket.request(1);
                return null;
            }
        }).get(30, TimeUnit.SECONDS);
        call("Page.enable", "{}");
        call("DOM.enable", "{}");
        call("CSS.enable", "{}");
    }

    String call(String method, String params) throws Exception {
        int id = ids.incrementAndGet();
        CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(id, future);
        socket.sendText("{\"id\":" + id + ",\"method\":\"" + method + "\",\"params\":" + params + "}", true);
        String response = future.get(60, TimeUnit.SECONDS);
        if (response.contains("\"error\":")) {
            throw new IllegalStateException(method + " failed: " + response);
        }
        return response;
    }

    void navigate(String url) throws Exception {
        loads.clear();
        call("Page.navigate", "{\"url\":" + quote(url) + "}");
        if (loads.poll(60, TimeUnit.SECONDS) == null) {
            throw new IllegalStateException("the page did not load - infrastructure failure, not a parity result: " + url);
        }
    }

    String evaluate(String expression) throws Exception {
        String response = call("Runtime.evaluate", "{\"expression\":" + quote(expression) + ",\"returnByValue\":true}");
        Matcher m = Pattern.compile("\"result\":\\{\"type\":\"string\",\"value\":\"").matcher(response);
        if (!m.find()) {
            throw new IllegalStateException("the script did not return a string: " + response);
        }
        return unquote(response, m.end());
    }

    List<Integer> nodes(String selector) throws Exception {
        String document = call("DOM.getDocument", "{\"depth\":-1}");
        Matcher root = Pattern.compile("\"root\":\\{\"nodeId\":(\\d+)").matcher(document);
        if (!root.find()) {
            throw new IllegalStateException("no document root: " + document);
        }
        String found = call("DOM.querySelectorAll", "{\"nodeId\":" + root.group(1) + ",\"selector\":" + quote(selector) + "}");
        Matcher ids = Pattern.compile("\"nodeIds\":\\[([^\\]]*)\\]").matcher(found);
        if (!ids.find()) {
            throw new IllegalStateException("no nodes: " + found);
        }
        return ids.group(1).isBlank() ? List.of() : Stream.of(ids.group(1).split(",")).map(s -> Integer.parseInt(s.trim())).toList();
    }

    void force(List<Integer> nodes, List<String> pseudo) throws Exception {
        String list = "[" + pseudo.stream().map(DevToolsSession::quote).collect(Collectors.joining(",")) + "]";
        for (int node : nodes) {
            call("CSS.forcePseudoState", "{\"nodeId\":" + node + ",\"forcedPseudoClasses\":" + list + "}");
        }
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    static String unquote(String json, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                char n = json.charAt(++i);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** How long, in total, the browser and all of its helper processes are given to exit after they are killed: one shared deadline, however many helpers there are. */
    private static final long EXIT_WAIT_SECONDS = 10;

    /** How many times the profile is swept: a helper process that is still exiting can write a file into a directory that was just emptied. */
    private static final int CLEANUP_ATTEMPTS = 3;

    @Override
    public void close() {
        try {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        } catch (RuntimeException ignored) {
            // The browser is being torn down anyway.
        }
        // Chrome's helper processes (renderers, GPU, crashpad) outlive the browser process and go on writing and deleting inside the profile. The tree is captured while
        // the parent is still alive, and only these handles (never a process found by name or by anything else) are terminated.
        List<ProcessHandle> tree = process.descendants().toList();
        process.destroyForcibly();
        tree.forEach(ProcessHandle::destroyForcibly);
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(EXIT_WAIT_SECONDS);
        try {
            process.waitFor(remaining(deadline), TimeUnit.NANOSECONDS);
            for (ProcessHandle helper : tree) {
                try {
                    helper.onExit().get(remaining(deadline), TimeUnit.NANOSECONDS);
                } catch (ExecutionException | TimeoutException notExited) {
                    // Bounded: once the shared deadline has passed a helper is only checked, not waited for (a zero wait still sees one that has exited).
                    // If one still holds files, the cleanup below reports it.
                }
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }
        List<IOException> failures = deleteTree(profile);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (!failures.isEmpty()) {
            // Thrown from close(), so when the measurement or an assertion has already failed, try-with-resources keeps that failure as the primary one and adds this as suppressed.
            throw cleanupFailure(profile, failures);
        }
    }

    /** The time left until {@code deadline}, never negative. */
    private static long remaining(long deadline) {
        return Math.max(0, deadline - System.nanoTime());
    }

    // ---- the throwaway profile's cleanup ----

    /** Deletes {@code root} and everything in it; returns the failures that were NOT an entry having already disappeared (an empty list is a complete cleanup). */
    static List<IOException> deleteTree(Path root) {
        return deleteTree(root, path -> { });
    }

    /**
     * As {@link #deleteTree(Path)}. {@code beforeDelete} is called with each entry just before its deletion is attempted: production passes nothing, and a test uses it to make
     * entries vanish or appear at exactly that moment. An entry that has disappeared (Chrome's helpers delete their own files) is expected and is not a failure; any other
     * failure is kept, the sweep goes on with the rest of the tree, and the whole sweep is repeated a bounded number of times before the failures are returned.
     */
    static List<IOException> deleteTree(Path root, Consumer<Path> beforeDelete) {
        List<IOException> failures = new ArrayList<>();
        for (int attempt = 1; attempt <= CLEANUP_ATTEMPTS; attempt++) {
            failures = sweep(root, beforeDelete);
            if (failures.isEmpty()) {
                return failures;
            }
            if (attempt < CLEANUP_ATTEMPTS) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return failures;
    }

    /** One pass over the tree, with no repetition: what it returns is exactly what that pass could not delete. */
    static List<IOException> sweep(Path root, Consumer<Path> beforeDelete) {
        List<IOException> failures = new ArrayList<>();
        try {
            // Links are not followed, so a symbolic link in the profile (Chrome's Singleton* ones) is removed itself and its target is left alone.
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException failure) {
                    if (!(failure instanceof NoSuchFileException)) {
                        failures.add(failure);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException failure) {
                    if (failure != null && !(failure instanceof NoSuchFileException)) {
                        failures.add(failure);
                    }
                    delete(directory);
                    return FileVisitResult.CONTINUE;
                }

                private void delete(Path path) {
                    beforeDelete.accept(path);
                    try {
                        Files.delete(path);
                    } catch (NoSuchFileException alreadyGone) {
                        // Expected: something else deleted it first.
                    } catch (IOException failure) {
                        failures.add(failure);
                    }
                }
            });
        } catch (IOException failure) {
            failures.add(failure);
        }
        return failures;
    }

    /** The exception for a cleanup that failed unexpectedly: it names the profile, says what the first failure was, and carries every failure as suppressed. */
    static IllegalStateException cleanupFailure(Path profile, List<IOException> failures) {
        IllegalStateException failure = new IllegalStateException("could not delete the throwaway Chrome profile " + profile + ": " + failures.size()
                + " unexpected failure(s), the first: " + failures.get(0));
        failures.forEach(failure::addSuppressed);
        return failure;
    }
}
