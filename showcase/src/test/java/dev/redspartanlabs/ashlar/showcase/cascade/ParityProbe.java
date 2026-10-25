package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the shared in-page probe ({@code /cascade/parity-probe.js}) measured, parsed, and the comparison of two
 * measurements through {@link ParityNormalizer} (exact equality except its four documented, precondition-guarded
 * rules). A difference names the case, the layout, the property and both values, so a failure says what moved.
 */
final class ParityProbe {

    /** One measured element: its phase (which rendering), its position, what it is, and one value per property. */
    record Row(String phase, int ordinal, String kind, String variant, String context, String parentDisplay, String[] values, String diagnostics) {
    }

    record Probe(List<String> props, List<Row> rows) {
        List<Row> phase(String phase) {
            return rows.stream().filter(r -> r.phase().equals(phase)).toList();
        }

        String value(Row row, String property) {
            int index = props.indexOf(property);
            if (index < 0) {
                throw new IllegalArgumentException("property not measured: " + property);
            }
            return row.values()[index];
        }
    }

    record Finding(String where, String property, String expected, String actual) {
        String line() {
            return where + " | " + property + " | " + expected + " -> " + actual;
        }
    }

    private ParityProbe() {
    }

    /** The probe's result lines out of a dumped DOM (the element the probe appended is the last {@code <pre>} with that id). */
    static List<String> lines(String dom, String preId) {
        Matcher m = Pattern.compile("<pre id=\"" + preId + "\">(.*?)</pre>", Pattern.DOTALL).matcher(dom);
        String found = null;
        while (m.find()) {
            found = m.group(1);
        }
        if (found == null || found.isBlank()) {
            throw new IllegalStateException("no results in " + preId + " - infrastructure failure, not a pass");
        }
        return Arrays.asList(found.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&").split("\\R"));
    }

    static Probe parse(List<String> lines, String what) {
        List<String> propNames = List.of();
        List<Row> rows = new ArrayList<>();
        for (String line : lines) {
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "props" -> propNames = List.of(f[1].split(","));
                case "row" -> rows.add(new Row(f[1], Integer.parseInt(f[2]), f[3], f[4], f[5], f[6], f[7].split("\u001f", -1), f.length > 8 ? f[8] : ""));
                default -> throw new IllegalStateException(what + ": unrecognised result line: " + line);
            }
        }
        if (propNames.isEmpty()) {
            throw new IllegalStateException(what + ": the probe produced no property list - infrastructure failure, not a pass");
        }
        return new Probe(propNames, rows);
    }

    /**
     * Compares two renderings row by row. {@code applied} counts each normalization the comparison needed (by rule name, for
     * example {@code H2}), so a suite can pin exactly which it used and how often.
     */
    static List<Finding> compare(String where, List<String> propNames, List<Row> expected, List<Row> actual, Map<String, Integer> applied) {
        List<Finding> findings = new ArrayList<>();
        if (expected.size() != actual.size()) {
            findings.add(new Finding(where, "#rows", String.valueOf(expected.size()), String.valueOf(actual.size())));
            return findings;
        }
        int w = propNames.indexOf("@w");
        int h = propNames.indexOf("@h");
        for (int i = 0; i < expected.size(); i++) {
            Row e = expected.get(i);
            Row a = actual.get(i);
            String label = where + " #" + e.ordinal() + " " + e.kind() + " " + e.variant() + " [" + e.context() + "]"
                    + " expectedBox=" + e.values()[w] + "x" + e.values()[h] + " actualBox=" + a.values()[w] + "x" + a.values()[h]
                    + " expected{" + e.diagnostics() + "} actual{" + a.diagnostics() + "}";
            if (e.ordinal() != a.ordinal() || !e.kind().equals(a.kind()) || !e.variant().equals(a.variant())) {
                findings.add(new Finding(label, "#structure", e.ordinal() + e.variant(), a.ordinal() + a.variant()));
                continue;
            }
            ParityNormalizer.Result r = ParityNormalizer.compare(
                    ParityNormalizer.side(propNames, e.values(), e.parentDisplay()), ParityNormalizer.side(propNames, a.values(), a.parentDisplay()));
            r.remaining().forEach(d -> findings.add(new Finding(label, d.property(), d.expected(), d.actual())));
            r.applied().forEach(rule -> applied.merge(rule.split(" ")[0], 1, Integer::sum));
        }
        return findings;
    }
}
