package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour parity of Checkbox and Radio Group: the SAME sequence of trusted browser input is run against (a) the legacy Tailwind markup and (b) the migrated markup, each with the
 * shipped {@code checkbox.js} (which this migration does not change), and the observable result must be identical after every step: which element has focus, which controls are
 * checked, which are indeterminate, whether the focused control matches {@code :focus-visible} and draws its ring, its accent colour, which controls are {@code :user-invalid}, and
 * whether the form is valid.
 *
 * <p>EACH RENDERING RUNS IN ITS OWN BROWSER (a fresh process and profile, one after the other): a browser remembers how the last interaction happened, and that decides what a
 * script-focused element matches for {@code :focus-visible}.
 *
 * <p>Beside the equality the steps carry expectations written from the native behaviour of these controls, not from either rendering's output, so both renderings must meet them:
 * Tab stops, Space, arrow keys that move AND select inside a radio group (skipping a disabled option and wrapping), a disabled group being skipped, label clicks that toggle and focus
 * (without showing the keyboard ring), the browser clearing {@code indeterminate} on the first toggle, form reset restoring the original selection, and a blocked submit marking the
 * unchecked required checkbox and the unselected required group {@code :user-invalid}.
 */
class ChoiceBehaviourTest {

    record Step(String action, String active, String... fragments) {
    }

    private static final String BODY = "<form id=\"f\"><button type=\"button\" id=\"before\">Before</button>"
            + ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "c1", "label", "One"))
            + ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "c2", "label", "Two", "checked", true))
            + ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "c3", "label", "Three", "indeterminate", true))
            + ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "c4", "label", "Four", "disabled", true))
            + ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "c5", "label", "Five", "required", true))
            + ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g1", "label", "Pre-selected", "selected", "b"))
            + ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g2", "label", "None selected"))
            + ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g3", "label", "Disabled group", "disabled", true, "selected", "a"))
            + ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g4", "label", "Required", "required", true))
            + "<button type=\"submit\" id=\"go\">Go</button><button type=\"reset\" id=\"rs\">Reset</button></form>";

    private static final String SNAPSHOT = """
            document.getElementById('f').addEventListener('submit', function (e) { e.preventDefault(); });
            window.__snap = function () {
              var ae = document.activeElement;
              var all = [].slice.call(document.querySelectorAll('input[type=checkbox],input[type=radio]'));
              var ids = function (fn) { return all.filter(fn).map(function (e) { return e.id; }).join(','); };
              var cs = getComputedStyle(ae);
              return 'active=' + (ae.id || ae.tagName.toLowerCase())
                + ';checked=' + ids(function (e) { return e.checked; })
                + ';indet=' + ids(function (e) { return e.indeterminate; })
                + ';fv=' + (ae.matches ? String(ae.matches(':focus-visible')) : 'false')
                + ';ring=' + (cs.boxShadow.indexOf('0px 0px 0px 4px') >= 0 ? 1 : 0)
                + ';accent=' + cs.accentColor
                + ';ui=' + ids(function (e) { return e.matches(':user-invalid'); })
                + ';valid=' + document.getElementById('f').checkValidity();
            };
            """;

    static List<Step> steps() {
        List<Step> s = new ArrayList<>();
        s.add(new Step("none", "body", "checked=c2,g1-b,g3-a;", "indet=c3;", "fv=false"));
        s.add(new Step("focus:before", "before"));
        s.add(new Step("key:Tab", "c1", "fv=true", "ring=1"));
        s.add(new Step("key:Space", "c1", "checked=c1,c2,g1-b,g3-a;"));
        s.add(new Step("key:Tab", "c2"));
        s.add(new Step("key:Space", "c2", "checked=c1,g1-b,g3-a;"));
        s.add(new Step("key:Tab", "c3", "indet=c3;"));
        s.add(new Step("key:Space", "c3", "checked=c1,c3,g1-b,g3-a;", "indet=;"));
        s.add(new Step("key:Space", "c3", "checked=c1,g1-b,g3-a;", "indet=;"));
        s.add(new Step("key:Tab", "c5", "fv=true"));
        s.add(new Step("key:Space", "c5", "checked=c1,c5,g1-b,g3-a;"));
        s.add(new Step("key:Space", "c5", "checked=c1,g1-b,g3-a;", "ui=c5;"));
        s.add(new Step("key:Tab", "g1-b", "ring=1"));
        s.add(new Step("key:ArrowDown", "g1-a", "checked=c1,g1-a,g3-a;"));
        s.add(new Step("key:ArrowDown", "g1-b", "checked=c1,g1-b,g3-a;"));
        s.add(new Step("key:ArrowUp", "g1-a", "checked=c1,g1-a,g3-a;"));
        s.add(new Step("key:ArrowRight", "g1-b", "checked=c1,g1-b,g3-a;"));
        s.add(new Step("key:ArrowLeft", "g1-a", "checked=c1,g1-a,g3-a;"));
        s.add(new Step("key:Tab", "g2-a", "checked=c1,g1-a,g3-a;"));
        s.add(new Step("key:Tab", "g4-a"));
        s.add(new Step("key:ArrowDown", "g4-b", "checked=c1,g1-a,g3-a,g4-b;"));
        s.add(new Step("key:Shift+Tab", "g2-a"));
        s.add(new Step("key:Tab", "g4-b"));
        s.add(new Step("key:Tab", "go"));
        s.add(new Step("click:label[for=g4-a]", "g4-a", "checked=c1,g1-a,g3-a,g4-a;", "fv=false", "ring=0"));
        s.add(new Step("click:label[for=c1]", "c1", "checked=g1-a,g3-a,g4-a;", "fv=false"));
        s.add(new Step("click:#rs", "rs", "checked=c2,g1-b,g3-a;", "ui=;"));
        s.add(new Step("click:#go", "c5", "ui=c5,g4-a,g4-b;", "valid=false"));
        s.add(new Step("click:label[for=c2]", "c2", "checked=g1-b,g3-a;"));
        s.add(new Step("focus:c5", "c5"));
        s.add(new Step("key:Space", "c5", "checked=c5,g1-b,g3-a;", "fv=true"));
        s.add(new Step("click:label[for=g4-b]", "g4-b", "checked=c5,g1-b,g3-a,g4-b;"));
        return s;
    }

    private static List<Step> steps;
    private static List<String> legacy;
    private static List<String> migrated;

    private static String resource(String name) throws IOException {
        try (InputStream in = ChoiceBehaviourTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException(name + " is not on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static void initialise(DevToolsSession cdp) throws Exception {
        String js = "(async function(){await import(URL.createObjectURL(new Blob([document.getElementById('src0').textContent],{type:'text/javascript'})));document.dispatchEvent(new Event('DOMContentLoaded'));return 'ok'})()";
        String response = cdp.call("Runtime.evaluate", "{\"expression\":" + DevToolsSession.quote(js) + ",\"awaitPromise\":true,\"returnByValue\":true}");
        if (!response.contains("\"value\":\"ok\"")) {
            throw new IllegalStateException("the script did not load: " + response);
        }
    }

    private static String scripts() throws IOException {
        return "<script id=\"src0\" type=\"text/plain\">" + resource("/META-INF/resources/ashlar/js/checkbox.js").replace("</script", "<\\/script") + "</script><script>" + SNAPSHOT + "</script>";
    }

    private static List<String> run(DevToolsSession cdp, List<Step> steps) throws Exception {
        List<String> transcript = new ArrayList<>();
        for (Step step : steps) {
            ChoiceBrowser.perform(cdp, step.action());
            transcript.add(cdp.evaluate("window.__snap()"));
        }
        return transcript;
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("choice-behaviour");
        steps = steps();
        ParityConfigs.Config config = ParityConfigs.get("light/none");
        String legacyBody = new TextEntryLegacyMarkup().rewrite(new ChoiceLegacyMarkup().rewrite(BODY));
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("choice-oracle.css")), config, "", legacyBody, scripts()));
            String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", BODY, scripts()));
            // Each rendering in its OWN browser, one after the other.
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(legacyUrl);
                initialise(cdp);
                legacy = run(cdp, steps);
            }
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(migratedUrl);
                initialise(cdp);
                migrated = run(cdp, steps);
            }
        }
    }

    private static String field(String snapshot, String name) {
        return List.of(snapshot.split(";")).stream().filter(f -> f.startsWith(name + "=")).findFirst().orElseThrow().substring(name.length() + 1);
    }

    @Test
    void checkboxJsIsTheShippedScriptAndItIsUnchangedInKind() throws Exception {
        String js = resource("/META-INF/resources/ashlar/js/checkbox.js").replaceAll("(?m)^\\s*//.*$", "");
        assertTrue(js.contains("checkbox.indeterminate = true;") && js.contains("[data-checkbox-indeterminate=\"true\"]"));
        assertFalse(js.contains("classList") || js.contains("className") || js.contains(".style"), "the script never touched a class or a style, and still does not");
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
    void bothRenderingsMeetEveryExpectationWrittenFromTheNativeBehaviour() {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            for (var entry : List.of(java.util.Map.entry("legacy", legacy), java.util.Map.entry("migrated", migrated))) {
                String snap = entry.getValue().get(i) + ";";
                if (!field(snap, "active").equals(step.active())) {
                    problems.add(entry.getKey() + " #" + i + " " + step.action() + ": focus is " + field(snap, "active") + ", expected " + step.active());
                }
                for (String fragment : step.fragments()) {
                    if (!snap.contains(fragment) && !snap.contains(fragment + ";")) {
                        problems.add(entry.getKey() + " #" + i + " " + step.action() + ": missing " + fragment + " in " + snap);
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void theComparisonIsNotVacuousTheSequenceChangesFocusStateAndWhatIsDrawn() {
        assertTrue(migrated.stream().map(s -> field(s, "active")).distinct().count() >= 10, "focus moved across many elements");
        assertTrue(migrated.stream().map(s -> field(s, "checked")).distinct().count() >= 12, "the checked set took many values");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "indet").contains("c3")) && migrated.stream().anyMatch(s -> field(s, "indet").isEmpty()), "indeterminate was set and then cleared by the browser");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "ring").equals("1")) && migrated.stream().anyMatch(s -> field(s, "ring").equals("0")), "the ring was drawn and not drawn");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "fv").equals("true")) && migrated.stream().anyMatch(s -> field(s, "fv").equals("false")), "focus-visible was both matched and not");
        assertTrue(migrated.stream().anyMatch(s -> !field(s, "ui").isEmpty()), "a control became :user-invalid through a real submit");
        assertTrue(migrated.stream().map(s -> field(s, "accent")).distinct().count() >= 2, "the accent colour changed with validity");
    }
}
