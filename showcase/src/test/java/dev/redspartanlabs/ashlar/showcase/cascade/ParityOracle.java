package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.Collection;
import java.util.List;

/**
 * Checks that make a legacy oracle trustworthy, shared by suites built on the harness.
 *
 * <p>A legacy rendering only means something if the stylesheet that draws it really contains the legacy classes. Tailwind
 * purges a class nothing scans, so a legacy class missing from the compiled oracle would render as nothing, and an
 * unstyled "legacy" would match an unstyled "migrated" and pass for the wrong reason. {@link #uncompiled} names every
 * token the stylesheet does not define.
 */
final class ParityOracle {

    private ParityOracle() {
    }

    /**
     * A class selector as the Tailwind CLI writes it: {@code :}, {@code /}, {@code .}, {@code [} and {@code ]} in the name are
     * backslash-escaped (an arbitrary value such as {@code border-[3px]} is written {@code .border-\[3px\]}).
     */
    private static String escapeClass(String token) {
        return "." + token.replaceAll("([:/.\\[\\]])", "\\\\$1");
    }

    /** Whether {@code css} defines a rule for the class {@code token}. */
    private static boolean compiles(String css, String token) {
        String selector = escapeClass(token);
        return css.contains(selector + "{") || css.contains(selector + ":") || css.contains(selector + ",");
    }

    /** The tokens of {@code tokens} that {@code css} has no rule for. An empty list means the oracle is not vacuous. */
    static List<String> uncompiled(String css, Collection<String> tokens) {
        return tokens.stream().filter(t -> !compiles(css, t)).toList();
    }
}
