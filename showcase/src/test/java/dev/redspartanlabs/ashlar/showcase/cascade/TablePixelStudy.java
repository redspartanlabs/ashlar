package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TEMPORARY LOCAL DIAGNOSTIC, not for merge: the analysis of the Table pixel study ({@link TablePixelStudyTest}). It reports raw counts and distributions only, and keeps the
 * stability of one input (a variant captured again and again) apart from the comparison of two inputs (variants captured side by side, as {@link TablePixelTest} pairs them).
 *
 * <p>Screenshots taken by one browser process are not independent observations, so every count is given with the number of browser processes it comes from. No picture is treated
 * as the correct one: where one input gave several pictures, each is compared with the one listed first (the one seen first), which is a reference for description only.
 */
final class TablePixelStudy {

    private TablePixelStudy() {
    }

    /** L: the legacy rendering (legacy markup, the legacy stylesheet). Mm, Mp: the migrated rendering with master's and with PR #32's built stylesheet. */
    static final List<String> VARIANTS = List.of("L", "Mm", "Mp");
    static final List<List<String>> COMPARISONS = List.of(List.of("L", "Mm"), List.of("L", "Mp"), List.of("Mm", "Mp"));

    /**
     * One navigation: the case and configuration, the variant, which browser process (numbered from 1), which round in that process and which navigation it was in the process, and
     * the three screenshots as picture ids: the first and second taken one after the other once the state is forced (as {@link TablePixelTest} takes them), the third after the state
     * was removed and forced again with no navigation in between (for a case that forces no state: simply a third screenshot).
     */
    record Capture(String caseKey, String fieldId, boolean forced, String config, String variant, int process, int round, int slot, String first, String second, String third) {
    }

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
        return pictureId.substring(0, 10);
    }

    /** A pair of pictures that differ in their bytes, as measured (cached, and collected so that the caller can render them). */
    static final class Measurements {
        private final Pictures pictures;
        private final Map<String, PixelDiagnostics.Difference> cache = new LinkedHashMap<>();

        Measurements(Pictures pictures) {
            this.pictures = pictures;
        }

        PixelDiagnostics.Difference measure(String a, String b) {
            return cache.computeIfAbsent(a + "|" + b, k -> {
                try {
                    return PixelDiagnostics.measure(pictures.get(a), pictures.get(b));
                } catch (IOException e) {
                    throw new IllegalStateException("cannot decode " + id(a) + " or " + id(b), e);
                }
            });
        }

        String describe(String a, String b) {
            PixelDiagnostics.Difference d = measure(a, b);
            if (!d.any()) {
                return id(a) + " vs " + id(b) + ": bytes differ, " + (d.sameSize() ? "PIXELS IDENTICAL" : d.describe());
            }
            String samples = d.samples().stream().limit(4).map(Object::toString).collect(Collectors.joining(", "));
            return id(a) + " vs " + id(b) + ": differing=" + d.differing() + " maxChannel=" + d.maxChannel() + " bounds=x" + d.minX() + ".." + d.maxX() + ",y" + d.minY() + ".." + d.maxY()
                    + (d.sameSize() ? "" : " SIZES DIFFER") + " first pixels=[" + samples + "]";
        }

        /** The pairs measured so far, in order: {reference, other}. */
        List<String[]> pairs() {
            return cache.keySet().stream().map(k -> k.split("\\|")).toList();
        }
    }

    private static <T> List<T> distinct(List<Capture> captures, Function<Capture, T> key) {
        return new ArrayList<>(captures.stream().map(key).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private static String processes(List<Capture> captures) {
        return captures.stream().map(c -> String.valueOf(c.process())).distinct().collect(Collectors.joining(","));
    }

    /** The full report: totals, the stability of each input, and the side-by-side comparisons. */
    static String report(List<Capture> captures, Pictures pictures, Measurements m) {
        StringBuilder out = new StringBuilder();
        List<String> cases = distinct(captures, Capture::caseKey);
        List<String> configs = distinct(captures, Capture::config);
        long processes = captures.stream().map(Capture::process).distinct().count();
        out.append("Totals: ").append(captures.size()).append(" navigations, ").append(captures.size() * 3L).append(" screenshots, ").append(processes).append(" browser processes, ")
                .append(pictures.size()).append(" distinct pictures\n");

        out.append("\n== 1. One input captured repeatedly (stability; no comparison between variants) ==\n");
        out.append("cell: navigations (processes) | distinct first shots [navigations each] | processes whose own first shots differ | 2nd shot != 1st | 3rd shot (after re-forcing) != 1st\n");
        int[] forcedTotals = new int[5];
        int[] controlTotals = new int[5];
        for (String caseKey : cases) {
            for (String config : configs) {
                for (String variant : VARIANTS) {
                    List<Capture> cell = captures.stream().filter(c -> c.caseKey().equals(caseKey) && c.config().equals(config) && c.variant().equals(variant)).toList();
                    if (cell.isEmpty()) {
                        continue;
                    }
                    Map<String, List<Capture>> byPicture = cell.stream().collect(Collectors.groupingBy(Capture::first, LinkedHashMap::new, Collectors.toList()));
                    Map<Integer, Set<String>> perProcess = new LinkedHashMap<>();
                    cell.forEach(c -> perProcess.computeIfAbsent(c.process(), p -> new LinkedHashSet<>()).add(c.first()));
                    long varyingProcesses = perProcess.values().stream().filter(s -> s.size() > 1).count();
                    List<Capture> inPage = cell.stream().filter(c -> !c.second().equals(c.first())).toList();
                    List<Capture> reforced = cell.stream().filter(c -> !c.third().equals(c.first())).toList();
                    String counts = byPicture.values().stream().map(l -> String.valueOf(l.size())).collect(Collectors.joining(","));
                    out.append(String.format("%-30s %-15s %-3s %3d (%d) | %d [%s] | %d | %d | %d%s%n", caseKey, config, variant, cell.size(), perProcess.size(), byPicture.size(), counts,
                            varyingProcesses, inPage.size(), reforced.size(), cell.get(0).forced() ? "" : "   (no state forced)"));
                    int[] t = cell.get(0).forced() ? forcedTotals : controlTotals;
                    t[0] += cell.size();
                    t[1] += byPicture.size() > 1 ? 1 : 0;
                    t[2] += (int) varyingProcesses;
                    t[3] += inPage.size();
                    t[4] += reforced.size();
                    List<String> ids = new ArrayList<>(byPicture.keySet());
                    for (int i = 1; i < ids.size(); i++) {
                        List<Capture> seen = byPicture.get(ids.get(i));
                        out.append("      first shot ").append(m.describe(ids.get(0), ids.get(i))).append(" | seen ").append(seen.size()).append("x in process(es) ").append(processes(seen))
                                .append(", slots ").append(seen.stream().map(c -> String.valueOf(c.slot())).collect(Collectors.joining(","))).append('\n');
                    }
                    for (Capture c : inPage) {
                        out.append("      2nd shot, process ").append(c.process()).append(" slot ").append(c.slot()).append(": ").append(m.describe(c.first(), c.second())).append('\n');
                    }
                    for (Capture c : reforced) {
                        out.append("      3rd shot, process ").append(c.process()).append(" slot ").append(c.slot()).append(": ").append(m.describe(c.first(), c.third())).append('\n');
                    }
                }
            }
        }
        out.append(String.format("Forced-state cells: %d navigations; %d cells with more than one first shot; %d processes whose own first shots differ; %d 2nd-shot and %d re-forced differences%n",
                forcedTotals[0], forcedTotals[1], forcedTotals[2], forcedTotals[3], forcedTotals[4]));
        out.append(String.format("Control cells (no state forced): %d navigations; %d cells with more than one first shot; %d processes whose own first shots differ; %d 2nd-shot and %d 3rd-shot differences%n",
                controlTotals[0], controlTotals[1], controlTotals[2], controlTotals[3], controlTotals[4]));

        out.append("\n== 2. Two inputs side by side: first shots of two variants from the same process and the same round (how TablePixelTest pairs them) ==\n");
        out.append("cell: identical pairs / pairs | processes with a differing pair / processes | the distinct differing pairs\n");
        for (List<String> comparison : COMPARISONS) {
            String a = comparison.get(0);
            String b = comparison.get(1);
            out.append("-- ").append(a).append(" vs ").append(b).append('\n');
            int[] forced = new int[4];
            int[] control = new int[4];
            for (String caseKey : cases) {
                for (String config : configs) {
                    Map<String, Capture> left = new HashMap<>();
                    Map<String, Capture> right = new HashMap<>();
                    for (Capture c : captures) {
                        if (c.caseKey().equals(caseKey) && c.config().equals(config)) {
                            String key = c.process() + "/" + c.round();
                            if (c.variant().equals(a)) {
                                left.put(key, c);
                            } else if (c.variant().equals(b)) {
                                right.put(key, c);
                            }
                        }
                    }
                    if (left.isEmpty()) {
                        continue;
                    }
                    int pairs = 0;
                    int identical = 0;
                    Set<Integer> allProcesses = new LinkedHashSet<>();
                    Set<Integer> differingProcesses = new LinkedHashSet<>();
                    Map<String, Integer> differing = new LinkedHashMap<>();
                    boolean isForced = false;
                    for (Map.Entry<String, Capture> e : left.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(java.util.Comparator.comparingInt(Capture::process).thenComparingInt(Capture::slot))).toList()) {
                        Capture r = right.get(e.getKey());
                        if (r == null) {
                            continue;
                        }
                        Capture l = e.getValue();
                        isForced = l.forced();
                        pairs++;
                        allProcesses.add(l.process());
                        if (l.first().equals(r.first())) {
                            identical++;
                        } else {
                            differingProcesses.add(l.process());
                            differing.merge(l.first() + "|" + r.first(), 1, Integer::sum);
                        }
                    }
                    out.append(String.format("%-30s %-15s %d/%d | %d/%d%s%n", caseKey, config, identical, pairs, differingProcesses.size(), allProcesses.size(), isForced ? "" : "   (no state forced)"));
                    for (Map.Entry<String, Integer> d : differing.entrySet()) {
                        String[] ids = d.getKey().split("\\|");
                        out.append("      ").append(d.getValue()).append("x ").append(m.describe(ids[0], ids[1])).append('\n');
                    }
                    int[] t = isForced ? forced : control;
                    t[0] += pairs;
                    t[1] += pairs - identical;
                    t[2] += differingProcesses.size();
                    t[3] += allProcesses.size();
                }
            }
            out.append(String.format("   %s vs %s, forced-state cells: %d of %d pairs differ, in %d of %d process-cells; controls: %d of %d pairs differ, in %d of %d process-cells%n", a, b,
                    forced[1], forced[0], forced[2], forced[3], control[1], control[0], control[2], control[3]));
        }

        out.append("\n== 3. Which pictures each variant produced, per cell (sets of distinct first shots) ==\n");
        for (String caseKey : cases) {
            for (String config : configs) {
                Map<String, Set<String>> sets = new LinkedHashMap<>();
                for (String v : VARIANTS) {
                    sets.put(v, captures.stream().filter(c -> c.caseKey().equals(caseKey) && c.config().equals(config) && c.variant().equals(v)).map(Capture::first)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
                }
                out.append(String.format("%-30s %-15s %s%n", caseKey, config, sets.entrySet().stream()
                        .map(e -> e.getKey() + "={" + e.getValue().stream().map(TablePixelStudy::id).collect(Collectors.joining(",")) + "}").collect(Collectors.joining("  "))));
            }
        }
        return out.toString();
    }

    /** One line per navigation. */
    static String csv(List<Capture> captures) {
        StringBuilder sb = new StringBuilder("case,field,forced,config,variant,process,round,slot,first,second,third\n");
        for (Capture c : captures) {
            sb.append('"').append(c.caseKey()).append("\",").append(c.fieldId()).append(',').append(c.forced()).append(',').append(c.config()).append(',').append(c.variant()).append(',')
                    .append(c.process()).append(',').append(c.round()).append(',').append(c.slot()).append(',').append(id(c.first())).append(',').append(id(c.second())).append(',')
                    .append(id(c.third())).append('\n');
        }
        return sb.toString();
    }
}
