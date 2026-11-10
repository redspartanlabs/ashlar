package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * File Upload's four public customisation properties, read by a real browser from the REAL application stylesheet: {@code --ash-file-upload-border},
 * {@code --ash-file-upload-background}, {@code --ash-file-upload-ring} and {@code --ash-file-upload-status-color}. Each is set on an ANCESTOR of the upload (the component only reads them,
 * so any ancestor works), and the resulting computed style is read in light and dark mode and under a skin, at rest, hovered, disabled-and-hovered, invalid and keyboard-focused. The
 * properties are also shown to leave the states they do not name alone: an invalid card's red border wins over a custom border (the custom background and the hover still apply), the focus
 * ring keeps its white offset in dark mode, and a disabled card still does not respond to hover.
 */
class FileUploadCustomPropertiesTest {

    private static final String RED = "rgb(255, 0, 0)";
    private static final String BLUE = "rgb(0, 0, 255)";
    private static final String GREEN = "rgb(0, 255, 0)";

    private record Probe(String id, String css, Map<String, Object> options, boolean hover, boolean focus) {
    }

    private static final List<Probe> PROBES = List.of(
            new Probe("border", "--ash-file-upload-border:" + RED, FileUploadBrowser.o(), false, false),
            new Probe("border-hover", "--ash-file-upload-border:" + RED, FileUploadBrowser.o(), true, false),
            new Probe("border-disabled-hover", "--ash-file-upload-border:" + RED, FileUploadBrowser.o("disabled", true), true, false),
            new Probe("border-invalid", "--ash-file-upload-border:" + BLUE, FileUploadBrowser.o("error", true, "errMsg", "Error text"), false, false),
            new Probe("border-invalid-hover", "--ash-file-upload-border:" + BLUE, FileUploadBrowser.o("error", true, "errMsg", "Error text"), true, false),
            new Probe("background", "--ash-file-upload-background:" + GREEN, FileUploadBrowser.o(), false, false),
            new Probe("background-hover", "--ash-file-upload-background:" + GREEN, FileUploadBrowser.o(), true, false),
            new Probe("background-invalid-hover", "--ash-file-upload-background:" + GREEN, FileUploadBrowser.o("error", true, "errMsg", "Error text"), true, false),
            new Probe("ring", "--ash-file-upload-ring:" + GREEN, FileUploadBrowser.o(), false, true),
            new Probe("ring-invalid", "--ash-file-upload-ring:" + GREEN, FileUploadBrowser.o("error", true, "errMsg", "Error text"), false, true),
            new Probe("status", "--ash-file-upload-status-color:" + RED, FileUploadBrowser.o(), false, false),
            new Probe("none", "", FileUploadBrowser.o(), false, false),
            new Probe("none-hover", "", FileUploadBrowser.o(), true, false),
            new Probe("none-invalid", "", FileUploadBrowser.o("error", true, "errMsg", "Error text"), false, false));

    private static final Map<String, Map<String, String>> LIGHT = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> DARK = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> SKIN = new LinkedHashMap<>();

    private static String body() {
        StringBuilder sb = new StringBuilder();
        for (Probe p : PROBES) {
            Map<String, Object> options = new LinkedHashMap<>(p.options());
            options.put("id", "p-" + p.id());
            sb.append("<div style=\"").append(p.css()).append("\">").append(FileUploadBrowser.fileUpload(options)).append("</div>");
        }
        return sb.toString();
    }

    private static String zone(String id) {
        return "[data-field=\"p-" + id + "\"] > label:nth-of-type(2)";
    }

    private static String read(String id) {
        return "(function(){var z=document.querySelector('" + zone(id) + "');var s=z.querySelector('[data-file-upload-status]');var c=getComputedStyle(z);"
                + "return JSON.stringify({border:c.borderTopColor,bg:c.backgroundColor,shadow:c.boxShadow,status:getComputedStyle(s).color,cursor:c.cursor})})()";
    }

    private static Map<String, Map<String, String>> measure(DevToolsSession cdp, String url) throws Exception {
        cdp.navigate(url);
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        for (Probe p : PROBES) {
            List<String> pseudo = p.hover() ? List.of("hover") : p.focus() ? List.of("focus", "focus-visible") : List.of();
            List<Integer> nodes = List.of();
            if (!pseudo.isEmpty()) {
                nodes = cdp.nodes(p.hover() ? zone(p.id()) : "#p-" + p.id());
                cdp.force(nodes, pseudo);
            }
            String json = cdp.evaluate(read(p.id()));
            Map<String, String> values = new LinkedHashMap<>();
            for (String key : List.of("border", "bg", "shadow", "status", "cursor")) {
                Matcher m = Pattern.compile("\"" + key + "\":\"([^\"]*)\"").matcher(json);
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
        ParitySession session = ParitySession.open("file-upload-custom");
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

    private static final List<Map<String, Map<String, String>>> THEMES = List.of(LIGHT, DARK, SKIN);

    @Test
    void theBorderColourReplacesTheBorderAtRestHoveredAndDisabledAndHoveredInEveryTheme() {
        for (Map<String, Map<String, String>> theme : THEMES) {
            assertEquals(RED, theme.get("border").get("border"));
            assertEquals(RED, theme.get("border-hover").get("border"), "one colour replaces the resting and the hovered one: a custom border does not change on hover");
            assertEquals(RED, theme.get("border-disabled-hover").get("border"), "a disabled card does not respond to hover");
            assertNotEquals(theme.get("none").get("border"), theme.get("none-hover").get("border"), "without the property, hover does change the border");
        }
    }

    @Test
    void anInvalidCardsRedBorderWinsOverACustomBorderAndOverHover() {
        for (Map<String, Map<String, String>> theme : THEMES) {
            assertNotEquals(BLUE, theme.get("border-invalid").get("border"));
            assertEquals(theme.get("none-invalid").get("border"), theme.get("border-invalid").get("border"), "the invalid red is what it is without the property");
            assertEquals(theme.get("none-invalid").get("border"), theme.get("border-invalid-hover").get("border"), "and it wins over a custom border while hovered");
        }
        assertEquals("oklch(0.704 0.191 22.216)", LIGHT.get("none-invalid").get("border"), "red-400 in light");
        assertEquals("oklch(0.637 0.237 25.331)", DARK.get("none-invalid").get("border"), "red-500 in dark");
    }

    @Test
    void theBackgroundReplacesTheBackgroundAtRestAndHoveredInEveryThemeAndInvalidStillTakesIt() {
        for (Map<String, Map<String, String>> theme : THEMES) {
            assertEquals(GREEN, theme.get("background").get("bg"));
            assertEquals(GREEN, theme.get("background-hover").get("bg"), "a custom background does not change on hover");
            assertEquals(GREEN, theme.get("background-invalid-hover").get("bg"), "an invalid card, hovered, still takes the custom background");
            assertNotEquals(theme.get("none").get("bg"), theme.get("none-hover").get("bg"), "without the property, hover does change the background");
        }
        assertTrue(DARK.get("none").get("bg").startsWith("oklab("), "the dark default is the translucent palette colour: " + DARK.get("none").get("bg"));
        assertEquals(GREEN, DARK.get("background").get("bg"), "a custom background is used exactly as given in dark mode, not halved");
    }

    @Test
    void theRingColoursTheFocusRingAndAnInvalidCardKeepsItsBlueRingColourSoItTakesTheCustomOneToo() {
        for (Map<String, Map<String, String>> theme : THEMES) {
            assertEquals("rgb(255, 255, 255) 0px 0px 0px 2px, rgb(0, 255, 0) 0px 0px 0px 4px", TextEntryBrowser.visibleShadow(theme.get("ring").get("shadow")), "the offset stays white, even in dark mode");
            assertEquals("rgb(255, 255, 255) 0px 0px 0px 2px, rgb(0, 255, 0) 0px 0px 0px 4px", TextEntryBrowser.visibleShadow(theme.get("ring-invalid").get("shadow")),
                    "an invalid focused card keeps the ring (never red) and so takes the custom colour");
            assertEquals("none", TextEntryBrowser.visibleShadow(theme.get("none").get("shadow")), "no ring without keyboard focus");
        }
    }

    @Test
    void theStatusColourColoursTheStatusTextInEveryTheme() {
        for (Map<String, Map<String, String>> theme : THEMES) {
            assertEquals(RED, theme.get("status").get("status"));
            assertNotEquals(RED, theme.get("none").get("status"));
        }
    }

    @Test
    void withoutAnyPropertyTheDefaultsAreTheLegacyColoursAndDarkModeChangesThem() {
        assertEquals("oklch(0.869 0.022 252.894)", LIGHT.get("none").get("border"));
        assertEquals("oklch(0.446 0.043 257.281)", DARK.get("none").get("border"));
        assertEquals("oklch(0.984 0.003 247.858)", LIGHT.get("none").get("bg"));
        assertEquals("oklch(0.372 0.044 257.287)", LIGHT.get("none").get("status"));
        assertEquals("oklch(0.869 0.022 252.894)", DARK.get("none").get("status"));
        assertEquals("not-allowed", LIGHT.get("border-disabled-hover").get("cursor"));
    }
}
