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
 * The pre-migration Tailwind class strings of the Text Entry family (Form Field, Text Input, Number Input, Textarea, Search Input), read from
 * {@code /parity/text-entry/text-entry-legacy.html} (the one source of truth for what "legacy" means), and the rewrite that turns migrated markup back into the markup the
 * templates rendered before.
 *
 * <p>The templates chose some of the legacy strings from their parameters, so the rewrite reads those choices from the markup it is given: a text input's {@code pl-3}/{@code pl-9}
 * and {@code pr-3}/{@code pr-9} (from the leading and trailing modifier classes), a textarea's {@code resize-y} or {@code resize-none} (from the fixed modifier), the display token the
 * search clear button carried ({@code hidden} without a value, {@code flex} with one, from the {@code hidden} attribute that now does that job, which the rewrite removes), and the
 * character count's red pair at its limit (from {@code data-at-limit}, which the rewrite removes). A control is told apart by its classes: the narrow modifier is a Number Input, the
 * textarea control a Textarea, the search field a Search Input, and anything else with the field control a Text Input. Everything else on the page (the {@code data-*} hooks, the ARIA
 * attributes, the caller's content, the Icon inside a field) is left exactly as served. It fails loudly when a class value that carries one of the family's classes is not a known part.
 */
final class TextEntryLegacyMarkup {

    /** The parts that had Tailwind classes. */
    static final List<String> ROLES = List.of("field", "label", "required", "help", "error", "box", "icon-start", "icon-end", "text-control", "number-control", "textarea-control",
            "search-control", "search-clear", "textarea-row", "textarea-help", "textarea-count", "textarea-count-limit", "variant-tokens");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:field|textarea|search-input)(?:__[a-z-]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern HIDDEN = Pattern.compile("\\shidden(?:=\"[^\"]*\")?(?=[\\s>/])");
    private static final Pattern AT_LIMIT = Pattern.compile("\\sdata-at-limit(?:=\"[^\"]*\")?(?=[\\s>/])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    TextEntryLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = TextEntryLegacyMarkup.class.getResourceAsStream("/parity/text-entry/text-entry-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/text-entry/text-entry-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("text-entry-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("text-entry-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    private static String swap(String classes, String from, String to) {
        List<String> tokens = Arrays.asList(classes.split("\\s+"));
        if (!tokens.contains(from)) {
            throw new IllegalStateException(from + " is not in the legacy string: " + classes);
        }
        return tokens.stream().map(t -> t.equals(from) ? to : t).collect(Collectors.joining(" "));
    }

    /** How many parts of the family {@code html} contains whose class attribute has exactly this token set (order-insensitive). */
    static int count(String html, String... tokens) {
        Set<String> wanted = Set.of(tokens);
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (Set.of(m.group(1).trim().split("\\s+")).equals(wanted)) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Text Entry part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find() || !OWN_TOKEN.matcher(cls.group(1)).find()) {
                continue;
            }
            Set<String> tokens = Set.of(cls.group(1).trim().split("\\s+"));
            String attribute = "class=\"" + cls.group(1) + "\"";
            String legacy;
            String body = tag;
            if (tokens.contains("ash-field__control")) {
                if (tokens.contains("ash-textarea__control")) {
                    legacy = classes("textarea-control");
                    if (tokens.contains("ash-textarea__control--fixed")) {
                        legacy = swap(legacy, "resize-y", "resize-none");
                    }
                    requireOnly(tokens, "ash-field__control", "ash-field__control--readonly", "ash-textarea__control", "ash-textarea__control--fixed");
                } else if (tokens.contains("ash-search-input__field")) {
                    legacy = classes("search-control");
                    requireOnly(tokens, "ash-field__control", "ash-search-input__field");
                } else if (tokens.contains("ash-field__control--narrow")) {
                    legacy = classes("number-control");
                    requireOnly(tokens, "ash-field__control", "ash-field__control--readonly", "ash-field__control--narrow");
                } else {
                    legacy = classes("text-control");
                    if (tokens.contains("ash-field__control--leading")) {
                        legacy = swap(legacy, "pl-3", "pl-9");
                    }
                    if (tokens.contains("ash-field__control--trailing")) {
                        legacy = swap(legacy, "pr-3", "pr-9");
                    }
                    requireOnly(tokens, "ash-field__control", "ash-field__control--readonly", "ash-field__control--leading", "ash-field__control--trailing");
                }
            } else if (tokens.equals(Set.of("ash-field"))) {
                legacy = classes("field");
            } else if (tokens.equals(Set.of("ash-field__label"))) {
                legacy = classes("label");
            } else if (tokens.equals(Set.of("ash-field__required"))) {
                legacy = classes("required");
            } else if (tokens.equals(Set.of("ash-field__help"))) {
                legacy = classes("help");
            } else if (tokens.equals(Set.of("ash-field__error"))) {
                legacy = classes("error");
            } else if (tokens.equals(Set.of("ash-field__box"))) {
                legacy = classes("box");
            } else if (tokens.equals(Set.of("ash-field__icon"))) {
                legacy = classes("icon-start");
            } else if (tokens.equals(Set.of("ash-field__icon", "ash-field__icon--end"))) {
                legacy = classes("icon-end");
            } else if (tokens.equals(Set.of("ash-search-input__clear"))) {
                boolean hidden = HIDDEN.matcher(tag).find();
                legacy = (hidden ? "hidden " : "flex ") + classes("search-clear");
                body = HIDDEN.matcher(tag).replaceFirst("");
            } else if (tokens.equals(Set.of("ash-textarea__row"))) {
                legacy = classes("textarea-row");
            } else if (tokens.equals(Set.of("ash-textarea__help"))) {
                legacy = classes("textarea-help");
            } else if (tokens.equals(Set.of("ash-textarea__count"))) {
                boolean limit = AT_LIMIT.matcher(tag).find();
                legacy = classes(limit ? "textarea-count-limit" : "textarea-count");
                body = AT_LIMIT.matcher(tag).replaceFirst("");
            } else {
                throw new IllegalStateException("a class value that carries a Text Entry class but is not a known part: " + cls.group(1));
            }
            String rewritten = body.replace(attribute, "class=\"" + legacy + "\"");
            out.append(html, cursor, tags.start()).append(rewritten);
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }

    private static void requireOnly(Set<String> actual, String... allowed) {
        Set<String> extra = new TreeSet<>(actual);
        extra.removeAll(Set.of(allowed));
        if (!extra.isEmpty()) {
            throw new IllegalStateException("unexpected Text Entry classes " + extra + " on a control");
        }
    }
}
