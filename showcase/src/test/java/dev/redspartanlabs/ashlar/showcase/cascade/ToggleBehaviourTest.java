package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour parity of Toggle: the SAME sequence of trusted browser input (real key events and real mouse clicks) is run against (a) the legacy Tailwind markup and (b) the migrated
 * markup, and the observable result must be identical after every step: which element has focus, which toggles are checked, whether each thumb has moved (its computed
 * {@code translate}), whether the focused toggle matches {@code :focus-visible} and draws its ring on the track, which inputs are {@code :user-invalid}, and whether the form is valid.
 * A Toggle has no script, so there is none to load: everything here is the browser's own behaviour of a real checkbox inside one label.
 *
 * <p>EACH RENDERING RUNS IN ITS OWN BROWSER (a fresh process and profile, one after the other): a browser remembers how the last interaction happened, and that decides what a
 * script-focused element matches for {@code :focus-visible}.
 *
 * <p>Beside the equality the steps carry expectations written from the native behaviour, not from either rendering's output, so both renderings must meet them: Tab stops (a disabled
 * toggle is skipped), Space toggles, a click on the label text, on the track and on the thumb (which does not catch the pointer, so the click reaches the track) each toggle and focus
 * the input WITHOUT the keyboard ring, a click on a disabled toggle changes nothing (the click lands on a non-focusable label, so focus returns to the page), the thumb has moved exactly when the input is checked, form reset restores the original state, and a
 * blocked submit marks the unchecked required toggle {@code :user-invalid}.
 */
class ToggleBehaviourTest {

    record Step(String action, String active, String... fragments) {
    }

    private static String switchPart(String id, String part) {
        return "[data-field=\"" + id + "\"] label > span:first-child > span:nth-of-type(" + (part.equals("track") ? 1 : 2) + ")";
    }

    private static String text(String id) {
        return "[data-field=\"" + id + "\"] label > span:last-child";
    }

    private static final String BODY = "<form id=\"f\"><button type=\"button\" id=\"before\">Before</button>"
            + ToggleBrowser.toggle(ToggleBrowser.o("id", "t1", "label", "One"))
            + ToggleBrowser.toggle(ToggleBrowser.o("id", "t2", "label", "Two", "checked", true))
            + ToggleBrowser.toggle(ToggleBrowser.o("id", "t3", "label", "Three", "disabled", true))
            + ToggleBrowser.toggle(ToggleBrowser.o("id", "t4", "label", "Four", "required", true))
            + ToggleBrowser.toggle(ToggleBrowser.o("id", "t5", "label", "Five", "start", true))
            + ToggleBrowser.toggle(ToggleBrowser.o("id", "t6", "label", "Six", "error", true, "errMsg", "Bad", "help", "Help"))
            + "<button type=\"submit\" id=\"go\">Go</button><button type=\"reset\" id=\"rs\">Reset</button></form>";

    private static final String SNAPSHOT = """
            document.getElementById('f').addEventListener('submit', function (e) { e.preventDefault(); });
            window.__snap = function () {
              var ae = document.activeElement;
              var all = [].slice.call(document.querySelectorAll('input[type=checkbox]'));
              var ids = function (fn) { return all.filter(fn).map(function (e) { return e.id; }).join(','); };
              var isToggle = ae.type === 'checkbox';
              var track = isToggle ? ae.nextElementSibling : null;
              return 'active=' + (ae.id || ae.tagName.toLowerCase())
                + ';checked=' + ids(function (e) { return e.checked; })
                + ';moved=' + ids(function (e) { return getComputedStyle(e.nextElementSibling.nextElementSibling).translate !== 'none'; })
                + ';fv=' + (ae.matches ? String(ae.matches(':focus-visible')) : 'false')
                + ';ring=' + (track && getComputedStyle(track).boxShadow.indexOf('0px 0px 0px 4px') >= 0 ? 1 : 0)
                + ';ui=' + ids(function (e) { return e.matches(':user-invalid'); })
                + ';valid=' + document.getElementById('f').checkValidity();
            };
            """;

    static List<Step> steps() {
        List<Step> s = new ArrayList<>();
        s.add(new Step("none", "body", "checked=t2;", "moved=t2;", "fv=false"));
        s.add(new Step("focus:before", "before"));
        s.add(new Step("key:Tab", "t1", "fv=true", "ring=1"));
        s.add(new Step("key:Space", "t1", "checked=t1,t2;", "moved=t1,t2;"));
        s.add(new Step("key:Tab", "t2"));
        s.add(new Step("key:Space", "t2", "checked=t1;", "moved=t1;"));
        s.add(new Step("key:Tab", "t4", "fv=true", "ring=1"));
        s.add(new Step("key:Space", "t4", "checked=t1,t4;", "moved=t1,t4;"));
        s.add(new Step("key:Space", "t4", "checked=t1;", "moved=t1;"));
        s.add(new Step("key:Tab", "t5", "ring=1"));
        s.add(new Step("key:Space", "t5", "checked=t1,t5;", "moved=t1,t5;"));
        s.add(new Step("key:Tab", "t6", "ring=1"));
        s.add(new Step("key:Shift+Tab", "t5"));
        s.add(new Step("key:Tab", "t6"));
        s.add(new Step("key:Tab", "go", "ring=0"));
        s.add(new Step("click:" + text("t1"), "t1", "checked=t5;", "moved=t5;", "fv=false", "ring=0"));
        s.add(new Step("click:" + text("t1"), "t1", "checked=t1,t5;", "moved=t1,t5;", "fv=false", "ring=0"));
        s.add(new Step("click:" + switchPart("t2", "track"), "t2", "checked=t1,t2,t5;", "fv=false", "ring=0"));
        s.add(new Step("click:" + switchPart("t2", "thumb"), "t2", "checked=t1,t5;", "moved=t1,t5;", "fv=false"));
        s.add(new Step("click:" + switchPart("t3", "track"), "body", "checked=t1,t5;"));
        s.add(new Step("click:" + text("t3"), "body", "checked=t1,t5;"));
        s.add(new Step("click:" + text("t6"), "t6", "checked=t1,t5,t6;", "ring=0"));
        s.add(new Step("click:#rs", "rs", "checked=t2;", "moved=t2;", "ui=;"));
        s.add(new Step("click:#go", "t4", "ui=t4;", "valid=false"));
        s.add(new Step("key:Space", "t4", "checked=t2,t4;", "moved=t2,t4;", "ui=;", "valid=true", "ring=1"));
        s.add(new Step("focus:t1", "t1"));
        s.add(new Step("key:Space", "t1", "checked=t1,t2,t4;", "fv=true"));
        return s;
    }

    private static List<Step> steps;
    private static List<String> legacy;
    private static List<String> migrated;

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
        ParitySession session = ParitySession.open("toggle-behaviour");
        steps = steps();
        ParityConfigs.Config config = ParityConfigs.get("light/none");
        String legacyBody = new TextEntryLegacyMarkup().rewrite(new ChoiceLegacyMarkup().rewrite(new ToggleLegacyMarkup().rewrite(BODY)));
        String script = "<script>" + SNAPSHOT + "</script>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("toggle-oracle.css")), config, "", legacyBody, script));
            String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", BODY, script));
            // Each rendering in its OWN browser, one after the other.
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(legacyUrl);
                legacy = run(cdp, steps);
            }
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(migratedUrl);
                migrated = run(cdp, steps);
            }
        }
    }

    private static String field(String snapshot, String name) {
        return List.of(snapshot.split(";")).stream().filter(f -> f.startsWith(name + "=")).findFirst().orElseThrow().substring(name.length() + 1);
    }

    @Test
    void aToggleHasNoScriptAndOnlyCarriesNativeBehaviour() {
        assertTrue(!BODY.contains("<script") && !BODY.contains("data-toggle"), "nothing but a real checkbox in one label");
    }

    @Test
    void afterEveryStepTheObservableBehaviourIsIdenticalInTheLegacyAndTheMigratedRendering() {
        assertEquals(steps.size(), legacy.size());
        assertEquals(steps.size(), migrated.size());
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            if (!legacy.get(i).equals(migrated.get(i))) {
                differences.add("step #" + i + " input=" + steps.get(i).action() + "\n  legacy snapshot:   " + legacy.get(i) + "\n  migrated snapshot: " + migrated.get(i));
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
                    if (!snap.contains(fragment)) {
                        problems.add(entry.getKey() + " #" + i + " " + step.action() + ": missing " + fragment + " in " + snap);
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void theThumbHasMovedExactlyWhenTheInputIsCheckedAtEveryStep() {
        for (List<String> transcript : List.of(legacy, migrated)) {
            for (int i = 0; i < transcript.size(); i++) {
                assertEquals(field(transcript.get(i) + ";", "checked"), field(transcript.get(i) + ";", "moved"), "step #" + i + " " + steps.get(i).action());
            }
        }
    }

    @Test
    void theComparisonIsNotVacuousTheSequenceChangesFocusStateAndWhatIsDrawn() {
        assertTrue(migrated.stream().map(s -> field(s, "active")).distinct().count() >= 8, "focus moved across many elements");
        assertTrue(migrated.stream().map(s -> field(s, "checked")).distinct().count() >= 8, "the checked set took many values");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "ring").equals("1")) && migrated.stream().anyMatch(s -> field(s, "ring").equals("0")), "the ring was drawn and not drawn");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "fv").equals("true")) && migrated.stream().anyMatch(s -> field(s, "fv").equals("false")), "focus-visible was both matched and not");
        assertTrue(migrated.stream().anyMatch(s -> !field(s, "ui").isEmpty()), "a toggle became :user-invalid through a real submit");
    }
}
