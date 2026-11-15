package dev.redspartanlabs.ashlar.showcase.cascade;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * The retry of {@link TablePixelTest} (issue #34). Chrome on Linux sometimes renders the same page with a few different anti-aliased pixels; a comparison rejected for that reason
 * alone gets ONE more chance, in a fresh browser that replays its block up to it, judged by the unchanged {@link PixelPolicy}. A retry that passes does not prove the first
 * rejection harmless: the comparison is reported as unstable and uses one of the run's two retry slots.
 *
 * <p>The ledger records every first-pass comparison, in capture order, with the verdict it got; nothing about the first pass changes. After the whole first pass,
 * {@link Ledger#resolve} gives each comparison exactly one final state:
 * <ul>
 * <li>identical or tolerated: as decided by the policy, no retry;</li>
 * <li>rejected and not eligible ({@link TablePixelNoiseEnvelopes#check}): fails, no retry;</li>
 * <li>rejected and eligible, while a slot is free: one retry, which uses the slot whatever its result; RECOVERED when the policy accepts the retry pair, FAILED_AFTER_RETRY when it
 * rejects it, RETRY_INFRA_FAILURE when the retry cannot capture;</li>
 * <li>rejected and eligible when both slots are used: BUDGET_EXCEEDED, reported, never retried.</li>
 * </ul>
 */
final class TablePixelRetry {

    private TablePixelRetry() {
    }

    /** Retry slots per complete TablePixelTest run, shared by both parity runs. */
    static final int RETRY_SLOTS = 2;

    enum State {
        IDENTICAL(false), TOLERATED(false), RECOVERED(false), NOT_ELIGIBLE(true), FAILED_AFTER_RETRY(true), RETRY_INFRA_FAILURE(true), BUDGET_EXCEEDED(true);

        private final boolean fails;

        State(boolean fails) {
            this.fails = fails;
        }

        boolean fails() {
            return fails;
        }
    }

    /** How to capture a comparison again: its configuration, both stylesheet lists, the page environment (null when none), its block's first case, and its position in that block. */
    record ReplaySpec(String configId, List<String> legacySheets, List<String> migratedSheets, String environment, int blockStart, int position) {
        ReplaySpec {
            legacySheets = List.copyOf(legacySheets);
            migratedSheets = List.copyOf(migratedSheets);
        }
    }

    /** The two pictures of a comparison and the legacy (reference) geometry the policy uses. */
    record Capture(String legacyPng, String migratedPng, PixelPolicy.Geometry legacyGeometry) {
    }

    /** One first-pass comparison. {@code first} (the pictures) is kept only for a rejected comparison. */
    record Comparison(int order, TablePixelNoiseEnvelopes.Mode mode, String configId, String fieldId, String caseKey, String label, PixelPolicy.Verdict verdict, Capture first,
                      ReplaySpec spec) {
    }

    /** Captures a comparison again, in a fresh browser. */
    @FunctionalInterface
    interface Replay {
        Capture replay(Comparison comparison) throws Exception;
    }

    /** The final state of one comparison, why, the eligibility decision (rejected comparisons only), and the retry (when one ran). */
    record Outcome(Comparison comparison, State state, String reason, TablePixelNoiseEnvelopes.Eligibility eligibility, Capture retry, PixelPolicy.Verdict retryVerdict,
                   long retryMillis) {
    }

    static TablePixelNoiseEnvelopes.Eligibility eligibility(TablePixelNoiseEnvelopes.Table table, Comparison c) {
        BufferedImage legacy;
        BufferedImage migrated;
        try {
            legacy = PixelDiagnostics.decode(c.first().legacyPng());
            migrated = PixelDiagnostics.decode(c.first().migratedPng());
        } catch (IOException | RuntimeException e) {
            legacy = null;
            migrated = null;
        }
        return TablePixelNoiseEnvelopes.check(table, c.mode(), c.configId(), c.fieldId(), c.caseKey(), c.verdict(), legacy, migrated);
    }

    /** One run's comparisons, their final states and its retry slots. One ledger per TablePixelTest run; the mechanism test makes its own. */
    static final class Ledger {
        private final List<Comparison> comparisons = new ArrayList<>();
        private List<Outcome> outcomes;
        private int slotsUsed;
        private int replaysStarted;

        void record(Comparison c) {
            if (outcomes != null) {
                throw new IllegalStateException("the ledger is resolved; no comparison can be added");
            }
            if (c.order() != comparisons.size()) {
                throw new IllegalStateException("comparison " + c.order() + " recorded as number " + comparisons.size());
            }
            if (!c.verdict().accepted() && c.first() == null) {
                throw new IllegalStateException(c.label() + ": a rejected comparison must keep its pictures");
            }
            comparisons.add(c);
        }

        int size() {
            return comparisons.size();
        }

        List<Comparison> firstPass() {
            return List.copyOf(comparisons);
        }

        /** Gives every comparison its final state, retrying (with {@code replay}) at most {@link #RETRY_SLOTS} eligible rejections, in capture order. Once per ledger. */
        List<Outcome> resolve(TablePixelNoiseEnvelopes.Table table, Replay replay) {
            if (outcomes != null) {
                throw new IllegalStateException("the ledger is already resolved");
            }
            List<Outcome> out = new ArrayList<>();
            for (Comparison c : comparisons) {
                PixelPolicy.Verdict verdict = c.verdict();
                if (verdict.exact()) {
                    out.add(new Outcome(c, State.IDENTICAL, "identical", null, null, null, 0));
                    continue;
                }
                if (verdict.accepted()) {
                    out.add(new Outcome(c, State.TOLERATED, verdict.describe(), null, null, null, 0));
                    continue;
                }
                TablePixelNoiseEnvelopes.Eligibility eligibility = eligibility(table, c);
                if (!eligibility.eligible()) {
                    out.add(new Outcome(c, State.NOT_ELIGIBLE, eligibility.reason(), eligibility, null, null, 0));
                    continue;
                }
                if (slotsUsed >= RETRY_SLOTS) {
                    out.add(new Outcome(c, State.BUDGET_EXCEEDED, "eligible, but both retry slots of this run are used, so it is not retried", eligibility, null, null, 0));
                    continue;
                }
                slotsUsed++;
                replaysStarted++;
                long started = System.nanoTime();
                try {
                    Capture retry = replay.replay(c);
                    PixelPolicy.Verdict retried = PixelPolicy.compare(retry.legacyPng(), retry.migratedPng(), PixelPolicy.forCase(c.caseKey()), retry.legacyGeometry());
                    long ms = (System.nanoTime() - started) / 1_000_000L;
                    out.add(retried.accepted()
                            ? new Outcome(c, State.RECOVERED, "the retry pair was " + (retried.exact() ? "identical" : "tolerated: " + retried.describe()), eligibility, retry, retried, ms)
                            : new Outcome(c, State.FAILED_AFTER_RETRY, "the retry pair was rejected too: " + retried.describe(), eligibility, retry, retried, ms));
                } catch (Exception | AssertionError failure) {
                    long ms = (System.nanoTime() - started) / 1_000_000L;
                    out.add(new Outcome(c, State.RETRY_INFRA_FAILURE, "the retry could not capture the pair (infrastructure failure): " + failure, eligibility, null, null, ms));
                }
            }
            outcomes = List.copyOf(out);
            verifyAccounting();
            return outcomes;
        }

        /** Every comparison has exactly one final state that follows from its first-pass verdict; no rejection is lost; the slots bound the retries. */
        private void verifyAccounting() {
            if (outcomes.size() != comparisons.size()) {
                throw new IllegalStateException(outcomes.size() + " final states for " + comparisons.size() + " comparisons");
            }
            int recovered = 0;
            for (int i = 0; i < comparisons.size(); i++) {
                Comparison c = comparisons.get(i);
                Outcome o = outcomes.get(i);
                if (o.comparison() != c) {
                    throw new IllegalStateException("final state " + i + " is not for comparison " + i);
                }
                State expectedFirst = c.verdict().exact() ? State.IDENTICAL : c.verdict().accepted() ? State.TOLERATED : null;
                if (expectedFirst != null ? o.state() != expectedFirst : o.state() == State.IDENTICAL || o.state() == State.TOLERATED) {
                    throw new IllegalStateException(c.label() + ": the final state " + o.state() + " does not follow from its first-pass verdict");
                }
                recovered += o.state() == State.RECOVERED ? 1 : 0;
            }
            if (recovered > RETRY_SLOTS || slotsUsed > RETRY_SLOTS || replaysStarted != slotsUsed) {
                throw new IllegalStateException("retry slots: " + slotsUsed + " used, " + replaysStarted + " retries started, " + recovered + " recovered");
            }
        }

        List<Outcome> outcomes() {
            if (outcomes == null) {
                throw new IllegalStateException("the ledger is not resolved");
            }
            return outcomes;
        }

        int slotsUsed() {
            return slotsUsed;
        }

        int replaysStarted() {
            return replaysStarted;
        }

        List<Outcome> withState(State state) {
            return outcomes().stream().filter(o -> o.state() == state).toList();
        }

        /** A digest of the first pass: every comparison's order, label and verdict, in capture order. Two runs that judged every comparison alike have the same digest. */
        String firstPassDigest() {
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                for (Comparison c : comparisons) {
                    sha.update((c.order() + "|" + c.label() + "|" + verdictName(c.verdict()) + "|" + c.verdict().describe() + "\n").getBytes(StandardCharsets.UTF_8));
                }
                return HexFormat.of().formatHex(sha.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        String summary() {
            long withTailwind = comparisons.stream().filter(c -> c.mode() == TablePixelNoiseEnvelopes.Mode.WITH_TAILWIND).count();
            long rejected = comparisons.stream().filter(c -> !c.verdict().accepted()).count();
            long tolerated = comparisons.stream().filter(c -> c.verdict().accepted() && !c.verdict().exact()).count();
            return String.format("first pass: %d comparisons (%d with Tailwind, %d without), %d identical, %d tolerated, %d rejected; retry slots used %d of %d; recovered %d, "
                            + "failed after retry %d, not eligible %d, infrastructure %d, over budget %d", comparisons.size(), withTailwind, comparisons.size() - withTailwind,
                    comparisons.size() - rejected - tolerated, tolerated, rejected, slotsUsed, RETRY_SLOTS, withState(State.RECOVERED).size(),
                    withState(State.FAILED_AFTER_RETRY).size(), withState(State.NOT_ELIGIBLE).size(), withState(State.RETRY_INFRA_FAILURE).size(),
                    withState(State.BUDGET_EXCEEDED).size());
        }

        /** The report: the summary, the digest, and every first-pass rejection with its evidence and final state (recovered ones included), pictures within the budget. */
        String report(String prefix, PixelDiagnostics.PictureBudget pictures) {
            StringBuilder out = new StringBuilder();
            out.append(prefix).append(' ').append(summary()).append('\n');
            out.append(prefix).append(" first-pass digest ").append(firstPassDigest()).append('\n');
            for (Outcome o : outcomes()) {
                Comparison c = o.comparison();
                if (c.verdict().accepted()) {
                    continue;
                }
                out.append(prefix).append(o.state() == State.RECOVERED ? " UNSTABLE (recovered) [" : " rejection [").append(c.order()).append("] ").append(c.label()).append(": ")
                        .append(measure(c.first())).append(" | policy said: ").append(c.verdict().describe()).append(" | eligibility: ")
                        .append(o.eligibility() == null ? "-" : (o.eligibility().eligible() ? "eligible, " : "not eligible, ") + o.eligibility().reason()).append(" | final: ")
                        .append(o.state()).append(": ").append(o.reason()).append('\n');
                if (o.eligibility() != null && !o.eligibility().checks().isEmpty()) {
                    out.append(prefix).append("   envelope checks [").append(c.order()).append("]: ").append(String.join("; ", o.eligibility().checks())).append('\n');
                }
                if (o.retry() != null) {
                    out.append(prefix).append("   retry [").append(c.order()).append("]: ").append(o.retryMillis()).append(" ms | retry pair: ").append(measure(o.retry()))
                            .append(" | reproduced the first legacy picture: ").append(o.retry().legacyPng().equals(c.first().legacyPng())).append(", the first migrated picture: ")
                            .append(o.retry().migratedPng().equals(c.first().migratedPng())).append('\n');
                }
                out.append(pictures.line(prefix, "[" + c.order() + "] first legacy", c.first().legacyPng())).append('\n');
                out.append(pictures.line(prefix, "[" + c.order() + "] first migrated", c.first().migratedPng())).append('\n');
                if (o.retry() != null) {
                    out.append(pictures.line(prefix, "[" + c.order() + "] retry legacy", o.retry().legacyPng())).append('\n');
                    out.append(pictures.line(prefix, "[" + c.order() + "] retry migrated", o.retry().migratedPng())).append('\n');
                }
            }
            return out.toString();
        }

        private static String measure(Capture capture) {
            try {
                return PixelDiagnostics.measure(capture.legacyPng(), capture.migratedPng()).describe();
            } catch (IOException | RuntimeException e) {
                return "not measurable: " + e;
            }
        }
    }

    static String verdictName(PixelPolicy.Verdict v) {
        return v.exact() ? "identical" : v.accepted() ? "tolerated" : "rejected";
    }
}
