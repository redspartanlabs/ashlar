package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour parity of the Text Entry family: the SAME sequence of trusted browser input is run against (a) the legacy Tailwind markup with the ORIGINAL scripts (the test fixtures
 * {@code search-input-legacy.js} and {@code textarea-legacy.js}, verbatim copies of the scripts at commit 831a2f5; {@code text-input.js} was never changed) and (b) the migrated markup
 * with the migrated scripts, and the observable result must be identical after every step: which element has focus, every value, whether the clear button is displayed, whether it
 * matches {@code :focus-visible}, the character count's text and colour, which fields match {@code :user-invalid} and {@code :invalid}, and every field's border colour.
 *
 * <p>EACH RENDERING RUNS IN ITS OWN BROWSER (a fresh process and profile, one after the other). A browser remembers how the last interaction happened, and that decides what a
 * script-focused element matches for {@code :focus-visible}; a tab shared between the two renderings gave the second one the first one's history (this differed on Linux CI for the
 * Tree). Fresh browsers give both the same, empty history.
 *
 * <p>Beside the equality the steps carry expectations written from the documented behaviour, not from either rendering's output, so both renderings must meet them: label clicks
 * focus their control, Tab skips a hidden clear button and stops on a shown one, Enter and Space on it clear the field and return focus to it, so does a click, Escape clears a
 * type=search field natively, the phone mask formats as the user types, the count turns at its limit and back, a number out of range and an emptied required field become
 * {@code :user-invalid} on blur, and Enter in a field submits the form and marks the invalid ones.
 *
 * <p>The fingerprint of classes and attributes the scripts write is NOT part of the equality: it differs by design (the original wrote classes, the migrated ones write the
 * {@code hidden} and {@code data-at-limit} attributes). {@link #theMigratedScriptsWriteOnlyTheNativeAttributes} proves that difference is exactly that.
 */
class TextEntryBehaviourTest {

    private static final String MARKER = "// ==== END OF PROVENANCE HEADER (the original file follows byte for byte) ====\n";

    record Step(String action, String active, String... fragments) {
    }

    private static final String BODY = "<form id=\"f\"><button type=\"button\" id=\"before\">Before</button>"
            + TextEntryBrowser.text(TextEntryBrowser.o("id", "tn", "label", "Name", "required", true, "pattern", "[a-z]+"))
            + TextEntryBrowser.text(TextEntryBrowser.o("id", "ph", "label", "Phone", "mask", "phone"))
            + TextEntryBrowser.number(TextEntryBrowser.o("id", "nm", "label", "Qty", "min", "1", "max", "5"))
            + TextEntryBrowser.textarea(TextEntryBrowser.o("id", "ta", "label", "Notes", "maxlength", 5, "count", true))
            + TextEntryBrowser.search(TextEntryBrowser.o("id", "se", "label", "Find"))
            + "<button type=\"submit\" id=\"go\">Go</button></form>";

    private static final String SNAPSHOT = """
            window.__snap = function () {
              var ids = ['tn', 'ph', 'nm', 'ta', 'se'];
              var ae = document.activeElement;
              var active = ae.hasAttribute('data-search-input-clear') ? 'clear' : (ae.id || ae.tagName.toLowerCase());
              var btn = document.querySelector('[data-search-input-clear]');
              var cnt = document.querySelector('[data-textarea-count]');
              var f = function (fn) { return ids.map(function (id) { return id + ':' + fn(document.getElementById(id)); }).join(','); };
              return 'active=' + active
                + ';vals=' + f(function (e) { return e.value; })
                + ';clear=' + getComputedStyle(btn).display
                + ';clearbox=' + Math.round(btn.getBoundingClientRect().width)
                + ';fv=' + (ae.matches ? String(ae.matches(':focus-visible')) : 'false')
                + ';count=' + cnt.textContent
                + ';countcolor=' + getComputedStyle(cnt).color
                + ';ui=' + f(function (e) { return e.matches(':user-invalid') ? 1 : 0; })
                + ';inv=' + f(function (e) { return e.matches(':invalid') ? 1 : 0; })
                + ';border=' + f(function (e) { return getComputedStyle(e).borderTopColor; })
                + ';ring=' + f(function (e) { return getComputedStyle(e).boxShadow.indexOf('0px 0px 0px 2px') >= 0 ? 1 : 0; });
            };
            document.getElementById('f').addEventListener('submit', function (e) { e.preventDefault(); });
            """;

    static List<Step> steps() {
        List<Step> s = new ArrayList<>();
        s.add(new Step("none", "body", "clear=none", "count=0 / 5", "se:;", "fv=false"));
        s.add(new Step("focus:before", "before"));
        s.add(new Step("key:Tab", "tn", "fv=true"));
        s.add(new Step("type:ab", "tn", "tn:ab", "ring=tn:1,ph:0,nm:0,ta:0,se:0"));
        s.add(new Step("key:Tab", "ph"));
        s.add(new Step("type:5055551234", "ph", "ph:(505) 555-1234"));
        s.add(new Step("key:Tab", "nm"));
        s.add(new Step("type:9", "nm", "nm:9"));
        s.add(new Step("key:Tab", "ta", "nm:1,ta:0"));
        s.add(new Step("type:abc", "ta", "ta:abc", "count=3 / 5"));
        s.add(new Step("type:de", "ta", "ta:abcde", "count=5 / 5"));
        s.add(new Step("type:f", "ta", "ta:abcde", "count=5 / 5"));
        s.add(new Step("key:Backspace", "ta", "ta:abcd", "count=4 / 5"));
        s.add(new Step("key:Tab", "se", "clear=none", "clearbox=0"));
        s.add(new Step("type:xyz", "se", "se:xyz", "clear=flex", "clearbox=28", "fv=true"));
        s.add(new Step("key:Tab", "clear", "fv=true"));
        s.add(new Step("key:Enter", "se", "se:;", "clear=none"));
        s.add(new Step("type:q", "se", "se:q", "clear=flex"));
        s.add(new Step("key:Escape", "se", "clear=none"));
        s.add(new Step("type:w", "se", "se:w", "clear=flex"));
        s.add(new Step("key:Tab", "clear"));
        s.add(new Step("key:Space", "se", "se:;", "clear=none"));
        s.add(new Step("type:m", "se", "clear=flex"));
        s.add(new Step("click:[data-search-input-clear]", "se", "se:;", "clear=none"));
        s.add(new Step("key:Tab", "go"));
        s.add(new Step("click:label[for=ta]", "ta"));
        s.add(new Step("click:label[for=se]", "se"));
        s.add(new Step("hover:#ph", "se"));
        s.add(new Step("click:#tn", "tn"));
        s.add(new Step("key:Backspace", "tn", "tn:a,"));
        s.add(new Step("key:Backspace", "tn", "tn:,"));
        s.add(new Step("key:Tab", "ph", "ui=tn:1"));
        s.add(new Step("key:Shift+Tab", "tn", "ui=tn:1"));
        s.add(new Step("type:ok", "tn", "tn:ok", "inv=tn:0"));
        s.add(new Step("key:Backspace", "tn", "tn:o,"));
        s.add(new Step("key:Backspace", "tn", "tn:,"));
        s.add(new Step("key:Enter", "tn", "ui=tn:1"));
        s.add(new Step("type:X", "tn", "tn:X", "ui=tn:1"));
        s.add(new Step("focus:nm", "nm"));
        return s;
    }

    private static String legacyScript;
    private static List<String> legacy;
    private static List<String> migrated;
    private static List<Step> steps;

    private static String resource(String name) throws IOException {
        try (InputStream in = TextEntryBehaviourTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException(name + " is not on the test classpath");
            }
            // Git stores these files with LF; a Windows checkout with autocrlf turns them into CRLF, which must not change what the fixture IS (its hash, its marker).
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static String sha256(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static void initialise(DevToolsSession cdp, int scripts) throws Exception {
        StringBuilder js = new StringBuilder("(async function(){");
        for (int i = 0; i < scripts; i++) {
            js.append("await import(URL.createObjectURL(new Blob([document.getElementById('src").append(i).append("').textContent],{type:'text/javascript'})));");
        }
        js.append("document.dispatchEvent(new Event('DOMContentLoaded'));return 'ok'})()");
        String response = cdp.call("Runtime.evaluate", "{\"expression\":" + DevToolsSession.quote(js.toString()) + ",\"awaitPromise\":true,\"returnByValue\":true}");
        if (!response.contains("\"value\":\"ok\"")) {
            throw new IllegalStateException("the scripts did not load: " + response);
        }
    }

    private static String page(String body, List<String> scripts) {
        StringBuilder tags = new StringBuilder();
        for (int i = 0; i < scripts.size(); i++) {
            tags.append("<script id=\"src").append(i).append("\" type=\"text/plain\">").append(scripts.get(i).replace("</script", "<\\/script")).append("</script>");
        }
        tags.append("<script>").append(SNAPSHOT).append("</script>");
        return tags.toString();
    }

    private static List<String> run(DevToolsSession cdp, List<Step> steps) throws Exception {
        List<String> transcript = new ArrayList<>();
        for (Step step : steps) {
            TextEntryBrowser.perform(cdp, step.action());
            transcript.add(cdp.evaluate("window.__snap()"));
        }
        return transcript;
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("text-entry-behaviour");
        steps = steps();
        legacyScript = resource("/parity/text-entry/search-input-legacy.js");
        String legacyTextarea = resource("/parity/text-entry/textarea-legacy.js");
        String textInput = resource("/META-INF/resources/ashlar/js/text-input.js");
        String search = resource("/META-INF/resources/ashlar/js/search-input.js");
        String textarea = resource("/META-INF/resources/ashlar/js/textarea.js");
        ParityConfigs.Config config = ParityConfigs.get("light/none");
        String legacyBody = new IconLegacyMarkup().rewrite(new TextEntryLegacyMarkup().rewrite(BODY));
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("text-entry-oracle.css")), config, "", legacyBody,
                    page(legacyBody, List.of(textInput, legacyScript, legacyTextarea))));
            String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", BODY, page(BODY, List.of(textInput, search, textarea))));
            // Each rendering in its OWN browser, one after the other.
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(legacyUrl);
                initialise(cdp, 3);
                legacy = run(cdp, steps);
            }
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(migratedUrl);
                initialise(cdp, 3);
                migrated = run(cdp, steps);
            }
        }
    }

    private static String field(String snapshot, String name) {
        return List.of(snapshot.split(";")).stream().filter(f -> f.startsWith(name + "=")).findFirst().orElseThrow().substring(name.length() + 1);
    }

    @Test
    void theLegacyScriptFixturesAreTheOriginalsByteForByte() throws Exception {
        for (String name : List.of("search-input", "textarea")) {
            String all = resource("/parity/text-entry/" + name + "-legacy.js");
            int marker = all.indexOf(MARKER);
            assertTrue(marker > 0, "the provenance header is present in " + name);
            String header = all.substring(0, marker);
            String original = all.substring(marker + MARKER.length());
            assertTrue(header.contains("TEST-ONLY") && header.contains("831a2f5") && header.contains(sha256(original)), name + ": the header names the commit and the hash of the body");
            String blob = name.equals("search-input") ? "111f111b75e76dc6f7b109058eea5932b2ddf6ca" : "c4cd28776f0043b5402748dfb8a4cb12bb9a7aab";
            assertTrue(header.contains("git blob id of the original : " + blob), name + ": the header names the original's git blob id");
            assertTrue(original.contains(name.equals("search-input") ? "classList.toggle(\"hidden\"" : "classList.toggle(\"text-red-600\""), name + ": it is the pre-migration script, which toggled Tailwind classes");
        }
    }

    @Test
    void theMigratedScriptsAreNotTheLegacyOnesSoTheComparisonComparesTwoThings() throws Exception {
        String search = resource("/META-INF/resources/ashlar/js/search-input.js");
        String textarea = resource("/META-INF/resources/ashlar/js/textarea.js");
        assertFalse(search.contains("classList") || textarea.contains("classList"), "neither migrated script toggles a class");
        assertNotEquals(resource("/parity/text-entry/search-input-legacy.js"), search);
    }

    @Test
    void afterEveryStepTheObservableBehaviourIsIdenticalInTheLegacyAndTheMigratedRendering() {
        assertEquals(steps.size(), legacy.size());
        assertEquals(steps.size(), migrated.size());
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            if (!legacy.get(i).equals(migrated.get(i))) {
                List<String> fields = new ArrayList<>();
                String[] a = legacy.get(i).split(";");
                String[] b = migrated.get(i).split(";");
                for (int k = 0; k < Math.max(a.length, b.length); k++) {
                    String x = k < a.length ? a[k] : "(missing)";
                    String y = k < b.length ? b[k] : "(missing)";
                    if (!x.equals(y)) {
                        fields.add("[" + x + " -> " + y + "]");
                    }
                }
                differences.add("step #" + i + " input=" + steps.get(i).action() + " (the step expects focus on '" + steps.get(i).active() + "')\n  differing fields (legacy -> migrated): " + String.join(" ", fields)
                        + "\n  legacy snapshot:   " + legacy.get(i) + "\n  migrated snapshot: " + migrated.get(i));
            }
        }
        assertTrue(differences.isEmpty(), differences.size() + " of " + steps.size() + " steps differ:\n" + String.join("\n", differences));
    }

    @Test
    void bothRenderingsMeetEveryExpectationWrittenFromTheDocumentedBehaviour() {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            for (var entry : List.of(java.util.Map.entry("legacy", legacy), java.util.Map.entry("migrated", migrated))) {
                String snap = entry.getValue().get(i);
                if (!field(snap, "active").equals(step.active())) {
                    problems.add(entry.getKey() + " #" + i + " " + step.action() + ": focus is " + field(snap, "active") + ", expected " + step.active());
                }
                for (String fragment : step.fragments()) {
                    if (!snap.contains(fragment)) {
                        problems.add(entry.getKey() + " #" + i + " " + step.action() + ": missing " + fragment + " in " + snap);
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void theComparisonIsNotVacuousTheSequenceChangesFocusStateAndWhatIsDrawn() {
        assertTrue(migrated.stream().map(s -> field(s, "active")).distinct().count() >= 8, "focus moved across many elements");
        assertTrue(migrated.stream().map(s -> field(s, "clear")).distinct().count() == 2, "the clear button was both hidden and shown");
        assertTrue(migrated.stream().map(s -> field(s, "countcolor")).distinct().count() == 2, "the count changed colour at its limit and back");
        assertTrue(migrated.stream().map(s -> field(s, "border")).distinct().count() >= 6, "the fields' borders changed with focus, hover and validity");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "ui").contains(":1")), "a field became :user-invalid through real interaction");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "fv").equals("true")) && migrated.stream().anyMatch(s -> field(s, "fv").equals("false")), "focus-visible was both matched and not");
    }

    @Test
    void theMigratedScriptsWriteOnlyTheNativeAttributes() throws Exception {
        // The one difference between the two transcripts' page state: the legacy script toggled classes, the migrated ones do not. Proven on the page text itself.
        String search = resource("/META-INF/resources/ashlar/js/search-input.js");
        String textarea = resource("/META-INF/resources/ashlar/js/textarea.js");
        assertTrue(search.contains("clearButton.hidden = !hasValue"));
        assertTrue(textarea.contains("toggleAttribute(\"data-at-limit\", atLimit)"));
        assertFalse(search.contains("className") || textarea.contains("className") || search.contains(".style") || textarea.contains(".style"));
    }
}
