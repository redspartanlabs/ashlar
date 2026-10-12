package dev.redspartanlabs.ashlar.showcase.cascade;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A same-origin reverse proxy in front of the real running showcase, so the real pages, real stylesheets
 * and real scripts load in a real browser while a measurement script is injected - without editing any
 * page template and without a browser-automation dependency.
 *
 * <p>Per load it can: force the stored theme before the page's own bootstrap script runs; serve a
 * different stylesheet in place of the application's own {@code /ashlar/css/app.css} (a baseline build, a
 * mutated one, or Ashlar's stylesheet alone); serve extra virtual files and link them into the page; rewrite
 * the page's HTML (to render a migrated component with its legacy markup, in the same run); and choose which
 * probe script to inject. Loads are sequential per proxy, so one mutable configuration is enough; use one
 * proxy per thread to load in parallel.
 */
final class RealPageProxy implements AutoCloseable {

    private static final String APP_CSS_PATH = "/ashlar/css/app.css";
    private static final String DEFAULT_PROBE = "/cascade/real-page-probe.js";

    /**
     * What to do for the next load. {@code cssOverride} may be null (serve the application's own stylesheet).
     * {@code probeConfig} is a JavaScript expression placed in {@code window.__ASH_PROBE.config}.
     * {@code virtualResources} maps a request path to bytes served directly (a CSS file). {@code extraLinks}
     * are stylesheet hrefs linked at the end of the head. {@code htmlRewrite} may be null.
     */
    record Load(String theme, String mode, byte[] cssOverride, String probe, String probeConfig,
                List<String> extraLinks, Map<String, byte[]> virtualResources, Function<String, String> htmlRewrite) {

        Load(String theme, String mode, byte[] cssOverride) {
            this(theme, mode, cssOverride, DEFAULT_PROBE, "null", List.of(), Map.of(), null);
        }
    }

    private final HttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();
    private final int appPort;
    private final Map<String, String> probeScripts = new ConcurrentHashMap<>();
    private volatile Load current = new Load("light", "snapshot", null);

    RealPageProxy(int appPort) throws IOException {
        this.appPort = appPort;
        probeScript(DEFAULT_PROBE);
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    private String probeScript(String resource) throws IOException {
        String cached = probeScripts.get(resource);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = RealPageProxy.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("missing " + resource);
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            probeScripts.put(resource, script);
            return script;
        }
    }

    void configure(Load load) {
        this.current = load;
    }

    String url(String pathAndQuery) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + pathAndQuery;
    }

    private void handle(HttpExchange ex) throws IOException {
        Load load = current;
        String path = ex.getRequestURI().getPath();
        String pathAndQuery = ex.getRequestURI().getRawPath()
                + (ex.getRequestURI().getRawQuery() == null ? "" : "?" + ex.getRequestURI().getRawQuery());
        try {
            if (load.cssOverride() != null && path.equals(APP_CSS_PATH)) {
                send(ex, 200, "text/css; charset=utf-8", load.cssOverride());
                return;
            }
            byte[] virtual = load.virtualResources().get(path);
            if (virtual != null) {
                send(ex, 200, "text/css; charset=utf-8", virtual);
                return;
            }
            HttpResponse<byte[]> upstream = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + appPort + pathAndQuery)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            String type = upstream.headers().firstValue("Content-Type").orElse("application/octet-stream");
            byte[] body = upstream.body();
            if (type.startsWith("text/html")) {
                String html = new String(body, StandardCharsets.UTF_8);
                if (load.htmlRewrite() != null) {
                    html = load.htmlRewrite().apply(html);
                }
                body = inject(html, load).getBytes(StandardCharsets.UTF_8);
            }
            send(ex, upstream.statusCode(), type, body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            send(ex, 502, "text/plain", new byte[0]);
        }
    }

    private String inject(String html, Load load) throws IOException {
        // Before the page's own bootstrap script, so it finds the stored theme it expects.
        String themeScript = "<script>try{localStorage.setItem(\"theme\",\"" + load.theme() + "\")}catch(e){}</script>";
        String probe = "<script>window.__ASH_PROBE={mode:\"" + load.mode() + "\",config:" + load.probeConfig() + "};</script><script>"
                + probeScript(load.probe()) + "</script>";
        int head = html.indexOf("<head>");
        if (head >= 0) {
            html = html.substring(0, head + 6) + themeScript + html.substring(head + 6);
        }
        if (!load.extraLinks().isEmpty()) {
            StringBuilder links = new StringBuilder();
            load.extraLinks().forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
            int headEnd = html.indexOf("</head>");
            if (headEnd >= 0) {
                html = html.substring(0, headEnd) + links + html.substring(headEnd);
            }
        }
        int bodyEnd = html.lastIndexOf("</body>");
        return bodyEnd >= 0 ? html.substring(0, bodyEnd) + probe + html.substring(bodyEnd) : html + probe;
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            ex.getResponseBody().write(body);
        }
        ex.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
