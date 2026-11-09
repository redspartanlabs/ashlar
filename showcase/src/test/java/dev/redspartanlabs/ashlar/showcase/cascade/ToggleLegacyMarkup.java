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
 * The pre-migration Tailwind class strings of Toggle, read from {@code /parity/toggle/toggle-legacy.html} (the one source of truth for what "legacy" means), and the rewrite that
 * turns migrated markup back into the markup the template rendered before. The label's second string (the text before the switch) is chosen by the migrated modifier class. The
 * required marker ({@code ash-field__required}) belongs to Text Entry's rewrite and is left for it. Everything else on the page is left exactly as served, and an unknown Toggle
 * class fails loudly rather than leaving some of it migrated.
 *
 * <p>Apply it BEFORE {@link ChoiceLegacyMarkup}: a Toggle's help and error text carry the shared {@code ash-choice__description} / {@code ash-choice__error} class beside their own, a
 * pair the Choice rewrite refuses.
 */
final class ToggleLegacyMarkup {

    static final List<String> ROLES = List.of("toggle", "label", "label-start", "switch", "input", "track", "thumb", "text", "required", "help", "error");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-toggle(?:__[a-z-]+)?(?:--[a-z]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    ToggleLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = ToggleLegacyMarkup.class.getResourceAsStream("/parity/toggle/toggle-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/toggle/toggle-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("toggle-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("toggle-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** Rewrites every migrated Toggle part in {@code html} to its legacy classes. Everything else is untouched. */
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
            String legacy;
            if (tokens.equals(Set.of("ash-toggle"))) {
                legacy = classes("toggle");
            } else if (tokens.equals(Set.of("ash-toggle__label"))) {
                legacy = classes("label");
            } else if (tokens.equals(Set.of("ash-toggle__label", "ash-toggle__label--start"))) {
                legacy = classes("label-start");
            } else if (tokens.equals(Set.of("ash-toggle__switch"))) {
                legacy = classes("switch");
            } else if (tokens.equals(Set.of("ash-toggle__input", "ash-sr-only"))) {
                legacy = classes("input");
            } else if (tokens.equals(Set.of("ash-toggle__track"))) {
                legacy = classes("track");
            } else if (tokens.equals(Set.of("ash-toggle__thumb"))) {
                legacy = classes("thumb");
            } else if (tokens.equals(Set.of("ash-toggle__text"))) {
                legacy = classes("text");
            } else if (tokens.equals(Set.of("ash-choice__description", "ash-toggle__help"))) {
                legacy = classes("help");
            } else if (tokens.equals(Set.of("ash-choice__error", "ash-toggle__error"))) {
                legacy = classes("error");
            } else {
                throw new IllegalStateException("a class value that carries a Toggle class but is not a known part: " + cls.group(1));
            }
            out.append(html, cursor, tags.start()).append(tag.replace("class=\"" + cls.group(1) + "\"", "class=\"" + legacy + "\""));
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
