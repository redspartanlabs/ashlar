package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toggle's motion, measured on pages that DO run transitions (the other parity pages switch them off so a state is measured at rest). In a real browser, for the legacy rendering and the
 * migrated one, each in its own browser: the computed transition properties, durations, easing and delay of the track and the thumb; then a real state change (the input is clicked), which
 * must start exactly one {@code translate} transition on the thumb and one {@code background-color} transition on the track, each 150ms with the standard ease; the animation is then
 * paused halfway (75ms) and the thumb's computed {@code translate} and the track's colour read, so that the thumb is shown to MOVE (strictly between its two ends) and to be in the same
 * place as the legacy one at the same moment; and finally the same under {@code prefers-reduced-motion: reduce}, where nothing may animate and the thumb is at its end at once.
 */
class ToggleMotionTest {

    private static final String PROBE = """
            (function () {
              var inp = document.getElementById('tm1');
              var track = inp.nextElementSibling;
              var thumb = track.nextElementSibling;
              var out = {};
              var cs = function (e) { var s = getComputedStyle(e); return [s.transitionProperty, s.transitionDuration, s.transitionTimingFunction, s.transitionDelay].join(' | '); };
              out.track = cs(track);
              out.thumb = cs(thumb);
              out.start = getComputedStyle(thumb).translate;
              out.startTrack = getComputedStyle(track).backgroundColor;
              inp.click();
              out.endImmediately = getComputedStyle(thumb).translate;
              var moving = thumb.getAnimations().filter(function (a) { return a.transitionProperty === 'translate'; });
              var fading = track.getAnimations().filter(function (a) { return a.transitionProperty === 'background-color'; });
              out.thumbAnimations = moving.length;
              out.trackAnimations = fading.length;
              out.otherAnimations = thumb.getAnimations().length + track.getAnimations().length - moving.length - fading.length;
              if (moving.length) {
                var t = moving[0].effect.getTiming();
                out.thumbTiming = t.duration + ' ' + t.easing + ' ' + t.delay;
                moving[0].pause(); moving[0].currentTime = 0; out.thumbAt0 = getComputedStyle(thumb).translate;
                moving[0].currentTime = 75; out.thumbAt75 = getComputedStyle(thumb).translate;
                moving[0].currentTime = 150; out.thumbAt150 = getComputedStyle(thumb).translate;
              }
              if (fading.length) {
                var u = fading[0].effect.getTiming();
                out.trackTiming = u.duration + ' ' + u.easing + ' ' + u.delay;
                fading[0].pause(); fading[0].currentTime = 0; out.trackAt0 = getComputedStyle(track).backgroundColor;
                fading[0].currentTime = 75; out.trackAt75 = getComputedStyle(track).backgroundColor;
                fading[0].currentTime = 150; out.trackAt150 = getComputedStyle(track).backgroundColor;
              }
              return JSON.stringify(out);
            })()
            """;

    private static final String BODY = ToggleBrowser.toggle(ToggleBrowser.o("id", "tm1", "label", "Motion"));

    private static String legacy;
    private static String migrated;
    private static String legacyReduced;
    private static String migratedReduced;

    /** A parity page with the shared "no transitions" rule taken out. */
    private static String livePage(List<String> sheets, String body) {
        String page = TextEntryBrowser.page(sheets, ParityConfigs.get("light/none"), "", body, "");
        String off = "<style>*,*::before,*::after{transition:none !important;animation-play-state:paused !important}</style>";
        assertTrue(page.contains(off), "the shared page rule that switches transitions off is expected here, and is removed");
        return page.replace(off, "");
    }

    private static String probe(ParitySession session, String url, boolean reduced) throws Exception {
        try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            if (reduced) {
                cdp.call("Emulation.setEmulatedMedia", "{\"features\":[{\"name\":\"prefers-reduced-motion\",\"value\":\"reduce\"}]}");
            }
            cdp.navigate(url);
            return cdp.evaluate(PROBE);
        }
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("toggle-motion");
        String legacyBody = new TextEntryLegacyMarkup().rewrite(new ChoiceLegacyMarkup().rewrite(new ToggleLegacyMarkup().rewrite(BODY)));
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(livePage(List.of(ParityFrames.generated("toggle-oracle.css")), legacyBody));
            String migratedUrl = server.registerRaw(livePage(List.of(ParityFrames.APP), BODY));
            legacy = probe(session, legacyUrl, false);
            migrated = probe(session, migratedUrl, false);
            legacyReduced = probe(session, legacyUrl, true);
            migratedReduced = probe(session, migratedUrl, true);
        }
    }

    private static String value(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\":\"([^\"]*)\"").matcher(json);
        if (!m.find()) {
            throw new AssertionError("no " + key + " in " + json);
        }
        return m.group(1);
    }

    private static double px(String s) {
        return Double.parseDouble(s.replace("px", ""));
    }

    @Test
    void theMigratedAndTheLegacyTogglesTransitionIdenticallyAtEveryPointOfTheMovement() {
        assertEquals(legacy, migrated);
    }

    @Test
    void theComputedTransitionsAreTheMeasuredOnes() {
        for (String json : List.of(legacy, migrated)) {
            assertEquals("color, background-color, border-color, outline-color, text-decoration-color, fill, stroke, --tw-gradient-from, --tw-gradient-via, --tw-gradient-to | 0.15s | cubic-bezier(0.4, 0, 0.2, 1) | 0s",
                    value(json, "track"));
            assertEquals("transform, translate, scale, rotate | 0.15s | cubic-bezier(0.4, 0, 0.2, 1) | 0s", value(json, "thumb"));
        }
    }

    @Test
    void aStateChangeStartsOneThumbTransitionAndOneTrackTransitionOf150MillisecondsAndNothingElseMoves() {
        for (String json : List.of(legacy, migrated)) {
            assertTrue(json.contains("\"thumbAnimations\":1") && json.contains("\"trackAnimations\":1") && json.contains("\"otherAnimations\":0"), json);
            assertEquals("150 cubic-bezier(0.4, 0, 0.2, 1) 0", value(json, "thumbTiming"));
            assertEquals("150 cubic-bezier(0.4, 0, 0.2, 1) 0", value(json, "trackTiming"));
        }
    }

    @Test
    void theThumbReallyMovesTwentyPixelsAndIsStrictlyBetweenItsEndsHalfwayThrough() {
        for (String json : List.of(legacy, migrated)) {
            assertEquals("none", value(json, "start"));
            assertTrue(List.of("0px", "none").contains(value(json, "thumbAt0")), value(json, "thumbAt0"));
            assertEquals("20px", value(json, "thumbAt150"));
            double mid = px(value(json, "thumbAt75"));
            assertTrue(mid > 2 && mid < 18, "halfway through the thumb is between its ends: " + mid);
            assertNotEquals(value(json, "trackAt0"), value(json, "trackAt150"), "the track changes colour");
            assertNotEquals(value(json, "trackAt75"), value(json, "trackAt0"));
            assertNotEquals(value(json, "trackAt75"), value(json, "trackAt150"));
        }
    }

    @Test
    void underReducedMotionNothingTransitionsAndTheThumbIsAtItsEndAtOnce() {
        assertEquals(legacyReduced, migratedReduced);
        for (String json : List.of(legacyReduced, migratedReduced)) {
            assertEquals("none | 0.15s | cubic-bezier(0.4, 0, 0.2, 1) | 0s", value(json, "track"));
            assertEquals("none | 0.15s | cubic-bezier(0.4, 0, 0.2, 1) | 0s", value(json, "thumb"));
            assertEquals("20px", value(json, "endImmediately"), "the thumb is at its end at once");
            assertTrue(json.contains("\"thumbAnimations\":0") && json.contains("\"trackAnimations\":0") && json.contains("\"otherAnimations\":0"), json);
        }
    }
}
