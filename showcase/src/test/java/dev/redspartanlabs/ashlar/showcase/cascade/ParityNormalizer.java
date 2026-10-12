package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares the computed styles of one element before and after a component migration. Exact string
 * equality is the default for every property. Four narrow normalizations exist, each with explicit
 * preconditions, because a migrated component legitimately serializes the same rendering differently
 * in these cases and nowhere else:
 *
 * <ul>
 *   <li><b>H1 box-shadow</b>: Tailwind's ring utilities compute to a list that includes fully
 *       transparent zero-size layers. Layers exactly equal to {@value #TRANSPARENT_ZERO_LAYER} are
 *       dropped from both sides; the remaining lists must then be identical, in order.</li>
 *   <li><b>H2 border style</b>: a border style is invisible when the border width is 0. Ignored only
 *       when that side's computed width is {@code 0px} on both sides.</li>
 *   <li><b>H3 vertical-align</b>: ignored by layout on a flex or grid item. Ignored only when the
 *       element's parent is a flex or grid container on both sides.</li>
 *   <li><b>H4 border radius</b>: a radius at least half the shorter side of the box renders as a
 *       full pill whatever its value. Ignored only when both radii are at least half the shorter
 *       side of that side's own measured box.</li>
 * </ul>
 *
 * Nothing else is normalized, and any precondition that cannot be established (a missing property, an
 * unmeasured box) means no normalization.
 */
final class ParityNormalizer {

    static final String TRANSPARENT_ZERO_LAYER = "rgba(0, 0, 0, 0) 0px 0px 0px 0px";

    private static final Set<String> FLEX_OR_GRID = Set.of("flex", "inline-flex", "grid", "inline-grid");
    private static final Set<String> SIDES = Set.of("top", "right", "bottom", "left");
    private static final Set<String> CORNERS = Set.of("top-left", "top-right", "bottom-right", "bottom-left");

    private ParityNormalizer() {
    }

    /** One side of a comparison: computed properties, and the context the preconditions need. */
    record Side(Map<String, String> props, String parentDisplay) {
        double width() {
            return px(props.get("width"));
        }

        double height() {
            return px(props.get("height"));
        }
    }

    /** A property that still differs after the allowed normalizations. */
    record Difference(String property, String expected, String actual) {
    }

    /** {@code applied} names each normalization that was needed ("H1 box-shadow", ...), one entry per use. */
    record Result(List<Difference> remaining, List<String> applied) {
    }

    static Result compare(Side expected, Side actual) {
        List<Difference> remaining = new ArrayList<>();
        List<String> applied = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>(expected.props().keySet());
        names.addAll(actual.props().keySet());
        for (String name : names) {
            String e = expected.props().get(name);
            String a = actual.props().get(name);
            if (e != null && e.equals(a)) {
                continue;
            }
            String rule = normalizedBy(name, e, a, expected, actual);
            if (rule == null) {
                remaining.add(new Difference(name, e, a));
            } else {
                applied.add(rule + " " + name);
            }
        }
        return new Result(remaining, applied);
    }

    /** The rule that makes this difference harmless, or null when none does. */
    private static String normalizedBy(String name, String e, String a, Side expected, Side actual) {
        if (e == null || a == null) {
            return null;
        }
        if (name.equals("box-shadow")) {
            return shadowLayers(e).equals(shadowLayers(a)) ? "H1" : null;
        }
        if (name.startsWith("border-") && name.endsWith("-style")) {
            String side = name.substring("border-".length(), name.length() - "-style".length());
            if (SIDES.contains(side) && "0px".equals(expected.props().get("border-" + side + "-width"))
                    && "0px".equals(actual.props().get("border-" + side + "-width"))) {
                return "H2";
            }
            return null;
        }
        if (name.equals("vertical-align")) {
            return isFlexOrGridItem(expected) && isFlexOrGridItem(actual) ? "H3" : null;
        }
        if (name.startsWith("border-") && name.endsWith("-radius")) {
            String corner = name.substring("border-".length(), name.length() - "-radius".length());
            if (CORNERS.contains(corner) && isFullPill(e, expected) && isFullPill(a, actual)) {
                return "H4";
            }
        }
        return null;
    }

    /** The box-shadow layers that remain once fully transparent zero-size layers are dropped. */
    static List<String> shadowLayers(String boxShadow) {
        List<String> layers = new ArrayList<>();
        if (boxShadow == null || boxShadow.trim().equals("none")) {
            return layers;
        }
        int depth = 0;
        int start = 0;
        for (int i = 0; i < boxShadow.length(); i++) {
            char c = boxShadow.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                layers.add(boxShadow.substring(start, i).trim());
                start = i + 1;
            }
        }
        layers.add(boxShadow.substring(start).trim());
        layers.removeIf(TRANSPARENT_ZERO_LAYER::equals);
        return layers;
    }

    /** Unknown parent display (null) means the precondition cannot be established, so it is not a flex or grid item. */
    private static boolean isFlexOrGridItem(Side side) {
        return side.parentDisplay() != null && FLEX_OR_GRID.contains(side.parentDisplay());
    }

    private static boolean isFullPill(String radius, Side side) {
        double r = px(radius);
        double shorter = Math.min(side.width(), side.height());
        return !Double.isNaN(r) && !Double.isNaN(shorter) && shorter > 0 && r >= shorter / 2;
    }

    /** A computed pixel value such as {@code 12px} or {@code 3.35544e+07px}; NaN for anything else. */
    static double px(String value) {
        if (value == null || !value.endsWith("px")) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value.substring(0, value.length() - 2));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Builds a side from parallel name and value arrays (the probe's wire format). */
    static Side side(List<String> names, String[] values, String parentDisplay) {
        Map<String, String> props = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            props.put(names.get(i), values[i]);
        }
        return new Side(props, parentDisplay);
    }
}
