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

/**
 * A same-origin reverse proxy in front of the real running showcase, so the
 * real pages, real stylesheets and real scripts load in a real browser while a
 * measurement script is injected - without editing any page template and
 * without a browser-automation dependency.
 *
 * <p>Per load it can: force the stored theme before the page's own bootstrap
 * script runs; serve a different stylesheet in place of the application's
 * {@code /ashlar/css/app.css} (the baseline build, or a deliberately mutated
 * one); and inject the probe. Loads are sequential, so one mutable
 * configuration is enough.
 */
final class RealPageProxy implements AutoCloseable {

    private static final String APP_CSS_PATH = "/ashlar/css/app.css";

    /** What to do for the next load. {@code cssOverride} may be null (serve the application's own stylesheet). */
    record Load(String theme, String mode, byte[] cssOverride) {
    }

    private final HttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();
    private final int appPort;
    private final String probeScript;
    private volatile Load current = new Load("light", "snapshot", null);

    RealPageProxy(int appPort) throws IOException {
        this.appPort = appPort;
        try (InputStream in = RealPageProxy.class.getResourceAsStream("/cascade/real-page-probe.js")) {
            if (in == null) {
                throw new IOException("missing /cascade/real-page-probe.js");
            }
            this.probeScript = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    void configure(Load load) {
        this.current = load;
    }

    String url(String pathAndQuery) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + pathAndQuery;
    }

    private void handle(HttpExchange ex) throws IOException {
        Load load = current;
        String pathAndQuery = ex.getRequestURI().getRawPath()
                + (ex.getRequestURI().getRawQuery() == null ? "" : "?" + ex.getRequestURI().getRawQuery());
        try {
            if (load.cssOverride() != null && ex.getRequestURI().getPath().equals(APP_CSS_PATH)) {
                send(ex, 200, "text/css; charset=utf-8", load.cssOverride());
                return;
            }
            HttpResponse<byte[]> upstream = client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + appPort + pathAndQuery)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            String type = upstream.headers().firstValue("Content-Type").orElse("application/octet-stream");
            byte[] body = upstream.body();
            if (type.startsWith("text/html")) {
                body = inject(new String(body, StandardCharsets.UTF_8), load).getBytes(StandardCharsets.UTF_8);
            }
            send(ex, upstream.statusCode(), type, body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            send(ex, 502, "text/plain", new byte[0]);
        }
    }

    private String inject(String html, Load load) {
        // Before the page's own bootstrap script, so it finds the stored theme it expects.
        String themeScript = "<script>try{localStorage.setItem(\"theme\",\"" + load.theme() + "\")}catch(e){}</script>";
        String probe = "<script>window.__ASH_PROBE={mode:\"" + load.mode() + "\"};</script><script>" + probeScript + "</script>";
        int head = html.indexOf("<head>");
        if (head >= 0) {
            html = html.substring(0, head + 6) + themeScript + html.substring(head + 6);
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
