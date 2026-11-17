package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The motion of the Tooltip's bubble and the Popover's surface, legacy against migrated, with transitions ON (the parity suites measure every state at rest).
 *
 * <ul>
 *   <li>Duration, easing and delay are compared exactly, for both, in both themes: 150ms, ease-out, no delay. tooltip.js and popover.js wait 150ms (plus 50) for an exit, so
 *       the duration must not move.</li>
 *   <li>The transition PROPERTY differs by design: Tailwind's transition utility listed twenty-three properties, of which only a few ever change. Which properties change
 *       between the closed and the open state is MEASURED here, from the legacy rendering itself (every property of the full computed style). The Tooltip lists exactly that
 *       set. The Popover lists that set and translate, because popover.js re-nudges an open surface when the window is resized and the legacy rule animated that move (the
 *       behavior suites check the move is animated). Every migrated property is also in the legacy list (nothing new is animated).</li>
 *   <li>Under prefers-reduced-motion: reduce, both renderings compute transition-property: none.</li>
 * </ul>
 */
class FloatingMotionTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, String> MIGRATED = new LinkedHashMap<>();
    private static final Map<String, Set<String>> CHANGED = new LinkedHashMap<>();
    private static final Map<String, String> REDUCED = new LinkedHashMap<>();

    private static final Map<String, String> PARTS = Map.of("tooltip bubble", "[data-tooltip-bubble]", "popover surface", "[data-popover-surface]");

    private static String body(boolean open) {
        return OverlayBrowser.withoutScripts(FloatingBrowser.inState(FloatingBrowser.tooltip(FloatingBrowser.trigger("mt", "Tip"), FloatingBrowser.SHORT_TIP, null), true, open))
                + OverlayBrowser.withoutScripts(FloatingBrowser.inState(FloatingBrowser.popover(FloatingBrowser.trigger("mp", "Pop"), FloatingBrowser.BODY, "Title", null), true, open));
    }

    private static String page(String sheet, String theme, String body) {
        return "<!doctype html><html data-theme=\"" + theme + "\"><head><meta charset=\"utf-8\"><link rel=\"stylesheet\" href=\"" + sheet + "\"></head>"
                + "<body style=\"margin:200px 400px\">" + body + "</body></html>";
    }

    private static String transitions(DevToolsSession cdp, String selector) throws Exception {
        return cdp.evaluate("(function(){var s=getComputedStyle(document.querySelector('" + selector + "'));return [s.transitionProperty,s.transitionDuration,"
                + "s.transitionTimingFunction,s.transitionDelay].join(' | ')})()");
    }

    private static Map<String, String> all(DevToolsSession cdp, String selector) throws Exception {
        String dump = cdp.evaluate("(function(){var s=getComputedStyle(document.querySelector('" + selector + "'));var o=[];for(var i=0;i<s.length;i++){o.push(s[i]+'='+s.getPropertyValue(s[i]))}"
                + "return o.join('\\n')})()");
        Map<String, String> m = new LinkedHashMap<>();
        for (String line : dump.split("\n")) {
            int eq = line.indexOf('=');
            m.put(line.substring(0, eq), line.substring(eq + 1));
        }
        return m;
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("floating-motion");
        String closed = body(false);
        String open = body(true);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 1200, 800);
            String oracle = ParityFrames.generated("floating-oracle.css");
            for (String theme : List.of("light", "dark")) {
                for (Map.Entry<String, String> part : PARTS.entrySet()) {
                    cdp.navigate(server.registerRaw(page(oracle, theme, FloatingBrowser.legacy(open))));
                    String[] legacy = transitions(cdp, part.getValue()).split(" \\| ");
                    cdp.navigate(server.registerRaw(page(ParityFrames.APP, theme, open)));
                    String[] migrated = transitions(cdp, part.getValue()).split(" \\| ");
                    for (int i = 1; i <= 3; i++) {
                        if (!legacy[i].split(",\\s*")[0].equals(migrated[i].split(",\\s*")[0])) {
                            FINDINGS.add(theme + " " + part.getKey() + " " + List.of("", "duration", "timing", "delay").get(i) + ": " + legacy[i] + " -> " + migrated[i]);
                        }
                    }
                    Set<String> legacyList = new TreeSet<>(Arrays.asList(legacy[0].split(",\\s*")));
                    for (String p : migrated[0].split(",\\s*")) {
                        if (!legacyList.contains(p)) {
                            FINDINGS.add(theme + " " + part.getKey() + ": the migrated rule animates " + p + ", which the legacy one did not");
                        }
                    }
                    MIGRATED.put(theme + " " + part.getKey(), String.join(" | ", migrated));
                    if (theme.equals("light")) {
                        cdp.navigate(server.registerRaw(page(oracle, theme, FloatingBrowser.legacy(closed))));
                        Map<String, String> a = all(cdp, part.getValue());
                        cdp.navigate(server.registerRaw(page(oracle, theme, FloatingBrowser.legacy(open))));
                        Map<String, String> b = all(cdp, part.getValue());
                        Set<String> changed = new TreeSet<>();
                        a.forEach((k, v) -> {
                            if (!v.equals(b.get(k)) && !k.startsWith("--")) {
                                changed.add(k);
                            }
                        });
                        CHANGED.put(part.getKey(), changed);
                    }
                }
            }
            OverlayBrowser.reducedMotion(cdp, true);
            for (Map.Entry<String, String> part : PARTS.entrySet()) {
                cdp.navigate(server.registerRaw(page(oracle, "light", FloatingBrowser.legacy(open))));
                String legacy = transitions(cdp, part.getValue()).split(" \\| ")[0];
                cdp.navigate(server.registerRaw(page(ParityFrames.APP, "light", open)));
                String migrated = transitions(cdp, part.getValue()).split(" \\| ")[0];
                REDUCED.put(part.getKey(), legacy + " / " + migrated);
            }
        }
    }

    @Test
    void durationEasingAndDelayAreTheLegacyOnesExactly() {
        assertEquals(List.of(), FINDINGS);
    }

    @Test
    void theMigratedRulesAnimateWhatChangesBetweenTheStatesAndThePopoversResizeMove() {
        assertEquals(Set.of("opacity", "scale"), CHANGED.get("tooltip bubble"));
        assertEquals(Set.of("opacity", "scale"), CHANGED.get("popover surface"));
        for (String theme : List.of("light", "dark")) {
            assertEquals(CHANGED.get("tooltip bubble"), listed(theme + " tooltip bubble"), theme + " tooltip bubble");
            Set<String> popover = new TreeSet<>(CHANGED.get("popover surface"));
            popover.add("translate");
            assertEquals(popover, listed(theme + " popover surface"), theme + " popover surface");
        }
    }

    private static Set<String> listed(String key) {
        return new TreeSet<>(Arrays.asList(MIGRATED.get(key).split(" \\| ")[0].split(",\\s*")));
    }

    @Test
    void everyAnimatedPropertyHasTheSameDurationAndDelayAndTheDurationIs150ms() {
        MIGRATED.forEach((part, value) -> {
            String[] p = value.split(" \\| ");
            assertTrue(Set.of(p[1].split(",\\s*")).size() == 1 && Set.of(p[3].split(",\\s*")).size() == 1, part + ": " + value);
            assertEquals("0.15s", p[1].split(",\\s*")[0], part);
            assertEquals("0s", p[3].split(",\\s*")[0], part);
        });
    }

    @Test
    void reducedMotionTurnsEveryTransitionOffInBothRenderings() {
        assertEquals(2, REDUCED.size());
        REDUCED.forEach((part, value) -> assertEquals("none / none", value, part));
    }
}
