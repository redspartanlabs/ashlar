package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The retry of {@link TablePixelTest} exercised for real, with deliberate defects (issue #34). Every control captures in fresh browsers through {@link TablePixelTest#replayBlock}
 * (the case at its own position in its block), is judged by the unchanged {@link PixelPolicy}, and is resolved by the same {@link TablePixelRetry.Ledger} and
 * {@link TablePixelNoiseEnvelopes#check} as TablePixelTest, with a ledger and a budget of its own: nothing here touches TablePixelTest's comparisons, counts or budget.
 *
 * <ul>
 * <li>R1-R4: a pixel recolored inside a measured envelope, in the first capture AND in the retry. The color equals neither documented color there, so the defect is NOT eligible under
 * the production table; under a control table that documents exactly the injected pairs it is eligible, is retried, and must FAIL after the retry.</li>
 * <li>F1: a pixel recolored in the first capture only, on a case where no instability was ever seen, under a control table documenting exactly that pair: it must RECOVER, using a
 * slot of its own budget.</li>
 * <li>E1-E3: defects outside every envelope: NOT eligible, and no retry browser is started.</li>
 * <li>Budget: three eligible rejections in one ledger: the first two are retried, the third is over budget and no browser is started for it.</li>
 * </ul>
 * A defect pixel is painted by a fixed-position element; picture coordinates are the field's (at 8,8 in the page) minus 6, so a picture pixel (x, y) is the page pixel (x+2, y+2).
 * Every injected pixel is checked in the pictures, so a misplaced defect fails the control instead of passing it.
 */
class TablePixelRetryMechanismTest {

    private static final String CLEAR_HOVER = "clear selection hover";
    private static final String CLEAR_FOCUS = "clear selection focus-visible";
    private static final String SORT_HOVER = "sort button hover";
    private static final String TWO_SELECTED = "selectable, two selected";
    private static final int F1_X = 25;
    private static final int F1_Y = 35;
    private static final int F1_COLOR = 0xFF00FF;

    private static ParitySession session;
    private static CascadeFixtureServer server;
    private static List<TableParityTest.Field> fields;

    @BeforeAll
    static void open() throws Exception {
        session = ParitySession.open("table-pixel-retry-mechanism");
        fields = TableParityTest.fields();
        server = new CascadeFixtureServer(session.generatedDir());
    }

    @AfterAll
    static void close() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    /** The real replay, counting the browsers it starts. */
    private static final class CountingReplay implements TablePixelRetry.Replay {
        private final UnaryOperator<String> migratedPage;
        private int launches;

        CountingReplay(UnaryOperator<String> migratedPage) {
            this.migratedPage = migratedPage;
        }

        @Override
        public TablePixelRetry.Capture replay(TablePixelRetry.Comparison c) throws Exception {
            launches++;
            return TablePixelTest.replayBlock(session, server, fields, c.spec(), migratedPage);
        }
    }

    private static TableParityTest.Field field(String caseKey) {
        return fields.stream().filter(f -> f.spec().key().equals(caseKey)).findFirst().orElseThrow();
    }

    private static TablePixelRetry.ReplaySpec spec(String configId, TableParityTest.Field f) {
        int index = fields.indexOf(f);
        int blockStart = index / TablePixelTest.CHUNK * TablePixelTest.CHUNK;
        return new TablePixelRetry.ReplaySpec(configId, List.of(ParityFrames.generated("table-oracle.css")), List.of(ParityFrames.APP), null, blockStart, index - blockStart);
    }

    /** One opaque pixel of the given color at picture coordinates (x, y), on the compared case's migrated page only. */
    private static UnaryOperator<String> pixel(int x, int y, int rgb) {
        return css(String.format("body::after{content:\"\";position:fixed;left:%dpx;top:%dpx;width:1px;height:1px;background:#%06X;z-index:2147483647;pointer-events:none}",
                x + 2, y + 2, rgb));
    }

    private static UnaryOperator<String> css(String rule) {
        return page -> page.replace("</head>", "<style id=\"retry-control\">" + rule + "</style></head>");
    }

    /** A fresh-browser first capture of the case (at its own position in its block), judged and recorded in the ledger as the run with Tailwind would record it. */
    private static TablePixelRetry.Comparison firstCapture(TablePixelRetry.Ledger ledger, String configId, String caseKey, UnaryOperator<String> migratedPage) throws Exception {
        TableParityTest.Field f = field(caseKey);
        TablePixelRetry.ReplaySpec s = spec(configId, f);
        TablePixelRetry.Capture first = TablePixelTest.replayBlock(session, server, fields, s, migratedPage);
        PixelPolicy.Verdict verdict = PixelPolicy.compare(first.legacyPng(), first.migratedPng(), PixelPolicy.forCase(caseKey), first.legacyGeometry());
        assertFalse(verdict.accepted(), configId + " " + caseKey + ": the injected defect must be rejected by the policy in the first capture: " + verdict.describe());
        TablePixelRetry.Comparison c = new TablePixelRetry.Comparison(ledger.size(), TablePixelNoiseEnvelopes.Mode.WITH_TAILWIND, configId, f.id(), caseKey,
                "control " + configId + " " + f.id() + " (" + caseKey + ")", verdict, first, s);
        ledger.record(c);
        return c;
    }

    private static int rgb(String png, int x, int y) throws Exception {
        return PixelDiagnostics.decode(png).getRGB(x, y) & 0xFFFFFF;
    }

    /** A copy of the production table in which one envelope's pixel also documents the injected color against each documented color there (the channel limit is unchanged). */
    private static TablePixelNoiseEnvelopes.Table withInjectedPairs(String name, String configId, String fieldId, TablePixelNoiseEnvelopes.Point point, int injected) {
        List<TablePixelNoiseEnvelopes.Envelope> envelopes = new ArrayList<>();
        boolean found = false;
        for (TablePixelNoiseEnvelopes.Envelope e : TablePixelNoiseEnvelopes.PRODUCTION.envelopes()) {
            if (!e.configId().equals(configId) || !e.fieldId().equals(fieldId)) {
                envelopes.add(e);
                continue;
            }
            found = true;
            Map<TablePixelNoiseEnvelopes.Point, TablePixelNoiseEnvelopes.PixelRule> pixels = new HashMap<>(e.pixels());
            TablePixelNoiseEnvelopes.PixelRule rule = pixels.get(point);
            Set<TablePixelNoiseEnvelopes.ColorPair> pairs = new HashSet<>(rule.pairs());
            for (TablePixelNoiseEnvelopes.ColorPair documented : rule.pairs()) {
                pairs.add(TablePixelNoiseEnvelopes.ColorPair.of(injected, documented.low()));
                pairs.add(TablePixelNoiseEnvelopes.ColorPair.of(injected, documented.high()));
            }
            pixels.put(point, new TablePixelNoiseEnvelopes.PixelRule(rule.channelLimit(), pairs));
            envelopes.add(new TablePixelNoiseEnvelopes.Envelope(e.fieldId(), e.caseKey(), e.configId(), e.maxPixels(), pixels));
        }
        assertTrue(found, "no production envelope for " + configId + " " + fieldId);
        return new TablePixelNoiseEnvelopes.Table(name, envelopes);
    }

    // ---------------------------------------------------------------- R1-R4: a deterministic defect inside an envelope must fail after its retry

    private void inEnvelopeDefectFailsAfterItsRetry(String id, String configId, String caseKey, int x, int y, int injected) throws Exception {
        TableParityTest.Field f = field(caseKey);
        TablePixelNoiseEnvelopes.Point point = new TablePixelNoiseEnvelopes.Point(x, y);
        TablePixelNoiseEnvelopes.PixelRule rule = TablePixelNoiseEnvelopes.PRODUCTION.find(configId, f.id()).orElseThrow().pixels().get(point);
        assertNotNull(rule, id + ": " + point + " is not in the production envelope");
        for (TablePixelNoiseEnvelopes.ColorPair documented : rule.pairs()) {
            for (int color : new int[] {documented.low(), documented.high()}) {
                assertTrue(injected != color, id + ": the injected color must equal neither documented color");
                assertTrue(TablePixelNoiseEnvelopes.ColorPair.of(injected, color).channel() <= rule.channelLimit(), id + ": the injected color must stay within the channel limit");
            }
        }
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        CountingReplay replay = new CountingReplay(pixel(x, y, injected));
        TablePixelRetry.Comparison c = firstCapture(ledger, configId, caseKey, pixel(x, y, injected));
        assertEquals(injected, rgb(c.first().migratedPng(), x, y), id + ": the defect is in the first capture");
        TablePixelNoiseEnvelopes.Eligibility production = TablePixelRetry.eligibility(TablePixelNoiseEnvelopes.PRODUCTION, c);
        assertFalse(production.eligible(), id + ": the defect must NOT be eligible under the production table: " + production.reason());
        TablePixelNoiseEnvelopes.Table table = withInjectedPairs(id + " control", configId, f.id(), point, injected);
        assertTrue(TablePixelRetry.eligibility(table, c).eligible(), id + ": the defect must be eligible under its control table: " + TablePixelRetry.eligibility(table, c).reason());
        TablePixelRetry.Outcome outcome = ledger.resolve(table, replay).get(0);
        System.out.print(ledger.report("RETRY-MECHANISM " + id, new PixelDiagnostics.PictureBudget()));
        assertEquals(TablePixelRetry.State.FAILED_AFTER_RETRY, outcome.state(), id + ": " + outcome.reason());
        assertEquals(1, replay.launches, id + ": exactly one retry browser");
        assertEquals(injected, rgb(outcome.retry().migratedPng(), x, y), id + ": the defect is in the retry too");
    }

    @Test
    void r1DarkFathomClearSelectionHoverBarCorner() throws Exception {
        inEnvelopeDefectFailsAfterItsRetry("R1", "dark/fathom", CLEAR_HOVER, 23, 50, 0x013547);
    }

    /**
     * R2 is on a case with a tolerance (the hovered sort button: up to 24 levels at edge pixels), so a defect there must differ by more than the policy tolerates and no more than the
     * envelope allows (27 at (22,24)). The defect is the legacy color at that pixel, as this browser renders it, with blue moved by 26 levels away from the documented colors: the
     * policy rejects it in the first capture and in the retry, whichever documented color the legacy page shows then. Its control table documents exactly that one pair there.
     */
    @Test
    void r2LightFoundrySortButtonHoverFirstLetterFringe() throws Exception {
        String configId = "light/foundry";
        int x = 22;
        int y = 24;
        TableParityTest.Field f = field(SORT_HOVER);
        TablePixelNoiseEnvelopes.Point point = new TablePixelNoiseEnvelopes.Point(x, y);
        TablePixelNoiseEnvelopes.PixelRule rule = TablePixelNoiseEnvelopes.PRODUCTION.find(configId, f.id()).orElseThrow().pixels().get(point);
        assertNotNull(rule, "R2: " + point + " is not in the production envelope");
        int usual = rgb(TablePixelTest.replayBlock(session, server, fields, spec(configId, f), UnaryOperator.identity()).legacyPng(), x, y);
        Set<Integer> documented = new HashSet<>();
        rule.pairs().forEach(p -> {
            documented.add(p.low());
            documented.add(p.high());
        });
        int blue = usual & 0xFF;
        int best = -1;
        for (int candidate : new int[] {blue - 26, blue + 26}) {
            if (candidate >= 0 && candidate <= 255 && (best < 0 || distance(candidate, documented) > distance(best, documented))) {
                best = candidate;
            }
        }
        int injected = (usual & 0xFFFF00) | best;
        assertFalse(documented.contains(injected), "R2: the injected color must equal neither documented color");
        TablePixelNoiseEnvelopes.ColorPair pair = TablePixelNoiseEnvelopes.ColorPair.of(usual, injected);
        assertTrue(pair.channel() > PixelPolicy.forCase(SORT_HOVER).maxChannelDifference() && pair.channel() <= rule.channelLimit(),
                "R2: the defect must be beyond the policy's tolerance and within the envelope's channel limit: " + pair);

        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        CountingReplay replay = new CountingReplay(pixel(x, y, injected));
        TablePixelRetry.Comparison c = firstCapture(ledger, configId, SORT_HOVER, pixel(x, y, injected));
        assertEquals(usual, rgb(c.first().legacyPng(), x, y), "R2: the legacy color is the one the defect was chosen against");
        assertEquals(injected, rgb(c.first().migratedPng(), x, y), "R2: the defect is in the first capture");
        TablePixelNoiseEnvelopes.Eligibility production = TablePixelRetry.eligibility(TablePixelNoiseEnvelopes.PRODUCTION, c);
        assertFalse(production.eligible(), "R2: the defect must NOT be eligible under the production table: " + production.reason());
        List<TablePixelNoiseEnvelopes.Envelope> envelopes = new ArrayList<>();
        for (TablePixelNoiseEnvelopes.Envelope e : TablePixelNoiseEnvelopes.PRODUCTION.envelopes()) {
            if (e.configId().equals(configId) && e.fieldId().equals(f.id())) {
                Map<TablePixelNoiseEnvelopes.Point, TablePixelNoiseEnvelopes.PixelRule> pixels = new HashMap<>(e.pixels());
                Set<TablePixelNoiseEnvelopes.ColorPair> pairs = new HashSet<>(rule.pairs());
                pairs.add(pair);
                pixels.put(point, new TablePixelNoiseEnvelopes.PixelRule(rule.channelLimit(), pairs));
                envelopes.add(new TablePixelNoiseEnvelopes.Envelope(e.fieldId(), e.caseKey(), e.configId(), e.maxPixels(), pixels));
            } else {
                envelopes.add(e);
            }
        }
        TablePixelNoiseEnvelopes.Table table = new TablePixelNoiseEnvelopes.Table("R2 control", envelopes);
        assertTrue(TablePixelRetry.eligibility(table, c).eligible(), "R2: the defect must be eligible under its control table: " + TablePixelRetry.eligibility(table, c).reason());
        TablePixelRetry.Outcome outcome = ledger.resolve(table, replay).get(0);
        System.out.print(ledger.report("RETRY-MECHANISM R2", new PixelDiagnostics.PictureBudget()));
        assertEquals(TablePixelRetry.State.FAILED_AFTER_RETRY, outcome.state(), "R2: " + outcome.reason());
        assertEquals(1, replay.launches, "R2: exactly one retry browser");
        assertEquals(injected, rgb(outcome.retry().migratedPng(), x, y), "R2: the defect is in the retry too");
    }

    /** The smallest distance from a blue level to the blue level of any of the colors. */
    private static int distance(int blue, Set<Integer> colors) {
        return colors.stream().mapToInt(c -> Math.abs((c & 0xFF) - blue)).min().orElse(Integer.MAX_VALUE);
    }

    @Test
    void r3DarkHarborClearSelectionFocusBarCorner() throws Exception {
        inEnvelopeDefectFailsAfterItsRetry("R3", "dark/harbor", CLEAR_FOCUS, 26, 50, 0x082C3E);
    }

    @Test
    void r4LightSterlingClearSelectionHoverOneLevel() throws Exception {
        inEnvelopeDefectFailsAfterItsRetry("R4", "light/sterling", CLEAR_HOVER, 23, 50, 0xE2EBF8);
    }

    // ---------------------------------------------------------------- F1 and the budget: a defect in the first capture only must recover

    /** The control table for F1: the production table plus one envelope for a case that never showed instability, documenting exactly the injected pair. */
    private static TablePixelNoiseEnvelopes.Table f1Table(int usual) {
        List<TablePixelNoiseEnvelopes.Envelope> envelopes = new ArrayList<>(TablePixelNoiseEnvelopes.PRODUCTION.envelopes());
        envelopes.add(TablePixelNoiseEnvelopes.envelope(field(TWO_SELECTED).id(), TWO_SELECTED, "light/foundry", 1, TablePixelNoiseEnvelopes.pixel(F1_X, F1_Y, usual, F1_COLOR)));
        return new TablePixelNoiseEnvelopes.Table("F1 control", envelopes);
    }

    /** The legacy color at the F1 pixel, checked to sit in a flat area (so the injected pixel is the only change). */
    private static int usualAtF1(TablePixelRetry.Comparison c) throws Exception {
        BufferedImage legacy = PixelDiagnostics.decode(c.first().legacyPng());
        int usual = legacy.getRGB(F1_X, F1_Y) & 0xFFFFFF;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                assertEquals(usual, legacy.getRGB(F1_X + dx, F1_Y + dy) & 0xFFFFFF, "F1 needs a flat area around (" + F1_X + "," + F1_Y + ")");
            }
        }
        assertTrue(usual != F1_COLOR, "the injected color must differ from the page");
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(c.first().legacyPng(), c.first().migratedPng());
        assertEquals(1, d.differing(), "F1 changes exactly one pixel: " + d.describe());
        assertEquals(F1_COLOR, rgb(c.first().migratedPng(), F1_X, F1_Y), "the defect is in the first capture");
        return usual;
    }

    @Test
    void f1ADefectInTheFirstCaptureOnlyRecoversAndUsesASlotOfItsOwnBudget() throws Exception {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        CountingReplay clean = new CountingReplay(UnaryOperator.identity());
        TablePixelRetry.Comparison c = firstCapture(ledger, "light/foundry", TWO_SELECTED, pixel(F1_X, F1_Y, F1_COLOR));
        int usual = usualAtF1(c);
        assertFalse(TablePixelRetry.eligibility(TablePixelNoiseEnvelopes.PRODUCTION, c).eligible(), "F1 must NOT be eligible under the production table");
        TablePixelNoiseEnvelopes.Table table = f1Table(usual);
        assertTrue(TablePixelRetry.eligibility(table, c).eligible(), "F1 must be eligible under its control table: " + TablePixelRetry.eligibility(table, c).reason());
        TablePixelRetry.Outcome outcome = ledger.resolve(table, clean).get(0);
        System.out.print(ledger.report("RETRY-MECHANISM F1", new PixelDiagnostics.PictureBudget()));
        assertEquals(TablePixelRetry.State.RECOVERED, outcome.state(), outcome.reason());
        assertEquals(1, clean.launches, "one retry browser");
        assertEquals(1, ledger.slotsUsed(), "the recovery uses one slot of this ledger's own budget");
        assertEquals(usual, rgb(outcome.retry().migratedPng(), F1_X, F1_Y), "the retry is clean");
        assertTrue(outcome.retryVerdict().exact(), "the clean retry pair is identical");
    }

    @Test
    void aThirdEligibleRejectionIsOverBudgetAndStartsNoBrowser() throws Exception {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        CountingReplay clean = new CountingReplay(UnaryOperator.identity());
        int usual = -1;
        for (int i = 0; i < 3; i++) {
            TablePixelRetry.Comparison c = firstCapture(ledger, "light/foundry", TWO_SELECTED, pixel(F1_X, F1_Y, F1_COLOR));
            int u = usualAtF1(c);
            assertTrue(usual == -1 || usual == u, "the page is the same every time");
            usual = u;
        }
        List<TablePixelRetry.Outcome> outcomes = ledger.resolve(f1Table(usual), clean);
        System.out.print(ledger.report("RETRY-MECHANISM budget", new PixelDiagnostics.PictureBudget()));
        assertEquals(List.of(TablePixelRetry.State.RECOVERED, TablePixelRetry.State.RECOVERED, TablePixelRetry.State.BUDGET_EXCEEDED),
                outcomes.stream().map(TablePixelRetry.Outcome::state).toList());
        assertEquals(2, clean.launches, "the third eligible rejection starts no browser");
        assertEquals(2, ledger.slotsUsed());
        assertNull(outcomes.get(2).retry());
    }

    // ---------------------------------------------------------------- E1-E3: defects outside every envelope are not eligible and start no retry

    private TablePixelRetry.Comparison outsideEveryEnvelopeIsNotEligible(String id, String configId, String caseKey, UnaryOperator<String> defect) throws Exception {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        CountingReplay replay = new CountingReplay(defect);
        TablePixelRetry.Comparison c = firstCapture(ledger, configId, caseKey, defect);
        TablePixelRetry.Outcome outcome = ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, replay).get(0);
        System.out.print(ledger.report("RETRY-MECHANISM " + id, new PixelDiagnostics.PictureBudget()));
        assertEquals(TablePixelRetry.State.NOT_ELIGIBLE, outcome.state(), id + ": " + outcome.reason());
        assertEquals(0, replay.launches, id + ": no retry browser");
        assertEquals(0, ledger.slotsUsed(), id + ": no retry slot");
        return c;
    }

    @Test
    void e1ADotOutsideTheEnvelope() throws Exception {
        TablePixelRetry.Comparison c = outsideEveryEnvelopeIsNotEligible("E1", "dark/fathom", CLEAR_HOVER, pixel(29, 46, 0x003E56));
        assertEquals(0x003E56, rgb(c.first().migratedPng(), 29, 46), "E1's pixel is in the capture");
    }

    @Test
    void e2TheSelectionBarRadiusOnePixelSmaller() throws Exception {
        outsideEveryEnvelopeIsNotEligible("E2", "dark/fathom", CLEAR_HOVER, css("[data-table-selection-bar]{border-radius:calc(var(--ash-radius-md) - 1px) !important}"));
    }

    @Test
    void e3TheHoveredLabelColorOneLevelOff() throws Exception {
        outsideEveryEnvelopeIsNotEligible("E3", "light/foundry", SORT_HOVER, css("[data-table-sort]:hover{color:rgb(from var(--ash-table-sort-hover) r g calc(b + 1)) !important}"));
    }
}
