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
 *   <li>{@code /page/N} - the ladder fixture with a scenario's links and attributes;</li>
 *   <li>{@code /ashlar/js/*} - Ashlar's REAL scripts, from the library on the test classpath, at the path the showcase serves them from, so an ES module's
 *       relative import (overlay.js, imported by modal.js and drawer.js) resolves exactly as it does in the application;</li>
 *   <li>{@code /scripts/N/*} - a registered set of script files (a legacy copy, or a deliberately broken variant of the real scripts), served side by side
 *       so their relative imports resolve within the set (a set registered over Ashlar's scripts serves the real script for any name it does not hold).</li>
 * </ul>
 */
final class CascadeFixtureServer implements AutoCloseable {

    private final HttpServer server;
    private final Path generatedDir;
    private final String template;
    private final Map<Integer, String> pages = new ConcurrentHashMap<>();
    private final AtomicInteger nextPage = new AtomicInteger();
    private final Map<Integer, Map<String, String>> scriptSets = new ConcurrentHashMap<>();
    private final AtomicInteger nextScriptSet = new AtomicInteger();
    private final java.util.Set<Integer> overAshlar = ConcurrentHashMap.newKeySet();

    CascadeFixtureServer(Path generatedDir) throws IOException {
        this.generatedDir = generatedDir;
        this.template = new String(resource("/cascade/ladder.html"), StandardCharsets.UTF_8);
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/css/", ex -> serveResource(ex, "/css/", "/static/css/"));
        server.createContext("/cascade/", ex -> serveResource(ex, "/cascade/", "/cascade/"));
        server.createContext("/generated/", this::serveGenerated);
        server.createContext("/ashlar-real/", this::serveRealAshlarCss);
        server.createContext("/page/", this::servePage);
        server.createContext("/ashlar/js/", this::serveAshlarScript);
        server.createContext("/scripts/", this::serveScriptSet);
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

    /**
     * Registers a set of script files (name to source) and returns the URL of the folder they are served from, ending in "/". A module in the set imports
     * another one of the set with a relative specifier ("./overlay.js").
     */
    String registerScripts(Map<String, String> files) {
        int id = nextScriptSet.incrementAndGet();
        scriptSets.put(id, Map.copyOf(files));
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/scripts/" + id + "/";
    }

    /**
     * As {@link #registerScripts}, but every name the set does not hold is served from Ashlar's REAL scripts: a variant of a few scripts, on a page that also loads other
     * components (a Popover inside a Modal, say), whose own scripts must be there for the page to work at all.
     */
    String registerScriptsOverAshlar(Map<String, String> overrides) {
        int id = nextScriptSet.incrementAndGet();
        scriptSets.put(id, Map.copyOf(overrides));
        overAshlar.add(id);
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/scripts/" + id + "/";
    }

    /** The URL a page uses for Ashlar's real script {@code name}, as the showcase serves it. */
    String ashlarScript(String name) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/ashlar/js/" + name;
    }

    private void serveAshlarScript(HttpExchange ex) throws IOException {
        String name = ex.getRequestURI().getPath().substring("/ashlar/js/".length());
        if (!simpleName(name) || !name.endsWith(".js")) {
            respond(ex, 404, "text/plain", new byte[0]);
            return;
        }
        try {
            respond(ex, 200, "text/javascript; charset=utf-8", resource("/META-INF/resources/ashlar/js/" + name));
        } catch (IOException missing) {
            respond(ex, 404, "text/plain", new byte[0]);
        }
    }

    private void serveScriptSet(HttpExchange ex) throws IOException {
        String[] parts = ex.getRequestURI().getPath().substring("/scripts/".length()).split("/", 2);
        Map<String, String> set = parts.length == 2 && parts[0].matches("[0-9]+") ? scriptSets.get(Integer.parseInt(parts[0])) : null;
        String source = set == null || !simpleName(parts[1]) ? null : set.get(parts[1]);
        if (source == null && set != null && simpleName(parts[1]) && parts[1].endsWith(".js") && overAshlar.contains(Integer.parseInt(parts[0]))) {
            try {
                respond(ex, 200, "text/javascript; charset=utf-8", resource("/META-INF/resources/ashlar/js/" + parts[1]));
            } catch (IOException missing) {
                respond(ex, 404, "text/plain", new byte[0]);
            }
            return;
        }
        if (source == null) {
            respond(ex, 404, "text/plain", new byte[0]);
            return;
        }
        respond(ex, 200, "text/javascript; charset=utf-8", source.getBytes(StandardCharsets.UTF_8));
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
