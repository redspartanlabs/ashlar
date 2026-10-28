package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the controlled oracle's pages and measures them in Chrome: every rendering (a legacy and a migrated
 * one, in each stylesheet mode and configuration) is an iframe of one host page, and the shared probe reads them all.
 * One browser launch measures every frame of a page, which is what keeps a suite's cost to a few seconds instead of a launch
 * per rendering. A collection large enough to put too many stylesheet requests on one page is measured in several pages
 * (see {@link #MAX_STYLESHEET_REQUESTS_PER_PAGE}), and every page first proves that every stylesheet of every frame loaded.
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

    /**
     * The most stylesheet requests one Chrome page is asked to make. Every frame links the same handful of stylesheets (an oracle or
     * application stylesheet plus the ten skins), so a suite of N frames puts N times that many requests on one page, all against the
     * fixture server, which answers one request at a time and sends no cache headers. A page of about 1,600 requests (the Stepper
     * suite's 148 frames x 11 stylesheets) was seen, on one CPU, to lose a few of them: Chrome cancelled the stylesheet request before
     * it reached the server, the frame finished loading unstyled, and the probe measured an unstyled page as if it were a result.
     * A page is therefore capped at a few hundred requests, and a larger collection is measured in several pages. The cap lowers the
     * odds, it does not prove them away; what makes a loss visible is {@link #verifySheets}. Settable with
     * {@code -Dashlar.parity.maxSheetRequests} for experiments.
     */
    static final int MAX_STYLESHEET_REQUESTS_PER_PAGE = Integer.getInteger("ashlar.parity.maxSheetRequests", 250);

    private static final String FRAME_END = "</iframe>\n";
    private static final Pattern FRAME_ID = Pattern.compile("^<iframe data-id=\"([^\"]*)\"");
    private static final Pattern STYLESHEET_LINK = Pattern.compile("<link rel=\"stylesheet\" href=\"([^\"]*)\">");

    private ParityFrames() {
    }

    /** The stylesheets a frame was built to load: its id and the {@code href}s of its stylesheet links, in order. */
    record ExpectedFrame(String id, List<String> hrefs) {
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

    /** Splits a frame collection into its frames (each exactly as {@link #frame} wrote it), in order. */
    static List<String> frameList(String frames) {
        List<String> list = new ArrayList<>();
        int from = 0;
        for (int end = frames.indexOf(FRAME_END, from); end >= 0; end = frames.indexOf(FRAME_END, from)) {
            list.add(frames.substring(from, end + FRAME_END.length()));
            from = end + FRAME_END.length();
        }
        if (!frames.substring(from).isBlank()) {
            list.add(frames.substring(from));
        }
        return list;
    }

    /** The frame's linked stylesheets, read from the head of its document (before its inline style). */
    static ExpectedFrame expected(String frame) {
        Matcher id = FRAME_ID.matcher(frame);
        if (!id.find()) {
            throw new IllegalArgumentException("not a frame written by ParityFrames.frame: " + frame.substring(0, Math.min(80, frame.length())));
        }
        String doc = frame.replace("&quot;", "\"").replace("&amp;", "&");
        int head = doc.indexOf("<head>");
        int style = doc.indexOf("<style>");
        List<String> hrefs = new ArrayList<>();
        if (head >= 0 && style > head) {
            Matcher m = STYLESHEET_LINK.matcher(doc.substring(head, style));
            while (m.find()) {
                hrefs.add(m.group(1));
            }
        }
        return new ExpectedFrame(id.group(1), hrefs);
    }

    /**
     * Divides {@code frames} into consecutive batches of whole frames, each making at most {@code maxRequests} stylesheet requests
     * (a single frame that alone exceeds the cap gets a batch of its own). Order is preserved: concatenating the batches gives
     * {@code frames} back.
     */
    static List<String> batches(String frames, int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("maxRequests must be positive: " + maxRequests);
        }
        List<String> batches = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int requests = 0;
        for (String frame : frameList(frames)) {
            int own = expected(frame).hrefs().size();
            if (current.length() > 0 && requests + own > maxRequests) {
                batches.add(current.toString());
                current.setLength(0);
                requests = 0;
            }
            current.append(frame);
            requests += own;
        }
        if (current.length() > 0) {
            batches.add(current.toString());
        }
        return batches;
    }

    /**
     * Checks the page's report of which stylesheets each frame ended up with against the ones the frame was built to load. Anything
     * short of every stylesheet of every frame is an INFRASTRUCTURE failure naming the frame and the stylesheet: a page measured
     * without its CSS is not a parity result, and must never be compared as one.
     *
     * <p>Report lines: {@code sheets<TAB>frame count}, then {@code sheet<TAB>index<TAB>frame id<TAB>href<TAB>ok|missing}.
     */
    static void verifySheets(List<ExpectedFrame> expected, List<String> report, String what) {
        List<String> problems = new ArrayList<>();
        int reportedFrames = -1;
        List<List<String[]>> byFrame = new ArrayList<>();
        for (int i = 0; i < expected.size(); i++) {
            byFrame.add(new ArrayList<>());
        }
        for (String line : report) {
            String[] f = line.split("\t", -1);
            if (f[0].equals("sheets") && f.length == 2) {
                reportedFrames = Integer.parseInt(f[1]);
            } else if (f[0].equals("sheet") && f.length == 5) {
                int index = Integer.parseInt(f[1]);
                if (index < 0 || index >= expected.size()) {
                    problems.add("a stylesheet was reported for frame #" + index + " (" + f[2] + "), which the page was not built to contain");
                } else {
                    byFrame.get(index).add(f);
                }
            } else {
                throw new IllegalStateException(what + ": unrecognised stylesheet report line: " + line);
            }
        }
        if (reportedFrames != expected.size()) {
            problems.add("the page holds " + reportedFrames + " frames, expected " + expected.size());
        }
        for (int i = 0; i < expected.size(); i++) {
            ExpectedFrame frame = expected.get(i);
            List<String[]> got = byFrame.get(i);
            List<String> gotHrefs = got.stream().map(g -> g[3]).toList();
            for (String href : frame.hrefs()) {
                if (!gotHrefs.contains(href)) {
                    problems.add("frame " + frame.id() + ": stylesheet " + href + " is not linked in the loaded document");
                }
            }
            for (String[] g : got) {
                if (!g[4].equals("ok")) {
                    problems.add("frame " + frame.id() + ": stylesheet " + g[3] + " did not load (" + g[4] + ")");
                }
            }
        }
        if (!problems.isEmpty()) {
            int shown = Math.min(problems.size(), 20);
            throw new IllegalStateException(what + ": INFRASTRUCTURE FAILURE, not a parity result - " + problems.size()
                    + " stylesheet(s) missing before measuring:\n  " + String.join("\n  ", problems.subList(0, shown))
                    + (problems.size() > shown ? "\n  ... and " + (problems.size() - shown) + " more" : ""));
        }
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
        return measure(session, frames, extraProps, chromeFlags, what, MAX_STYLESHEET_REQUESTS_PER_PAGE);
    }

    /**
     * As above, making at most {@code maxRequests} stylesheet requests per Chrome page. A collection within the cap is one page,
     * exactly as before; a larger one is several, whose rows are joined in frame order. Each frame is measured on its own, so which
     * page it was on cannot change what is measured. Before any frame is measured, the page reports whether every stylesheet of every
     * frame loaded, and a frame that is missing one fails the run (see {@link #verifySheets}).
     */
    static ParityProbe.Probe measure(ParitySession session, String frames, List<String> extraProps, List<String> chromeFlags, String what, int maxRequests) throws Exception {
        String probeJs;
        try (InputStream in = ParityFrames.class.getResourceAsStream("/cascade/parity-probe.js")) {
            if (in == null) {
                throw new IOException("missing /cascade/parity-probe.js");
            }
            probeJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String extra = "[" + String.join(",", extraProps.stream().map(p -> "\"" + p + "\"").toList()) + "]";
        List<String> pages = batches(frames, maxRequests);
        List<String> props = null;
        List<ParityProbe.Row> rows = new ArrayList<>();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            List<String> flags = new ArrayList<>(List.of("--hide-scrollbars"));
            flags.addAll(chromeFlags);
            for (int i = 0; i < pages.size(); i++) {
                String batch = pages.get(i);
                String label = pages.size() == 1 ? what : what + " (page " + (i + 1) + " of " + pages.size() + ")";
                String dom = session.chrome().dumpDom(server.registerRaw(page(batch, probeJs, extra)), flags);
                verifySheets(frameList(batch).stream().map(ParityFrames::expected).toList(), ParityProbe.lines(dom, "ash-parity-sheets"), label);
                ParityProbe.Probe probe = ParityProbe.parse(ParityProbe.lines(dom, "ash-parity-frames"), label);
                if (props == null) {
                    props = probe.props();
                } else if (!props.equals(probe.props())) {
                    throw new IllegalStateException(label + ": the pages of one run measured different properties");
                }
                rows.addAll(probe.rows());
            }
        }
        if (props == null) {
            throw new IllegalStateException(what + ": no frames to measure - infrastructure failure, not a pass");
        }
        return new ParityProbe.Probe(props, rows);
    }

    /** One host page: the frames, the shared probe, and the script that reports the stylesheets, then (only if all loaded) the rows. */
    private static String page(String frames, String probeJs, String extra) {
        return "<!doctype html><html><body style=\"margin:0\">\n" + frames + "<script>" + probeJs + "</script>\n<script>"
                + "addEventListener('load',function(){setTimeout(function(){var extra=" + extra + ";"
                + "var all=[].slice.call(document.querySelectorAll('iframe'));"
                + "var sheets=['sheets\\t'+all.length];var missing=0;"
                + "all.forEach(function(f,i){var seen={};f.contentWindow.performance.getEntriesByType('resource').forEach(function(e){seen[e.name]=e.responseStatus});"
                + "[].forEach.call(f.contentDocument.querySelectorAll('link[rel~=stylesheet]'),function(l){"
                + "var st=!l.sheet?'missing':!seen[l.href]?'no-response':seen[l.href]===200?'ok':'http-'+seen[l.href];"
                + "if(st!=='ok'){missing++}sheets.push(['sheet',i,f.getAttribute('data-id'),l.getAttribute('href'),st].join('\\t'))})});"
                + "var sp=document.createElement('pre');sp.id='ash-parity-sheets';sp.textContent=sheets.join('\\n');document.body.appendChild(sp);"
                + "if(missing){return}"
                + "var out=['props\\t'+window.__ashParity.propsWith(extra).join(',')];"
                + "all.forEach(function(f){"
                + "window.__ashParity.measure(f.contentWindow,f.contentDocument,f.getAttribute('data-id'),extra).forEach(function(r){out.push(r)})});"
                + "var pre=document.createElement('pre');pre.id='ash-parity-frames';pre.textContent=out.join('\\n');document.body.appendChild(pre)},300)})"
                + "</script></body></html>";
    }
}
