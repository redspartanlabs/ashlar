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
 * The pre-migration Tailwind class strings of Checkbox and Radio Group, read from {@code /parity/choice/choice-legacy.html} (the one source of truth for what "legacy" means),
 * and the rewrite that turns migrated markup back into the markup the templates rendered before.
 *
 * <p>Two things the templates used to choose are read from the markup the rewrite is given. A Radio Group's option label and description were given a disabled string when the group
 * was disabled OR the option was, so the rewrite looks at the option's own {@code disabled} attribute and at whether the enclosing {@code <fieldset>} is disabled. A control is
 * a checkbox or a radio by its {@code type}; a description or a label is a Radio option's when it sits inside a fieldset and a Checkbox's otherwise. The required marker
 * ({@code ash-field__required}) belongs to Text Entry's rewrite and is left for it. Everything else on the page is left exactly as served, and an unknown Checkbox or Radio Group
 * class fails loudly rather than leaving some of it migrated.
 */
final class ChoiceLegacyMarkup {

    static final List<String> ROLES = List.of("checkbox", "choice-row", "choice-body", "checkbox-input", "checkbox-label", "required", "checkbox-help", "checkbox-error", "radio-fieldset",
            "radio-legend", "radio-options", "radio-options-horizontal", "radio-input", "radio-label", "radio-label-disabled", "radio-description", "radio-description-disabled", "radio-help",
            "radio-error");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:checkbox|choice|radio-group)(?:__[a-z-]+)?(?:--[a-z]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern DISABLED = Pattern.compile("\\sdisabled(?:=\"[^\"]*\")?(?=[\\s>/])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    ChoiceLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = ChoiceLegacyMarkup.class.getResourceAsStream("/parity/choice/choice-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/choice/choice-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("choice-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("choice-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

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

    private static boolean insideFieldset(String html, int position) {
        return html.lastIndexOf("<fieldset", position) > html.lastIndexOf("</fieldset>", position);
    }

    private static boolean insideDisabledFieldset(String html, int position) {
        int open = html.lastIndexOf("<fieldset", position);
        if (open < 0 || open < html.lastIndexOf("</fieldset>", position)) {
            return false;
        }
        return DISABLED.matcher(html.substring(open, html.indexOf('>', open) + 1)).find();
    }

    /** Rewrites every migrated Checkbox and Radio Group part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean optionDisabled = false;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find() || !OWN_TOKEN.matcher(cls.group(1)).find()) {
                continue;
            }
            Set<String> tokens = Set.of(cls.group(1).trim().split("\\s+"));
            boolean inFieldset = insideFieldset(html, tags.start());
            String legacy;
            if (tokens.equals(Set.of("ash-checkbox"))) {
                legacy = classes("checkbox");
            } else if (tokens.equals(Set.of("ash-choice"))) {
                legacy = classes("choice-row");
            } else if (tokens.equals(Set.of("ash-choice__body"))) {
                legacy = classes("choice-body");
            } else if (tokens.equals(Set.of("ash-choice__input"))) {
                if (tag.contains("type=\"radio\"")) {
                    optionDisabled = DISABLED.matcher(tag).find();
                    legacy = classes("radio-input");
                } else {
                    legacy = classes("checkbox-input");
                }
            } else if (tokens.equals(Set.of("ash-choice__label"))) {
                if (inFieldset) {
                    legacy = classes(optionDisabled || insideDisabledFieldset(html, tags.start()) ? "radio-label-disabled" : "radio-label");
                } else {
                    legacy = classes("checkbox-label");
                }
            } else if (tokens.equals(Set.of("ash-choice__description"))) {
                if (inFieldset) {
                    legacy = classes(optionDisabled || insideDisabledFieldset(html, tags.start()) ? "radio-description-disabled" : "radio-description");
                } else {
                    legacy = classes("checkbox-help");
                }
            } else if (tokens.equals(Set.of("ash-choice__error"))) {
                legacy = classes("checkbox-error");
            } else if (tokens.equals(Set.of("ash-choice__description", "ash-radio-group__help"))) {
                legacy = classes("radio-help");
            } else if (tokens.equals(Set.of("ash-choice__error", "ash-radio-group__error"))) {
                legacy = classes("radio-error");
            } else if (tokens.equals(Set.of("ash-radio-group"))) {
                legacy = classes("radio-fieldset");
            } else if (tokens.equals(Set.of("ash-radio-group__legend"))) {
                legacy = classes("radio-legend");
            } else if (tokens.equals(Set.of("ash-radio-group__options"))) {
                legacy = classes("radio-options");
            } else if (tokens.equals(Set.of("ash-radio-group__options", "ash-radio-group__options--horizontal"))) {
                legacy = classes("radio-options-horizontal");
            } else {
                throw new IllegalStateException("a class value that carries a Checkbox or Radio Group class but is not a known part: " + cls.group(1));
            }
            out.append(html, cursor, tags.start()).append(tag.replace("class=\"" + cls.group(1) + "\"", "class=\"" + legacy + "\""));
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
