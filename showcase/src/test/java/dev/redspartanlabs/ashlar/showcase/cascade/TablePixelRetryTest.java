package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The retry of TablePixelTest without a browser: the production noise envelopes against their evidence, the strict eligibility rule, every row of the decision table, the shared
 * budget, and the report. Pictures are synthetic; the replay is a fake that counts its calls.
 */
class TablePixelRetryTest {

    private static final TablePixelNoiseEnvelopes.Mode WITH = TablePixelNoiseEnvelopes.Mode.WITH_TAILWIND;
    private static final TablePixelNoiseEnvelopes.Mode FREE = TablePixelNoiseEnvelopes.Mode.TAILWIND_FREE;
    private static final String EVIDENCE = "/parity/table/instability/";
    private static final int BASE = 0x202020;

    // ---------------------------------------------------------------- synthetic pictures

    /** A 210 x 60 opaque picture of one color, with the given pixels ({x, y, ARGB or RGB}) set; an RGB value is made opaque. */
    private static BufferedImage picture(int[]... pixels) {
        BufferedImage image = new BufferedImage(210, 60, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 60; y++) {
            for (int x = 0; x < 210; x++) {
                image.setRGB(x, y, 0xFF000000 | BASE);
            }
        }
        for (int[] p : pixels) {
            image.setRGB(p[0], p[1], (p[2] >>> 24) == 0 ? 0xFF000000 | p[2] : p[2]);
        }
        return image;
    }

    private static String png(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static TablePixelNoiseEnvelopes.Eligibility check(TablePixelNoiseEnvelopes.Mode mode, String config, String field, String caseKey, BufferedImage legacy,
                                                              BufferedImage migrated) {
        PixelPolicy.Verdict rejected = new PixelPolicy.Verdict(false, false, "exact", true, -1, -1, -1, -1, "the pictures differ and this case is compared exactly");
        return TablePixelNoiseEnvelopes.check(TablePixelNoiseEnvelopes.PRODUCTION, mode, config, field, caseKey, rejected, legacy, migrated);
    }

    private static final String SORT_HOVER = "sort button hover";
    private static final String CLEAR_HOVER = "clear selection hover";
    private static final String CLEAR_FOCUS = "clear selection focus-visible";

    // ---------------------------------------------------------------- the production table and its evidence

    @Test
    void theProductionTableHoldsExactlyTheEightMeasuredEnvelopesForTheFourCasesWithTailwind() {
        List<String> keys = TablePixelNoiseEnvelopes.PRODUCTION.envelopes().stream().map(e -> e.configId() + " " + e.fieldId()).sorted().toList();
        assertEquals(List.of("dark/fathom tb19", "dark/fathom tb20", "dark/harbor tb19", "dark/harbor tb20", "light/foundry tb6", "light/foundry tb8", "light/sterling tb19",
                "light/sterling tb20"), keys);
        for (TablePixelNoiseEnvelopes.Envelope e : TablePixelNoiseEnvelopes.PRODUCTION.envelopes()) {
            int index = Integer.parseInt(e.fieldId().substring(2)) - 1;
            assertEquals(TableBrowser.CASES.get(index).key(), e.caseKey(), e.fieldId() + " is the case its envelope names");
            assertTrue(e.maxPixels() >= 1 && e.maxPixels() <= e.pixels().size(), e.fieldId());
            e.pixels().forEach((p, rule) -> {
                assertEquals(1, rule.pairs().size(), p + ": one documented pair per pixel");
                assertEquals(rule.pairs().stream().mapToInt(TablePixelNoiseEnvelopes.ColorPair::channel).max().orElseThrow(), rule.channelLimit(), p + ": the limit is the observed difference");
            });
        }
    }

    /** One row of evidence.tsv. */
    private record Row(String observation, String report, String mode, String config, String field, String caseKey, String kind, int differing, int maxChannel, String bounds,
                       String complete, String x, String y, String colorA, String colorB, String channel, String source, String image) {
    }

    private static List<Row> evidence() throws IOException {
        try (InputStream in = TablePixelRetryTest.class.getResourceAsStream(EVIDENCE + "evidence.tsv")) {
            assertNotNull(in, "evidence.tsv is a test resource");
            List<String> lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            assertEquals("observation\trun\tartifact\treport\tmode\tconfiguration\tfield\tcase\tkind\tdiffering\tmax_channel\tbounds\tcomplete\tx\ty\tcolor_a\tcolor_b\tchannel\tpixel_source\timage",
                    lines.get(0));
            List<Row> rows = new ArrayList<>();
            for (String line : lines.subList(1, lines.size())) {
                String[] c = line.split("\t", -1);
                assertEquals(20, c.length, line);
                rows.add(new Row(c[0], c[3], c[4], c[5], c[6], c[7], c[8], Integer.parseInt(c[9]), Integer.parseInt(c[10]), c[11], c[12], c[13], c[14], c[15], c[16], c[17], c[18],
                        c[19]));
            }
            return rows;
        }
    }

    @Test
    void theProductionTableIsExactlyWhatTheEvidenceSupportsNoMoreNoLess() throws IOException {
        // envelopes rebuilt from every observation that is not a tolerated pair
        Map<String, Map<TablePixelNoiseEnvelopes.Point, Set<TablePixelNoiseEnvelopes.ColorPair>>> pixels = new TreeMap<>();
        Map<String, Integer> maxDiffering = new HashMap<>();
        Map<String, String> caseOf = new HashMap<>();
        for (Row r : evidence()) {
            if (r.kind().equals("tolerated")) {
                continue;
            }
            assertEquals("with-tailwind", r.mode(), r.observation() + ": every observation behind an envelope is from the run with Tailwind");
            String key = r.config() + " " + r.field();
            caseOf.put(key, r.caseKey());
            maxDiffering.merge(key, r.differing(), Math::max);
            Map<TablePixelNoiseEnvelopes.Point, Set<TablePixelNoiseEnvelopes.ColorPair>> m = pixels.computeIfAbsent(key, k -> new LinkedHashMap<>());
            if (!r.x().equals("-")) {
                TablePixelNoiseEnvelopes.ColorPair pair = TablePixelNoiseEnvelopes.ColorPair.of(Integer.parseInt(r.colorA(), 16), Integer.parseInt(r.colorB(), 16));
                assertEquals(pair.channel(), Integer.parseInt(r.channel()), r.observation() + ": the recorded channel difference");
                m.computeIfAbsent(new TablePixelNoiseEnvelopes.Point(Integer.parseInt(r.x()), Integer.parseInt(r.y())), k -> new HashSet<>()).add(pair);
            }
        }
        assertEquals(pixels.keySet(), new HashSet<>(TablePixelNoiseEnvelopes.PRODUCTION.envelopes().stream().map(e -> e.configId() + " " + e.fieldId()).toList()));
        for (TablePixelNoiseEnvelopes.Envelope e : TablePixelNoiseEnvelopes.PRODUCTION.envelopes()) {
            String key = e.configId() + " " + e.fieldId();
            assertEquals(caseOf.get(key), e.caseKey(), key);
            assertEquals(maxDiffering.get(key), e.maxPixels(), key + ": the most pixels that differed at once");
            Map<TablePixelNoiseEnvelopes.Point, Set<TablePixelNoiseEnvelopes.ColorPair>> documented = new HashMap<>();
            e.pixels().forEach((p, rule) -> documented.put(p, rule.pairs()));
            assertEquals(pixels.get(key), documented, key + ": every coordinate and color pair, and nothing else");
        }
    }

    @Test
    void theCountOnlyObservationsAgreeWithTheirEnvelopes() throws IOException {
        for (Row r : evidence()) {
            if (!r.x().equals("-") || r.kind().equals("tolerated")) {
                continue;
            }
            TablePixelNoiseEnvelopes.Envelope e = TablePixelNoiseEnvelopes.PRODUCTION.find(r.config(), r.field()).orElseThrow();
            int maxLimit = e.pixels().values().stream().mapToInt(TablePixelNoiseEnvelopes.PixelRule::channelLimit).max().orElseThrow();
            assertTrue(r.differing() <= e.maxPixels() && r.maxChannel() <= maxLimit, r.observation() + " (" + r.report() + ") lies within its envelope's counts");
            assertEquals("none", r.source());
        }
    }

    @Test
    void theEvidenceAgreesWithEveryPreservedPicture() throws IOException {
        Map<String, List<Row>> byObservation = new LinkedHashMap<>();
        evidence().forEach(r -> byObservation.computeIfAbsent(r.observation(), k -> new ArrayList<>()).add(r));
        int fromPictures = 0;
        int fromEnlargements = 0;
        int textOnly = 0;
        for (List<Row> rows : byObservation.values()) {
            Row first = rows.get(0);
            if (first.image().equals("-")) {
                textOnly += first.x().equals("-") ? 0 : 1;
                continue;
            }
            if (first.image().contains(";")) {
                // a full legacy and migrated picture: the pixels that differ, and their colors, are exactly the rows
                String[] files = first.image().split(";");
                BufferedImage a = image(files[0]);
                BufferedImage b = image(files[1]);
                Map<String, String> found = new TreeMap<>();
                for (int y = 0; y < a.getHeight(); y++) {
                    for (int x = 0; x < a.getWidth(); x++) {
                        if (a.getRGB(x, y) != b.getRGB(x, y)) {
                            found.put(x + "," + y, String.format("%06X %06X", a.getRGB(x, y) & 0xFFFFFF, b.getRGB(x, y) & 0xFFFFFF));
                        }
                    }
                }
                Map<String, String> recorded = new TreeMap<>();
                rows.forEach(r -> recorded.put(r.x() + "," + r.y(), r.colorA() + " " + r.colorB()));
                assertEquals(recorded, found, first.observation());
                assertEquals(first.differing(), found.size(), first.observation());
                fromPictures++;
            } else {
                // an enlargement (reference, candidate and difference panels): its marked pixels are exactly the rows, and each row's colors are the two panels' colors there
                BufferedImage zoom = image(first.image());
                String[] b = first.bounds().replace("x", "").replace("y", "").split("[.,]+");
                int minX = Integer.parseInt(b[0]);
                int minY = Integer.parseInt(b[2]);
                int maxY = Integer.parseInt(b[3]);
                int x0 = Math.max(0, minX - 6);
                int y0 = Math.max(0, minY - 6);
                int scale = zoom.getHeight() / (maxY + 6 - y0 + 1);
                int panel = (zoom.getWidth() - 16) / 3;
                Set<String> marked = new HashSet<>();
                for (int y = 0; y < zoom.getHeight(); y++) {
                    for (int x = 2 * (panel + 8); x < 2 * (panel + 8) + panel; x++) {
                        if ((zoom.getRGB(x, y) & 0xFFFFFF) == 0xFF00FF) {
                            marked.add((x0 + (x - 2 * (panel + 8)) / scale) + "," + (y0 + y / scale));
                        }
                    }
                }
                Set<String> recorded = new HashSet<>();
                for (Row r : rows) {
                    recorded.add(r.x() + "," + r.y());
                    int px = (Integer.parseInt(r.x()) - x0) * scale + scale / 2;
                    int py = (Integer.parseInt(r.y()) - y0) * scale + scale / 2;
                    Set<String> panels = Set.of(String.format("%06X", zoom.getRGB(px, py) & 0xFFFFFF), String.format("%06X", zoom.getRGB(panel + 8 + px, py) & 0xFFFFFF));
                    assertEquals(Set.of(r.colorA(), r.colorB()), panels, r.observation() + " at " + r.x() + "," + r.y());
                }
                assertEquals(recorded, marked, first.observation() + ": the enlargement's marked pixels");
                assertEquals(first.differing(), marked.size(), first.observation());
                fromEnlargements++;
            }
        }
        assertEquals(3, fromPictures, "observations verified against a full legacy and migrated picture");
        assertEquals(35, fromEnlargements, "observations verified against an enlargement");
        assertEquals(3, textOnly, "observations known only from report text (tolerated pairs, listed in full there)");
    }

    private static BufferedImage image(String file) throws IOException {
        try (InputStream in = TablePixelRetryTest.class.getResourceAsStream(EVIDENCE + "images/" + file)) {
            assertNotNull(in, file);
            return ImageIO.read(in);
        }
    }

    // ---------------------------------------------------------------- eligibility: only documented pixels with documented color pairs

    @Test
    void theDocumentedPairsAreEligibleInEitherDirectionAndInAnyNonemptySubset() {
        BufferedImage usual = picture(new int[] {22, 23, 0xEDF1ED}, new int[] {22, 24, 0xEDE3C5});
        BufferedImage alternate = picture(new int[] {22, 23, 0xEDF1EE}, new int[] {22, 24, 0xEDE7E0});
        assertTrue(check(WITH, "light/foundry", "tb6", SORT_HOVER, usual, alternate).eligible());
        assertTrue(check(WITH, "light/foundry", "tb6", SORT_HOVER, alternate, usual).eligible(), "either rendering may show either color");
        BufferedImage only = picture(new int[] {22, 23, 0xEDF1ED}, new int[] {22, 24, 0xEDE7E0});
        assertTrue(check(WITH, "light/foundry", "tb8", "active sort button hover", usual, only).eligible(), "one of the two documented pixels");
        // all eleven documented dark/harbor tb19 pixels at once
        List<int[]> a = new ArrayList<>();
        List<int[]> b = new ArrayList<>();
        TablePixelNoiseEnvelopes.PRODUCTION.find("dark/harbor", "tb19").orElseThrow().pixels().forEach((p, rule) -> {
            TablePixelNoiseEnvelopes.ColorPair pair = rule.pairs().iterator().next();
            a.add(new int[] {p.x(), p.y(), pair.low()});
            b.add(new int[] {p.x(), p.y(), pair.high()});
        });
        assertTrue(check(WITH, "dark/harbor", "tb19", CLEAR_FOCUS, picture(a.toArray(int[][]::new)), picture(b.toArray(int[][]::new))).eligible());
    }

    @Test
    void anUndocumentedColorIsNotEligibleEvenWithinTheChannelLimit() {
        BufferedImage legacy = picture(new int[] {22, 24, 0xEDE3C5});
        TablePixelNoiseEnvelopes.Eligibility e = check(WITH, "light/foundry", "tb6", SORT_HOVER, legacy, picture(new int[] {22, 24, 0xEDE3C6}));
        assertFalse(e.eligible(), "1 level, well within 27, but not a documented color");
        assertTrue(e.reason().contains("not a documented color pair"), e.reason());
        assertFalse(check(WITH, "light/foundry", "tb6", SORT_HOVER, picture(new int[] {22, 23, 0xEDE3C5}), picture(new int[] {22, 23, 0xEDE7E0})).eligible(),
                "a documented pair at the wrong documented pixel");
        assertFalse(check(WITH, "dark/fathom", "tb20", CLEAR_HOVER, picture(new int[] {23, 50, 0x013142}), picture(new int[] {23, 50, 0x013143})).eligible());
    }

    @Test
    void anAlphaDifferenceIsNotEligible() {
        BufferedImage legacy = picture(new int[] {22, 24, 0xFFEDE3C5});
        TablePixelNoiseEnvelopes.Eligibility e = check(WITH, "light/foundry", "tb6", SORT_HOVER, legacy, picture(new int[] {22, 24, 0xFEEDE3C5}));
        assertFalse(e.eligible());
        assertTrue(e.reason().contains("not opaque"), e.reason());
        assertFalse(check(WITH, "light/foundry", "tb6", SORT_HOVER, legacy, picture(new int[] {22, 24, 0xFEEDE7E0})).eligible(), "a documented color, but not opaque");
    }

    @Test
    void aPixelOutsideItsDocumentedCoordinatesIsNotEligible() {
        TablePixelNoiseEnvelopes.Eligibility e = check(WITH, "light/foundry", "tb6", SORT_HOVER, picture(new int[] {21, 24, 0xEDE3C5}), picture(new int[] {21, 24, 0xEDE7E0}));
        assertFalse(e.eligible());
        assertTrue(e.reason().contains("outside"), e.reason());
        assertFalse(check(WITH, "light/foundry", "tb6", SORT_HOVER, picture(new int[] {22, 24, 0xEDE3C5}, new int[] {40, 40, 0x111111}),
                picture(new int[] {22, 24, 0xEDE7E0}, new int[] {40, 40, 0x111112})).eligible(), "one documented pixel and one other");
    }

    @Test
    void theMaximumNumberOfDifferingPixelsIsEnforced() {
        TablePixelNoiseEnvelopes.Table one = new TablePixelNoiseEnvelopes.Table("at most one", List.of(TablePixelNoiseEnvelopes.envelope("tb6", SORT_HOVER, "light/foundry", 1,
                TablePixelNoiseEnvelopes.pixel(22, 23, 0xEDF1ED, 0xEDF1EE), TablePixelNoiseEnvelopes.pixel(22, 24, 0xEDE3C5, 0xEDE7E0))));
        PixelPolicy.Verdict rejected = new PixelPolicy.Verdict(false, false, "exact", true, -1, -1, -1, -1, "differ");
        BufferedImage usual = picture(new int[] {22, 23, 0xEDF1ED}, new int[] {22, 24, 0xEDE3C5});
        TablePixelNoiseEnvelopes.Eligibility e = TablePixelNoiseEnvelopes.check(one, WITH, "light/foundry", "tb6", SORT_HOVER, rejected, usual,
                picture(new int[] {22, 23, 0xEDF1EE}, new int[] {22, 24, 0xEDE7E0}));
        assertFalse(e.eligible());
        assertTrue(e.reason().contains("at most 1"), e.reason());
        assertTrue(TablePixelNoiseEnvelopes.check(one, WITH, "light/foundry", "tb6", SORT_HOVER, rejected, usual, picture(new int[] {22, 23, 0xEDF1EE}, new int[] {22, 24, 0xEDE3C5}))
                .eligible(), "one documented pixel is within that maximum");
    }

    @Test
    void unknownCasesConfigurationsTheRunWithoutTailwindAndAcceptedPairsAreNeverEligible() {
        BufferedImage usual = picture(new int[] {22, 24, 0xEDE3C5});
        BufferedImage alternate = picture(new int[] {22, 24, 0xEDE7E0});
        assertFalse(check(FREE, "light/foundry", "tb6", SORT_HOVER, usual, alternate).eligible(), "the run without Tailwind");
        assertFalse(check(WITH, "light/none", "tb6", SORT_HOVER, usual, alternate).eligible(), "a configuration without an envelope");
        assertFalse(check(WITH, "light/foundry", "tb7", "sort button focus-visible", usual, alternate).eligible(), "a case without an envelope");
        assertFalse(check(WITH, "light/foundry", "tb6", "active sort button hover", usual, alternate).eligible(), "a field id that names another case");
        PixelPolicy.Verdict accepted = new PixelPolicy.Verdict(true, false, "hover-text-edge", true, 1, 27, 0, 0, "within the policy");
        assertFalse(TablePixelNoiseEnvelopes.check(TablePixelNoiseEnvelopes.PRODUCTION, WITH, "light/foundry", "tb6", SORT_HOVER, accepted, usual, alternate).eligible());
        // a picture one row shorter, otherwise the same: only the size check can refuse it
        BufferedImage shorter = usual.getSubimage(0, 0, usual.getWidth(), usual.getHeight() - 1);
        TablePixelNoiseEnvelopes.Eligibility sizes = check(WITH, "light/foundry", "tb6", SORT_HOVER, alternate, shorter);
        assertFalse(sizes.eligible(), "different sizes");
        assertTrue(sizes.reason().contains("different sizes"), sizes.reason());
        assertFalse(check(WITH, "light/foundry", "tb6", SORT_HOVER, usual, picture(new int[] {22, 24, 0xEDE3C5})).eligible(), "no differing pixel");
        assertFalse(check(WITH, "light/foundry", "tb6", SORT_HOVER, null, alternate).eligible(), "no picture");
    }

    // ---------------------------------------------------------------- the decision table and the budget

    private static final PixelPolicy.Verdict IDENTICAL = new PixelPolicy.Verdict(true, true, "exact", true, 0, 0, 0, 0, "identical");
    private static final PixelPolicy.Verdict TOLERATED = new PixelPolicy.Verdict(true, false, "focus-ring", true, 1, 3, 0, 0, "within the policy");

    /** An eligible rejection: dark/fathom tb20 with the documented alternate at (23,50). */
    private static TablePixelRetry.Capture eligiblePair() {
        return new TablePixelRetry.Capture(png(picture(new int[] {23, 50, 0x013142})), png(picture(new int[] {23, 50, 0x013A4D})), null);
    }

    /** A rejection that is not eligible: a pixel outside every envelope. */
    private static TablePixelRetry.Capture ineligiblePair() {
        return new TablePixelRetry.Capture(png(picture(new int[] {100, 10, 0x111111})), png(picture(new int[] {100, 10, 0x999999})), null);
    }

    private static TablePixelRetry.Capture identicalPair() {
        String p = png(picture());
        return new TablePixelRetry.Capture(p, p, null);
    }

    private static TablePixelRetry.Comparison comparison(TablePixelRetry.Ledger ledger, TablePixelNoiseEnvelopes.Mode mode, PixelPolicy.Verdict verdict, TablePixelRetry.Capture first) {
        TablePixelRetry.Comparison c = new TablePixelRetry.Comparison(ledger.size(), mode, "dark/fathom", "tb20", CLEAR_HOVER, "dark/fathom tb20 #" + ledger.size(), verdict,
                first, new TablePixelRetry.ReplaySpec("dark/fathom", List.of("legacy.css"), List.of("app.css"), null, 12, 7));
        ledger.record(c);
        return c;
    }

    private static TablePixelRetry.Comparison rejected(TablePixelRetry.Ledger ledger, TablePixelNoiseEnvelopes.Mode mode, TablePixelRetry.Capture first) {
        try {
            return comparison(ledger, mode, PixelPolicy.compare(first.legacyPng(), first.migratedPng(), PixelPolicy.EXACT, null), first);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A replay that returns the given captures in turn (or throws them), counting its calls. */
    private static final class FakeReplay implements TablePixelRetry.Replay {
        private final List<Object> results;
        private int calls;

        FakeReplay(Object... results) {
            this.results = List.of(results);
        }

        @Override
        public TablePixelRetry.Capture replay(TablePixelRetry.Comparison comparison) throws Exception {
            Object r = results.get(calls++);
            if (r instanceof Exception e) {
                throw e;
            }
            if (r instanceof Error e) {
                throw e;
            }
            return (TablePixelRetry.Capture) r;
        }
    }

    private static List<TablePixelRetry.State> states(List<TablePixelRetry.Outcome> outcomes) {
        return outcomes.stream().map(TablePixelRetry.Outcome::state).toList();
    }

    @Test
    void everyRowOfTheDecisionTable() {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        comparison(ledger, WITH, IDENTICAL, null);
        comparison(ledger, WITH, TOLERATED, null);
        rejected(ledger, WITH, ineligiblePair());
        rejected(ledger, WITH, eligiblePair());
        rejected(ledger, WITH, eligiblePair());
        FakeReplay replay = new FakeReplay(identicalPair(), eligiblePair());
        List<TablePixelRetry.Outcome> out = ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, replay);
        assertEquals(List.of(TablePixelRetry.State.IDENTICAL, TablePixelRetry.State.TOLERATED, TablePixelRetry.State.NOT_ELIGIBLE, TablePixelRetry.State.RECOVERED,
                TablePixelRetry.State.FAILED_AFTER_RETRY), states(out));
        assertEquals(2, replay.calls, "only the eligible rejections are retried");
        assertTrue(out.get(3).retryVerdict().exact());
        assertFalse(out.get(4).retryVerdict().accepted());
        assertTrue(out.get(2).reason().contains("outside"), out.get(2).reason());
    }

    @Test
    void aRetryThatCannotCaptureIsAnInfrastructureFailureAndUsesItsSlot() {
        for (Throwable failure : new Throwable[] {new IllegalStateException("Chrome did not start"), new AssertionError("one page captured twice gave two pictures")}) {
            TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
            rejected(ledger, WITH, eligiblePair());
            List<TablePixelRetry.Outcome> out = ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, new FakeReplay(failure));
            assertEquals(TablePixelRetry.State.RETRY_INFRA_FAILURE, out.get(0).state());
            assertTrue(out.get(0).reason().contains(failure.getMessage()), out.get(0).reason());
            assertEquals(1, ledger.slotsUsed());
        }
    }

    @Test
    void theThirdEligibleRejectionIsOverBudgetWithoutAReplayAndAFailedRetryUsesItsSlot() {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        for (int i = 0; i < 4; i++) {
            rejected(ledger, WITH, eligiblePair());
        }
        FakeReplay replay = new FakeReplay(eligiblePair(), identicalPair());
        List<TablePixelRetry.Outcome> out = ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, replay);
        assertEquals(List.of(TablePixelRetry.State.FAILED_AFTER_RETRY, TablePixelRetry.State.RECOVERED, TablePixelRetry.State.BUDGET_EXCEEDED, TablePixelRetry.State.BUDGET_EXCEEDED),
                states(out));
        assertEquals(2, replay.calls, "no replay for the third and fourth");
        assertEquals(2, ledger.slotsUsed());
        assertEquals(2, ledger.replaysStarted());
        assertTrue(out.get(2).retry() == null && out.get(3).retry() == null);
        for (int n : new int[] {0, 1, 2}) {
            TablePixelRetry.Ledger fewer = new TablePixelRetry.Ledger();
            for (int i = 0; i < n; i++) {
                rejected(fewer, WITH, eligiblePair());
            }
            List<TablePixelRetry.Outcome> o = fewer.resolve(TablePixelNoiseEnvelopes.PRODUCTION, new FakeReplay(identicalPair(), identicalPair()));
            assertTrue(o.stream().allMatch(x -> x.state() == TablePixelRetry.State.RECOVERED), n + " eligible rejections are all retried");
        }
    }

    @Test
    void oneBudgetIsSharedByBothParityRunsInCaptureOrder() {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        rejected(ledger, WITH, eligiblePair());
        rejected(ledger, FREE, eligiblePair());
        rejected(ledger, WITH, eligiblePair());
        rejected(ledger, WITH, eligiblePair());
        FakeReplay replay = new FakeReplay(identicalPair(), identicalPair());
        assertEquals(List.of(TablePixelRetry.State.RECOVERED, TablePixelRetry.State.NOT_ELIGIBLE, TablePixelRetry.State.RECOVERED, TablePixelRetry.State.BUDGET_EXCEEDED),
                states(ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, replay)));
        assertEquals(2, replay.calls);
    }

    @Test
    void theLedgerIsKeptInCaptureOrderAndResolvedOnce() {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        comparison(ledger, WITH, IDENTICAL, null);
        assertThrows(IllegalStateException.class, () -> ledger.record(new TablePixelRetry.Comparison(5, WITH, "c", "f", "k", "out of order", IDENTICAL, null, null)));
        assertThrows(IllegalStateException.class, () -> ledger.record(new TablePixelRetry.Comparison(1, WITH, "c", "f", "k", "rejected without pictures",
                new PixelPolicy.Verdict(false, false, "exact", true, -1, -1, -1, -1, "differ"), null, null)));
        assertThrows(IllegalStateException.class, ledger::outcomes);
        ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, new FakeReplay());
        assertThrows(IllegalStateException.class, () -> ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, new FakeReplay()));
        assertThrows(IllegalStateException.class, () -> comparison(ledger, WITH, IDENTICAL, null));
    }

    // ---------------------------------------------------------------- the report

    @Test
    void everyFirstPassRejectionIsReportedExactlyOnceWithItsEvidenceRecoveredOnesIncluded() {
        TablePixelRetry.Ledger ledger = new TablePixelRetry.Ledger();
        comparison(ledger, WITH, IDENTICAL, null);
        comparison(ledger, WITH, TOLERATED, null);
        rejected(ledger, WITH, ineligiblePair());
        rejected(ledger, WITH, eligiblePair());
        rejected(ledger, WITH, eligiblePair());
        rejected(ledger, WITH, eligiblePair());
        ledger.resolve(TablePixelNoiseEnvelopes.PRODUCTION, new FakeReplay(identicalPair(), eligiblePair()));
        String report = ledger.report("T", new PixelDiagnostics.PictureBudget());
        for (int order : new int[] {2, 3, 4, 5}) {
            assertEquals(1, report.lines().filter(l -> l.startsWith("T rejection [" + order + "]") || l.startsWith("T UNSTABLE (recovered) [" + order + "]")).count(), "rejection " + order);
        }
        assertTrue(report.contains("T UNSTABLE (recovered) [3]"), report);
        assertTrue(report.contains("final: RECOVERED"), report);
        assertTrue(report.contains("final: FAILED_AFTER_RETRY"), report);
        assertTrue(report.contains("final: NOT_ELIGIBLE"), report);
        assertTrue(report.contains("final: BUDGET_EXCEEDED"), report);
        assertTrue(report.contains("T   retry [3]:") && report.contains("T   retry [4]:") && !report.contains("T   retry [5]:"), report);
        assertTrue(report.contains("T picture [3] retry migrated "), "a recovered comparison keeps both captures: " + report);
        assertFalse(report.contains("[0]") || report.contains("[1]"), "identical and tolerated pairs are not rejections");
        assertTrue(report.contains("T first pass: 6 comparisons (6 with Tailwind, 0 without), 1 identical, 1 tolerated, 4 rejected; retry slots used 2 of 2; recovered 1, "
                + "failed after retry 1, not eligible 1, infrastructure 0, over budget 1"), report);
    }

    @Test
    void theFirstPassDigestFollowsEveryVerdictInOrder() {
        TablePixelRetry.Ledger a = new TablePixelRetry.Ledger();
        TablePixelRetry.Ledger b = new TablePixelRetry.Ledger();
        TablePixelRetry.Ledger c = new TablePixelRetry.Ledger();
        comparison(a, WITH, IDENTICAL, null);
        comparison(a, WITH, TOLERATED, null);
        comparison(b, WITH, IDENTICAL, null);
        comparison(b, WITH, TOLERATED, null);
        comparison(c, WITH, TOLERATED, null);
        comparison(c, WITH, IDENTICAL, null);
        assertEquals(a.firstPassDigest(), b.firstPassDigest());
        assertNotEquals(a.firstPassDigest(), c.firstPassDigest());
    }
}
