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
 * The Timeline's and the Timeline Item's pre-migration Tailwind class strings, read from {@code /parity/timeline/timeline-legacy.html} (the one
 * source of truth for what "legacy" means), and the rewrite that turns migrated Timeline markup back into the markup the components rendered
 * before.
 *
 * <p>The rewrite replaces each migrated class value with the legacy class value of the same part. The migrated marker carries the shared
 * marker class and one variant modifier, which together are the legacy marker's classes for that variant. The visually hidden status text
 * ({@code ash-sr-only}, used elsewhere by Alert and Spinner too) is rewritten only where it sits in a Timeline Item's title, so another
 * component's own rewrite is left alone. Everything else on the page, including the caller's projected content and the Icons' own markup
 * (which the Icon rewrite handles), is left exactly as served. It fails loudly when an {@code ash-timeline} class value is not one of the
 * known parts, rather than leaving some of it migrated.
 */
final class TimelineLegacyMarkup {

    static final List<String> VARIANTS = List.of("neutral", "info", "success", "warning", "danger");

    static final List<String> ROLES = List.of("timeline", "item", "rail", "marker-neutral", "marker-info", "marker-success", "marker-warning", "marker-danger",
            "connector", "body", "date", "title", "content", "status");

    /** The migrated class value of each part (apart from the status text, see above), exactly as the templates and {@code TimelineStyles} write it. */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("timeline", "ash-timeline"),
            Map.entry("item", "ash-timeline-item"),
            Map.entry("rail", "ash-timeline-item__rail"),
            Map.entry("marker-neutral", "ash-timeline-item__marker ash-timeline-item__marker--neutral"),
            Map.entry("marker-info", "ash-timeline-item__marker ash-timeline-item__marker--info"),
            Map.entry("marker-success", "ash-timeline-item__marker ash-timeline-item__marker--success"),
            Map.entry("marker-warning", "ash-timeline-item__marker ash-timeline-item__marker--warning"),
            Map.entry("marker-danger", "ash-timeline-item__marker ash-timeline-item__marker--danger"),
            Map.entry("connector", "ash-timeline-item__connector"),
            Map.entry("body", "ash-timeline-item__body"),
            Map.entry("date", "ash-timeline-item__date"),
            Map.entry("title", "ash-timeline-item__title"),
            Map.entry("content", "ash-timeline-item__content"),
            Map.entry("status", "ash-sr-only"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern TIMELINE_TOKEN = Pattern.compile("(?<![\\w-])ash-timeline");
    /** The status text sits first inside the title paragraph; this is the only place it is rewritten. */
    private static final Pattern STATUS = Pattern.compile("(<p class=\"ash-timeline-item__title\">\\s*)<span class=\"ash-sr-only\">");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    TimelineLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = TimelineLegacyMarkup.class.getResourceAsStream("/parity/timeline/timeline-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/timeline/timeline-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("timeline-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("timeline-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    /** The legacy classes of a part. */
    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** The legacy marker classes of a variant, as {@code TimelineStyles.marker} returned them. */
    String marker(String variant) {
        return classes("marker-" + variant);
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** How many migrated Timelines {@code html} contains. */
    static int count(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).trim().equals("ash-timeline")) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Timeline and Timeline Item in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> {
            if (!role.equals("status")) {
                legacyByMigrated.put(migrated, classes(role));
            }
        });
        String withStatus = STATUS.matcher(html).replaceAll(r -> Matcher.quoteReplacement(r.group(1) + "<span class=\"" + classes("status") + "\">"));
        Matcher m = CLASS_ATTRIBUTE.matcher(withStatus);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!TIMELINE_TOKEN.matcher(value).find()) {
                continue;
            }
            String legacy = legacyByMigrated.get(value);
            if (legacy == null) {
                throw new IllegalStateException("an ash-timeline class value that is not a known Timeline or Timeline Item part: " + value);
            }
            out.append(withStatus, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(withStatus.substring(cursor)).toString();
    }
}
