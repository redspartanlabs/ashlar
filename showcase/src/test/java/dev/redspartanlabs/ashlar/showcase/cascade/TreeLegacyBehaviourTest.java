package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
 * Keyboard and mouse behaviour parity: the SAME sequence of trusted browser input ({@link TreeBrowser}) is run against (a) the legacy Tailwind markup with the original
 * {@code tree.js} (the test fixture {@code /parity/tree/tree-legacy.js}, a verbatim copy of the script at commit 25b3165) and (b) the migrated markup with the migrated
 * {@code tree.js}, and the observable result must be identical after every step: which element has focus, which items hold the Tab stop, every {@code aria-expanded}, which
 * panels are {@code inert}, whether the focused element matches {@code :focus-visible}, the URL hash, and whether what is drawn agrees with the attributes. The only field
 * left out of the comparison is the class and style fingerprint, which differs BY DESIGN: the original script toggled a class and wrote an inline style, the migrated one does not.
 *
 * <p>That includes the known accessibility defect: Enter or Space on an item that is both a parent and a link toggles it and never follows the link. The original script did
 * that too, and the comparison shows the migration did not change it. It is reported as a separate follow-up, not changed here.
 */
class TreeLegacyBehaviourTest {

    /** SHA-256 of the original script, recorded in the fixture's header and here, so an edit to the copy is caught. */
    private static final String ORIGINAL_SHA256 = "21b7a5a4c0ff17b7ab0b80479ad45e6c70c41dd322d1cf7e1c5abb7ea73b8314";
    private static final String MARKER = "// ==== END OF PROVENANCE HEADER (the original file follows byte for byte) ====\n";

    private static List<TreeBrowser.Step> steps;
    private static List<String> legacy;
    private static List<String> migrated;
    private static String legacyScript;

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("tree-legacy-behaviour");
        steps = TreeBrowser.steps();
        legacyScript = TreeBrowser.script(TreeLegacyBehaviourTest.class, "/parity/tree/tree-legacy.js");
        String migratedScript = TreeBrowser.script(TreeLegacyBehaviourTest.class, "/META-INF/resources/ashlar/js/tree.js");
        TreeLegacyMarkup tree = new TreeLegacyMarkup();
        IconLegacyMarkup icons = new IconLegacyMarkup();
        // Each rendering runs in its OWN fresh browser (process and profile), one after the other, so both start from identical conditions. Sharing one tab let the
        // second run inherit the first run's input-modality history, which decides whether a script focus matches :focus-visible (it differed on Linux CI).
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(server.registerRaw(TreeBrowser.page(List.of(ParityFrames.generated("tree-oracle.css")), TreeBrowser.body(true, tree, icons), legacyScript)));
                TreeBrowser.initialise(cdp);
                legacy = TreeBrowser.run(cdp, steps);
            }
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(server.registerRaw(TreeBrowser.page(List.of(ParityFrames.ASHLAR), TreeBrowser.body(false, tree, icons), migratedScript)));
                TreeBrowser.initialise(cdp);
                migrated = TreeBrowser.run(cdp, steps);
            }
        }
    }

    private static String withoutFingerprint(String snapshot) {
        return snapshot.replaceAll(";cls=\\d+$", "");
    }

    private static String field(String snapshot, String name) {
        return List.of(snapshot.split(";")).stream().filter(f -> f.startsWith(name + "=")).findFirst().orElseThrow().substring(name.length() + 1);
    }

    @Test
    void theLegacyScriptFixtureIsTheOriginalByteForByte() throws Exception {
        int marker = legacyScript.indexOf(MARKER);
        assertTrue(marker > 0, "the provenance header is present");
        String header = legacyScript.substring(0, marker);
        assertTrue(header.contains("25b3165") && header.contains("8b4dffa9f3328cd48bdfa4cce8ef957cdea10385") && header.contains(ORIGINAL_SHA256), "the header names the commit, the blob and the hash");
        String original = legacyScript.substring(marker + MARKER.length());
        assertEquals(ORIGINAL_SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original.getBytes(StandardCharsets.UTF_8))), "the copy below the marker was edited");
        assertTrue(original.contains("group.style.gridTemplateRows") && original.contains("rotate-90"), "it is the pre-migration script: it wrote an inline style and toggled a class");
    }

    @Test
    void theMigratedScriptIsNotTheLegacyOneSoTheComparisonComparesTwoThings() throws Exception {
        String current = TreeBrowser.script(TreeLegacyBehaviourTest.class, "/META-INF/resources/ashlar/js/tree.js");
        assertFalse(current.contains("group.style.gridTemplateRows") || current.contains("rotate-90"), "the migrated script no longer writes either");
        assertNotEquals(legacyScript, current);
    }

    @Test
    void theLegacyRenderingMeetsEveryExpectationTheMigratedOneDoes() {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            TreeBrowser.Step step = steps.get(i);
            if (!field(legacy.get(i), "active").equals(step.active())) {
                problems.add("legacy #" + i + " " + step.action() + ": focus is " + field(legacy.get(i), "active") + ", expected " + step.active());
            }
            for (String fragment : step.fragments()) {
                if (!legacy.get(i).contains(fragment)) {
                    problems.add("legacy #" + i + " " + step.action() + ": expected " + fragment + " in " + legacy.get(i));
                }
            }
        }
        assertEquals(List.of(), problems, "the expectations were written from the documented keyboard model, so the ORIGINAL must meet them too");
    }

    @Test
    void afterEveryStepTheObservableBehaviourIsIdenticalInTheLegacyAndTheMigratedRendering() {
        assertEquals(steps.size(), legacy.size());
        assertEquals(steps.size(), migrated.size());
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            if (!withoutFingerprint(legacy.get(i)).equals(withoutFingerprint(migrated.get(i)))) {
                differences.add("step #" + i + " input=" + steps.get(i).action() + " (the step expects focus on '" + steps.get(i).active() + "')"
                        + "\n  differing fields (legacy -> migrated): " + differingFields(withoutFingerprint(legacy.get(i)), withoutFingerprint(migrated.get(i)))
                        + "\n  legacy snapshot:   " + legacy.get(i)
                        + "\n  migrated snapshot: " + migrated.get(i));
            }
        }
        assertTrue(differences.isEmpty(), differences.size() + " of " + steps.size() + " steps differ:\n" + String.join("\n", differences));
    }

    /** Diagnostic only: names each field whose value differs between two snapshots, with both values. The comparison itself is unchanged. */
    private static String differingFields(String legacySnapshot, String migratedSnapshot) {
        List<String> out = new ArrayList<>();
        List<String> a = List.of(legacySnapshot.split(";"));
        List<String> b = List.of(migratedSnapshot.split(";"));
        for (int k = 0; k < Math.max(a.size(), b.size()); k++) {
            String left = k < a.size() ? a.get(k) : "(missing)";
            String right = k < b.size() ? b.get(k) : "(missing)";
            if (!left.equals(right)) {
                out.add("[" + left + " -> " + right + "]");
            }
        }
        return String.join(" ", out);
    }

    @Test
    void theOnlyDifferenceIsThatTheOriginalWroteClassesAndStylesAndTheMigratedScriptDoesNot() {
        String migratedFirst = field(migrated.get(0), "cls");
        for (int i = 0; i < steps.size(); i++) {
            assertEquals(migratedFirst, field(migrated.get(i), "cls"), "the migrated script wrote nothing presentational at step #" + i);
        }
        assertTrue(legacy.stream().map(s -> field(s, "cls")).distinct().count() > 3, "the original script did write classes and styles as items expanded and collapsed");
    }

    @Test
    void theComparisonIsNotVacuousTheSequenceChangesFocusAndStateAndTheStateIsDrawn() {
        assertTrue(migrated.stream().map(s -> field(s, "active")).distinct().count() >= 14, "focus moved across many items");
        assertTrue(migrated.stream().map(s -> field(s, "exp")).distinct().count() >= 8, "the expansion state took many different values");
        assertTrue(migrated.stream().allMatch(s -> field(s, "visual").equals("true")) && legacy.stream().allMatch(s -> field(s, "visual").equals("true")),
                "and in both renderings what is drawn followed the attributes at every step");
        assertEquals(List.of(), migrated.stream().filter(s -> s.isEmpty()).toList());
    }

    @Test
    void theKnownParentLinkAccessibilityDefectIsIdenticalInBothRenderings() {
        for (List<String> transcript : List.of(legacy, migrated)) {
            int enter = -1;
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).action().equals("key:Enter") && steps.get(i).active().equals("m") && List.of(steps.get(i).fragments()).contains("m:true")) {
                    enter = i;
                }
            }
            assertTrue(enter > 0);
            assertEquals("m:true", "m:" + field(transcript.get(enter), "exp").split("m:")[1].split(",")[0], "Enter on a parent that is also a link toggles it");
            assertEquals("#c", field(transcript.get(enter), "hash"), "and does not follow its link (#m): KNOWN ACCESSIBILITY DEFECT, reported separately, unchanged by this migration");
        }
    }
}
