package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour parity of the Facets family: the SAME sequence of trusted browser input (real key and mouse events) is run against (a) the legacy Tailwind markup and (b) the migrated markup,
 * each in its own fresh browser, and the observable result must be identical after every step: which element has focus, which options are checked, whether the focused option matches
 * {@code :focus-visible} and draws its ring, and what the form would submit ({@code FormData}, which is the browser's own serialisation of the query string).
 *
 * <p>There is no script to run: the Facets family has no JavaScript, and none is added. Beside the equality the steps carry expectations written from the native behaviour of checkboxes,
 * not from either rendering's output, so both renderings must meet them: Tab stops in document order across facets, Space toggling the focused option, a disabled option being skipped
 * by Tab and never submitted (checked or not), a click on the row, on its text, on its count and on the label toggling and focusing the checkbox without showing the keyboard ring, a click
 * on a disabled row doing nothing, each facet submitting under its own name (so two checked machines are sent as two values of one field), and a form reset restoring the original
 * selection.
 */
class FacetBehaviourTest {

    record Step(String action, String active, String... fragments) {
    }

    static final String BODY = "<form id=\"f\"><button type=\"button\" id=\"before\">Before</button>"
            + FacetBrowser.facetedSearch(List.of(
                    FacetBrowser.Fac.of("Status", "status", FacetBrowser.Opt.of("Running", "running").count(42).on(), FacetBrowser.Opt.of("Ready", "ready").count(18),
                            FacetBrowser.Opt.of("Failed", "failed").count(3), FacetBrowser.Opt.of("Maintenance", "maintenance").count(0).off()),
                    FacetBrowser.Fac.of("Machine", "machine", FacetBrowser.Opt.of("ACB-01", "acb-01").count(82), FacetBrowser.Opt.of("ACB-02", "acb-02").count(61).on()),
                    FacetBrowser.Fac.of("Type", "type", FacetBrowser.Opt.of("Batch", "batch").count(96), FacetBrowser.Opt.of("Archived", "archived").count(2).on().off())))
            + "<button type=\"submit\" id=\"go\">Go</button><button type=\"reset\" id=\"rs\">Reset</button></form>";

    static final String SNAPSHOT = """
            document.getElementById('f').addEventListener('submit', function (e) { e.preventDefault(); });
            window.__snap = function () {
              var ae = document.activeElement;
              var all = [].slice.call(document.querySelectorAll('input[type=checkbox]'));
              var values = function (fn) { return all.filter(fn).map(function (e) { return e.value; }).join(','); };
              var cs = getComputedStyle(ae);
              return 'active=' + (ae.value || ae.id || ae.tagName.toLowerCase())
                + ';checked=' + values(function (e) { return e.checked; })
                + ';fv=' + (ae.matches ? String(ae.matches(':focus-visible')) : 'false')
                + ';ring=' + (cs.boxShadow.indexOf('0px 0px 0px 4px') >= 0 ? 1 : 0)
                + ';formdata=' + new URLSearchParams(new FormData(document.getElementById('f'))).toString();
            };
            """;

    private static String row(String value) {
        return "click:label:has(> input[value=\"" + value + "\"])";
    }

    static List<Step> steps() {
        List<Step> s = new ArrayList<>();
        s.add(new Step("none", "body", "checked=running,acb-02,archived;", "formdata=status=running&machine=acb-02", "fv=false"));
        s.add(new Step("focus:before", "before"));
        s.add(new Step("key:Tab", "running", "fv=true", "ring=1"));
        s.add(new Step("key:Space", "running", "checked=acb-02,archived;", "formdata=machine=acb-02"));
        s.add(new Step("key:Tab", "ready"));
        s.add(new Step("key:Space", "ready", "checked=ready,acb-02,archived;", "formdata=status=ready&machine=acb-02"));
        s.add(new Step("key:Tab", "failed"));
        s.add(new Step("key:Tab", "acb-01"));
        s.add(new Step("key:Space", "acb-01", "checked=ready,acb-01,acb-02,archived;", "formdata=status=ready&machine=acb-01&machine=acb-02"));
        s.add(new Step("key:Tab", "acb-02"));
        s.add(new Step("key:Tab", "batch"));
        s.add(new Step("key:Tab", "go"));
        s.add(new Step("key:Shift+Tab", "batch", "ring=1"));
        s.add(new Step("key:Space", "batch", "checked=ready,acb-01,acb-02,batch,archived;", "formdata=status=ready&machine=acb-01&machine=acb-02&type=batch"));
        s.add(new Step(row("failed"), "failed", "checked=ready,failed,acb-01,acb-02,batch,archived;", "formdata=status=ready&status=failed&machine=acb-01&machine=acb-02&type=batch", "fv=false", "ring=0"));
        s.add(new Step(row("maintenance"), "body", "checked=ready,failed,acb-01,acb-02,batch,archived;", "formdata=status=ready&status=failed&machine=acb-01&machine=acb-02&type=batch"));
        s.add(new Step("click:label:has(> input[value=\"ready\"]) > span[aria-hidden]", "ready", "checked=failed,acb-01,acb-02,batch,archived;", "formdata=status=failed&machine=acb-01&machine=acb-02&type=batch", "ring=0"));
        s.add(new Step("click:label:has(> input[value=\"running\"]) > span:first-of-type", "running", "checked=running,failed,acb-01,acb-02,batch,archived;", "ring=0"));
        s.add(new Step(row("archived"), "body", "checked=running,failed,acb-01,acb-02,batch,archived;", "formdata=status=running&status=failed&machine=acb-01&machine=acb-02&type=batch"));
        s.add(new Step("click:#rs", "rs", "checked=running,acb-02,archived;", "formdata=status=running&machine=acb-02"));
        s.add(new Step("focus:before", "before"));
        s.add(new Step("key:Tab", "running", "fv=true", "ring=1"));
        s.add(new Step("key:Shift+Tab", "before", "ring=0"));
        s.add(new Step("click:#go", "go", "formdata=status=running&machine=acb-02"));
        return s;
    }

    private static List<Step> steps;
    private static List<String> legacy;
    private static List<String> migrated;

    static List<String> transcript(ParitySession session, String url, List<Step> steps) throws Exception {
        try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            cdp.navigate(url);
            List<String> transcript = new ArrayList<>();
            for (Step step : steps) {
                FacetBrowser.perform(cdp, step.action());
                transcript.add(cdp.evaluate("window.__snap()"));
            }
            return transcript;
        }
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("facet-behaviour");
        steps = steps();
        ParityConfigs.Config config = ParityConfigs.get("light/none");
        String legacyBody = new FacetLegacyMarkup().rewrite(BODY);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("facet-oracle.css")), config, "", legacyBody, "<script>" + SNAPSHOT + "</script>"));
            String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", BODY, "<script>" + SNAPSHOT + "</script>"));
            // Each rendering in its OWN browser, one after the other: a browser remembers how the last interaction happened, and that decides what a script-focused element matches for :focus-visible.
            legacy = transcript(session, legacyUrl, steps);
            migrated = transcript(session, migratedUrl, steps);
        }
    }

    static String field(String snapshot, String name) {
        return List.of(snapshot.split(";")).stream().filter(f -> f.startsWith(name + "=")).findFirst().orElseThrow().substring(name.length() + 1);
    }

    @Test
    void theFacetsHaveNoScriptOfTheirOwnSoNothingIsLoadedForThem() {
        assertFalse(BODY.contains("<script") || BODY.contains("type=\"module\""), "the three templates write no script");
    }

    @Test
    void afterEveryStepTheObservableBehaviourIsIdenticalInTheLegacyAndTheMigratedRendering() {
        assertEquals(steps.size(), legacy.size());
        assertEquals(steps.size(), migrated.size());
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            if (!legacy.get(i).equals(migrated.get(i))) {
                differences.add("step #" + i + " input=" + steps.get(i).action() + " (the step expects focus on '" + steps.get(i).active() + "')\n  legacy snapshot:   " + legacy.get(i)
                        + "\n  migrated snapshot: " + migrated.get(i));
            }
        }
        assertTrue(differences.isEmpty(), differences.size() + " of " + steps.size() + " steps differ:\n" + String.join("\n", differences));
    }

    @Test
    void bothRenderingsMeetEveryExpectationWrittenFromTheNativeBehaviour() {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            for (Map.Entry<String, List<String>> entry : List.of(Map.entry("legacy", legacy), Map.entry("migrated", migrated))) {
                String snap = entry.getValue().get(i) + ";";
                if (!field(snap, "active").equals(step.active())) {
                    problems.add(entry.getKey() + " #" + i + " " + step.action() + ": focus is " + field(snap, "active") + ", expected " + step.active());
                }
                for (String fragment : step.fragments()) {
                    boolean formdata = fragment.startsWith("formdata=");
                    boolean met = formdata ? field(snap, "formdata").equals(fragment.substring("formdata=".length())) : snap.contains(fragment) || snap.contains(fragment + ";");
                    if (!met) {
                        problems.add(entry.getKey() + " #" + i + " " + step.action() + ": missing " + fragment + " in " + snap);
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void aDisabledOptionIsNeverSubmittedCheckedOrNotAndEveryFacetSubmitsUnderItsOwnName() {
        for (String snap : migrated) {
            assertFalse(field(snap, "formdata").contains("maintenance") || field(snap, "formdata").contains("archived"), "a disabled option is not submitted: " + snap);
        }
        assertTrue(migrated.stream().anyMatch(s -> field(s, "formdata").contains("machine=acb-01&machine=acb-02")), "two checked values of one facet are two values of one field");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "formdata").contains("status=") && field(s, "formdata").contains("machine=") && field(s, "formdata").contains("type=")), "three facets, three names");
    }

    @Test
    void theComparisonIsNotVacuousTheSequenceChangesFocusSelectionAndWhatIsDrawn() {
        assertTrue(migrated.stream().map(s -> field(s, "active")).distinct().count() >= 9, "focus moved across many elements");
        assertTrue(migrated.stream().map(s -> field(s, "checked")).distinct().count() >= 8, "the checked set took many values");
        assertTrue(migrated.stream().map(s -> field(s, "formdata")).distinct().count() >= 8, "the submission took many values");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "ring").equals("1")) && migrated.stream().anyMatch(s -> field(s, "ring").equals("0")), "the ring was drawn and not drawn");
        assertTrue(migrated.stream().anyMatch(s -> field(s, "fv").equals("true")) && migrated.stream().anyMatch(s -> field(s, "fv").equals("false")), "focus-visible was both matched and not");
    }
}
