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
 * Motion: the legacy menu and chevron (Tailwind's transition and transition-transform utilities, duration-150, ease-out) against the migrated ones, with transitions on.
 * Duration, easing and delay are the legacy ones exactly; the migrated rules animate exactly what changes between the closed and the open state (the menu's opacity and
 * scale, the chevron's rotate), which the legacy lists included among their twenty-three and four properties; and under prefers-reduced-motion: reduce neither rendering
 * has a transition left (motion-reduce:transition-none).
 */
class DropdownMenuMotionTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, String> MIGRATED = new LinkedHashMap<>();
    private static final Map<String, Set<String>> CHANGED = new LinkedHashMap<>();
    private static final Map<String, String> REDUCED = new LinkedHashMap<>();

    private static final Map<String, String> PARTS = Map.of("menu", "[data-dropdown-menu-list]", "chevron", "[data-dropdown-menu-chevron]");

    private static String body(boolean open) {
        return OverlayBrowser.withoutScripts(DropdownMenuBrowser.inState(DropdownMenuBrowser.menu("Actions", null, DropdownMenuBrowser.ACTIONS), true, open));
    }

    private static String page(String sheet, String theme, String body) {
        return "<!doctype html><html data-theme=\"" + theme + "\"><head><meta charset=\"utf-8\"><link rel=\"stylesheet\" href=\"" + sheet + "\"></head>"
                + "<body style=\"margin:100px 300px\">" + body + "</body></html>";
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
        ParitySession session = ParitySession.open("dropdown-menu-motion");
        String closed = body(false);
        String open = body(true);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 1200, 800);
            String oracle = ParityFrames.generated("dropdown-menu-oracle.css");
            for (String theme : List.of("light", "dark")) {
                for (Map.Entry<String, String> part : PARTS.entrySet()) {
                    cdp.navigate(server.registerRaw(page(oracle, theme, DropdownMenuBrowser.legacy(open))));
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
                        cdp.navigate(server.registerRaw(page(oracle, theme, DropdownMenuBrowser.legacy(closed))));
                        Map<String, String> a = all(cdp, part.getValue());
                        cdp.navigate(server.registerRaw(page(oracle, theme, DropdownMenuBrowser.legacy(open))));
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
                cdp.navigate(server.registerRaw(page(oracle, "light", DropdownMenuBrowser.legacy(open))));
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
    void theMigratedRulesAnimateExactlyWhatChangesBetweenTheStates() {
        assertEquals(Set.of("opacity", "scale"), CHANGED.get("menu"));
        assertEquals(Set.of("rotate"), CHANGED.get("chevron"));
        for (String theme : List.of("light", "dark")) {
            for (String part : PARTS.keySet()) {
                assertEquals(CHANGED.get(part), new TreeSet<>(Arrays.asList(MIGRATED.get(theme + " " + part).split(" \\| ")[0].split(",\\s*"))), theme + " " + part);
            }
        }
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
