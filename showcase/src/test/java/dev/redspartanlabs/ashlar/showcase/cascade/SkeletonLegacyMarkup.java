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
 * The Skeleton's pre-migration Tailwind class strings, read from {@code /parity/skeleton/skeleton-legacy.html} (the one source of
 * truth for what "legacy" means), and the rewrite that turns migrated Skeleton markup back into the markup the component rendered
 * before.
 *
 * <p>The rewrite only touches the {@code class} attribute of an element whose class carries an {@code ash-skeleton} token: the
 * known migrated value of each shape is replaced by the legacy class value of the same shape. Everything else on the page,
 * including the inline width and height, is left exactly as served. It fails loudly on an {@code ash-skeleton} class value it does
 * not know, rather than leaving some of it migrated.
 */
final class SkeletonLegacyMarkup {

    static final List<String> SHAPES = List.of("rectangle", "rounded");

    /** The migrated class value of each shape, exactly as SkeletonStyles writes it. */
    static final Map<String, String> MIGRATED = Map.of(
            "rectangle", "ash-skeleton",
            "rounded", "ash-skeleton ash-skeleton--rounded");

    private static final Pattern LEGACY = Pattern.compile("<div data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    SkeletonLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = SkeletonLegacyMarkup.class.getResourceAsStream("/parity/skeleton/skeleton-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/skeleton/skeleton-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("skeleton-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(SHAPES))) {
            throw new IllegalStateException("skeleton-legacy.html must define exactly " + SHAPES + ", found " + legacyByRole.keySet());
        }
    }

    /** The legacy classes of a shape. */
    String classes(String shape) {
        String found = legacyByRole.get(shape);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + shape);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** How many class attributes in {@code html} carry the {@code ash-skeleton} base class. */
    static int count(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (Arrays.asList(m.group(1).trim().split("\\s+")).contains("ash-skeleton")) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Skeleton in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((shape, migrated) -> legacyByMigrated.put(migrated, classes(shape)));
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!Pattern.compile("(?<![\\w-])ash-skeleton").matcher(value).find()) {
                continue;
            }
            String legacy = legacyByMigrated.get(value);
            if (legacy == null) {
                throw new IllegalStateException("an ash-skeleton class value that is not a known Skeleton shape: " + value);
            }
            out.append(html, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
