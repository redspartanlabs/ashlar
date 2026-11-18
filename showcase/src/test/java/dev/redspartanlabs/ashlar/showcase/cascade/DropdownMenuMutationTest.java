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
 * NEGATIVE CONTROLS: deliberately broken variants of dropdown-menu.js, each run in a real browser against the checks of {@link DropdownMenuBehaviourTest} that target its
 * fault, and each of which must FAIL them (be "killed") FOR THE INTENDED REASON: a failure naming the check aimed at the fault, not merely any failure. The variants are
 * the real script with one exact text replacement each (applied exactly once, or the suite fails), served as a script set over Ashlar's real scripts. The unbroken
 * script, served the same way, must pass the same checks: that is what makes each kill a property of the mutation and not of the harness.
 *
 * <p>Escape, focus and ownership: DM1 an Escape from an entry is not stopped at the root (an enclosing Popover closes too); DM2 it does not give focus back; DM5
 * aria-expanded is not set when the menu opens (which is also what a Modal or Drawer reads to leave the Escape to it). Dismissal: DM3 a click on an entry does not close
 * the menu; DM4 an outside click does not; DM17 the menu is not hidden at the end of its exit. The keyboard model: DM6 the arrows do not wrap; DM7 the disabled entry is
 * not skipped; DM8 Home does nothing; DM9 ArrowUp on the trigger opens on the first entry; DM10 Space does not follow a link entry. State: DM16 the chevron is not turned.
 * And the three corrections: DM11 the state is read back off the lagging hidden attribute (S1, the defect itself); DM12 a stale close's transitionend still hides the
 * menu; DM13 a stale close's fallback timer still hides it; DM14 Tab from the trigger does not close the menu (S3); DM15 an Escape on the trigger is consumed while the
 * menu fades out (S4). DM18: Tab from an entry does not close the menu (not a defect: the entry-focused counterpart of S3, on a page and in a Modal and a Drawer). DM14 is
 * caught by Tab and Shift+Tab from the trigger on a page, in a Modal and in a Drawer; none of the S3 controls reads where focus lands, which is the overlay's (S6).
 *
 * <p>A stale close ends by its transitionend or by its fallback timer, whichever comes first, and the first stops the other, so a guard on one of the two is only tested by
 * a run that takes that path. DM12 and DM13 are therefore each aimed at a check that cannot take the other path whatever the scheduling (a
 * {@link DropdownMenuScenarios#STALE} scenario) and must be caught by it, and each also by a real or one-task S1 scenario that takes its path; the real ones state the
 * path they took as an expectation the unbroken script must meet as well. DM11, DM14 and DM15 are each caught by a check on a page and by one inside a Modal.
 *
 * <p>Not a control, because it would be equivalent: making a reopen during the fade go through the hidden branch of openMenu. The menu is not hidden then, so that branch
 * would only unhide it and force a reflow, which change nothing that can be observed.
 */
class DropdownMenuMutationTest {

    record Mutant(String id, String what, String find, String replace, List<String> scenarios, List<String> probes) {
    }

    private static final String ON_TRANSITION_END = "    function onTransitionEnd(event) {\n        if (event.target !== element) {\n            return;\n        }\n        stop();\n        if (isCurrent()) {";

    static final List<Mutant> MUTANTS = List.of(
            new Mutant("DM1", "an Escape from an entry is not stopped at the root", "                event.stopPropagation();\n                closeMenu(true);",
                    "                closeMenu(true);", List.of("inside a Popover the first Escape closes only the menu and the second the Popover (keyboard)"), List.of()),
            new Mutant("DM2", "an Escape from an entry does not give focus back", "                event.stopPropagation();\n                closeMenu(true);",
                    "                event.stopPropagation();\n                closeMenu(false);", List.of("Escape from an entry closes the menu"), List.of()),
            new Mutant("DM3", "a click on an entry does not close the menu", "item.addEventListener(\"click\", () => closeMenu(true));",
                    "item.addEventListener(\"click\", () => {});", List.of("a click on an entry follows it"), List.of()),
            new Mutant("DM4", "an outside click does not close the menu", "if (isOpen() && !event.composedPath().includes(root)) {", "if (false) {",
                    List.of("a click outside closes the menu"), List.of()),
            new Mutant("DM5", "aria-expanded is not set when the menu opens", "trigger.setAttribute(\"aria-expanded\", \"true\");",
                    "trigger.setAttribute(\"aria-expanded\", \"false\");", List.of("a click opens the menu"), List.of()),
            new Mutant("DM6", "the arrows do not wrap", ": (currentIndex + offset + enabled.length) % enabled.length;",
                    ": Math.max(0, Math.min(enabled.length - 1, currentIndex + offset));", List.of("the arrows move through the enabled entries"), List.of()),
            new Mutant("DM7", "the disabled entry is not skipped", "return items.filter((item) => item.getAttribute(\"aria-disabled\") !== \"true\");", "return items;",
                    List.of("the arrows move through the enabled entries"), List.of()),
            new Mutant("DM8", "Home does nothing", "moveFocusToEdge(true);", "void 0;", List.of("the arrows move through the enabled entries"), List.of()),
            new Mutant("DM9", "ArrowUp on the trigger opens the menu on its first entry", "openMenu(\"last\");", "openMenu(\"first\");",
                    List.of("ArrowUp on the trigger opens the menu on its last entry"), List.of()),
            new Mutant("DM10", "Space does not follow a link entry", "document.activeElement.click();", "void 0;", List.of("Space on a link entry follows it too"), List.of()),
            new Mutant("DM11", "S1: the state is read back off the lagging hidden attribute", "        return opened;", "        return !menu.hidden;",
                    List.of("S1: a click on the trigger while the menu fades out reopens it", "S4: inside a Modal, a second Escape while the menu fades out"), List.of()),
            new Mutant("DM12", "S1: a stale close's transitionend still hides the menu", ON_TRANSITION_END, ON_TRANSITION_END.replace("if (isCurrent()) {", "if (true) {"),
                    List.of("a stale close's transitionend", "S1: a click on the trigger while the menu fades out reopens it"), List.of()),
            new Mutant("DM13", "S1: a stale close's fallback timer still hides the menu", "    function onTimeout() {\n        stop();\n        if (isCurrent()) {",
                    "    function onTimeout() {\n        stop();\n        if (true) {", List.of("a stale close's fallback timer", "S1: a close and a reopen in one task"), List.of()),
            new Mutant("DM14", "S3: Tab from the trigger does not close the menu",
                    "                    // and focus is not pulled back to the trigger.\n                    closeMenu(false);",
                    "                    // and focus is not pulled back to the trigger.\n                    void 0;",
                    List.of("S3: Tab from the trigger closes the menu", "S3: Shift+Tab from the trigger closes the menu", "S3: inside a Modal, Tab from the trigger",
                            "S3: inside a Drawer, Tab from the trigger", "S3: inside a Modal, Shift+Tab from the trigger", "S3: inside a Drawer, Shift+Tab from the trigger"),
                    List.of()),
            new Mutant("DM15", "S4: an Escape on the trigger is consumed while the menu fades out", "                    if (isOpen()) {\n                        event.preventDefault();",
                    "                    if (isOpen() || !menu.hidden) {\n                        event.preventDefault();",
                    List.of("S4: inside a Modal, a second Escape while the menu fades out", "S4: inside a Modal, two Escapes in one task"), List.of()),
            new Mutant("DM16", "the chevron is not turned", "chevron.dataset.state = \"open\";", "chevron.dataset.state = \"closed\";", List.of(), List.of("reduced")),
            new Mutant("DM17", "the menu is not hidden at the end of its exit", "            menu.hidden = true;", "            void 0;", List.of("a click opens the menu"), List.of()),
            new Mutant("DM18", "Tab from an entry does not close the menu", "                // select.js's own trigger keydown handler uses for Tab.\n                closeMenu(false);",
                    "                // select.js's own trigger keydown handler uses for Tab.\n                void 0;",
                    List.of("Tab from an entry closes the menu", "Tab from an entry inside a Modal", "Tab from an entry inside a Drawer"), List.of()));

    /** What each mutant must be caught FOR: for each entry, text that at least one of its failures contains, naming a check aimed at its fault. Every entry must be met. */
    static final Map<String, List<String>> REASONS = Map.ofEntries(
            Map.entry("DM1", List.of("inside a Popover the first Escape closes only the menu and the second the Popover (keyboard) | step 6 (settle) pop: expected open")),
            Map.entry("DM2", List.of("Escape from an entry closes the menu and gives focus back to its trigger | step 4 (settle) active: expected trigger:m1")),
            Map.entry("DM3", List.of("a click on an entry follows it, closes the menu and gives focus back to the trigger | step 3 (settle) dd:m1: expected closed")),
            Map.entry("DM4", List.of("a click outside closes the menu and leaves focus where the click put it | step 3 (settle) dd:m1: expected closed")),
            Map.entry("DM5", List.of("a click opens the menu and leaves focus on the trigger, a second click closes it | step 1 (wait:250) dd:m1: expected open")),
            Map.entry("DM6", List.of("skipping the disabled one, and Home and End go to the ends | step 5 (key:ArrowDown) active: expected item:m1:Edit")),
            Map.entry("DM7", List.of("skipping the disabled one, and Home and End go to the ends | step 4 (key:ArrowDown) active: expected item:m1:Delete")),
            Map.entry("DM8", List.of("skipping the disabled one, and Home and End go to the ends | step 7 (key:Home) active: expected item:m1:Edit")),
            Map.entry("DM9", List.of("ArrowUp on the trigger opens the menu on its last entry | step 2 (wait:250) active: expected item:m1:Delete")),
            Map.entry("DM10", List.of("Space on a link entry follows it too | step 5 (settle) hash: expected #d")),
            Map.entry("DM11", List.of("S1: a click on the trigger while the menu fades out reopens it | step 5 (settled) dd:m1: expected open",
                    "S4: inside a Modal, a second Escape while the menu fades out closes the Modal | step 8 (settle) ov:mo: expected hidden")),
            Map.entry("DM12", List.of("a stale close's transitionend does not hide a menu reopened while the close still waits | step 3 (reopen-end:m1) dd:m1: expected open",
                    "S1: a click on the trigger while the menu fades out reopens it | step 5 (settled) dd:m1: expected open")),
            Map.entry("DM13", List.of("a stale close's fallback timer does not hide a menu reopened before its fade could start | step 4 (settled) dd:m1: expected open",
                    "S1: a close and a reopen in one task leave the menu open | step 3 (settle) dd:m1: expected open")),
            Map.entry("DM14", List.of("S3: Tab from the trigger closes the menu and lets the browser move focus on | step 3 (settle) dd:m1: expected closed",
                    "S3: inside a Modal, Tab from the trigger closes the menu, and Escape then closes the Modal | step 7 (settle) ov:mo: expected hidden")),
            Map.entry("DM15", List.of("S4: inside a Modal, a second Escape while the menu fades out closes the Modal | step 8 (settle) ov:mo: expected hidden",
                    "S4: inside a Modal, two Escapes in one task close the menu and then the Modal | step 6 (settle) ov:mo: expected hidden")),
            Map.entry("DM16", List.of("reduced motion | the chevron is turned while the menu is open: expected 180deg")),
            Map.entry("DM17", List.of("a click opens the menu and leaves focus on the trigger, a second click closes it | step 3 (settle) dd:m1: expected closed")),
            Map.entry("DM18", List.of("Tab from an entry closes the menu and lets the browser move focus on | step 4 (settle) dd:m1: expected closed",
                    "Tab from an entry inside a Modal closes the menu, and Escape then closes the Modal | step 6 (settle) dd:md: expected closed",
                    "Tab from an entry inside a Drawer closes the menu, and Escape then closes the Drawer | step 6 (settle) dd:rd: expected closed")));

    private static String real;
    private static final Map<String, List<String>> KILLS = new LinkedHashMap<>();
    private static final List<String> CONTROL = new ArrayList<>();

    private static String mutate(Mutant m) {
        int first = real.indexOf(m.find());
        if (first < 0 || real.indexOf(m.find(), first + 1) >= 0) {
            throw new IllegalStateException(m.id() + ": the text to replace must occur exactly once in dropdown-menu.js: " + m.find());
        }
        return real.replace(m.find(), m.replace());
    }

    private static List<DropdownMenuScenarios.Scenario> scenarios(List<String> prefixes) {
        List<DropdownMenuScenarios.Scenario> all = new ArrayList<>(DropdownMenuScenarios.SHARED);
        all.addAll(DropdownMenuScenarios.FIXED);
        all.addAll(DropdownMenuScenarios.STALE);
        List<DropdownMenuScenarios.Scenario> chosen = new ArrayList<>();
        for (String prefix : prefixes) {
            List<DropdownMenuScenarios.Scenario> found = all.stream().filter(s -> s.name().startsWith(prefix)).toList();
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
        found.addAll(DropdownMenuScenarios.check(cdp, server.registerRaw(DropdownMenuBrowser.scenarioPage(false, base)), where, scenarios(scenarioPrefixes)));
        for (String probe : probes) {
            found.addAll(switch (probe) {
                case "reduced" -> DropdownMenuScenarios.reducedMotionProbe(cdp, server, false, base);
                default -> throw new IllegalArgumentException(probe);
            });
        }
        return found;
    }

    @BeforeAll
    static void run() throws Exception {
        real = OverlayBrowser.script("/META-INF/resources/ashlar/js/dropdown-menu.js");
        List<String> allScenarios = MUTANTS.stream().flatMap(m -> m.scenarios().stream()).distinct().toList();
        List<String> allProbes = MUTANTS.stream().flatMap(m -> m.probes().stream()).distinct().toList();
        ParitySession session = ParitySession.open("dropdown-menu-mutation");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            String control = server.registerScriptsOverAshlar(Map.of("dropdown-menu.js", real));
            CONTROL.addAll(checks(cdp, server, control, "control", allScenarios, allProbes));
            for (Mutant m : MUTANTS) {
                String base = server.registerScriptsOverAshlar(Map.of("dropdown-menu.js", mutate(m)));
                KILLS.put(m.id(), checks(cdp, server, base, m.id(), m.scenarios(), m.probes()));
            }
        }
        KILLS.forEach((id, failures) -> {
            System.out.println("DROPDOWN-MUTATION " + id + ": " + (failures.isEmpty() ? "SURVIVED" : "killed by " + failures.size() + " check(s)"));
            failures.forEach(f -> System.out.println("DROPDOWN-MUTATION   " + id + " | " + (f.length() > 300 ? f.substring(0, 300) + "..." : f)));
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
            assertFalse(mutate(m).equals(real), m.id() + " changes the script");
        }
    }

    @Test
    void theUnbrokenScriptServedTheSameWayPassesEveryCheckTheMutantsAreRunAgainst() {
        assertEquals(List.of(), CONTROL);
    }

    @Test
    void everyNegativeControlIsKilledAndEachCorrectionHasControlsOfItsOwn() {
        List<String> survivors = KILLS.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Map.Entry::getKey).toList();
        assertEquals(List.of(), survivors, "mutants that no check caught");
        assertEquals(MUTANTS.size(), KILLS.size());
        assertTrue(MUTANTS.stream().allMatch(m -> !m.scenarios().isEmpty() || !m.probes().isEmpty()), "every mutant has checks aimed at its fault");
        // The two ways a stale close can end are separate code paths, and each has a control of its own; so do S3 and S4.
        assertTrue(Set.of("DM11", "DM12", "DM13", "DM14", "DM15").stream().allMatch(id -> MUTANTS.stream().anyMatch(m -> m.id().equals(id))));
    }
}
