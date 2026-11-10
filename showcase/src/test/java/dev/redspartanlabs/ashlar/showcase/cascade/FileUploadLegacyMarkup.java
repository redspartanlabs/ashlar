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
 * The pre-migration Tailwind class strings of File Upload, read from {@code /parity/file-upload/file-upload-legacy.html} (the one source of truth for what "legacy" means), and the rewrite
 * that turns migrated markup back into the markup the template rendered before: the root, the card, the hidden input, the icon wrapper, the Icon's size classes (the one {@code svg}
 * that follows the icon wrapper), the status line, and the utility the Choose File span carried beside the Button's own classes. The field's label, required marker, help and error text
 * are {@code ash-field__*}, which is Text Entry's to rewrite (their legacy strings are exactly File Upload's: {@link FileUploadLegacyMarkupTest} proves it). Everything else on the page is
 * left exactly as served, and an unknown File Upload class fails loudly rather than leaving some of it migrated.
 *
 * <p>Apply it BEFORE {@link TextEntryLegacyMarkup} (the root carries {@code ash-field} beside {@code ash-file-upload}, a pair that rewrite refuses) and before {@link ButtonLegacyMarkup}
 * (the Choose File span carries one more class than a Button's own).
 */
final class FileUploadLegacyMarkup {

    static final List<String> ROLES = List.of("root", "label", "required", "zone", "input", "icon", "icon-svg", "status", "choose", "help", "error");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-file-upload(?:__[a-z-]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    FileUploadLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = FileUploadLegacyMarkup.class.getResourceAsStream("/parity/file-upload/file-upload-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/file-upload/file-upload-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("file-upload-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("file-upload-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** Rewrites every migrated File Upload part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean iconSvgNext = false;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            boolean hasClass = cls.find();
            String legacy = null;
            if (iconSvgNext && tag.startsWith("<svg")) {
                iconSvgNext = false;
                if (!hasClass || !cls.group(1).equals("ash-icon ash-icon--lg")) {
                    throw new IllegalStateException("the icon in the File Upload icon wrapper is not ash-icon ash-icon--lg: " + tag);
                }
                legacy = "ash-icon " + classes("icon-svg");
            } else if (hasClass && OWN_TOKEN.matcher(cls.group(1)).find()) {
                Set<String> tokens = Set.of(cls.group(1).trim().split("\\s+"));
                if (tokens.equals(Set.of("ash-field", "ash-file-upload"))) {
                    legacy = classes("root");
                } else if (tokens.equals(Set.of("ash-file-upload__zone"))) {
                    legacy = classes("zone");
                } else if (tokens.equals(Set.of("ash-file-upload__input", "ash-sr-only"))) {
                    legacy = classes("input");
                } else if (tokens.equals(Set.of("ash-file-upload__icon"))) {
                    legacy = classes("icon");
                    iconSvgNext = true;
                } else if (tokens.equals(Set.of("ash-file-upload__status"))) {
                    legacy = classes("status");
                } else if (tokens.equals(Set.of("ash-btn", "ash-btn--secondary", "ash-btn--sm", "ash-file-upload__choose"))) {
                    legacy = "ash-btn ash-btn--secondary ash-btn--sm " + classes("choose");
                } else {
                    throw new IllegalStateException("a class value that carries a File Upload class but is not a known part: " + cls.group(1));
                }
            }
            if (legacy == null) {
                continue;
            }
            out.append(html, cursor, tags.start()).append(tag.replace("class=\"" + cls.group(1) + "\"", "class=\"" + legacy + "\""));
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
