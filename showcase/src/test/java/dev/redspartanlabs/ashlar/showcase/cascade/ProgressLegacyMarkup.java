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
 * The Progress's pre-migration Tailwind class strings, read from {@code /parity/progress/progress-legacy.html} (the
 * one source of truth for what "legacy" means), and the rewrite that turns migrated Progress markup back into the
 * markup the component rendered before.
 *
 * <p>The rewrite only touches the {@code class} attribute of a Progress's own elements: each migrated class value is
 * replaced by the legacy class value of the same role. Everything else on the page is left exactly as served. It
 * fails loudly when a Progress does not have the expected shape (a size modifier that disagrees with the
 * {@code data-progress-size} attribute, or an {@code ash-progress} class that is not one of the known roles), rather
 * than leaving some of it migrated.
 */
final class ProgressLegacyMarkup {

    static final List<String> ROLES = List.of("wrapper", "header-between", "header-end", "label", "value",
            "track-small", "track-medium", "track-large", "fill");

    /** The migrated class value of each role, exactly as the component renders it. */
    static final Map<String, String> MIGRATED = Map.of(
            "wrapper", "ash-progress",
            "header-between", "ash-progress__header ash-progress__header--between",
            "header-end", "ash-progress__header ash-progress__header--end",
            "label", "ash-progress__label",
            "value", "ash-progress__value",
            "track-small", "ash-progress__track ash-progress__track--small",
            "track-medium", "ash-progress__track ash-progress__track--medium",
            "track-large", "ash-progress__track ash-progress__track--large",
            "fill", "ash-progress__fill");

    private static final Pattern LEGACY = Pattern.compile("<(?:div|span) data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern WRAPPER = Pattern.compile("<div class=\"ash-progress\" data-progress data-progress-size=\"([a-z]+)\">");
    private static final Pattern TRACK_MODIFIER = Pattern.compile("ash-progress__track--([a-z]+)");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    ProgressLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = ProgressLegacyMarkup.class.getResourceAsStream("/parity/progress/progress-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/progress/progress-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("progress-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("progress-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    /** The legacy classes of a role. */
    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** The configuration the in-page probe needs to flip a Progress to its legacy classes: migrated class value to legacy class value. */
    String mapJson() {
        return ROLES.stream().map(role -> "\"" + MIGRATED.get(role) + "\":\"" + classes(role) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    /** How many migrated Progress components {@code html} contains. */
    static int count(String html) {
        Matcher m = WRAPPER.matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** Rewrites every migrated Progress in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        checkSizes(html);
        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> legacyByMigrated.put(migrated, classes(role)));
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!Pattern.compile("\\bash-progress").matcher(value).find()) {
                continue;
            }
            String legacy = legacyByMigrated.get(value);
            if (legacy == null) {
                throw new IllegalStateException("an ash-progress class value that is not a known Progress role: " + value);
            }
            out.append(html, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }

    /** Each Progress's size attribute must agree with its track's modifier. */
    private static void checkSizes(String html) {
        List<int[]> bounds = new java.util.ArrayList<>();
        List<String> sizes = new java.util.ArrayList<>();
        Matcher wrappers = WRAPPER.matcher(html);
        while (wrappers.find()) {
            bounds.add(new int[] {wrappers.end(), html.length()});
            sizes.add(wrappers.group(1));
            if (bounds.size() > 1) {
                bounds.get(bounds.size() - 2)[1] = wrappers.start();
            }
        }
        for (int i = 0; i < bounds.size(); i++) {
            Matcher track = TRACK_MODIFIER.matcher(html.substring(bounds.get(i)[0], bounds.get(i)[1]));
            if (!track.find()) {
                throw new IllegalStateException("a Progress has no track modifier");
            }
            if (!track.group(1).equals(sizes.get(i))) {
                throw new IllegalStateException("track modifier " + track.group(1) + " does not match data-progress-size " + sizes.get(i));
            }
        }
    }
}
