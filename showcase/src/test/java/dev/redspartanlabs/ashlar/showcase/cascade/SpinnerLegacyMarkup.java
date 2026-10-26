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
 * The Spinner's pre-migration Tailwind class strings, read from {@code /parity/spinner/spinner-legacy.html} (the one source of
 * truth for what "legacy" means), and the rewrite that turns migrated Spinner markup back into the markup the component rendered
 * before.
 *
 * <p>A Spinner is three elements (the wrapper, the ring, and the optional screen-reader label), and the label's migrated class
 * ({@code ash-sr-only}) is shared with other components, so the rewrite works on a whole Spinner at a time: it recognises the
 * exact markup spinner.jte renders and replaces the three class values, and nothing else. It fails loudly when an
 * {@code ash-spinner} class is left over, rather than leaving some of it migrated.
 */
final class SpinnerLegacyMarkup {

    static final List<String> SIZES = List.of("small", "medium", "large", "xlarge");

    private static final Pattern LEGACY = Pattern.compile("<span data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern WRAPPER_ATTRIBUTE = Pattern.compile("class=\"ash-spinner\"");
    private static final Pattern SPINNER = Pattern.compile(
            "<span class=\"ash-spinner\"([^>]*)>(\\s*)<span aria-hidden=\"true\" class=\"ash-spinner__ring ash-spinner__ring--(small|medium|large|xlarge)\"></span>"
                    + "(?:(\\s*)<span class=\"ash-sr-only\">(.*?)</span>)?(\\s*)</span>", Pattern.DOTALL);

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    SpinnerLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = SpinnerLegacyMarkup.class.getResourceAsStream("/parity/spinner/spinner-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/spinner/spinner-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("spinner-legacy.html defines " + m.group(1) + " twice");
            }
        }
        List<String> required = new java.util.ArrayList<>(List.of("wrapper", "label"));
        SIZES.forEach(size -> required.add("ring-" + size));
        if (!legacyByRole.keySet().equals(Set.copyOf(required))) {
            throw new IllegalStateException("spinner-legacy.html must define exactly " + required + ", found " + legacyByRole.keySet());
        }
    }

    /** The legacy classes of a role ({@code wrapper}, {@code label} or {@code ring-<size>}). */
    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** The migrated class value of a ring of this size, exactly as SpinnerStyles writes it. */
    static String migratedRing(String size) {
        return "ash-spinner__ring ash-spinner__ring--" + size;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** How many migrated Spinners {@code html} contains. */
    static int count(String html) {
        Matcher m = WRAPPER_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** Rewrites every migrated Spinner in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        String out = SPINNER.matcher(html).replaceAll(m -> {
            StringBuilder sb = new StringBuilder();
            sb.append("<span class=\"").append(classes("wrapper")).append('"').append(m.group(1)).append('>').append(m.group(2));
            sb.append("<span aria-hidden=\"true\" class=\"").append(classes("ring-" + m.group(3))).append("\"></span>");
            if (m.group(5) != null) {
                sb.append(m.group(4)).append("<span class=\"").append(classes("label")).append("\">").append(m.group(5)).append("</span>");
            }
            sb.append(m.group(6)).append("</span>");
            return Matcher.quoteReplacement(sb.toString());
        });
        if (Pattern.compile("class=\"[^\"]*ash-spinner").matcher(out).find()) {
            throw new IllegalStateException("an ash-spinner class that is not part of a Spinner this rewrite recognises");
        }
        return out;
    }
}
