package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toggle's public customisation properties, read by a real browser from the REAL application stylesheet: {@code --ash-toggle-track-off}, {@code --ash-toggle-track-on},
 * {@code --ash-toggle-thumb-shadow}, {@code --ash-toggle-ring} and {@code --ash-toggle-label-color}. Each is set on an ANCESTOR of the toggle (the component only reads them, so any
 * ancestor works), and the resulting computed style is read in light and dark mode, at rest and hovered, for a checked and an unchecked toggle, with a skin (whose flat thumb shadow is
 * itself set on the root, so a nearer ancestor must win). The properties are also shown to leave the states they do not name alone: an invalid toggle's ring stays red whatever
 * {@code --ash-toggle-ring} says, and a disabled toggle still does not respond to hover.
 */
class ToggleCustomPropertiesTest {

    private static final String RED = "rgb(255, 0, 0)";
    private static final String BLUE = "rgb(0, 0, 255)";
    private static final String GREEN = "rgb(0, 255, 0)";

    private record Probe(String id, String css, Map<String, Object> options, boolean hover, boolean focus) {
    }

    private static final List<Probe> PROBES = List.of(
            new Probe("off", "--ash-toggle-track-off:" + RED, ToggleBrowser.o(), false, false),
            new Probe("off-hover", "--ash-toggle-track-off:" + RED, ToggleBrowser.o(), true, false),
            new Probe("off-checked", "--ash-toggle-track-off:" + RED, ToggleBrowser.o("checked", true), false, false),
            new Probe("on", "--ash-toggle-track-on:" + BLUE, ToggleBrowser.o("checked", true), false, false),
            new Probe("on-hover", "--ash-toggle-track-on:" + BLUE, ToggleBrowser.o("checked", true), true, false),
            new Probe("on-unchecked", "--ash-toggle-track-on:" + BLUE, ToggleBrowser.o(), false, false),
            new Probe("off-disabled-hover", "--ash-toggle-track-off:" + RED, ToggleBrowser.o("disabled", true), true, false),
            new Probe("shadow", "--ash-toggle-thumb-shadow:0 0 0 3px " + GREEN, ToggleBrowser.o(), false, false),
            new Probe("ring", "--ash-toggle-ring:" + GREEN, ToggleBrowser.o(), false, true),
            new Probe("ring-invalid", "--ash-toggle-ring:" + GREEN, ToggleBrowser.o("error", true), false, true),
            new Probe("label", "--ash-toggle-label-color:" + RED, ToggleBrowser.o(), false, false),
            new Probe("none", "", ToggleBrowser.o(), false, false));

    private static final Map<String, Map<String, String>> LIGHT = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> DARK = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> SKIN = new LinkedHashMap<>();

    private static String body() {
        StringBuilder sb = new StringBuilder();
        for (Probe p : PROBES) {
            Map<String, Object> options = new LinkedHashMap<>(p.options());
            options.put("id", "p-" + p.id());
            sb.append("<div style=\"").append(p.css()).append("\">").append(ToggleBrowser.toggle(options)).append("</div>");
        }
        return sb.toString();
    }

    private static String read(String id) {
        String i = "p-" + id;
        return "(function(){var inp=document.getElementById('" + i + "');var track=inp.nextElementSibling;var thumb=track.nextElementSibling;var text=inp.closest('label').lastElementChild;"
                + "return JSON.stringify({bg:getComputedStyle(track).backgroundColor,trackShadow:getComputedStyle(track).boxShadow,thumbShadow:getComputedStyle(thumb).boxShadow,color:getComputedStyle(text).color})})()";
    }

    private static Map<String, Map<String, String>> measure(DevToolsSession cdp, String url) throws Exception {
        cdp.navigate(url);
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        for (Probe p : PROBES) {
            List<String> pseudo = p.hover() ? List.of("hover") : p.focus() ? List.of("focus", "focus-visible") : List.of();
            List<Integer> nodes = List.of();
            if (!pseudo.isEmpty()) {
                nodes = cdp.nodes(p.hover() ? "#p-" + p.id() + " + span" : "#p-" + p.id());
                cdp.force(nodes, pseudo);
            }
            String json = cdp.evaluate(read(p.id()));
            Map<String, String> values = new LinkedHashMap<>();
            for (String key : List.of("bg", "trackShadow", "thumbShadow", "color")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\":\"([^\"]*)\"").matcher(json);
                values.put(key, m.find() ? m.group(1) : "?");
            }
            out.put(p.id(), values);
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, List.of());
            }
        }
        return out;
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("toggle-custom");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String id : List.of("light/none", "dark/none", "dark/axiom")) {
                String url = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), ParityConfigs.get(id), "", body(), ""));
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    Map<String, Map<String, String>> measured = measure(cdp, url);
                    (id.equals("light/none") ? LIGHT : id.equals("dark/none") ? DARK : SKIN).putAll(measured);
                }
            }
        }
    }

    @Test
    void theUncheckedTrackColourReplacesTheTrackInEveryStateAndInBothThemes() {
        for (Map<String, Map<String, String>> theme : List.of(LIGHT, DARK, SKIN)) {
            assertEquals(RED, theme.get("off").get("bg"));
            assertEquals(RED, theme.get("off-hover").get("bg"), "one colour replaces the resting and the hovered one");
            assertEquals(RED, theme.get("off-disabled-hover").get("bg"));
            assertNotEquals(RED, theme.get("off-checked").get("bg"), "the unchecked colour does not touch a checked track");
        }
    }

    @Test
    void theCheckedTrackColourReplacesTheCheckedTrackInEveryStateAndLeavesTheUncheckedOneAlone() {
        for (Map<String, Map<String, String>> theme : List.of(LIGHT, DARK, SKIN)) {
            assertEquals(BLUE, theme.get("on").get("bg"));
            assertEquals(BLUE, theme.get("on-hover").get("bg"));
            assertNotEquals(BLUE, theme.get("on-unchecked").get("bg"));
            assertEquals(theme.get("none").get("bg"), theme.get("on-unchecked").get("bg"), "an unchecked track is what it is without the property");
        }
    }

    @Test
    void theThumbShadowPropertyReplacesTheShadowAndAnAncestorNearerThanASkinWins() {
        for (Map<String, Map<String, String>> theme : List.of(LIGHT, DARK, SKIN)) {
            assertEquals("rgb(0, 255, 0) 0px 0px 0px 3px", theme.get("shadow").get("thumbShadow"));
        }
        assertTrue(LIGHT.get("none").get("thumbShadow").contains("0px 1px 3px 0px"), "the default is Tailwind's shadow-sm: " + LIGHT.get("none").get("thumbShadow"));
        assertEquals("none", TextEntryBrowser.visibleShadow(SKIN.get("none").get("thumbShadow")), "a skin flattens it on the root: " + SKIN.get("none").get("thumbShadow"));
    }

    @Test
    void theRingPropertyColoursTheFocusRingButNotAnInvalidTogglesRedRing() {
        for (Map<String, Map<String, String>> theme : List.of(LIGHT, DARK, SKIN)) {
            String ring = TextEntryBrowser.visibleShadow(theme.get("ring").get("trackShadow"));
            assertEquals("rgb(255, 255, 255) 0px 0px 0px 2px, rgb(0, 255, 0) 0px 0px 0px 4px", ring, "the offset stays white, even in dark mode");
            String invalid = TextEntryBrowser.visibleShadow(theme.get("ring-invalid").get("trackShadow"));
            assertTrue(invalid.startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, ") && invalid.contains("0px 0px 0px 4px") && !invalid.contains("rgb(0, 255, 0)"), invalid);
        }
    }

    @Test
    void theLabelColourPropertyColoursTheTextInBothThemes() {
        for (Map<String, Map<String, String>> theme : List.of(LIGHT, DARK, SKIN)) {
            assertEquals(RED, theme.get("label").get("color"));
            assertNotEquals(RED, theme.get("none").get("color"));
        }
    }

    @Test
    void withoutAnyPropertyTheDefaultsAreTheLegacyColoursAndDarkModeChangesThem() {
        assertEquals("oklch(0.869 0.022 252.894)", LIGHT.get("none").get("bg"));
        assertEquals("oklch(0.446 0.043 257.281)", DARK.get("none").get("bg"));
        assertEquals("oklch(0.372 0.044 257.287)", LIGHT.get("none").get("color"));
        assertEquals("oklch(0.869 0.022 252.894)", DARK.get("none").get("color"));
    }
}
