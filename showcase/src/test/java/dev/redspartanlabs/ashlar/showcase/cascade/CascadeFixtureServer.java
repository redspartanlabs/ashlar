package dev.redspartanlabs.ashlar.showcase.cascade;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A loopback-only HTTP server that serves the real compiled stylesheets to a
 * real browser:
 * <ul>
 *   <li>{@code /css/*} - the showcase's real built resources ({@code app.css}, skins);</li>
 *   <li>{@code /generated/*} - the Path A builds compiled by the real Tailwind CLI;</li>
 *   <li>{@code /cascade/*} - the test-only probe stylesheets;</li>
 *   <li>{@code /page/N} - the ladder fixture with a scenario's links and attributes.</li>
 * </ul>
 */
final class CascadeFixtureServer implements AutoCloseable {

    private final HttpServer server;
    private final Path generatedDir;
    private final String template;
    private final Map<Integer, String> pages = new ConcurrentHashMap<>();
    private final AtomicInteger nextPage = new AtomicInteger();

    CascadeFixtureServer(Path generatedDir) throws IOException {
        this.generatedDir = generatedDir;
        this.template = new String(resource("/cascade/ladder.html"), StandardCharsets.UTF_8);
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/css/", ex -> serveResource(ex, "/css/", "/static/css/"));
        server.createContext("/cascade/", ex -> serveResource(ex, "/cascade/", "/cascade/"));
        server.createContext("/generated/", this::serveGenerated);
        server.createContext("/ashlar-real/", this::serveRealAshlarCss);
        server.createContext("/page/", this::servePage);
        server.start();
    }

    /** Registers a fixture page and returns its URL. */
    String register(List<String> hrefs, Map<String, String> htmlAttributes) {
        StringBuilder links = new StringBuilder();
        for (String href : hrefs) {
            links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">\n");
        }
        StringBuilder attrs = new StringBuilder();
        htmlAttributes.forEach((k, v) -> attrs.append(' ').append(k).append("=\"")
                .append(v.replace("&", "&amp;").replace("\"", "&quot;")).append('"'));
        String html = template.replace("<!--LINKS-->", links).replace("<!--HTML_ATTRS-->", attrs);
        int id = nextPage.incrementAndGet();
        pages.put(id, html);
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/page/" + id;
    }

    /** Registers a complete HTML document as a page and returns its URL. */
    String registerRaw(String html) {
        int id = nextPage.incrementAndGet();
        pages.put(id, html);
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/page/" + id;
    }

    /** Serves the REAL generated ashlar.css (the file packaged into the jar) at {@code /ashlar-real/ashlar.css}. */
    private void serveRealAshlarCss(HttpExchange ex) throws IOException {
        String generated = System.getProperty("ashlar.css.generated");
        Path file = generated == null ? null : Path.of(generated);
        if (file == null || !ex.getRequestURI().getPath().equals("/ashlar-real/ashlar.css") || !Files.isRegularFile(file)) {
            respond(ex, 404, "text/plain", new byte[0]);
            return;
        }
        respond(ex, 200, "text/css; charset=utf-8", Files.readAllBytes(file));
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream in = CascadeFixtureServer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing test resource " + path);
            }
            return in.readAllBytes();
        }
    }

    private static boolean simpleName(String name) {
        return !name.isEmpty() && !name.contains("/") && !name.contains("\\") && !name.contains("..");
    }

    private void serveResource(HttpExchange ex, String urlPrefix, String classpathPrefix) throws IOException {
        String name = ex.getRequestURI().getPath().substring(urlPrefix.length());
        if (!simpleName(name)) {
            respond(ex, 400, "text/plain", new byte[0]);
            return;
        }
        try {
            respond(ex, 200, "text/css; charset=utf-8", resource(classpathPrefix + name));
        } catch (IOException missing) {
            respond(ex, 404, "text/plain", new byte[0]);
        }
    }

    private void serveGenerated(HttpExchange ex) throws IOException {
        String name = ex.getRequestURI().getPath().substring("/generated/".length());
        Path file = generatedDir.resolve(name);
        if (!simpleName(name) || !Files.isRegularFile(file)) {
            respond(ex, 404, "text/plain", new byte[0]);
            return;
        }
        respond(ex, 200, "text/css; charset=utf-8", Files.readAllBytes(file));
    }

    private void servePage(HttpExchange ex) throws IOException {
        String id = ex.getRequestURI().getPath().substring("/page/".length());
        String html = id.matches("\\d+") ? pages.get(Integer.parseInt(id)) : null;
        if (html == null) {
            respond(ex, 404, "text/plain", new byte[0]);
            return;
        }
        respond(ex, 200, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange ex, int status, String type, byte[] body) throws IOException {
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
