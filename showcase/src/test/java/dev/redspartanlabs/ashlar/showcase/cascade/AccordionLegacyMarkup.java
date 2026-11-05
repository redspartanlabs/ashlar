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
 * The pre-migration Tailwind class strings of Accordion and Accordion Item (migrated together), read from {@code /parity/accordion/accordion-legacy.html} (the one
 * source of truth for what "legacy" means), and the rewrite that turns migrated markup back into the markup the templates rendered before.
 *
 * <p>Two legacy parts depended on state, so the rewrite reads the state attributes: a chevron had {@code rotate-90} while the {@code aria-expanded} of the button
 * before it was {@code "true"}, and a panel carried an inline {@code grid-template-rows} of {@code 0fr} while it had the {@code inert} attribute and {@code 1fr}
 * otherwise. The item, the heading and the title span had no classes before, so their whole class attribute is removed. Everything else on the page (the
 * {@code data-*} hooks, the ARIA attributes, the caller's content, the Icon inside a chevron) is left exactly as served. It fails loudly when a class value that carries
 * one of the two components' classes is not a known part.
 */
final class AccordionLegacyMarkup {

    /** The parts that had Tailwind classes. */
    static final List<String> ROLES = List.of("accordion", "accordion-button", "accordion-chevron", "accordion-chevron-open", "accordion-panel", "accordion-clip", "accordion-content");

    /** The parts that had no class before the migration: the rewrite removes the whole class attribute. */
    static final Set<String> CLASSLESS = Set.of("item", "heading", "title");

    /** The migrated class value of each part, exactly as the templates write it. */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("accordion", "ash-accordion"),
            Map.entry("item", "ash-accordion__item"),
            Map.entry("heading", "ash-accordion__heading"),
            Map.entry("accordion-button", "ash-accordion__button"),
            Map.entry("title", "ash-accordion__title"),
            Map.entry("accordion-chevron", "ash-accordion__chevron"),
            Map.entry("accordion-panel", "ash-accordion__panel"),
            Map.entry("accordion-clip", "ash-accordion__clip"),
            Map.entry("accordion-content", "ash-accordion__content"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-accordion(?![a-zA-Z0-9])(?:__[a-z]+)?");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern INERT = Pattern.compile("\\sinert(?=[\\s=>/])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    AccordionLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = AccordionLegacyMarkup.class.getResourceAsStream("/parity/accordion/accordion-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/accordion/accordion-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("accordion-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("accordion-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** How many migrated parts of one kind {@code html} contains (a class attribute that is exactly the part's class). */
    static int count(String html, String role) {
        String migrated = MIGRATED.get(role);
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).trim().equals(migrated)) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Accordion and Accordion Item part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> roleByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> roleByMigrated.put(migrated, role));

        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean lastButtonExpanded = false;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find()) {
                continue;
            }
            String value = cls.group(1).trim();
            if (!OWN_TOKEN.matcher(value).find()) {
                continue;
            }
            String role = roleByMigrated.get(value);
            if (role == null) {
                throw new IllegalStateException("a class value that carries an Accordion or Accordion Item class but is not a known part: " + value);
            }
            String attribute = "class=\"" + cls.group(1) + "\"";
            String rewritten;
            switch (role) {
                case "item", "heading", "title" -> rewritten = tag.replaceFirst(" " + Pattern.quote(attribute), "");
                case "accordion-button" -> {
                    lastButtonExpanded = tag.contains("aria-expanded=\"true\"");
                    rewritten = tag.replace(attribute, "class=\"" + classes(role) + "\"");
                }
                case "accordion-chevron" -> rewritten = tag.replace(attribute, "class=\"" + classes(lastButtonExpanded ? "accordion-chevron-open" : "accordion-chevron") + "\"");
                case "accordion-panel" -> {
                    String rows = INERT.matcher(tag).find() ? "0fr" : "1fr";
                    rewritten = tag.replace(attribute, "style=\"grid-template-rows: " + rows + "\" class=\"" + classes(role) + "\"");
                }
                default -> rewritten = tag.replace(attribute, "class=\"" + classes(role) + "\"");
            }
            out.append(html, cursor, tags.start()).append(rewritten);
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
