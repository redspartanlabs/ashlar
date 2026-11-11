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
import java.time.Duration;
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A minimal Chrome DevTools protocol client for the parity suites that must hold a state a measurement of a resting page cannot: it launches a headless
 * Chrome that is stated to be a device that can hover, navigates a page, forces pseudo-classes on real elements ({@code CSS.forcePseudoState}) and evaluates
 * a script. It was written for, and extracted unchanged from, {@code ButtonStateParityTest}; it adds nothing to the shared parity harness and changes no
 * normalization. Every failure to start or load is an infrastructure failure, never a parity result.
 *
 * <p>Starting a session is all-or-nothing: if any step of the startup fails, the browser process and everything it started are terminated and the throwaway
 * profile is removed before the failure is thrown, so a failed start leaves nothing behind. The failure says which phase failed, what went wrong, how long it took, how
 * many processes were terminated and whether the profile was removed.
 */
final class DevToolsSession implements AutoCloseable {

    /**
     * How long each startup step is given. The defaults are what the harness has always allowed (30 s for Chrome to publish its port, 30 s to open the socket) plus a
     * bound for the target lookup, which used to be unbounded: a request to Chrome's own loopback endpoint answers in milliseconds, so ten seconds is a hung endpoint, not a slow
     * one, and the lookup as a whole gets the same thirty seconds as the other steps.
     */
    record Timing(Duration portFile, Duration pageTarget, Duration listRequest, Duration webSocket) {
        static final Timing DEFAULT = new Timing(Duration.ofSeconds(30), Duration.ofSeconds(30), Duration.ofSeconds(10), Duration.ofSeconds(30));
    }

    /** Reads the file in which Chrome publishes its DevTools port. A seam for tests, which make the read fail the way a locked file does. */
    @FunctionalInterface
    interface PortReader {
        List<String> read(Path file) throws IOException;
    }

    /** How often the port file and the target list are polled. */
    private static final long POLL_MILLIS = 50;
    private static final long TARGET_POLL_MILLIS = 100;
    private static final int MAX_TARGET_ATTEMPTS = 100;

    private final Process process;
    private final Path profile;
    private final WebSocket socket;
    private final AtomicInteger ids = new AtomicInteger();
    private final Map<Integer, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<String> loads = new LinkedBlockingQueue<>();

    DevToolsSession(Path chrome) throws Exception {
        this(profile -> chromeCommand(chrome, profile), Timing.DEFAULT, Files::readAllLines);
    }

    /** As {@link #DevToolsSession(Path)}, for a browser started by {@code command} (given the profile directory it must use) and the given timings and port reader. */
    DevToolsSession(Function<Path, List<String>> command, Timing timing, PortReader portReader) throws Exception {
        Started started = start(command, timing, portReader, pending, loads);
        process = started.process();
        profile = started.profile();
        socket = started.socket();
        try {
            call("Page.enable", "{}");
            call("DOM.enable", "{}");
            call("CSS.enable", "{}");
        } catch (Throwable failure) {
            // The socket is open and the browser is running: this is a startup failure like any other, so the same cleanup applies.
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "startup failed");
            } catch (RuntimeException ignored) {
                // The browser is being torn down anyway.
            }
            throw startupFailure("enable", failure, started.began(), process, profile);
        }
    }

    private static List<String> chromeCommand(Path chrome, Path profile) {
        List<String> command = new ArrayList<>(List.of(chrome.toString(), "--headless=new", "--disable-gpu", "--no-first-run", "--no-default-browser-check",
                "--user-data-dir=" + profile, "--remote-debugging-port=0", "--remote-allow-origins=*", "--hide-scrollbars",
                "--blink-settings=primaryHoverType=2,availableHoverTypes=2", "about:blank"));
        if (System.getenv("CI") != null) {
            command.add("--no-sandbox");
        }
        return command;
    }

    private record Started(Process process, Path profile, WebSocket socket, long began) {
    }

    // ---- startup ----

    /** Launches the browser and brings it to an open DevTools socket; on any failure nothing it started is left running or on disk. */
    private static Started start(Function<Path, List<String>> command, Timing timing, PortReader portReader,
                                 Map<Integer, CompletableFuture<String>> pending, LinkedBlockingQueue<String> loads) throws Exception {
        long began = System.nanoTime();
        Path profile = Files.createTempDirectory("ashlar-state-chrome");
        Process process = null;
        String phase = "launch";
        try {
            process = new ProcessBuilder(command.apply(profile)).redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            phase = "port-file";
            int port = awaitPort(profile.resolve("DevToolsActivePort"), process::isAlive, timing.portFile(), portReader);
            phase = "page-target";
            HttpClient http = HttpClient.newHttpClient();
            String webSocketUrl = awaitPageTarget(http, port, timing.pageTarget(), timing.listRequest());
            phase = "websocket";
            WebSocket socket = connect(http, webSocketUrl, timing.webSocket(), pending, loads);
            return new Started(process, profile, socket, began);
        } catch (Throwable failure) {
            throw startupFailure(phase, failure, began, process, profile);
        }
    }

    /**
     * Waits for Chrome to publish its DevTools port: the first line of {@code DevToolsActivePort}, once the file has both its lines. Chrome writes that file itself, and while it
     * does (or while something else, such as a scanner, has it open) a read can fail with an I/O error that is gone a moment later; such a read is retried until the deadline, not
     * treated as the end of the startup. A failure that does not go away is still reported: when the deadline passes the error says how many reads failed and carries the last one.
     * The wait also ends as soon as the browser has exited, and a port file that cannot be a port fails at once.
     */
    static int awaitPort(Path portFile, BooleanSupplier browserAlive, Duration timeout, PortReader reader) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        IOException lastFailedRead = null;
        int failedReads = 0;
        while (true) {
            try {
                if (Files.isRegularFile(portFile)) {
                    List<String> lines = reader.read(portFile);
                    if (lines.size() >= 2) {
                        return Integer.parseInt(lines.get(0).trim());
                    }
                }
            } catch (IOException transientFailure) {
                lastFailedRead = transientFailure;
                failedReads++;
            } catch (NumberFormatException permanent) {
                throw new IllegalStateException("the DevTools port file does not start with a port number", permanent);
            }
            if (!browserAlive.getAsBoolean()) {
                throw new IllegalStateException("Chrome exited before it published its DevTools port" + readFailures(failedReads), lastFailedRead);
            }
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("Chrome did not publish its DevTools port within " + timeout.toMillis() + " ms" + readFailures(failedReads), lastFailedRead);
            }
            Thread.sleep(POLL_MILLIS);
        }
    }

    private static String readFailures(int failedReads) {
        return failedReads == 0 ? "" : " (" + failedReads + " read(s) of the port file failed; the last is the cause)";
    }

    /**
     * Looks up the page target on Chrome's DevTools endpoint and returns its socket URL. Each request is bounded, and bounded by what is left of {@code timeout}, so a hung
     * endpoint ends the lookup at the deadline instead of holding it for good; a request that fails or times out is tried again until then.
     */
    static String awaitPageTarget(HttpClient http, int port, Duration timeout, Duration requestTimeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        IOException lastFailure = null;
        for (int attempt = 0; attempt < MAX_TARGET_ATTEMPTS; attempt++) {
            long left = deadline - System.nanoTime();
            if (left <= 0) {
                break;
            }
            Duration bound = Duration.ofNanos(Math.min(requestTimeout.toNanos(), left));
            try {
                String list = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/json/list")).timeout(bound).build(), HttpResponse.BodyHandlers.ofString()).body();
                Matcher m = Pattern.compile("\"type\":\\s*\"page\"[^}]*?\"webSocketDebuggerUrl\":\\s*\"([^\"]+)\"|\"webSocketDebuggerUrl\":\\s*\"([^\"]+)\"[^}]*?\"type\":\\s*\"page\"").matcher(list);
                if (m.find()) {
                    return m.group(1) != null ? m.group(1) : m.group(2);
                }
                lastFailure = null;
            } catch (IOException failed) {
                lastFailure = failed;
            }
            Thread.sleep(TARGET_POLL_MILLIS);
        }
        throw new IllegalStateException("Chrome has no page target within " + timeout.toMillis() + " ms (each lookup is bounded to " + requestTimeout.toMillis() + " ms)", lastFailure);
    }

    private static WebSocket connect(HttpClient http, String url, Duration timeout, Map<Integer, CompletableFuture<String>> pending,
                                     LinkedBlockingQueue<String> loads) throws Exception {
        CompletableFuture<WebSocket> opening = http.newWebSocketBuilder().buildAsync(URI.create(url), listener(pending, loads));
        try {
            return opening.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException e) {
            opening.cancel(true);
            throw e;
        }
    }

    private static WebSocket.Listener listener(Map<Integer, CompletableFuture<String>> pending, LinkedBlockingQueue<String> loads) {
        return new WebSocket.Listener() {
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
        };
    }

    /**
     * Ends a failed startup: terminates the browser and everything it started, removes the profile, and builds the exception to throw. Its message names the phase, the
     * failure, the elapsed time, the number of processes terminated and whether the profile was removed; paths are left out of it (the cause carries the original failure).
     */
    private static IllegalStateException startupFailure(String phase, Throwable failure, long began, Process process, Path profile) {
        return startupFailure(phase, failure, began, profile, process == null ? null : () -> terminate(process), () -> deleteTree(profile));
    }

    /**
     * As above, with the two cleanup steps given (the terminator is null when no browser was launched). They are attempted independently, in this order: a failure of the first
     * does not stop the second. Whatever they throw never replaces {@code failure}, which stays the cause of the result; it is added to the result as suppressed, and the message
     * says that the step failed. An interrupt that arrived before the cleanup is cleared while it runs (it would cut the waits short) and always restored afterwards; one that arrives during it simply stays set.
     */
    static IllegalStateException startupFailure(String phase, Throwable failure, long began, Path profile, Supplier<Termination> terminator, Supplier<List<IOException>> sweeper) {
        boolean interrupted = failure instanceof InterruptedException || Thread.interrupted();
        List<RuntimeException> unexpected = new ArrayList<>();
        String terminated = "terminated 0 browser process(es)";
        if (terminator != null) {
            try {
                Termination termination = terminator.get();
                terminated = "terminated " + termination.processes() + " browser process(es)";
                interrupted |= termination.interrupted();
            } catch (RuntimeException cleanupFailure) {
                terminated = "termination FAILED (" + cleanupFailure.getClass().getSimpleName() + ")";
                unexpected.add(cleanupFailure);
            }
        }
        List<IOException> cleanup = List.of();
        String removed;
        try {
            cleanup = sweeper.get();
            removed = cleanup.isEmpty() ? "removed" : "NOT fully removed (" + cleanup.size() + " failure(s))";
        } catch (RuntimeException cleanupFailure) {
            removed = "cleanup FAILED (" + cleanupFailure.getClass().getSimpleName() + ")";
            unexpected.add(cleanupFailure);
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began);
        IllegalStateException result = new IllegalStateException("Chrome startup failed in phase '" + phase + "' after " + elapsed + " ms: " + failure.getClass().getSimpleName() + ": "
                + scrub(String.valueOf(failure.getMessage()), profile) + "; " + terminated + "; profile " + removed + " - infrastructure failure, not a parity result", failure);
        cleanup.forEach(result::addSuppressed);
        unexpected.forEach(result::addSuppressed);
        return result;
    }

    /** {@code message} without the profile's path or its parent directory's. */
    private static String scrub(String message, Path profile) {
        String scrubbed = message.replace(profile.toString(), "<profile>");
        Path parent = profile.getParent();
        return parent == null ? scrubbed : scrubbed.replace(parent.toString(), "<temp>");
    }

    // ---- a live session ----

    /** The throwaway profile directory (for tests, which check that it is removed). */
    Path profile() {
        return profile;
    }

    /** The browser process (for tests, which check that it and what it started are gone). */
    ProcessHandle browser() {
        return process.toHandle();
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
        Termination termination = terminate(process);
        List<IOException> failures = deleteTree(profile);
        if (termination.interrupted()) {
            Thread.currentThread().interrupt();
        }
        if (!failures.isEmpty()) {
            // Thrown from close(), so when the measurement or an assertion has already failed, try-with-resources keeps that failure as the primary one and adds this as suppressed.
            throw cleanupFailure(profile, failures);
        }
    }

    /** What {@link #terminate(Process)} did: how many processes it was asked to kill, and whether the thread was interrupted while it waited for them to exit. */
    record Termination(int processes, boolean interrupted) {
    }

    /**
     * Kills a browser process and everything it started, and waits a bounded time for them to exit. Chrome's helper processes (renderers, GPU, crashpad) outlive the browser
     * process and go on writing and deleting inside the profile. The tree is captured while the parent is still alive, and only these handles (never a process found by name
     * or by anything else) are terminated. An interrupt is not acted on here (the wait ends and the cleanup that follows still runs); the caller restores it.
     */
    static Termination terminate(Process process) {
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
                    // If one still holds files, the cleanup that follows reports it.
                }
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }
        return new Termination(tree.size() + 1, interrupted);
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
