package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pixel parity: the open Modal and Drawer, painted. Each case is rendered alone on a page (the overlay covers the whole viewport: backdrop, surface, shadow, rules, text, the
 * close icon), legacy (the rewritten markup on the Modal and Drawer oracle) and migrated (the markup as served, on the showcase's own stylesheet), in all 22 configurations,
 * and the two screenshots of the viewport must be identical: the comparison is {@link PixelPolicy#EXACT}, unchanged, with no tolerance and no retry. Each page is captured
 * twice and the two captures must be identical before they are compared, so a rendering that is not even stable against itself is reported as such rather than as a
 * difference between the two renderings.
 *
 * <p>The cases: the small dialog with a footer, the large dialog, a dialog without a close button, the right drawer with a footer, the left drawer, and the small dialog with its
 * close button keyboard-focused (the ring). Computed-style parity of every part in every state is {@link OverlayParityTest}'s; this suite shows the paint agrees too.
 */
class OverlayPixelTest {

    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final List<String> UNSTABLE = new ArrayList<>();
    private static final Map<String, String> SAMPLE = new LinkedHashMap<>();
    private static int compared;

    private static final Pattern DATA = Pattern.compile("\"data\":\"([^\"]+)\"");

    private record Shot(String key, String markup, boolean ring) {
    }

    private static final String BODY = "<p>The body of the dialog, a sentence long enough to wrap onto a second line inside the narrowest dialog.</p>";
    private static final String FOOTER = "<span>Cancel</span><span>Continue</span>";

    private static List<Shot> shots() {
        return List.of(
                new Shot("modal small with footer", OverlayBrowser.modal("p", "Confirm the change", OverlayBrowser.o("size", "Small"), BODY, FOOTER), false),
                new Shot("modal large", OverlayBrowser.modal("p", "A large dialog", OverlayBrowser.o("size", "Large"), BODY, null), false),
                new Shot("modal without close button", OverlayBrowser.modal("p", "No close button", OverlayBrowser.o("showCloseButton", false), BODY, FOOTER), false),
                new Shot("drawer right with footer", OverlayBrowser.drawer("p", "Filters", null, BODY, FOOTER), false),
                new Shot("drawer left", OverlayBrowser.drawer("p", "Menu", "Left", BODY, null), false),
                new Shot("modal small, close button keyboard-focused", OverlayBrowser.modal("p", "Confirm the change", OverlayBrowser.o("size", "Small"), BODY, FOOTER), true));
    }

    private static String capture(DevToolsSession cdp) throws Exception {
        Matcher m = DATA.matcher(cdp.call("Page.captureScreenshot", "{\"format\":\"png\"}"));
        if (!m.find()) {
            throw new IllegalStateException("no screenshot data");
        }
        return m.group(1);
    }

    /** The page's picture, captured twice; null (and the case recorded as unstable) when the two captures differ. */
    private static String picture(DevToolsSession cdp, String url, Shot s, String where) throws Exception {
        cdp.navigate(url);
        List<Integer> nodes = List.of();
        if (s.ring()) {
            nodes = cdp.nodes("[aria-label=\"Close\"]");
            cdp.force(nodes, List.of("focus", "focus-visible"));
        }
        Thread.sleep(50);
        String first = capture(cdp);
        String second = capture(cdp);
        if (s.ring()) {
            cdp.force(nodes, List.of());
        }
        if (!first.equals(second)) {
            UNSTABLE.add(where + ": " + PixelDiagnostics.measure(first, second).describe());
            return null;
        }
        return first;
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("overlay-pixels");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 900, 700);
                    for (Shot s : shots()) {
                        String open = OverlayBrowser.withoutScripts(OverlayBrowser.inState(s.markup(), true, true));
                        String legacyMarkup = OverlayBrowser.legacy(open);
                        if (legacyMarkup.contains("ash-overlay") || legacyMarkup.contains("ash-modal") || legacyMarkup.contains("ash-drawer")) {
                            throw new IllegalStateException("the legacy rendering must carry no migrated class: " + s.key());
                        }
                        String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("overlay-oracle.css")), config, "", legacyMarkup, ""));
                        String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", open, ""));
                        String where = config.id() + " " + s.key();
                        String legacy = picture(cdp, legacyUrl, s, where + " (legacy)");
                        String migrated = picture(cdp, migratedUrl, s, where + " (migrated)");
                        if (legacy == null || migrated == null) {
                            continue;
                        }
                        compared++;
                        PixelPolicy.Verdict verdict = PixelPolicy.compare(legacy, migrated, PixelPolicy.EXACT, null);
                        if (!verdict.accepted()) {
                            MISMATCHES.add(where + ": " + PixelDiagnostics.measure(legacy, migrated).describe());
                        }
                        if (config.id().equals("light/none")) {
                            SAMPLE.put(s.key(), legacy);
                        }
                    }
                }
            }
        }
    }

    @Test
    void everyOpenOverlayPaintsTheSameAsTheLegacyOneInEveryConfiguration() {
        assertEquals(List.of(), UNSTABLE, "a rendering that differs from itself cannot be compared");
        assertEquals(List.of(), MISMATCHES);
    }

    @Test
    void theComparisonIsNotVacuous() {
        assertEquals(22 * shots().size(), compared, "every case in every configuration was compared (an unstable rendering is not)");
        // The ring and the overlays really are in the pictures: the focused and unfocused dialog differ, and the two drawers differ.
        assertNotEquals(SAMPLE.get("modal small with footer"), SAMPLE.get("modal small, close button keyboard-focused"));
        assertNotEquals(SAMPLE.get("drawer right with footer"), SAMPLE.get("drawer left"));
        assertTrue(SAMPLE.size() == shots().size(), "a sample of every case");
    }
}
