package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the controlled oracle's pages and measures them in ONE Chrome run: every rendering (a legacy and a migrated
 * one, in each stylesheet mode and configuration) is an iframe of one host page, and the shared probe reads them all.
 * One browser launch measures every frame, which is what keeps a suite's cost to a few seconds instead of a launch per
 * rendering.
 *
 * <p>The stylesheet modes a suite builds frames for:
 * <ul>
 *   <li><b>L</b>, legacy: the cumulative Tailwind oracle (the whole former showcase stylesheet plus every migrated
 *       component's legacy classes) with the legacy markup;</li>
 *   <li><b>P</b>, present: the application's real stylesheet (Tailwind and Ashlar together) with the migrated markup;</li>
 *   <li><b>R</b>, removed: Ashlar's generated stylesheet alone, plus the page-level declarations Tailwind used to give
 *       {@code html} ({@link PageEnvironment}), with the migrated markup;</li>
 *   <li><b>S</b>, scoped: the legacy markup given only the utilities and the few preflight rules it used, in that same
 *       unstyled page, so the surroundings are identical on both sides of a "Tailwind removed" comparison.</li>
 * </ul>
 */
final class ParityFrames {

    static final String ASHLAR = "/ashlar-real/ashlar.css";
    static final String APP = "/css/app.css";

    private ParityFrames() {
    }

    /** The path a compiled stylesheet in the generated directory is served at. */
    static String generated(String fileName) {
        return "/generated/" + fileName;
    }

    /** One frame: a complete document in the given configuration, linking {@code sheets}, with {@code extraCss} after them. */
    static String frame(String id, ParityConfigs.Config config, List<String> sheets, String extraCss, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        String skin = config.skin() == null ? "" : " data-skin=\"" + config.skin() + "\"";
        String doc = "<!doctype html><html data-theme=\"" + config.theme() + "\"" + skin + "><head>" + links + "<style>" + extraCss
                + "</style></head><body style=\"margin:0;padding:8px\">" + body + "</body></html>";
        return "<iframe data-id=\"" + id + "\" style=\"width:440px;height:2400px;border:0\" srcdoc=\""
                + doc.replace("&", "&amp;").replace("\"", "&quot;") + "\"></iframe>\n";
    }

    static List<String> with(String first, List<String> rest) {
        List<String> all = new ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    /**
     * What the page keeps when Tailwind is removed: the base font and line height Tailwind's stylesheet gave it, plus the
     * showcase's OWN dark colour scheme (a rule in its application CSS, not Tailwind's). Without it dark pages would draw
     * their default text in black, and that would be a difference in the test page, not in the component.
     */
    static String environment(Path generatedDir) throws IOException {
        return PageEnvironment.tailwindRemovedRules(generatedDir.resolve("phase-c-baseline.css")) + ":root[data-theme=\"dark\"]{color-scheme:dark}\n";
    }

    /** Measures every {@code data-parity} element of every frame in {@code frames} (each frame's data-id is its phase name). */
    static ParityProbe.Probe measure(ParitySession session, String frames, List<String> extraProps, String what) throws Exception {
        return measure(session, frames, extraProps, List.of(), what);
    }

    /**
     * As {@link #measure(ParitySession, String, List, String)}, with extra Chrome command-line flags: for example
     * {@code --force-prefers-reduced-motion}, which makes the whole run behave as if the user asked for reduced motion.
     */
    static ParityProbe.Probe measure(ParitySession session, String frames, List<String> extraProps, List<String> chromeFlags, String what) throws Exception {
        String probeJs;
        try (InputStream in = ParityFrames.class.getResourceAsStream("/cascade/parity-probe.js")) {
            if (in == null) {
                throw new IOException("missing /cascade/parity-probe.js");
            }
            probeJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String extra = "[" + String.join(",", extraProps.stream().map(p -> "\"" + p + "\"").toList()) + "]";
        String page = "<!doctype html><html><body style=\"margin:0\">\n" + frames + "<script>" + probeJs + "</script>\n<script>"
                + "addEventListener('load',function(){setTimeout(function(){var extra=" + extra + ";"
                + "var out=['props\\t'+window.__ashParity.propsWith(extra).join(',')];"
                + "[].forEach.call(document.querySelectorAll('iframe'),function(f){"
                + "window.__ashParity.measure(f.contentWindow,f.contentDocument,f.getAttribute('data-id'),extra).forEach(function(r){out.push(r)})});"
                + "var pre=document.createElement('pre');pre.id='ash-parity-frames';pre.textContent=out.join('\\n');document.body.appendChild(pre)},300)})"
                + "</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            List<String> flags = new ArrayList<>(List.of("--hide-scrollbars"));
            flags.addAll(chromeFlags);
            String dom = session.chrome().dumpDom(server.registerRaw(page), flags);
            return ParityProbe.parse(ParityProbe.lines(dom, "ash-parity-frames"), what);
        }
    }
}
