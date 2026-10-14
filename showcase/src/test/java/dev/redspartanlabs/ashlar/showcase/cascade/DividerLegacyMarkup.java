package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The Divider's pre-migration Tailwind class strings, read from {@code /parity/divider/divider-legacy.html} (the one
 * source of truth for what "legacy" means), and the rewrite that turns migrated Divider markup back into the markup
 * the component rendered before.
 *
 * <p>The rewrite only touches a divider's own opening tag. Everything else on the page is left exactly as served. It
 * fails loudly when a divider does not have the expected shape (the wrong element for its orientation, or an
 * orientation attribute that disagrees with its modifier), rather than leaving some of it migrated.
 */
final class DividerLegacyMarkup {

    static final List<String> ORIENTATIONS = List.of("horizontal", "vertical");

    private static final Pattern LEGACY = Pattern.compile("<(?:hr|div) data-legacy-role=\"([a-z]+)\" class=\"([^\"]+)\"");
    /** A divider's opening tag. The vertical one spans several lines, so attributes are matched across newlines. */
    private static final Pattern DIVIDER = Pattern.compile(
            "<(hr|div)\\b((?:(?!>)[\\s\\S])*?)class=\"ash-divider ash-divider--([a-z]+)\"((?:(?!>)[\\s\\S])*?)>");
    private static final Pattern ORIENTATION_ATTRIBUTE = Pattern.compile("data-divider-orientation=\"([a-z]+)\"");

    private final Map<String, String> roles = new LinkedHashMap<>();

    DividerLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = DividerLegacyMarkup.class.getResourceAsStream("/parity/divider/divider-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/divider/divider-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (roles.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("divider-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!roles.keySet().equals(Set.copyOf(ORIENTATIONS))) {
            throw new IllegalStateException("divider-legacy.html must define exactly " + ORIENTATIONS + ", found " + roles.keySet());
        }
    }

    /** The legacy classes of an orientation. */
    String classes(String orientation) {
        String found = roles.get(orientation);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + orientation);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return roles.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** The configuration the in-page probe needs to flip a divider to its legacy classes, as a JSON object. */
    String configJson() {
        return roles.entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    /** How many migrated dividers {@code html} contains. */
    static int count(String html) {
        Matcher m = DIVIDER.matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** Rewrites every migrated divider in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher m = DIVIDER.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String tag = m.group(1);
            String orientation = m.group(3);
            String expectedTag = switch (orientation) {
                case "horizontal" -> "hr";
                case "vertical" -> "div";
                default -> throw new IllegalStateException("unknown divider modifier ash-divider--" + orientation);
            };
            if (!tag.equals(expectedTag)) {
                throw new IllegalStateException("a " + orientation + " divider must be a <" + expectedTag + ">, found <" + tag + ">");
            }
            Matcher attribute = ORIENTATION_ATTRIBUTE.matcher(m.group(2) + " " + m.group(4));
            if (!attribute.find() || !attribute.group(1).equals(orientation)) {
                throw new IllegalStateException("divider modifier " + orientation + " does not match its data-divider-orientation");
            }
            out.append(html, cursor, m.start());
            out.append('<').append(tag).append(m.group(2)).append("class=\"").append(classes(orientation)).append('"').append(m.group(4)).append('>');
            cursor = m.end();
        }
        out.append(html.substring(cursor));
        String result = out.toString();
        if (Pattern.compile("class=\"[^\"]*\\bash-divider").matcher(result).find()) {
            throw new IllegalStateException("a divider was not fully rewritten to its legacy classes");
        }
        return result;
    }
}
