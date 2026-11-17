package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NEGATIVE CONTROLS: deliberately broken variants of tooltip.js and popover.js, each run in a real browser against the checks of {@link FloatingBehaviourTest} that target its
 * fault, and each of which must FAIL them (be "killed") FOR THE INTENDED REASON: a failure naming the check aimed at the fault, not merely any failure. The variants are the
 * real script with one exact text replacement each (applied exactly once, or the suite fails), served as a script set over Ashlar's real scripts. The unbroken scripts,
 * served the same way, must pass the same checks: that is what makes each kill a property of the mutation and not of the harness.
 *
 * <p>Tooltip: TM1 no show delay; TM2 a touch pointer counts as hovering; TM3 aria-describedby is overwritten, not added to; TM4 Escape is ignored; TM5 no nudge; TM6 the
 * pointer is not moved back when the bubble is nudged; and the four parts of the T1 correction, each on its own: TM7 the state is read back off the lagging hidden attribute
 * (the defect itself); TM8 a stale exit's transitionend still hides the bubble; TM9 a stale exit's fallback timer still hides it; TM10 a bubble that comes back during its
 * exit is placed again with its transitions switched off.
 *
 * <p>Popover: PM1 a click inside the surface closes it (the outside-click check counts only the trigger as inside); PM2 Escape does not give focus back; PM3 focus going nowhere closes it; PM4 aria-expanded is not set when it opens
 * (which is also what a Modal or Drawer reads to leave the Escape to it); PM5 no new nudge when the window is resized; PM6 Space does not activate a synthetic trigger; and the
 * two stale-callback mechanisms of the P1 correction, each on its own: PM7 a stale close's fallback timer still hides the surface (the defect itself); PM8 a stale close's
 * transitionend still hides it (here, a reopened popover).
 *
 * <p>A stale exit ends by its transitionend or by its fallback timer, whichever comes first, and the first stops the other, so a guard on one of the two is only tested by a
 * run that takes that path. Each stale-path mutant (TM8, TM9, PM8) is therefore aimed at a check that cannot take the other path whatever the scheduling (a
 * {@link FloatingScenarios#STALE} scenario) and must be caught by it; TM8 and TM9 must also be caught by the real-transition T1 scenario that takes their path, which
 * states the path it took as an expectation the unbroken scripts must meet as well. PM7 is aimed at the race probe, which likewise checks that the first close's timer
 * fired.
 *
 * <p>Not a control, because it would be equivalent: removing the "transitions off while placing" step of popover.js's open. A surface that comes out of display: none has
 * no before-change style, so its first placement is never animated with or without the step; the nudge probe still checks that it is not.
 */
class FloatingMutationTest {

    record Mutant(String id, String script, String what, String find, String replace, List<String> scenarios, List<String> probes) {
    }

    static final List<Mutant> MUTANTS = List.of(
            new Mutant("TM1", "tooltip.js", "no show delay", "const SHOW_DELAY_MS = 150;", "const SHOW_DELAY_MS = 0;",
                    List.of("a tooltip shows after its delay"), List.of()),
            new Mutant("TM2", "tooltip.js", "a touch pointer counts as hovering",
                    "        if (event.pointerType === \"touch\") {\n            return;\n        }\n        isHovered = true;", "        isHovered = true;",
                    List.of("a touch pointer does not show a tooltip"), List.of()),
            new Mutant("TM3", "tooltip.js", "aria-describedby is overwritten, not added to",
                    "        if (!tokens.includes(bubble.id)) {\n            tokens.push(bubble.id);\n        }", "        tokens.splice(0, tokens.length, bubble.id);",
                    List.of("the tooltip adds its id"), List.of()),
            new Mutant("TM4", "tooltip.js", "Escape is ignored", "if (event.key === \"Escape\" && shown) {", "if (false) {",
                    List.of("Escape hides a hovered tooltip"), List.of()),
            new Mutant("TM5", "tooltip.js", "no nudge", "        if (shift === 0) {\n            return;\n        }", "        if (true) {\n            return;\n        }",
                    List.of(), List.of("nudge")),
            new Mutant("TM6", "tooltip.js", "the pointer is not moved back when the bubble is nudged", "const arrowShift = clamp(-shift,", "const arrowShift = 0 * clamp(-shift,",
                    List.of(), List.of("nudge")),
            new Mutant("TM7", "tooltip.js", "T1: the state is read back off the lagging hidden attribute",
                    "            if (!shown && showTimer === null) {", "            if (bubble.hidden && showTimer === null) {",
                    List.of("the pointer coming back during the exit", "focus coming back during the exit"), List.of()),
            new Mutant("TM8", "tooltip.js", "T1: a stale exit's transitionend still hides the bubble",
                    "    function onTransitionEnd(event) {\n        if (event.target !== element) {\n            return;\n        }\n        stop();\n        if (isCurrent()) {",
                    "    function onTransitionEnd(event) {\n        if (event.target !== element) {\n            return;\n        }\n        stop();\n        if (true) {",
                    List.of("the pointer coming back during the exit", "a stale exit's transitionend"), List.of()),
            new Mutant("TM9", "tooltip.js", "T1: a stale exit's fallback timer still hides the bubble",
                    "    function onTimeout() {\n        stop();\n        if (isCurrent()) {", "    function onTimeout() {\n        stop();\n        if (true) {",
                    List.of("focus coming back late in the exit", "a stale exit's fallback timer"), List.of()),
            new Mutant("TM10", "tooltip.js", "T1: a bubble that comes back during its exit is placed again with its transitions switched off",
                    "        if (bubble.hidden) {\n            bubble.hidden = false;", "        if (true) {\n            bubble.hidden = false;",
                    List.of(), List.of("smooth")),
            new Mutant("PM1", "popover.js", "a click inside the surface closes it (only the trigger counts as inside)", "if (opened && !event.composedPath().includes(root)) {",
                    "if (opened && !event.composedPath().includes(triggerSlot)) {",
                    List.of("a click on a control inside the surface"), List.of()),
            new Mutant("PM2", "popover.js", "Escape does not give focus back", "            close(true);", "            close(false);",
                    List.of("Escape closes the popover from inside it"), List.of()),
            new Mutant("PM3", "popover.js", "focus going nowhere closes it", "if (opened && movingTo && !root.contains(movingTo)) {", "if (opened && !root.contains(movingTo)) {",
                    List.of("a click on plain text inside the surface"), List.of()),
            new Mutant("PM4", "popover.js", "aria-expanded is not set when it opens", "anchor.setAttribute(\"aria-expanded\", \"true\");",
                    "anchor.setAttribute(\"aria-expanded\", \"false\");", List.of("a popover opens on a click"), List.of()),
            new Mutant("PM5", "popover.js", "no new nudge when the window is resized", "    window.addEventListener(\"resize\", () => {\n        if (opened) {",
                    "    window.addEventListener(\"resize\", () => {\n        if (false) {", List.of(), List.of("nudge")),
            new Mutant("PM6", "popover.js", "Space does not activate a synthetic trigger", "if (event.key === \"Enter\" || event.key === \" \") {", "if (event.key === \"Enter\") {",
                    List.of("a trigger with nothing focusable inside becomes a button"), List.of()),
            new Mutant("PM7", "popover.js", "P1: a stale close's fallback timer still hides the surface",
                    "    function onTimeout() {\n        stop();\n        if (isCurrent()) {", "    function onTimeout() {\n        stop();\n        if (true) {",
                    List.of(), List.of("race")),
            new Mutant("PM8", "popover.js", "P1: a stale close's transitionend still hides the surface",
                    "    function onTransitionEnd(event) {\n        if (event.target !== element) {\n            return;\n        }\n        stop();\n        if (isCurrent()) {",
                    "    function onTransitionEnd(event) {\n        if (event.target !== element) {\n            return;\n        }\n        stop();\n        if (true) {",
                    List.of("a popover reopened during its exit stays open", "a stale close's transitionend"), List.of()));

    /** What each mutant must be caught FOR: for each entry, text that at least one of its failures contains, naming a check aimed at its fault. Every entry must be met. */
    static final Map<String, List<String>> REASONS = Map.ofEntries(
            Map.entry("TM1", List.of("a tooltip shows after its delay on hover, and hides at once when the pointer leaves | step 1 (wait:60) tip:tt1: expected hidden")),
            Map.entry("TM2", List.of("a touch pointer does not show a tooltip, a mouse pointer does | step 1 (wait:300) tip:tt1: expected hidden")),
            Map.entry("TM3", List.of("takes only its own id away | step 1 (wait:300) db:tt3: expected t3help own")),
            Map.entry("TM4", List.of("Escape hides a hovered tooltip and leaves focus where it was | step 4 (settle) tip:tt1: expected hidden")),
            Map.entry("TM5", List.of("nudge | the tooltip is moved 8px inside the left edge: expected true")),
            Map.entry("TM6", List.of("nudge | its pointer still points at the trigger: expected true")),
            Map.entry("TM7", List.of("the pointer coming back during the exit shows the tooltip again | step 4 (settled) tip:tt1: expected shown+described, was hidden",
                    "focus coming back during the exit shows the tooltip again | step 5 (settled) tip:tt1: expected shown+described, was hidden")),
            Map.entry("TM8", List.of("a stale exit's transitionend does not hide a tooltip shown again while the exit still waits | step 3 (refocus-end:tt1) tip:tt1: "
                    + "expected shown+described, was hidden",
                    "the pointer coming back during the exit shows the tooltip again | step 4 (settled) tip:tt1: expected shown+described, was hidden")),
            Map.entry("TM9", List.of("a stale exit's fallback timer does not hide a tooltip shown again before the exit could start | step 4 (settled) tip:tt1: "
                    + "expected shown+described, was hidden",
                    "after the old exit's fallback timer | step 4 (settled) tip:tt1: expected shown+described, was hidden")),
            Map.entry("TM10", List.of("smooth reopen | no style is written on the bubble when it comes back")),
            Map.entry("PM1", List.of("a click on a control inside the surface keeps the popover open | step 3 (wait:250) pop:pp1: expected open")),
            Map.entry("PM2", List.of("gives focus back to its trigger | step 4 (settle) active: expected pp1")),
            Map.entry("PM3", List.of("(focus goes nowhere) keeps the popover open | step 3 (wait:250) pop:pp1: expected open")),
            Map.entry("PM4", List.of("a popover opens on a click and closes on a second one, and focus stays on its trigger | step 1 (wait:250) pop:pp1: expected open")),
            Map.entry("PM5", List.of("nudge | the popover is moved again, 8px inside the new edge: expected true")),
            Map.entry("PM6", List.of("Enter opens the popover, Space closes it | step 4 (settle) pop:pp2: expected closed")),
            Map.entry("PM7", List.of("race | the second close's fade is cut short")),
            Map.entry("PM8", List.of("a stale close's transitionend does not hide a popover opened again while the close still waits | step 3 (reclick-end:pp1) pop:pp1: "
                    + "expected open, was expanded-but-hidden")));

    private static final Map<String, String> REAL = new LinkedHashMap<>();
    private static final Map<String, List<String>> KILLS = new LinkedHashMap<>();
    private static final List<String> CONTROL = new ArrayList<>();

    private static String mutate(Mutant m) {
        String source = REAL.get(m.script());
        int first = source.indexOf(m.find());
        if (first < 0 || source.indexOf(m.find(), first + 1) >= 0) {
            throw new IllegalStateException(m.id() + ": the text to replace must occur exactly once in " + m.script() + ": " + m.find());
        }
        return source.replace(m.find(), m.replace());
    }

    private static List<FloatingScenarios.Scenario> scenarios(List<String> prefixes) {
        List<FloatingScenarios.Scenario> all = new ArrayList<>(FloatingScenarios.SHARED);
        all.addAll(FloatingScenarios.T1);
        all.addAll(FloatingScenarios.STALE);
        List<FloatingScenarios.Scenario> chosen = new ArrayList<>();
        for (String prefix : prefixes) {
            List<FloatingScenarios.Scenario> found = all.stream().filter(s -> s.name().startsWith(prefix)).toList();
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
        OverlayBrowser.viewport(cdp, 1000, 800);
        found.addAll(FloatingScenarios.check(cdp, server.registerRaw(FloatingBrowser.scenarioPage(false, base, "")), where, scenarios(scenarioPrefixes)));
        for (String probe : probes) {
            found.addAll(switch (probe) {
                case "nudge" -> FloatingScenarios.nudgeProbe(cdp, server, false, base);
                case "smooth" -> FloatingScenarios.smoothReopenProbe(cdp, server, base);
                case "race" -> FloatingScenarios.raceFailures(FloatingScenarios.popoverRaceProbe(cdp, server, false, base));
                default -> throw new IllegalArgumentException(probe);
            });
        }
        return found;
    }

    @BeforeAll
    static void run() throws Exception {
        for (String name : List.of("tooltip.js", "popover.js")) {
            REAL.put(name, OverlayBrowser.script("/META-INF/resources/ashlar/js/" + name));
        }
        List<String> allScenarios = MUTANTS.stream().flatMap(m -> m.scenarios().stream()).distinct().toList();
        List<String> allProbes = MUTANTS.stream().flatMap(m -> m.probes().stream()).distinct().toList();
        ParitySession session = ParitySession.open("floating-mutation");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            String control = server.registerScriptsOverAshlar(Map.of("tooltip.js", REAL.get("tooltip.js"), "popover.js", REAL.get("popover.js")));
            CONTROL.addAll(checks(cdp, server, control, "control", allScenarios, allProbes));
            for (Mutant m : MUTANTS) {
                String base = server.registerScriptsOverAshlar(Map.of(m.script(), mutate(m)));
                KILLS.put(m.id(), checks(cdp, server, base, m.id(), m.scenarios(), m.probes()));
            }
        }
        KILLS.forEach((id, failures) -> {
            System.out.println("FLOATING-MUTATION " + id + ": " + (failures.isEmpty() ? "SURVIVED" : "killed by " + failures.size() + " check(s)"));
            failures.forEach(f -> System.out.println("FLOATING-MUTATION   " + id + " | " + (f.length() > 300 ? f.substring(0, 300) + "..." : f)));
        });
    }

    @Test
    void everyNegativeControlFailsForItsIntendedReason() {
        assertEquals(Set.copyOf(MUTANTS.stream().map(Mutant::id).toList()), REASONS.keySet(), "a reason for every mutant, and no other");
        List<String> wrong = new ArrayList<>();
        for (Mutant m : MUTANTS) {
            for (String reason : REASONS.get(m.id())) {
                if (KILLS.get(m.id()).stream().noneMatch(f -> f.contains(reason))) {
                    wrong.add(m.id() + " was not caught by the check aimed at it (" + reason + "); its failures: " + KILLS.get(m.id()));
                }
            }
        }
        assertEquals(List.of(), wrong);
    }

    @Test
    void everyMutationAppliesToExactlyOnePlaceInTheRealScript() {
        for (Mutant m : MUTANTS) {
            assertFalse(mutate(m).equals(REAL.get(m.script())), m.id() + " changes the script");
        }
    }

    @Test
    void theUnbrokenScriptsServedTheSameWayPassEveryCheckTheMutantsAreRunAgainst() {
        assertEquals(List.of(), CONTROL);
    }

    @Test
    void everyNegativeControlIsKilledAndBothP1AndT1MechanismsHaveTheirOwnControl() {
        List<String> survivors = KILLS.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Map.Entry::getKey).toList();
        assertEquals(List.of(), survivors, "mutants that no check caught");
        assertEquals(MUTANTS.size(), KILLS.size());
        assertTrue(MUTANTS.stream().allMatch(m -> !m.scenarios().isEmpty() || !m.probes().isEmpty()), "every mutant has checks aimed at its fault");
        // The two ways a stale exit can end (its transitionend and its fallback timer) are separate code paths in both scripts, and each has a control of its own.
        assertTrue(MUTANTS.stream().anyMatch(m -> m.id().equals("TM8")) && MUTANTS.stream().anyMatch(m -> m.id().equals("TM9")));
        assertTrue(MUTANTS.stream().anyMatch(m -> m.id().equals("PM7")) && MUTANTS.stream().anyMatch(m -> m.id().equals("PM8")));
    }
}
