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
 * The motion of the backdrop, the dialog and the panel, legacy against migrated, with transitions ON (the parity suites measure every state at rest).
 *
 * <ul>
 *   <li>Duration, easing and delay are compared exactly, for each animated element, in both themes.</li>
 *   <li>The transition PROPERTY differs by design: Tailwind's transition utility listed twenty-three properties and transition-transform four, of which only a few ever change.
 *       The migrated rules list exactly the properties that change. Which properties change is MEASURED here, from the legacy rendering itself: every property of the full
 *       computed style whose value differs between the closed and the open state. The migrated list must be exactly that set, so no change goes un-animated and nothing
 *       else is listed. Every migrated property is also in the legacy list (nothing new is animated).</li>
 *   <li>Every migrated property has the same duration and delay, which is what lets overlay.js treat the exit as over when all of them are.</li>
 *   <li>Under prefers-reduced-motion: reduce, both renderings compute transition-property: none.</li>
 * </ul>
 */
class OverlayMotionTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, String> MIGRATED_PROPERTY = new LinkedHashMap<>();
    private static final Map<String, Set<String>> CHANGED = new LinkedHashMap<>();
    private static final Map<String, String> REDUCED = new LinkedHashMap<>();

    private static final List<String> PARTS = List.of("modal backdrop", "modal dialog", "drawer backdrop", "drawer panel");

    private static String selector(String part) {
        return switch (part) {
            case "modal backdrop" -> "[data-modal-backdrop]";
            case "modal dialog" -> "[data-modal-dialog]";
            case "drawer backdrop" -> "[data-drawer-backdrop]";
            case "drawer panel" -> "[data-drawer-panel]";
            default -> throw new IllegalArgumentException(part);
        };
    }

    private static String page(String sheet, String theme, String body) {
        return OverlayBrowser.page(List.of(sheet), theme, body, "");
    }

    private static String body(boolean open) throws Exception {
        String modal = OverlayBrowser.withoutScripts(OverlayBrowser.inState(OverlayBrowser.modal("mm", "Motion", OverlayBrowser.o(), "<p>Body</p>", null), true, open));
        String drawer = OverlayBrowser.withoutScripts(OverlayBrowser.inState(OverlayBrowser.drawer("md", "Motion", null, "<p>Body</p>", null), true, open));
        return modal + drawer;
    }

    private static String transitions(DevToolsSession cdp, String part) throws Exception {
        return cdp.evaluate("(function(){var s=getComputedStyle(document.querySelector('" + selector(part) + "'));return [s.transitionProperty,s.transitionDuration,"
                + "s.transitionTimingFunction,s.transitionDelay].join(' | ')})()");
    }

    private static Map<String, String> all(DevToolsSession cdp, String part) throws Exception {
        String dump = cdp.evaluate("(function(){var s=getComputedStyle(document.querySelector('" + selector(part) + "'));var o=[];for(var i=0;i<s.length;i++){o.push(s[i]+'='+s.getPropertyValue(s[i]))}"
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
        ParitySession session = ParitySession.open("overlay-motion");
        String closed = body(false);
        String open = body(true);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 900, 700);
            String oracle = ParityFrames.generated("overlay-oracle.css");
            for (String theme : List.of("light", "dark")) {
                for (String part : PARTS) {
                    cdp.navigate(server.registerRaw(page(oracle, theme, OverlayBrowser.legacy(open))));
                    String[] legacy = transitions(cdp, part).split(" \\| ");
                    cdp.navigate(server.registerRaw(page(ParityFrames.APP, theme, open)));
                    String[] migrated = transitions(cdp, part).split(" \\| ");
                    for (int i = 1; i <= 3; i++) {
                        if (!legacy[i].equals(migrated[i])) {
                            FINDINGS.add(theme + " " + part + " " + List.of("", "duration", "timing", "delay").get(i) + ": " + legacy[i] + " -> " + migrated[i]);
                        }
                    }
                    Set<String> legacyList = new TreeSet<>(Arrays.asList(legacy[0].split(",\\s*")));
                    for (String p : migrated[0].split(",\\s*")) {
                        if (!legacyList.contains(p)) {
                            FINDINGS.add(theme + " " + part + ": the migrated rule animates " + p + ", which the legacy one did not");
                        }
                    }
                    MIGRATED_PROPERTY.put(theme + " " + part, migrated[0] + " | " + migrated[1] + " | " + migrated[3]);
                    if (theme.equals("light")) {
                        // What changes between the two states, measured on the legacy rendering.
                        cdp.navigate(server.registerRaw(page(oracle, theme, OverlayBrowser.legacy(closed))));
                        Map<String, String> a = all(cdp, part);
                        cdp.navigate(server.registerRaw(page(oracle, theme, OverlayBrowser.legacy(open))));
                        Map<String, String> b = all(cdp, part);
                        Set<String> changed = new TreeSet<>();
                        a.forEach((k, v) -> {
                            if (!v.equals(b.get(k)) && !k.startsWith("--")) {
                                changed.add(k);
                            }
                        });
                        CHANGED.put(part, changed);
                    }
                }
            }
            OverlayBrowser.reducedMotion(cdp, true);
            for (String part : PARTS) {
                cdp.navigate(server.registerRaw(page(oracle, "light", OverlayBrowser.legacy(open))));
                String legacy = transitions(cdp, part).split(" \\| ")[0];
                cdp.navigate(server.registerRaw(page(ParityFrames.APP, "light", open)));
                String migrated = transitions(cdp, part).split(" \\| ")[0];
                REDUCED.put(part, legacy + " / " + migrated);
            }
        }
    }

    @Test
    void durationEasingAndDelayAreTheLegacyOnesExactly() {
        assertEquals(List.of(), FINDINGS);
    }

    @Test
    void theMigratedRulesAnimateExactlyThePropertiesThatChangeBetweenTheStates() {
        assertEquals(Set.of("opacity"), CHANGED.get("modal backdrop"));
        assertEquals(Set.of("opacity", "scale", "translate"), CHANGED.get("modal dialog"));
        assertEquals(Set.of("opacity"), CHANGED.get("drawer backdrop"));
        assertEquals(Set.of("translate"), CHANGED.get("drawer panel"));
        for (String theme : List.of("light", "dark")) {
            for (String part : PARTS) {
                Set<String> listed = new TreeSet<>(Arrays.asList(MIGRATED_PROPERTY.get(theme + " " + part).split(" \\| ")[0].split(",\\s*")));
                assertEquals(CHANGED.get(part), listed, theme + " " + part);
            }
        }
    }

    @Test
    void everyAnimatedPropertyOfAnElementHasTheSameDurationAndDelay() {
        MIGRATED_PROPERTY.forEach((part, value) -> {
            String[] p = value.split(" \\| ");
            assertTrue(Set.of(p[1].split(",\\s*")).size() == 1 && Set.of(p[2].split(",\\s*")).size() == 1, part + ": " + value);
            assertEquals("0.2s", p[1].split(",\\s*")[0], part);
        });
    }

    @Test
    void reducedMotionTurnsEveryTransitionOffInBothRenderings() {
        REDUCED.forEach((part, value) -> assertEquals("none / none", value, part));
    }
}
