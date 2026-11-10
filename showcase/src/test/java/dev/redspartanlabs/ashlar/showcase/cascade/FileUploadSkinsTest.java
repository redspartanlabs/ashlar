package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the ten skins had to change for File Upload: nothing. The card's radius is Tailwind's {@code rounded-lg}, which was {@code var(--radius-lg)} and is now {@code var(--radius-lg, 0.5rem)},
 * so every skin's own radius token still reaches it; its colours read the palette variables the skins redefine; and the decorative Choose File span is a Button, which each skin already flattens
 * through {@code .ash-btn { --ash-btn-shadow }} (Button's migration). That this is enough is measured, not read: {@link FileUploadParityTest} compares the rendering in all 22
 * configurations and {@link FileUploadPixelTest} compares the pictures. This pins that no skin file names File Upload (so no skin edit was made), that Button's flat-shadow rule each skin
 * relies on is still there, and, in a browser, that the card's radius is exactly each skin's {@code --radius-lg}.
 */
class FileUploadSkinsTest {

    private static final List<String> SKINS = ParityConfigs.SKINS;
    private static final Map<String, String> RADII = new LinkedHashMap<>();

    private static String skin(String name) throws IOException {
        try (InputStream in = FileUploadSkinsTest.class.getResourceAsStream("/static/css/theme-" + name + ".css")) {
            if (in == null) {
                throw new IOException("theme-" + name + ".css is not on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("file-upload-skins");
        String body = FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "fu1")) + "<div id=\"probe\" style=\"width:10px;height:10px;border-radius:var(--radius-lg)\"></div>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String name : SKINS) {
                String url = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), ParityConfigs.get("light/" + name), "", body, ""));
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    cdp.navigate(url);
                    RADII.put(name, cdp.evaluate("(function(){return getComputedStyle(document.querySelector('[data-field=fu1] > label:nth-of-type(2)')).borderTopLeftRadius+'|'+getComputedStyle(document.getElementById('probe')).borderTopLeftRadius})()"));
                }
            }
        }
    }

    @Test
    void noSkinNamesFileUploadSoNoSkinWasEdited() throws IOException {
        assertEquals(10, SKINS.size());
        for (String name : SKINS) {
            String css = skin(name);
            assertFalse(css.contains("ash-file-upload") || css.contains("data-file-upload") || css.contains("--ash-file-upload") || css.contains("file-upload"), name + ": a skin does not style File Upload");
        }
    }

    @Test
    void theButtonsFlatShadowRuleTheChooseFileSpanReliesOnIsStillInEverySkinAndTheTailwindShadowRuleToo() throws IOException {
        for (String name : SKINS) {
            String css = skin(name);
            assertTrue(css.contains("[data-skin=\"" + name + "\"] .ash-btn {\n    --ash-btn-shadow: 0 0 #0000;\n}"), name + ": Button's flat shadow");
            assertTrue(css.contains("[data-skin=\"" + name + "\"] :is(.shadow-xs, .shadow-sm, .shadow, .shadow-md, .shadow-lg, .shadow-xl, .shadow-2xl) {\n    --tw-shadow: 0 0 #0000;\n}"), name);
        }
    }

    @Test
    void everySkinsRadiusTokenReachesTheCard() {
        assertEquals(10, RADII.size());
        for (Map.Entry<String, String> e : RADII.entrySet()) {
            String[] parts = e.getValue().split("\\|");
            assertEquals(parts[1], parts[0], e.getKey() + ": the card's radius is the skin's --radius-lg");
        }
        assertTrue(RADII.values().stream().map(v -> v.split("\\|")[0]).distinct().count() >= 3, "the skins really do differ: " + RADII);
    }
}
