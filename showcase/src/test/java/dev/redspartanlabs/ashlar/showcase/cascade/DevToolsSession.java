package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Override
    public void close() {
        try {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        } catch (RuntimeException ignored) {
            // The browser is being torn down anyway.
        }
        process.destroyForcibly();
        try {
            process.waitFor(10, TimeUnit.SECONDS);
            try (Stream<Path> walk = Files.walk(profile)) {
                walk.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        } catch (IOException | InterruptedException ignored) {
            // Best-effort cleanup of the throwaway profile.
        }
    }
}
