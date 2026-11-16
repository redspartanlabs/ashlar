package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NEGATIVE CONTROLS: deliberately broken variants of overlay.js, each run in a real browser against the checks of {@link OverlayStackTest} that target its fault, and each of
 * which must FAIL them (be "killed"). The variants are the real script with one exact text replacement each (applied exactly once, or the suite fails), served as a script set
 * next to the real modal.js and drawer.js, which import it as "./overlay.js" exactly as in the application. The unbroken scripts, served the same way, must pass the same
 * checks: that is what makes each kill a property of the mutation and not of the harness.
 *
 * <ul>
 *   <li>N1 the scroll lock is released by the first overlay to close, although another still holds it;</li>
 *   <li>N2 every new owner saves the page's overflow again, so the lock (not the page's own value) is "restored" and the page stays locked after the last close;</li>
 *   <li>N3 one Escape closes every overlay that owns the keyboard;</li>
 *   <li>N4 Escape skips a topmost overlay that does not close on it and closes the one beneath;</li>
 *   <li>N5 focus is restored to the opener whatever is still open, so it can land behind an active overlay;</li>
 *   <li>N6 an exit's end ignores the open/close generation, so an old exit hides an overlay reopened since;</li>
 *   <li>N7 a release by an owner that holds nothing is not ignored;</li>
 *   <li>N8 no stack offset is written, so document order decides which overlay paints on top;</li>
 *   <li>N9 the computed transition is ignored and a hard-coded 200ms (plus the 50ms margin) ends every exit;</li>
 *   <li>N10 (additional) the exit ends with the first transition to finish, not the last;</li>
 *   <li>N11 (additional) whether a popup owns an Escape is decided when the event reaches the document, not in the capture phase, so a popup whose own listener ran first
 *       is no longer seen;</li>
 *   <li>N12 (additional) a Popover's open state is read from any aria-expanded inside it rather than from its anchor, so a closed Popover holding an expanded Accordion
 *       keeps the enclosing overlay from ever closing on Escape (the defect this suite's review found and the fix removed).</li>
 * </ul>
 */
class OverlayMutationTest {

    private static final String SET = "overlay.js";

    record Mutant(String id, String what, String find, String replace, List<String> scenarios, List<String> probes) {
    }

    static final List<Mutant> MUTANTS = List.of(
            new Mutant("N1", "the scroll lock is released while another overlay still holds it",
                    "if (!scrollOwners.delete(owner) || scrollOwners.size > 0) {", "if (!scrollOwners.delete(owner)) {",
                    List.of("closing a drawer under a modal", "a drawer opened from a modal paints on top"), List.of()),
            new Mutant("N2", "every owner saves the overflow again, so the page stays locked after the last close",
                    "    if (scrollOwners.size === 0) {\n        // The longhands", "    if (true) {\n        // The longhands",
                    List.of("one Escape closes one of two stacked modals"), List.of("scroll")),
            new Mutant("N3", "one Escape closes every overlay that owns the keyboard",
                    "            top.escape(event);", "            keyboardStack.slice().reverse().forEach((overlay) => overlay.escape(event));",
                    List.of("one Escape closes only the topmost overlay", "one Escape closes one of two stacked modals"), List.of()),
            new Mutant("N4", "Escape skips a topmost overlay that does not close on it",
                    // A Modal that does not close on Escape is rendered WITHOUT data-close-on-escape (JTE omits a false boolean attribute); a Drawer always closes on Escape.
                    "            top.escape(event);", "            (keyboardStack.slice().reverse().find((overlay) => overlay.root.dataset.closeOnEscape === \"true\""
                            + " || overlay.root.hasAttribute(\"data-drawer\")) || top).escape(event);",
                    List.of("Escape on a topmost modal that does not close on Escape closes nothing", "Escape on a topmost modal that does not close on Escape does not close the drawer"),
                    List.of()),
            new Mutant("N5", "focus is restored to the opener whatever is still open",
                    "    const top = topmost();\n    const focusable = target && typeof target.focus === \"function\";",
                    "    const top = null;\n    const focusable = target && typeof target.focus === \"function\";",
                    List.of("a modal's deferred focus restore", "closing the overlay under the topmost one"), List.of()),
            new Mutant("N6", "an exit's end ignores the open/close generation",
                    "        window.clearTimeout(timer);\n        if (isCurrent()) {\n            done();\n        }", "        window.clearTimeout(timer);\n        done();",
                    List.of("drawer reopens during its exit"), List.of("transitions")),
            new Mutant("N7", "a release by an owner that holds nothing is not ignored",
                    "if (!scrollOwners.delete(owner) || scrollOwners.size > 0) {", "scrollOwners.delete(owner);\n    if (scrollOwners.size > 0) {",
                    List.of(), List.of("scroll")),
            new Mutant("N8", "no stack offset is written",
                    "        const offset = Math.min(index, MAX_STACK_OFFSET);", "        const offset = 0;",
                    List.of("a drawer opened from a modal paints on top"), List.of("depth")),
            new Mutant("N9", "a hard-coded 200ms ends every exit",
                    "    const bound = Math.max(0, ...elements.map(transitionMs));", "    const bound = 200;",
                    List.of(), List.of("transitions")),
            new Mutant("N10", "the exit ends with the first transition to finish",
                    "    Promise.allSettled(running.map((animation) => animation.finished)).then(finish);",
                    "    Promise.any(running.map((animation) => animation.finished)).then(finish, finish);",
                    List.of(), List.of("transitions")),
            new Mutant("N11", "whether a popup owns an Escape is decided when the event reaches the document",
                    "}, true);", "});",
                    List.of(), List.of("early listener")),
            new Mutant("N12", "a Popover's open state is read from anywhere inside it, so a closed Popover holding an expanded Accordion swallows the Escape",
                    "[data-popover-trigger][aria-expanded=\"true\"], [data-popover-trigger] [aria-expanded=\"true\"]", "[data-popover] [aria-expanded=\"true\"]",
                    List.of(), List.of("floating")));

    /** What each mutant must be caught FOR: text that at least one of its failures contains, naming the check aimed at its fault (not merely any failure). */
    static final Map<String, String> REASONS = Map.ofEntries(
            Map.entry("N1", "keeps the page locked until the modal closes too | step 3 (settle) overflow: expected hidden"),
            Map.entry("N2", "the page is unlocked only after the last | step 5 (settle) overflow: expected -, was hidden"),
            Map.entry("N3", "one Escape closes only the topmost overlay, and focus goes back to its opener inside the overlay below | step 3 (settle) m1: expected shown, was hidden"),
            Map.entry("N4", "does not close on Escape closes nothing, not even the modal under it | step 3 (settle) m1: expected shown, was hidden"),
            Map.entry("N5", "active: expected d1"),
            Map.entry("N6", "a drawer reopened during its exit is still shown after the old exit ends: expected shown, was hidden"),
            Map.entry("N7", "releasing a lock this owner never held must be a no-op"),
            Map.entry("N8", "top: expected d1, was m1"),
            Map.entry("N9", "a 10s exit is still running 400ms after close: expected shown, was hidden"),
            Map.entry("N10", "the backdrop has finished, the dialog has not: expected shown, was hidden"),
            Map.entry("N11", "the modal stays open although the popup's listener ran first: expected shown, was hidden"),
            Map.entry("N12", "nothing open owns the Escape: the first Escape closes the modal: expected hidden, was shown"));

    private static String overlay;
    private static String modal;
    private static String drawer;
    private static final Map<String, List<String>> KILLS = new LinkedHashMap<>();
    private static final List<String> CONTROL = new ArrayList<>();

    private static String mutate(Mutant m) {
        int first = overlay.indexOf(m.find());
        if (first < 0 || overlay.indexOf(m.find(), first + 1) >= 0) {
            throw new IllegalStateException(m.id() + ": the text to replace must occur exactly once in overlay.js: " + m.find());
        }
        return overlay.replace(m.find(), m.replace());
    }

    private static List<OverlayScenarios.Scenario> scenarios(List<String> prefixes) {
        List<OverlayScenarios.Scenario> all = new ArrayList<>(OverlayScenarios.SINGLE);
        all.addAll(OverlayScenarios.STACKED);
        List<OverlayScenarios.Scenario> chosen = new ArrayList<>();
        for (String prefix : prefixes) {
            List<OverlayScenarios.Scenario> found = all.stream().filter(s -> s.name().startsWith(prefix)).toList();
            if (found.size() != 1) {
                throw new IllegalStateException("exactly one scenario must start with '" + prefix + "', found " + found.size());
            }
            chosen.add(found.get(0));
        }
        return chosen;
    }

    private static List<String> checks(DevToolsSession cdp, CascadeFixtureServer server, String base, String where, List<String> scenarioPrefixes, List<String> probes)
            throws Exception {
        List<String> found = new ArrayList<>();
        String url = server.registerRaw(OverlayBrowser.scenarioPage(server, false, base, ""));
        found.addAll(OverlayScenarios.check(cdp, url, where, scenarios(scenarioPrefixes)));
        for (String probe : probes) {
            found.addAll(switch (probe) {
                case "transitions" -> OverlayScenarios.transitionProbe(cdp, server, base);
                case "scroll" -> OverlayScenarios.scrollProbe(cdp, server, base);
                case "depth" -> OverlayScenarios.depthProbe(cdp, server, base);
                case "early listener" -> OverlayScenarios.earlyListenerProbe(cdp, server, base);
                case "floating" -> OverlayScenarios.floatingProbe(cdp, server, base);
                default -> throw new IllegalArgumentException(probe);
            });
        }
        return found;
    }

    @BeforeAll
    static void run() throws Exception {
        overlay = OverlayBrowser.script("/META-INF/resources/ashlar/js/overlay.js");
        modal = OverlayBrowser.script("/META-INF/resources/ashlar/js/modal.js");
        drawer = OverlayBrowser.script("/META-INF/resources/ashlar/js/drawer.js");
        List<String> allScenarios = MUTANTS.stream().flatMap(m -> m.scenarios().stream()).distinct().toList();
        List<String> allProbes = MUTANTS.stream().flatMap(m -> m.probes().stream()).distinct().toList();
        ParitySession session = ParitySession.open("overlay-mutation");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 900, 700);
            // The other components' scripts (the floating probe's Popover, Tooltip, Dropdown Menu, Select, Combobox, Date Picker and Accordion) are served from the real
            // ones beside each set, so those pages work exactly as they do on the real scripts.
            String control = server.registerScriptsOverAshlar(Map.of(SET, overlay, "modal.js", modal, "drawer.js", drawer));
            CONTROL.addAll(checks(cdp, server, control, "control", allScenarios, allProbes));
            for (Mutant m : MUTANTS) {
                String base = server.registerScriptsOverAshlar(Map.of(SET, mutate(m), "modal.js", modal, "drawer.js", drawer));
                KILLS.put(m.id(), checks(cdp, server, base, m.id(), m.scenarios(), m.probes()));
            }
        }
        KILLS.forEach((id, failures) -> {
            System.out.println("OVERLAY-MUTATION " + id + ": " + (failures.isEmpty() ? "SURVIVED" : "killed by " + failures.size() + " check(s)"));
            failures.forEach(f -> System.out.println("OVERLAY-MUTATION   " + id + " | " + (f.length() > 300 ? f.substring(0, 300) + "..." : f)));
        });
    }

    @Test
    void everyNegativeControlFailsForItsIntendedReason() {
        assertEquals(java.util.Set.copyOf(MUTANTS.stream().map(Mutant::id).toList()), REASONS.keySet(), "a reason for every mutant, and no other");
        List<String> wrong = new ArrayList<>();
        for (Mutant m : MUTANTS) {
            String reason = REASONS.get(m.id());
            if (KILLS.get(m.id()).stream().noneMatch(f -> f.contains(reason))) {
                wrong.add(m.id() + " was not caught by the check aimed at it (" + reason + "); its failures: " + KILLS.get(m.id()));
            }
        }
        assertEquals(List.of(), wrong);
    }

    @Test
    void everyMutationAppliesToExactlyOnePlaceInTheRealScript() {
        for (Mutant m : MUTANTS) {
            assertFalse(mutate(m).equals(overlay), m.id() + " changes the script");
        }
    }

    @Test
    void theUnbrokenScriptsServedTheSameWayPassEveryCheckTheMutantsAreRunAgainst() {
        assertEquals(List.of(), CONTROL);
    }

    @Test
    void everyNegativeControlIsKilled() {
        List<String> survivors = KILLS.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Map.Entry::getKey).toList();
        assertEquals(List.of(), survivors, "mutants that no check caught");
        assertEquals(MUTANTS.size(), KILLS.size());
        assertTrue(MUTANTS.stream().allMatch(m -> !m.scenarios().isEmpty() || !m.probes().isEmpty()), "every mutant has checks aimed at its fault");
    }
}
