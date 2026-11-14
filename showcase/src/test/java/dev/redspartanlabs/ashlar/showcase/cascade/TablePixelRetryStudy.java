package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TEMPORARY DIAGNOSTIC, not for merge: the analysis of the fresh-browser retry study ({@link TablePixelRetryStudyTest}, {@link TablePixelRetryControlStudyTest}). It decides
 * nothing for {@link TablePixelTest}; it describes what candidate retry procedures would have done with recorded captures.
 *
 * <p>Units. A browser process is the independent unit. A primary attempt is one process replaying one of TablePixelTest's browser blocks; its 12 judged pairs are clustered in
 * that process. A procedure is one rejected pair plus its retries, each retry a process of its own; procedures share no capture. Retry counts n = 1..3 are read as prefixes of
 * the same retry sequence (nested, not independent), and the three policies are evaluated on the same captures of a procedure (paired, not independent). No complete
 * TablePixelTest run is simulated.
 *
 * <p>Policies, each with up to n retries in one mode (case-only or block-replay), accepting a procedure when any of its first n retries gives an accepted pair: P1 regenerates both
 * sides (retry legacy against retry migrated); P2 keeps the primary legacy picture and regenerates the migrated one; P3 keeps the primary migrated picture and regenerates the
 * legacy one. Accepted means what {@link PixelPolicy} accepts today (identical, or within a named tolerance); the policy itself is used read-only.
 */
final class TablePixelRetryStudy {

    private TablePixelRetryStudy() {
    }

    static final int MAX_RETRIES = 3;
    static final List<String> MODES = List.of("case-only", "block-replay");
    static final List<String> POLICIES = List.of("P1 both sides", "P2 migrated only", "P3 legacy only");

    /** Every distinct picture, once, by the SHA-256 of its base64 text. */
    static final class Pictures {
        private final Map<String, String> byId = new LinkedHashMap<>();

        String add(String base64) {
            String id = sha256(base64.getBytes(StandardCharsets.US_ASCII));
            byId.putIfAbsent(id, base64);
            return id;
        }

        String get(String id) {
            return byId.get(id);
        }

        int size() {
            return byId.size();
        }

        Map<String, String> all() {
            return byId;
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String id(String pictureId) {
        return pictureId == null ? "-" : pictureId.substring(0, 10);
    }

    /**
     * One legacy/migrated pair as captured: which unit and process it came from, its mode ({@code primary}, or a retry mode) and retry number (0 for the primary), the case and
     * the legacy page's navigation slot, the two picture ids, the legacy geometry (the policy's reference), whether each page's two screenshots agreed, and whether the injected
     * defect (if any) was present on the migrated page.
     */
    record Pair(String unit, String mode, int retry, int process, String config, int block, String caseKey, String fieldId, int legacySlot, String legacy, String migrated,
                String legacyGeometry, boolean legacyStable, boolean migratedStable, boolean defectPresent) {
    }

    /** One rejected primary pair and its retries. */
    record Procedure(String id, Pair primary, List<Pair> retries) {
    }

    static PixelPolicy.Verdict judge(Pictures p, String caseKey, String legacy, String migrated, String legacyGeometry) {
        try {
            return PixelPolicy.compare(p.get(legacy), p.get(migrated), PixelPolicy.forCase(caseKey), PixelPolicy.Geometry.parse(legacyGeometry == null ? "" : legacyGeometry));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static PixelPolicy.Verdict judge(Pictures p, Pair pair) {
        return judge(p, pair.caseKey(), pair.legacy(), pair.migrated(), pair.legacyGeometry());
    }

    static String verdictName(PixelPolicy.Verdict v) {
        return v.exact() ? "identical" : v.accepted() ? "tolerated" : "REJECTED";
    }

    /** Whether the policy, with up to n retries in the mode, accepts the procedure. */
    static boolean accepts(Procedure pr, Pictures p, String policy, String mode, int n) {
        for (Pair r : pr.retries()) {
            if (!r.mode().equals(mode) || r.retry() > n) {
                continue;
            }
            PixelPolicy.Verdict v = switch (policy) {
                case "P1 both sides" -> judge(p, r.caseKey(), r.legacy(), r.migrated(), r.legacyGeometry());
                case "P2 migrated only" -> judge(p, r.caseKey(), pr.primary().legacy(), r.migrated(), pr.primary().legacyGeometry());
                case "P3 legacy only" -> judge(p, r.caseKey(), r.legacy(), pr.primary().migrated(), r.legacyGeometry());
                default -> throw new IllegalArgumentException(policy);
            };
            if (v.accepted()) {
                return true;
            }
        }
        return false;
    }

    /** Which picture of a rejected primary pair the retries did not reproduce (a description, not a cause). */
    static String deviatingSide(Procedure pr) {
        if (pr.retries().isEmpty()) {
            return "no retries";
        }
        Set<String> legacy = pr.retries().stream().map(Pair::legacy).collect(Collectors.toSet());
        Set<String> migrated = pr.retries().stream().map(Pair::migrated).collect(Collectors.toSet());
        boolean l = legacy.contains(pr.primary().legacy());
        boolean m = migrated.contains(pr.primary().migrated());
        return l && !m ? "the primary migrated picture was not seen again in any retry" : !l && m ? "the primary legacy picture was not seen again in any retry"
                : l ? "both primary pictures were seen again in retries" : "neither primary picture was seen again in any retry";
    }

    // ---------------------------------------------------------------- uncertainty

    /** P(X <= k) for X ~ Binomial(n, p). */
    static double cdf(int k, int n, double p) {
        if (k < 0) {
            return 0;
        }
        if (k >= n || p <= 0) {
            return 1;
        }
        if (p >= 1) {
            return 0;
        }
        double logC = 0;
        double sum = 0;
        for (int i = 0; i <= k; i++) {
            if (i > 0) {
                logC += Math.log(n - i + 1) - Math.log(i);
            }
            sum += Math.exp(logC + i * Math.log(p) + (n - i) * Math.log1p(-p));
        }
        return Math.min(1, sum);
    }

    /** The exact (Clopper-Pearson) two-sided 95% interval for k successes in n trials. */
    static double[] interval(int k, int n) {
        if (n == 0) {
            return new double[] {0, 1};
        }
        double lo = 0;
        double hi = 1;
        if (k > 0) {
            double a = 0;
            double b = 1;
            for (int i = 0; i < 80; i++) {
                double m = (a + b) / 2;
                if (1 - cdf(k - 1, n, m) < 0.025) {
                    a = m;
                } else {
                    b = m;
                }
            }
            lo = (a + b) / 2;
        }
        if (k < n) {
            double a = 0;
            double b = 1;
            for (int i = 0; i < 80; i++) {
                double m = (a + b) / 2;
                if (cdf(k, n, m) > 0.025) {
                    a = m;
                } else {
                    b = m;
                }
            }
            hi = (a + b) / 2;
        }
        return new double[] {lo, hi};
    }

    static String rate(int k, int n) {
        double[] ci = interval(k, n);
        return String.format("%d of %d (95%% interval %.3f-%.3f)", k, n, ci[0], ci[1]);
    }

    private static String measure(Pictures p, String a, String b) {
        try {
            PixelDiagnostics.Difference d = PixelDiagnostics.measure(p.get(a), p.get(b));
            return d.any() ? "differing=" + d.differing() + " maxChannel=" + d.maxChannel() + " bounds=x" + d.minX() + ".." + d.maxX() + ",y" + d.minY() + ".." + d.maxY()
                    : "pixel-identical";
        } catch (IOException e) {
            return "not measurable: " + e;
        }
    }

    // ---------------------------------------------------------------- the primary study

    /** The report of the primary attempts and their procedures. {@code beyondCap} counts rejected pairs that were recorded but not retried. */
    static String primaryReport(List<Pair> primary, List<Procedure> procedures, Pictures p, int beyondCap) {
        StringBuilder out = new StringBuilder();
        Map<Integer, List<Pair>> byAttempt = new LinkedHashMap<>();
        primary.forEach(x -> byAttempt.computeIfAbsent(x.process(), k -> new ArrayList<>()).add(x));
        int identical = 0;
        int tolerated = 0;
        int rejected = 0;
        int unstable = 0;
        List<Pair> rejectedPairs = new ArrayList<>();
        for (Pair x : primary) {
            PixelPolicy.Verdict v = judge(p, x);
            if (v.exact()) {
                identical++;
            } else if (v.accepted()) {
                tolerated++;
            } else {
                rejected++;
                rejectedPairs.add(x);
            }
            if (!x.legacyStable() || !x.migratedStable()) {
                unstable++;
            }
        }
        long attemptsWithRejection = byAttempt.values().stream().filter(l -> l.stream().anyMatch(rejectedPairs::contains)).count();
        out.append("== Primary attempts (independent unit: one browser process each) ==\n");
        out.append(String.format("%d attempts, %d judged pairs (clustered 12 per attempt): %d identical, %d tolerated, %d rejected%n", byAttempt.size(), primary.size(), identical,
                tolerated, rejected));
        out.append("attempts with at least one rejected pair: ").append(rate((int) attemptsWithRejection, byAttempt.size())).append('\n');
        out.append("pairs whose two screenshots of one page differed: ").append(unstable).append('\n');
        out.append("SP3 (within tolerance): ").append(tolerated).append(" tolerated pairs; no retry follows a tolerated pair (retries follow rejections only), so their verdict is unchanged\n");
        out.append("per block and configuration: attempts, rejected pairs\n");
        Map<String, int[]> cells = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Pair>> e : byAttempt.entrySet()) {
            Pair first = e.getValue().get(0);
            int[] c = cells.computeIfAbsent("block " + first.block() + " " + first.config(), k -> new int[2]);
            c[0]++;
            c[1] += (int) e.getValue().stream().filter(rejectedPairs::contains).count();
        }
        cells.forEach((k, c) -> out.append(String.format("  %-30s %3d attempts, %d rejected pairs%n", k, c[0], c[1])));
        out.append("rejected primary pairs:\n");
        for (Pair x : rejectedPairs) {
            out.append(String.format("  %s (process %d) block %d %s %s %s, navigations %d-%d: %s | %s%n", x.unit(), x.process(), x.block(), x.config(), x.fieldId(), x.caseKey(),
                    x.legacySlot(), x.legacySlot() + 1, measure(p, x.legacy(), x.migrated()), judge(p, x).describe()));
        }
        if (beyondCap > 0) {
            out.append("rejected pairs beyond the procedure cap (recorded, not retried): ").append(beyondCap).append('\n');
        }
        out.append(procedureReport(procedures, p));
        return out.toString();
    }

    /** Every procedure with every retry, the policy outcomes, and the aggregates. */
    static String procedureReport(List<Procedure> procedures, Pictures p) {
        StringBuilder out = new StringBuilder("\n== Procedures (one per rejected pair; every retry a separate browser process; procedures share no capture) ==\n");
        for (Procedure pr : procedures) {
            Pair x = pr.primary();
            out.append(String.format("%s: %s %s %s (primary process %d): %s%n", pr.id(), x.config(), x.fieldId(), x.caseKey(), x.process(), deviatingSide(pr)));
            for (Pair r : pr.retries()) {
                out.append(String.format("    retry %d %-12s process %3d: retry pair %s | primary legacy vs retry migrated %s | retry legacy vs primary migrated %s%s%n", r.retry(),
                        r.mode(), r.process(), verdictName(judge(p, r)),
                        verdictName(judge(p, r.caseKey(), x.legacy(), r.migrated(), x.legacyGeometry())),
                        verdictName(judge(p, r.caseKey(), r.legacy(), x.migrated(), r.legacyGeometry())),
                        r.legacyStable() && r.migratedStable() ? "" : " (a page's two screenshots differed)"));
            }
        }
        out.append("\n== Policy outcomes (procedures accepted / procedures; n are nested prefixes of the same retries; policies are paired on the same captures) ==\n");
        for (String policy : POLICIES) {
            for (String mode : MODES) {
                StringBuilder line = new StringBuilder(String.format("  %-17s %-12s", policy, mode));
                for (int n = 1; n <= MAX_RETRIES; n++) {
                    final int nn = n;
                    int k = (int) procedures.stream().filter(pr -> accepts(pr, p, policy, mode, nn)).count();
                    line.append(String.format(" | n=%d: %s", n, rate(k, procedures.size())));
                }
                out.append(line).append('\n');
            }
        }
        out.append("\n== Retry pairs themselves rejected, by mode (unit: one retry process) ==\n");
        for (String mode : MODES) {
            List<Pair> rs = procedures.stream().flatMap(pr -> pr.retries().stream()).filter(r -> r.mode().equals(mode)).toList();
            int k = (int) rs.stream().filter(r -> !judge(p, r).accepted()).count();
            out.append(String.format("  %-12s %s%n", mode, rate(k, rs.size())));
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- controls

    /** A deliberate visual difference: its id, whether it is a must-reject regression or a sensitivity probe, the case it is injected into, and what it changes. */
    record Control(String id, String kind, String caseKey, String defect) {
    }

    /** One execution of a control: the injected CSS (identical in every capture of that control and configuration) and its procedure (primary pair and retries). */
    record ControlRun(Control control, String config, int execution, String css, Procedure procedure, int dotChecks, int dotChecksPassed) {
    }

    /** The control report: baseline verdicts, classification, defect presence, and what every candidate procedure would have done. */
    static String controlReport(List<ControlRun> runs, Pictures p) {
        StringBuilder out = new StringBuilder("== Controls (each execution: a block replay with the defect on the target's migrated page, then every retry with the same defect) ==\n");
        Map<String, List<ControlRun>> byControl = new LinkedHashMap<>();
        runs.forEach(r -> byControl.computeIfAbsent(r.control().id(), k -> new ArrayList<>()).add(r));
        for (List<ControlRun> list : byControl.values()) {
            Control c = list.get(0).control();
            int executions = list.size();
            int baselineRejected = (int) list.stream().filter(r -> !judge(p, r.procedure().primary()).accepted()).count();
            List<Pair> all = list.stream().flatMap(r -> concat(r.procedure()).stream()).toList();
            int present = (int) all.stream().filter(Pair::defectPresent).count();
            int captures = all.size();
            int allRejected = (int) all.stream().filter(x -> !judge(p, x).accepted()).count();
            Set<String> csses = list.stream().collect(Collectors.groupingBy(ControlRun::config, Collectors.mapping(ControlRun::css, Collectors.toSet()))).values().stream()
                    .map(s -> s.size() == 1 ? "one" : "SEVERAL").collect(Collectors.toCollection(LinkedHashSet::new));
            int dotChecks = list.stream().mapToInt(ControlRun::dotChecks).sum();
            int dotPassed = list.stream().mapToInt(ControlRun::dotChecksPassed).sum();
            boolean mustReject = c.kind().equals("must-reject");
            String classification = mustReject && baselineRejected == executions ? "must-reject: the current policy rejected every primary pair"
                    : mustReject ? "RECLASSIFY as a sensitivity probe: the current policy accepted " + (executions - baselineRejected) + " of " + executions + " primary pairs"
                    : "sensitivity probe: the current policy rejected " + baselineRejected + " of " + executions + " primary pairs";
            out.append(String.format("%s %s on %s: %s%n", c.id(), c.defect(), c.caseKey(), classification));
            out.append(String.format("    defect present on %d of %d migrated captures; CSS per configuration: %s%s; pairs rejected by the current policy: %d of %d%n", present, captures,
                    csses, dotChecks > 0 ? "; injected pixels found in the captures: " + dotPassed + " of " + dotChecks : "", allRejected, captures));
            out.append("    primary pair measured: ").append(list.stream().map(r -> r.config() + " " + measure(p, r.procedure().primary().legacy(), r.procedure().primary().migrated()))
                    .distinct().collect(Collectors.joining("; "))).append('\n');
            // every execution and every one of its retries, with the process it ran in and the current policy's verdict on that retry's own pair
            for (ControlRun r : list) {
                StringBuilder line = new StringBuilder(String.format("    %s execution %d (primary process %d): primary %s", r.config(), r.execution(), r.procedure().primary().process(),
                        verdictName(judge(p, r.procedure().primary()))));
                for (String mode : MODES) {
                    line.append("; ").append(mode).append(": ").append(r.procedure().retries().stream().filter(x -> x.mode().equals(mode))
                            .map(x -> "r" + x.retry() + " p" + x.process() + " " + verdictName(judge(p, x))).collect(Collectors.joining(", ")));
                }
                out.append(line).append('\n');
            }
            for (String policy : POLICIES) {
                for (String mode : MODES) {
                    StringBuilder line = new StringBuilder(String.format("    %-17s %-12s", policy, mode));
                    boolean unacceptable = false;
                    for (int n = 1; n <= MAX_RETRIES; n++) {
                        final int nn = n;
                        int accepted = (int) list.stream().filter(r -> judge(p, r.procedure().primary()).accepted() || accepts(r.procedure(), p, policy, mode, nn)).count();
                        int changed = (int) list.stream().filter(r -> !judge(p, r.procedure().primary()).accepted() && accepts(r.procedure(), p, policy, mode, nn)).count();
                        line.append(String.format(" | n=%d accepted %d of %d", n, accepted, executions));
                        if (changed > 0) {
                            unacceptable = true;
                            line.append(" (").append(changed).append(" turned from rejected to accepted)");
                        }
                    }
                    if (unacceptable) {
                        line.append(mustReject ? "  <- UNACCEPTABLE: accepts a deterministic regression the current policy rejects" : "  <- differs from the current policy's verdict");
                    }
                    out.append(line).append('\n');
                }
            }
        }
        return out.toString();
    }

    private static List<Pair> concat(Procedure pr) {
        List<Pair> all = new ArrayList<>();
        all.add(pr.primary());
        all.addAll(pr.retries());
        return all;
    }
}
