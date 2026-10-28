package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The harness's own reliability: a large frame collection is measured in several Chrome pages without losing, reordering or
 * mixing up a frame, and a stylesheet that did not load is an explicit infrastructure failure, never a measurement.
 * The first group needs no browser; the second measures real frames.
 */
class ParityFramesTest {

    private static final ParityConfigs.Config LIGHT = ParityConfigs.get("light/none");
    private static final List<String> EXTRA = List.of();

    private static ParitySession session;

    private static ParitySession session() {
        if (session == null) {
            session = ParitySession.open("parity-frames");
        }
        return session;
    }

    /** Frame {@code n}: {@code sheets} stylesheets, and one marked box whose width says which frame it is. */
    private static String frame(int n, int sheets) {
        List<String> links = new ArrayList<>();
        for (int i = 0; i < sheets; i++) {
            links.add("/css/app.css?s=" + i);
        }
        return ParityFrames.frame("f" + n, LIGHT, links, "", "<div data-parity=\"box\" style=\"width:" + (10 + n) + "px;height:5px\"></div>");
    }

    private static String frames(int count, int sheets) {
        StringBuilder all = new StringBuilder();
        for (int n = 0; n < count; n++) {
            all.append(frame(n, sheets));
        }
        return all.toString();
    }

    // ---- splitting (no browser) ----

    @Test
    void aCollectionIsSplitIntoWholeFramesInOrderAndTheBatchesJoinBackToTheCollection() {
        String all = frames(10, 3);
        List<String> batches = ParityFrames.batches(all, 7);
        assertEquals(5, batches.size(), "7 requests fit two frames of 3, so ten frames make five pages");
        assertEquals(all, String.join("", batches));
        for (String batch : batches) {
            long requests = ParityFrames.frameList(batch).stream().mapToInt(f -> ParityFrames.expected(f).hrefs().size()).sum();
            assertTrue(requests <= 7, "a page stays within the cap: " + requests);
        }
        assertEquals(List.of("f0", "f1"), ParityFrames.frameList(batches.get(0)).stream().map(f -> ParityFrames.expected(f).id()).toList());
        assertEquals(List.of("f8", "f9"), ParityFrames.frameList(batches.get(4)).stream().map(f -> ParityFrames.expected(f).id()).toList());
    }

    @Test
    void aCollectionWithinTheCapIsOnePageAsBefore() {
        String all = frames(4, 3);
        assertEquals(List.of(all), ParityFrames.batches(all, 12));
        assertEquals(List.of(all), ParityFrames.batches(all, 1000));
    }

    @Test
    void aFrameLargerThanTheCapStillGetsAPageOfItsOwn() {
        String all = frame(0, 5) + frame(1, 5);
        assertEquals(2, ParityFrames.batches(all, 3).size());
        assertEquals(all, String.join("", ParityFrames.batches(all, 3)));
    }

    @Test
    void theStepperSizedCollectionIsSplitIntoPagesWithinTheDefaultCap() {
        String all = frames(148, 11);
        List<String> batches = ParityFrames.batches(all, ParityFrames.MAX_STYLESHEET_REQUESTS_PER_PAGE);
        assertTrue(batches.size() > 1, "1,628 requests are not one page");
        assertEquals(all, String.join("", batches));
        for (String batch : batches) {
            assertTrue(ParityFrames.frameList(batch).size() * 11 <= ParityFrames.MAX_STYLESHEET_REQUESTS_PER_PAGE);
        }
    }

    @Test
    void theExpectedStylesheetsAreReadFromTheFrameHeadWithTheirEscapedCharactersRestored() {
        String f = ParityFrames.frame("a|b", LIGHT, List.of("/css/one.css?x=1&y=2", "/css/two.css"), "p[title=\"x\"]{}", "<p>body <link rel=\"stylesheet\" href=\"/not-this.css\"></p>");
        ParityFrames.ExpectedFrame e = ParityFrames.expected(f);
        assertEquals("a|b", e.id());
        assertEquals(List.of("/css/one.css?x=1&y=2", "/css/two.css"), e.hrefs());
    }

    // ---- the readiness check (no browser) ----

    private static List<ParityFrames.ExpectedFrame> expectedTwo() {
        return List.of(new ParityFrames.ExpectedFrame("one", List.of("/a.css", "/b.css")), new ParityFrames.ExpectedFrame("two", List.of("/a.css")));
    }

    @Test
    void aReportWithEveryStylesheetLoadedPasses() {
        ParityFrames.verifySheets(expectedTwo(), List.of("sheets\t2", "sheet\t0\tone\t/a.css\tok", "sheet\t0\tone\t/b.css\tok", "sheet\t1\ttwo\t/a.css\tok"), "t");
    }

    @Test
    void aStylesheetThatDidNotLoadNamesTheFrameAndTheStylesheet() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ParityFrames.verifySheets(expectedTwo(),
                List.of("sheets\t2", "sheet\t0\tone\t/a.css\tok", "sheet\t0\tone\t/b.css\tmissing", "sheet\t1\ttwo\t/a.css\tok"), "t"));
        assertTrue(e.getMessage().contains("INFRASTRUCTURE FAILURE"), e.getMessage());
        assertTrue(e.getMessage().contains("frame one: stylesheet /b.css did not load"), e.getMessage());
        assertTrue(!e.getMessage().contains("frame two"), e.getMessage());
    }

    @Test
    void aStylesheetThatIsNotEvenLinkedAFrameThatIsMissingAndAMissingCountAreAllInfrastructureFailures() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ParityFrames.verifySheets(expectedTwo(),
                List.of("sheets\t1", "sheet\t0\tone\t/a.css\tok"), "t"));
        assertTrue(e.getMessage().contains("holds 1 frames, expected 2"), e.getMessage());
        assertTrue(e.getMessage().contains("frame one: stylesheet /b.css is not linked"), e.getMessage());
        assertTrue(e.getMessage().contains("frame two: stylesheet /a.css is not linked"), e.getMessage());
    }

    @Test
    void anUnreadableReportIsAnInfrastructureFailureToo() {
        assertThrows(IllegalStateException.class, () -> ParityFrames.verifySheets(expectedTwo(), List.of("garbage"), "t"));
    }

    // ---- in a real browser ----

    @Test
    void aSplitRunMeasuresEveryFrameAndKeepsEachResultWithItsFrame() throws Exception {
        String all = frames(14, 2);
        ParityProbe.Probe whole = ParityFrames.measure(session(), all, EXTRA, List.of(), "whole", 1000);
        ParityProbe.Probe split = ParityFrames.measure(session(), all, EXTRA, List.of(), "split", 5);
        assertEquals(7, ParityFrames.batches(all, 5).size() , "pages of two frames each");
        assertEquals(14, split.rows().size());
        for (int n = 0; n < 14; n++) {
            ParityProbe.Row row = split.rows().get(n);
            assertEquals("f" + n, row.phase(), "frames come back in order");
            assertEquals((10 + n) + "", split.value(row, "@w").replace(".0", ""), "frame f" + n + " keeps its own box");
        }
        assertEquals(whole.props(), split.props());
        for (int n = 0; n < 14; n++) {
            ParityProbe.Row a = whole.rows().get(n);
            ParityProbe.Row b = split.rows().get(n);
            assertEquals(a.phase(), b.phase());
            assertEquals(a.ordinal(), b.ordinal());
            assertEquals(a.context(), b.context());
            assertEquals(List.of(a.values()), List.of(b.values()), "splitting changes nothing that is measured (" + a.phase() + ")");
        }
    }

    @Test
    void aStylesheetThatCannotLoadStopsTheRunBeforeAnythingIsMeasured() {
        String all = frame(0, 2) + ParityFrames.frame("broken", LIGHT, List.of("/css/app.css", "/css/no-such-sheet.css"), "",
                "<div data-parity=\"box\" style=\"width:30px;height:5px\"></div>") + frame(2, 2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ParityFrames.measure(session(), all, EXTRA, List.of(), "missing sheet", 1000));
        assertTrue(e.getMessage().contains("INFRASTRUCTURE FAILURE"), e.getMessage());
        assertTrue(e.getMessage().contains("frame broken: stylesheet /css/no-such-sheet.css did not load"), e.getMessage());
        assertTrue(!e.getMessage().contains("frame f0") && !e.getMessage().contains("frame f2"), "only the broken frame is named: " + e.getMessage());
    }

    @Test
    void aMissingStylesheetOnALaterPageStopsTheRunAndNamesThatPage() {
        String all = frames(6, 2) + ParityFrames.frame("late", LIGHT, List.of("/css/no-such-sheet.css"), "", "<div data-parity=\"box\"></div>");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ParityFrames.measure(session(), all, EXTRA, List.of(), "late sheet", 4));
        assertTrue(e.getMessage().contains("page 4 of 4"), e.getMessage());
        assertTrue(e.getMessage().contains("frame late: stylesheet /css/no-such-sheet.css"), e.getMessage());
    }
}
