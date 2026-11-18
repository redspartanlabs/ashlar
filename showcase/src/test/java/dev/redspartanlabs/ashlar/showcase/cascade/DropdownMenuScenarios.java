package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.List;

/**
 * The Dropdown Menu behavior scenarios, written once and run by three suites: {@link DropdownMenuLegacyCharacterizationTest} (the legacy script, recording what it did, its
 * defects included), {@link DropdownMenuBehaviourTest} (the migrated script: the same shared behavior, and the three corrections) and {@link DropdownMenuMutationTest} (the
 * same checks against deliberately broken variants, each of which must fail them).
 *
 * <p>A scenario is a list of actions ({@link DropdownMenuBrowser#perform}) on the behavior page ({@link DropdownMenuBrowser#scenarioBody}); after every action the snapshot
 * records each menu's state, each overlay's and the Popover's, the focused element, the location hash and, on a traced menu, which ending each of its exits took. An
 * expectation names a step, a snapshot field and the value it must have. The expectations were written from the component's documented behavior, not from either
 * implementation's output.
 */
final class DropdownMenuScenarios {

    private DropdownMenuScenarios() {
    }

    record Scenario(String name, List<String> actions, List<OverlayScenarios.Expect> expects) {
    }

    private static OverlayScenarios.Expect e(int step, String field, String value) {
        return new OverlayScenarios.Expect(step, field, value);
    }

    private static Scenario s(String name, List<String> actions, OverlayScenarios.Expect... expects) {
        return new Scenario(name, actions, List.of(expects));
    }

    // ---------------------------------------------------------------- shared: the same expectations for the legacy and the migrated script

    // The entry-focused Tab cases (the keyboard opens the menu on its first entry), in an overlay. Not a defect: the legacy script closed the menu on Tab from an entry too.
    // Only the menu and the overlay's own Escape are asserted; where focus lands is the overlay's focus handling (S6), which the S6 scenarios below state on their own.
    private static final List<String> ENTRY_TAB_MODAL = List.of("click:#openmo", "wait:400", "ftrig:md", "key:Enter", "wait:250", "key:Tab", "settle", "key:Escape", "settle");
    private static final List<String> ENTRY_TAB_DRAWER = List.of("click:#opendr", "wait:400", "ftrig:rd", "key:Enter", "wait:250", "key:Tab", "settle", "key:Escape", "settle");
    private static final List<String> ENTRY_SHIFT_MODAL = List.of("click:#openmo", "wait:400", "ftrig:md", "key:Enter", "wait:250", "key:Shift+Tab", "settle");
    private static final List<String> ENTRY_SHIFT_DRAWER = List.of("click:#opendr", "wait:400", "ftrig:rd", "key:Enter", "wait:250", "key:Shift+Tab", "settle");
    // Tab from a menu's trigger inside an overlay, observed right away: where focus goes is asserted here and nowhere else.
    private static final List<String> S6_MENU_MODAL = List.of("click:#openmo", "wait:400", "ctrig:md", "wait:250", "key:Tab", "wait:100");
    private static final List<String> S6_MENU_DRAWER = List.of("click:#opendr", "wait:400", "ctrig:rd", "wait:250", "key:Tab", "wait:100");

    static final List<Scenario> SHARED = List.of(
            s("a click opens the menu and leaves focus on the trigger, a second click closes it", List.of("ctrig:m1", "wait:250", "ctrig:m1", "settle"),
                    e(1, "dd:m1", "open"), e(1, "active", "trigger:m1"), e(3, "dd:m1", "closed"), e(3, "active", "trigger:m1")),
            s("Enter on the trigger opens the menu on its first entry", List.of("ftrig:m1", "key:Enter", "wait:250"), e(2, "dd:m1", "open"), e(2, "active", "item:m1:Edit")),
            s("Space on the trigger opens the menu on its first entry", List.of("ftrig:m1", "key:Space", "wait:250"), e(2, "dd:m1", "open"), e(2, "active", "item:m1:Edit")),
            s("ArrowDown on the trigger opens the menu on its first entry", List.of("ftrig:m1", "key:ArrowDown", "wait:250"), e(2, "dd:m1", "open"),
                    e(2, "active", "item:m1:Edit")),
            s("ArrowUp on the trigger opens the menu on its last entry", List.of("ftrig:m1", "key:ArrowUp", "wait:250"), e(2, "dd:m1", "open"), e(2, "active", "item:m1:Delete")),
            s("the arrows move through the enabled entries and wrap, skipping the disabled one, and Home and End go to the ends",
                    List.of("ftrig:m1", "key:Enter", "wait:250", "key:ArrowDown", "key:ArrowDown", "key:ArrowDown", "key:ArrowUp", "key:Home", "key:End"),
                    e(3, "active", "item:m1:Duplicate"), e(4, "active", "item:m1:Delete"), e(5, "active", "item:m1:Edit"), e(6, "active", "item:m1:Delete"),
                    e(7, "active", "item:m1:Edit"), e(8, "active", "item:m1:Delete"), e(8, "dd:m1", "open")),
            s("Escape from an entry closes the menu and gives focus back to its trigger", List.of("ftrig:m1", "key:Enter", "wait:250", "key:Escape", "settle"),
                    e(4, "dd:m1", "closed"), e(4, "active", "trigger:m1")),
            s("Enter on a link entry follows it, closes the menu and gives focus back to the trigger", List.of("ftrig:m1", "key:Enter", "wait:250", "key:Enter", "settle"),
                    e(4, "hash", "#e"), e(4, "dd:m1", "closed"), e(4, "active", "trigger:m1")),
            s("Space on a link entry follows it too", List.of("ftrig:m1", "key:Enter", "wait:250", "key:ArrowDown", "key:Space", "settle"),
                    e(5, "hash", "#d"), e(5, "dd:m1", "closed"), e(5, "active", "trigger:m1")),
            s("a click on an entry follows it, closes the menu and gives focus back to the trigger", List.of("ctrig:m1", "wait:250", "citem:m1:4", "settle"),
                    e(3, "hash", "#x"), e(3, "dd:m1", "closed"), e(3, "active", "trigger:m1")),
            s("a click on the disabled entry follows nothing and leaves the menu open, with focus on the entry", List.of("ctrig:m1", "wait:250", "citem:m1:3", "wait:250"),
                    e(3, "dd:m1", "open"), e(3, "hash", "-"), e(3, "active", "item:m1:Archive")),
            s("a click outside closes the menu and leaves focus where the click put it", List.of("ctrig:m1", "wait:250", "click:#text", "settle"),
                    e(3, "dd:m1", "closed"), e(3, "active", "body")),
            s("opening a second menu with a click closes the first", List.of("ctrig:m1", "wait:250", "ctrig:m2", "settle"),
                    e(3, "dd:m1", "closed"), e(3, "dd:m2", "open"), e(3, "active", "trigger:m2")),
            s("Tab from an entry closes the menu and lets the browser move focus on", List.of("ftrig:m1", "key:Enter", "wait:250", "key:Tab", "settle"),
                    e(4, "dd:m1", "closed"), e(4, "active", "after")),
            s("Shift+Tab from an entry closes the menu, focus going back to the trigger", List.of("ftrig:m1", "key:Enter", "wait:250", "key:Shift+Tab", "settle"),
                    e(4, "dd:m1", "closed"), e(4, "active", "trigger:m1")),
            s("Tab from an entry inside a Modal closes the menu, and Escape then closes the Modal", ENTRY_TAB_MODAL,
                    e(4, "dd:md", "open"), e(6, "dd:md", "closed"), e(8, "ov:mo", "hidden")),
            s("Tab from an entry inside a Drawer closes the menu, and Escape then closes the Drawer", ENTRY_TAB_DRAWER,
                    e(4, "dd:rd", "open"), e(6, "dd:rd", "closed"), e(8, "ov:dr", "hidden")),
            s("Shift+Tab from an entry inside a Modal closes the menu, focus going back to the trigger", ENTRY_SHIFT_MODAL,
                    e(4, "dd:md", "open"), e(6, "dd:md", "closed"), e(6, "active", "trigger:md"), e(6, "ov:mo", "shown")),
            s("Shift+Tab from an entry inside a Drawer closes the menu, focus going back to the trigger", ENTRY_SHIFT_DRAWER,
                    e(4, "dd:rd", "open"), e(6, "dd:rd", "closed"), e(6, "active", "trigger:rd"), e(6, "ov:dr", "shown")),
            s("inside a Modal the first Escape closes only the menu and the second the Modal (keyboard)",
                    List.of("click:#openmo", "wait:400", "ftrig:md", "key:Enter", "wait:250", "key:Escape", "settle", "key:Escape", "settle"),
                    e(4, "dd:md", "open"), e(6, "dd:md", "closed"), e(6, "ov:mo", "shown"), e(6, "active", "trigger:md"), e(8, "ov:mo", "hidden"), e(8, "active", "openmo")),
            s("inside a Drawer the first Escape closes only the menu and the second the Drawer (keyboard)",
                    List.of("click:#opendr", "wait:400", "ftrig:rd", "key:Enter", "wait:250", "key:Escape", "settle", "key:Escape", "settle"),
                    e(4, "dd:rd", "open"), e(6, "dd:rd", "closed"), e(6, "ov:dr", "shown"), e(6, "active", "trigger:rd"), e(8, "ov:dr", "hidden"), e(8, "active", "opendr")),
            s("inside a Modal the first Escape closes only the menu and the second the Modal (mouse)",
                    List.of("click:#openmo", "wait:400", "ctrig:md", "wait:250", "key:Escape", "settle", "key:Escape", "settle"),
                    e(3, "dd:md", "open"), e(5, "dd:md", "closed"), e(5, "ov:mo", "shown"), e(5, "active", "trigger:md"), e(7, "ov:mo", "hidden")),
            s("inside a Popover the first Escape closes only the menu and the second the Popover (mouse)",
                    List.of("click:#pop", "wait:250", "ctrig:pm", "wait:250", "key:Escape", "settle", "key:Escape", "settle"),
                    e(3, "dd:pm", "open"), e(5, "dd:pm", "closed"), e(5, "pop", "open"), e(5, "active", "trigger:pm"), e(7, "pop", "closed"), e(7, "active", "pop")),
            s("inside a Popover the first Escape closes only the menu and the second the Popover (keyboard)",
                    List.of("click:#pop", "wait:250", "ftrig:pm", "key:Enter", "wait:250", "key:Escape", "settle", "key:Escape", "settle"),
                    e(4, "active", "item:pm:Alpha"), e(6, "dd:pm", "closed"), e(6, "pop", "open"), e(6, "active", "trigger:pm"), e(8, "pop", "closed")),
            s("a click on an entry inside a Popover closes the menu and keeps the Popover open",
                    List.of("click:#pop", "wait:250", "ctrig:pm", "wait:250", "citem:pm:2", "settle"),
                    e(5, "dd:pm", "closed"), e(5, "pop", "open"), e(5, "hash", "#b"), e(5, "active", "trigger:pm")),
            s("a click outside a Popover closes its menu and the Popover", List.of("click:#pop", "wait:250", "ctrig:pm", "wait:250", "click:#text", "settle"),
                    e(5, "dd:pm", "closed"), e(5, "pop", "closed")),
            // Kept as they were (S2, S5, S6): recorded, not endorsed as a future contract. Each is a follow-up of its own.
            s("kept (S2): after a mouse open, ArrowDown on the trigger leaves focus on it", List.of("ctrig:m1", "wait:250", "key:ArrowDown", "wait:100"),
                    e(3, "dd:m1", "open"), e(3, "active", "trigger:m1")),
            s("kept (S2): after a mouse open, Enter on the trigger neither enters nor closes the menu", List.of("ctrig:m1", "wait:250", "key:Enter", "wait:250"),
                    e(3, "dd:m1", "open"), e(3, "active", "trigger:m1")),
            s("kept (S5): with focus outside the menu (a click on its own padding), Escape leaves it open",
                    List.of("ctrig:m1", "wait:250", "cpad:m1", "wait:100", "key:Escape", "settle"),
                    e(3, "active", "body"), e(5, "dd:m1", "open")),
            s("kept (S5): inside a Modal, with focus taken away, Escape closes neither the menu nor the Modal",
                    List.of("click:#openmo", "wait:400", "ctrig:md", "wait:250", "blur", "key:Escape", "settle"),
                    e(6, "dd:md", "open"), e(6, "ov:mo", "shown")),
            s("kept (S6, an overlay defect): Tab from a Modal's last tabbable control, when a visible link out of the Tab order follows it, leaves the Modal",
                    List.of("click:#openng", "wait:400", "focus:nlast", "key:Tab", "wait:100"),
                    e(2, "active", "nlast"), e(4, "active", "pageafter"), e(4, "ov:ng", "shown")),
            s("kept (S6, an overlay defect): Shift+Tab from a Modal's first control lands on the visible link out of the Tab order",
                    List.of("click:#openng", "wait:400", "js:document.querySelector('#ng [data-modal-close]').focus()", "key:Shift+Tab", "wait:100"),
                    e(2, "active", "close:ng"), e(4, "active", "nneg")),
            // The same defect as seen through a Dropdown Menu, which is why the S3 scenarios below do not assert where focus lands: while the menu fades out its links are still
            // on screen, and the overlay's focus handling counts them. Whether the menu closes on that Tab is S3, not this; the focus escape is the overlay's (S6), and
            // the S3 change does not remove it. Same outcome for the legacy and the migrated script.
            s("kept (S6, an overlay defect): inside a Modal, Tab from a Dropdown Menu trigger leaves the Modal", S6_MENU_MODAL,
                    e(3, "dd:md", "open"), e(5, "active", "pageafter"), e(5, "ov:mo", "shown")),
            s("kept (S6, an overlay defect): inside a Drawer, Tab from a Dropdown Menu trigger leaves the Drawer", S6_MENU_DRAWER,
                    e(3, "dd:rd", "open"), e(5, "active", "pageafter"), e(5, "ov:dr", "shown")));

    // ---------------------------------------------------------------- the three corrections: the same sequences, the legacy outcome and the corrected one

    // S1 and S4 wait on what the page shows (DropdownMenuBrowser.perform): the second input comes once the menu's exit is visibly under way. Each corrected scenario that
    // reopens a fading menu states which ending its old close took (exit=cleared: the reversed fade's transitionend), so a run whose scheduling took the other path fails as
    // such instead of passing without testing anything. The deterministic ones (reopen, escape2) happen in one task.
    private static final List<String> S1_CLICK = List.of("trace:m1", "ftrig:m1", "key:Enter", "entered", "escape-then:m1:click", "settled");
    private static final List<String> S1_ARROW = List.of("trace:m1", "ftrig:m1", "key:Enter", "entered", "escape-then:m1:ArrowDown", "settled");
    private static final List<String> S1_ENTER = List.of("trace:m1", "ftrig:m1", "key:Enter", "entered", "escape-then:m1:Enter", "settled");
    private static final List<String> S1_SPACE = List.of("trace:m1", "ftrig:m1", "key:Enter", "entered", "escape-then:m1:Space", "settled");
    private static final List<String> S1_MOUSE = List.of("trace:m1", "ctrig:m1", "entered", "click-then:m1", "settled");
    private static final List<String> S1_ONE_TASK = List.of("ctrig:m1", "wait:250", "reopen:m1", "settle");
    private static final List<String> S3_TAB = List.of("ctrig:m1", "wait:250", "key:Tab", "settle", "key:Escape", "settle");
    private static final List<String> S3_SHIFT_TAB = List.of("ctrig:m1", "wait:250", "key:Shift+Tab", "settle");
    private static final List<String> S3_MODAL = List.of("click:#openmo", "wait:400", "ctrig:md", "wait:250", "key:Tab", "settle", "key:Escape", "settle");
    private static final List<String> S3_DRAWER = List.of("click:#opendr", "wait:400", "ctrig:rd", "wait:250", "key:Tab", "settle", "key:Escape", "settle");
    private static final List<String> S3_SHIFT_MODAL = List.of("click:#openmo", "wait:400", "ctrig:md", "wait:250", "key:Shift+Tab", "settle", "key:Escape", "settle");
    private static final List<String> S3_SHIFT_DRAWER = List.of("click:#opendr", "wait:400", "ctrig:rd", "wait:250", "key:Shift+Tab", "settle", "key:Escape", "settle");
    private static final List<String> S4_MODAL = List.of("trace:md", "click:#openmo", "wait:400", "ftrig:md", "key:Enter", "entered", "escape-then:md:Escape", "settled", "settle");
    private static final List<String> S4_DRAWER = List.of("trace:rd", "click:#opendr", "wait:400", "ftrig:rd", "key:Enter", "entered", "escape-then:rd:Escape", "settled", "settle");
    private static final List<String> S4_POPOVER = List.of("trace:pm", "click:#pop", "wait:250", "ftrig:pm", "key:Enter", "entered", "escape-then:pm:Escape", "settled");
    private static final List<String> S4_MODAL_ONE_TASK = List.of("click:#openmo", "wait:400", "ftrig:md", "key:Enter", "wait:250", "escape2", "settle");
    private static final List<String> S4_POPOVER_ONE_TASK = List.of("click:#pop", "wait:250", "ftrig:pm", "key:Enter", "wait:250", "escape2", "settle");

    static final List<Scenario> LEGACY_FIXED = List.of(
            s("legacy (S1): a click on the trigger while the menu fades out is dropped", S1_CLICK, e(5, "dd:m1", "closed")),
            s("legacy (S1): ArrowDown on the trigger while the menu fades out is dropped", S1_ARROW, e(5, "dd:m1", "closed"), e(5, "active", "trigger:m1")),
            s("legacy (S1): Enter on the trigger while the menu fades out is dropped", S1_ENTER, e(5, "dd:m1", "closed"), e(5, "active", "trigger:m1")),
            s("legacy (S1): Space on the trigger while the menu fades out is dropped", S1_SPACE, e(5, "dd:m1", "closed"), e(5, "active", "trigger:m1")),
            s("legacy (S1): a second click while the menu fades out after a click is dropped", S1_MOUSE, e(4, "dd:m1", "closed")),
            s("legacy (S1): a close and a reopen in one task leave the menu closed", S1_ONE_TASK, e(3, "dd:m1", "closed")),
            s("legacy (S3): Tab from the trigger leaves the menu open, and Escape then no longer reaches it", S3_TAB,
                    e(3, "dd:m1", "open"), e(3, "active", "after"), e(5, "dd:m1", "open")),
            s("legacy (S3): Shift+Tab from the trigger leaves the menu open", S3_SHIFT_TAB, e(3, "dd:m1", "open"), e(3, "active", "before")),
            s("legacy (S3): inside a Modal, after Tab from the trigger, Escape closes neither the menu nor the Modal", S3_MODAL,
                    e(5, "dd:md", "open"), e(7, "dd:md", "open"), e(7, "ov:mo", "shown")),
            s("legacy (S3): inside a Drawer, after Tab from the trigger, Escape closes neither the menu nor the Drawer", S3_DRAWER,
                    e(5, "dd:rd", "open"), e(7, "dd:rd", "open"), e(7, "ov:dr", "shown")),
            s("legacy (S3): inside a Modal, after Shift+Tab from the trigger, Escape closes neither the menu nor the Modal", S3_SHIFT_MODAL,
                    e(5, "dd:md", "open"), e(7, "dd:md", "open"), e(7, "ov:mo", "shown")),
            s("legacy (S3): inside a Drawer, after Shift+Tab from the trigger, Escape closes neither the menu nor the Drawer", S3_SHIFT_DRAWER,
                    e(5, "dd:rd", "open"), e(7, "dd:rd", "open"), e(7, "ov:dr", "shown")),
            s("legacy (S4): inside a Modal, a second Escape while the menu fades out is consumed by it", S4_MODAL, e(7, "dd:md", "closed"), e(8, "ov:mo", "shown")),
            s("legacy (S4): inside a Drawer, a second Escape while the menu fades out is consumed by it", S4_DRAWER, e(7, "dd:rd", "closed"), e(8, "ov:dr", "shown")),
            s("legacy (S4): inside a Popover, a second Escape while the menu fades out is consumed by it", S4_POPOVER, e(7, "dd:pm", "closed"), e(7, "pop", "open")),
            s("legacy (S4): inside a Modal, two Escapes in one task close only the menu", S4_MODAL_ONE_TASK, e(6, "dd:md", "closed"), e(6, "ov:mo", "shown")),
            s("legacy (S4): inside a Popover, two Escapes in one task close only the menu", S4_POPOVER_ONE_TASK, e(6, "dd:pm", "closed"), e(6, "pop", "open")));

    static final List<Scenario> FIXED = List.of(
            s("S1: a click on the trigger while the menu fades out reopens it", S1_CLICK,
                    e(3, "dd:m1", "open"), e(5, "dd:m1", "open"), e(5, "active", "trigger:m1"), e(5, "exit", "cleared")),
            s("S1: ArrowDown on the trigger while the menu fades out reopens it on its first entry", S1_ARROW,
                    e(5, "dd:m1", "open"), e(5, "active", "item:m1:Edit"), e(5, "exit", "cleared")),
            s("S1: Enter on the trigger while the menu fades out reopens it on its first entry", S1_ENTER,
                    e(5, "dd:m1", "open"), e(5, "active", "item:m1:Edit"), e(5, "exit", "cleared")),
            s("S1: Space on the trigger while the menu fades out reopens it on its first entry", S1_SPACE,
                    e(5, "dd:m1", "open"), e(5, "active", "item:m1:Edit"), e(5, "exit", "cleared")),
            s("S1: a second click while the menu fades out after a click reopens it", S1_MOUSE,
                    e(4, "dd:m1", "open"), e(4, "active", "trigger:m1"), e(4, "exit", "cleared")),
            s("S1: a close and a reopen in one task leave the menu open", S1_ONE_TASK, e(3, "dd:m1", "open")),
            s("S3: Tab from the trigger closes the menu and lets the browser move focus on", S3_TAB,
                    e(3, "dd:m1", "closed"), e(3, "active", "after"), e(5, "dd:m1", "closed")),
            s("S3: Shift+Tab from the trigger closes the menu and lets the browser move focus back", S3_SHIFT_TAB, e(3, "dd:m1", "closed"), e(3, "active", "before")),
            // What S3 asserts in an overlay is the menu and who owns Escape: the menu closes (aria-expanded false with it, which dd: reads), so Escape reaches the Modal again.
            // Where focus lands on that Tab is deliberately not asserted here: it is the overlay's focus handling (the kept S6 scenarios in SHARED state it, and the S3 change
            // does not alter it).
            s("S3: inside a Modal, Tab from the trigger closes the menu, and Escape then closes the Modal", S3_MODAL,
                    e(5, "dd:md", "closed"), e(7, "ov:mo", "hidden")),
            s("S3: inside a Drawer, Tab from the trigger closes the menu, and Escape then closes the Drawer", S3_DRAWER,
                    e(5, "dd:rd", "closed"), e(7, "ov:dr", "hidden")),
            s("S3: inside a Modal, Shift+Tab from the trigger closes the menu, and Escape then closes the Modal", S3_SHIFT_MODAL,
                    e(5, "dd:md", "closed"), e(7, "ov:mo", "hidden")),
            s("S3: inside a Drawer, Shift+Tab from the trigger closes the menu, and Escape then closes the Drawer", S3_SHIFT_DRAWER,
                    e(5, "dd:rd", "closed"), e(7, "ov:dr", "hidden")),
            s("S4: inside a Modal, a second Escape while the menu fades out closes the Modal", S4_MODAL, e(7, "dd:md", "closed"), e(8, "ov:mo", "hidden")),
            s("S4: inside a Drawer, a second Escape while the menu fades out closes the Drawer", S4_DRAWER, e(7, "dd:rd", "closed"), e(8, "ov:dr", "hidden")),
            s("S4: inside a Popover, a second Escape while the menu fades out closes the Popover", S4_POPOVER, e(7, "dd:pm", "closed"), e(7, "pop", "closed")),
            s("S4: inside a Modal, two Escapes in one task close the menu and then the Modal", S4_MODAL_ONE_TASK, e(6, "dd:md", "closed"), e(6, "ov:mo", "hidden")),
            s("S4: inside a Popover, two Escapes in one task close the menu and then the Popover", S4_POPOVER_ONE_TASK, e(6, "dd:pm", "closed"), e(6, "pop", "closed")));

    // ---------------------------------------------------------------- the stale-close guards, each path on its own, independent of scheduling

    // A reopen in the same task as the close means the close's fade never runs: then either nothing but the old close's 200ms timer can end it (reopen), or a transitionend
    // is delivered to the menu in that same task, while the old close still waits and long before its timer is due, the way the end of a reversed fade reaches it in a real
    // run (reopen-end). Each states which ending it exercised. Migrated script only: the legacy one could not reopen a closing menu at all (S1).
    static final List<Scenario> STALE = List.of(
            s("a stale close's transitionend does not hide a menu reopened while the close still waits", List.of("trace:m1", "ctrig:m1", "entered", "reopen-end:m1", "settled"),
                    e(3, "dd:m1", "open"), e(4, "dd:m1", "open"), e(4, "exit", "cleared")),
            s("a stale close's fallback timer does not hide a menu reopened before its fade could start", List.of("trace:m1", "ctrig:m1", "entered", "reopen:m1", "settled"),
                    e(3, "dd:m1", "open"), e(4, "dd:m1", "open"), e(4, "exit", "fired")));

    // ---------------------------------------------------------------- running

    /** Runs every scenario on a fresh load of the page and returns the failures. */
    static List<String> check(DevToolsSession cdp, String url, String where, List<Scenario> scenarios) throws Exception {
        List<String> found = new ArrayList<>();
        for (Scenario s : scenarios) {
            List<String> transcript = new ArrayList<>();
            try {
                DropdownMenuBrowser.load(cdp, url);
                for (String action : s.actions()) {
                    DropdownMenuBrowser.perform(cdp, action);
                    transcript.add(action + " -> " + DropdownMenuBrowser.snapshot(cdp));
                }
            } catch (IllegalStateException ex) {
                found.add(where + " | " + s.name() + " | step " + transcript.size() + " (" + s.actions().get(transcript.size()) + ") could not be performed: " + ex.getMessage());
                continue;
            }
            for (OverlayScenarios.Expect x : s.expects()) {
                String actual = OverlayBrowser.field(transcript.get(x.step()), x.field());
                if (!actual.equals(x.value())) {
                    found.add(where + " | " + s.name() + " | step " + x.step() + " (" + s.actions().get(x.step()) + ") " + x.field() + ": expected " + x.value() + ", was " + actual
                            + " | " + transcript.get(x.step()));
                }
            }
        }
        return found;
    }

    // ---------------------------------------------------------------- probes

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static void expect(List<String> found, String where, String what, String expected, String actual) {
        if (!expected.equals(actual)) {
            found.add(where + " | " + what + ": expected " + expected + ", was " + actual);
        }
    }

    private static final String LIST = "document.querySelector('[data-dd=\"m1\"] [data-dropdown-menu-list]')";
    private static final String CHEVRON = "document.querySelector('[data-dd=\"m1\"] [data-dropdown-menu-chevron]')";

    /**
     * Under prefers-reduced-motion: reduce nothing waits: a click shows the menu fully opaque at once, an Escape hides it in the same task (read by a listener added after the
     * component's own, on its root), and neither the menu nor the chevron has a transition.
     */
    static List<String> reducedMotionProbe(DevToolsSession cdp, CascadeFixtureServer server, boolean legacyRendering, String base) throws Exception {
        List<String> found = new ArrayList<>();
        String where = "reduced motion";
        OverlayBrowser.viewport(cdp, 1000, 800);
        OverlayBrowser.reducedMotion(cdp, true);
        try {
            DropdownMenuBrowser.load(cdp, server.registerRaw(DropdownMenuBrowser.scenarioPage(legacyRendering, base)));
            DropdownMenuBrowser.perform(cdp, "ctrig:m1");
            expect(found, where, "the menu is shown, fully opaque, right after the click", "block 1", ev(cdp, "getComputedStyle(" + LIST + ").display+' '+getComputedStyle(" + LIST + ").opacity"));
            expect(found, where, "neither the menu nor the chevron has a transition", "none none", ev(cdp, "getComputedStyle(" + LIST + ").transitionProperty+' '+getComputedStyle("
                    + CHEVRON + ").transitionProperty"));
            expect(found, where, "the chevron is turned while the menu is open", "180deg", ev(cdp, "getComputedStyle(" + CHEVRON + ").rotate"));
            ev(cdp, "(function(){window.__rm='-';document.querySelector('[data-dd=\"m1\"] [data-dropdown-menu]').addEventListener('keydown',function(e){if(e.key==='Escape')"
                    + "window.__rm=getComputedStyle(" + LIST + ").display});return 'ok'})()");
            DropdownMenuBrowser.perform(cdp, "key:Escape");
            expect(found, where, "an Escape hides the menu in the same task", "none", ev(cdp, "window.__rm"));
            expect(found, where, "and the chevron is turned back", "none", ev(cdp, "getComputedStyle(" + CHEVRON + ").rotate"));
        } finally {
            OverlayBrowser.reducedMotion(cdp, false);
        }
        return found;
    }

    /**
     * Reopening a fading menu, as the correction does it: the menu is never hidden on the way (it stays rendered from the close to the reopen), and the reversed fade never
     * drops to transparent: it turns back from wherever the fade had got to. The reopen is a trusted click once the close's fade is visibly under way.
     */
    static List<String> smoothReopenProbe(DevToolsSession cdp, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        String where = "smooth reopen";
        OverlayBrowser.viewport(cdp, 1000, 800);
        DropdownMenuBrowser.load(cdp, server.registerRaw(DropdownMenuBrowser.scenarioPage(false, base)));
        DropdownMenuBrowser.perform(cdp, "trace:m1");
        DropdownMenuBrowser.perform(cdp, "ctrig:m1");
        DropdownMenuBrowser.perform(cdp, "entered");
        ev(cdp, "(function(){var m=" + LIST + ";window.__reopen={hidden:0,min:1};new MutationObserver(function(r){r.forEach(function(x){if(m.hidden)window.__reopen.hidden++})})"
                + ".observe(m,{attributes:true,attributeFilter:['hidden']});(function tick(){if(getComputedStyle(m).display!=='none'){window.__reopen.min=Math.min(window.__reopen.min,"
                + "+getComputedStyle(m).opacity)}else{window.__reopen.hidden++}if(!window.__reopen.stop)requestAnimationFrame(tick)})();return 'ok'})()");
        DropdownMenuBrowser.perform(cdp, "click-then:m1");
        DropdownMenuBrowser.perform(cdp, "settled");
        ev(cdp, "(function(){window.__reopen.stop=true;return 'ok'})()");
        expect(found, where, "the menu is open again", "open", OverlayBrowser.field("x -> " + DropdownMenuBrowser.snapshot(cdp), "dd:m1"));
        expect(found, where, "it is never hidden on the way back", "0", ev(cdp, "window.__reopen.hidden"));
        expect(found, where, "its opacity never drops to transparent on the way back", "true", ev(cdp, "window.__reopen.min>0.05"));
        return found;
    }
}
