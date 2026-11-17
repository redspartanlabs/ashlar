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
 * Pixel parity: the open Tooltip and Popover, painted. Each case is rendered alone on a page, its trigger in the middle of the viewport, legacy (the rewritten markup on the
 * Tooltip and Popover oracle) and migrated (the markup as served, on the showcase's own stylesheet), in all 22 configurations, and the two screenshots of the viewport must
 * be identical: the comparison is {@link PixelPolicy#EXACT}, unchanged, with no tolerance and no retry. Each page is captured twice and the two captures must be identical
 * before they are compared, so a rendering that is not even stable against itself is reported as such rather than as a difference between the two renderings.
 *
 * <p>The cases: the Tooltip open in each placement (its rotated pointer included), with a label that wraps at the bubble's maximum width, and nudged as tooltip.js nudges it
 * (inline translates on the bubble and the pointer, which the individual translate property must take exactly as the Tailwind one did); the Popover open in each placement
 * with a title (its ring and shadow included), and without a title. Computed-style parity of every part in every state is {@link FloatingParityTest}'s.
 */
class FloatingPixelTest {

    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final List<String> UNSTABLE = new ArrayList<>();
    private static final Map<String, String> SAMPLE = new LinkedHashMap<>();
    private static int compared;

    private static final Pattern DATA = Pattern.compile("\"data\":\"([^\"]+)\"");

    private record Shot(String key, String markup) {
    }

    private static final String BODY = "<p style=\"margin:0\">The popover's own content, a sentence long enough to wrap onto a second line.</p>";

    private static List<Shot> shots() {
        List<Shot> shots = new ArrayList<>();
        for (String p : FloatingBrowser.PLACEMENTS) {
            shots.add(new Shot("tooltip " + p.toLowerCase(), FloatingBrowser.tooltip(FloatingBrowser.trigger("t", p), FloatingBrowser.SHORT_TIP, p)));
        }
        shots.add(new Shot("tooltip top, wrapping label", FloatingBrowser.tooltip(FloatingBrowser.trigger("t", "Long"), FloatingBrowser.LONG_TIP, "Top")));
        shots.add(new Shot("tooltip top, nudged", FloatingBrowser.nudged(FloatingBrowser.tooltip(FloatingBrowser.trigger("t", "Top"), FloatingBrowser.SHORT_TIP, "Top"))));
        for (String p : FloatingBrowser.PLACEMENTS) {
            shots.add(new Shot("popover " + p.toLowerCase() + " with title", FloatingBrowser.popover(FloatingBrowser.trigger("p", p), BODY, "A title", p)));
        }
        shots.add(new Shot("popover bottom without title", FloatingBrowser.popover(FloatingBrowser.trigger("p", "Plain"), BODY, null, null)));
        return shots;
    }

    private static String capture(DevToolsSession cdp) throws Exception {
        Matcher m = DATA.matcher(cdp.call("Page.captureScreenshot", "{\"format\":\"png\"}"));
        if (!m.find()) {
            throw new IllegalStateException("no screenshot data");
        }
        return m.group(1);
    }

    /** The page's picture, captured twice; null (and the case recorded as unstable) when the two captures differ. */
    private static String picture(DevToolsSession cdp, String url, String where) throws Exception {
        cdp.navigate(url);
        Thread.sleep(50);
        String first = capture(cdp);
        String second = capture(cdp);
        if (!first.equals(second)) {
            UNSTABLE.add(where + ": " + PixelDiagnostics.measure(first, second).describe());
            return null;
        }
        return first;
    }

    private static String centered(String html) {
        return "<div style=\"position:absolute;left:420px;top:300px\">" + html + "</div>";
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("floating-pixels");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 900, 700);
                    for (Shot s : shots()) {
                        String open = OverlayBrowser.withoutScripts(FloatingBrowser.inState(s.markup(), true, true));
                        String legacyMarkup = FloatingBrowser.legacy(open);
                        if (legacyMarkup.contains("ash-tooltip") || legacyMarkup.contains("ash-popover")) {
                            throw new IllegalStateException("the legacy rendering must carry no migrated class: " + s.key());
                        }
                        String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("floating-oracle.css")), config, "", centered(legacyMarkup), ""));
                        String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", centered(open), ""));
                        String where = config.id() + " " + s.key();
                        String legacy = picture(cdp, legacyUrl, where + " (legacy)");
                        String migrated = picture(cdp, migratedUrl, where + " (migrated)");
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
    void everyOpenTooltipAndPopoverPaintsTheSameAsTheLegacyOneInEveryConfiguration() {
        assertEquals(List.of(), UNSTABLE, "a rendering that differs from itself cannot be compared");
        assertEquals(List.of(), MISMATCHES);
    }

    @Test
    void theComparisonIsNotVacuous() {
        assertEquals(22 * shots().size(), compared, "every case in every configuration was compared (an unstable rendering is not)");
        // The components really are in the pictures: the placements differ, the nudge moves the bubble, the title is drawn.
        assertNotEquals(SAMPLE.get("tooltip top"), SAMPLE.get("tooltip bottom"));
        assertNotEquals(SAMPLE.get("tooltip top"), SAMPLE.get("tooltip top, nudged"));
        assertNotEquals(SAMPLE.get("popover bottom with title"), SAMPLE.get("popover bottom without title"));
        assertTrue(SAMPLE.size() == shots().size(), "a sample of every case");
    }
}
