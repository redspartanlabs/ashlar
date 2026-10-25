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
 * The Alert's pre-migration Tailwind class strings, read from {@code /parity/alert/alert-legacy.html} (the one
 * source of truth for what "legacy" means), and the rewrite that turns migrated Alert markup back into the markup
 * the component rendered before.
 *
 * <p>The rewrite only touches the inside of an Alert: each {@code <div class="ash-alert ash-alert--v" ...>} up to
 * its own closing tag. Everything else on the page is left exactly as served. It fails loudly when an Alert does
 * not have the expected shape, rather than leaving some of it migrated.
 */
final class AlertLegacyMarkup {

    static final List<String> VARIANTS = List.of("info", "success", "warning", "error");

    private static final Pattern LEGACY = Pattern.compile(
            "<(?:div|svg|span|h3) data-legacy-role=\"([a-z-]+)\"(?: data-legacy-variant=\"([a-z]+)\")? class=\"([^\"]+)\"");
    private static final Pattern CONTAINER = Pattern.compile(
            "<div class=\"ash-alert ash-alert--([a-z]+)\" data-alert data-alert-variant=\"([a-z]+)\">");
    private static final Pattern DIV_TAG = Pattern.compile("<(/?)div\\b");

    /** role -> variant ("" for roles that do not depend on it) -> legacy classes. */
    private final Map<String, Map<String, String>> roles = new LinkedHashMap<>();

    AlertLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = AlertLegacyMarkup.class.getResourceAsStream("/parity/alert/alert-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/alert/alert-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            roles.computeIfAbsent(m.group(1), k -> new LinkedHashMap<>()).put(m.group(2) == null ? "" : m.group(2), m.group(3));
        }
        for (String role : List.of("container", "icon", "title", "body-titled", "body-plain")) {
            if (!roles.containsKey(role) || !roles.get(role).keySet().equals(Set.copyOf(VARIANTS))) {
                throw new IllegalStateException("alert-legacy.html must define " + role + " for " + VARIANTS + ", found " + roles.get(role));
            }
        }
        for (String role : List.of("content", "label")) {
            if (roles.get(role) == null || roles.get(role).size() != 1 || !roles.get(role).containsKey("")) {
                throw new IllegalStateException("alert-legacy.html must define " + role + " exactly once");
            }
        }
    }

    /** The legacy classes of a role; {@code variant} is ignored for the roles that do not depend on it. */
    String classes(String role, String variant) {
        Map<String, String> byVariant = roles.get(role);
        String found = byVariant.size() == 1 && byVariant.containsKey("") ? byVariant.get("") : byVariant.get(variant);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role + " / " + variant);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return roles.values().stream().flatMap(m -> m.values().stream()).flatMap(s -> Arrays.stream(s.split("\\s+")))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** The configuration the in-page probe needs to flip an Alert to its legacy classes, as a JSON object. */
    String configJson() {
        return roles.entrySet().stream().map(e -> "\"" + e.getKey() + "\":{" + e.getValue().entrySet().stream()
                        .map(v -> "\"" + v.getKey() + "\":\"" + v.getValue() + "\"").collect(Collectors.joining(",")) + "}")
                .collect(Collectors.joining(",", "{", "}"));
    }

    /** How many migrated Alerts {@code html} contains. */
    static int count(String html) {
        Matcher m = CONTAINER.matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** Rewrites every migrated Alert in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        Matcher m = CONTAINER.matcher(html);
        while (m.find(cursor)) {
            String variant = m.group(1);
            if (!variant.equals(m.group(2))) {
                throw new IllegalStateException("alert modifier " + variant + " does not match data-alert-variant " + m.group(2));
            }
            int end = closingIndex(html, m.end());
            out.append(html, cursor, m.start());
            out.append("<div class=\"").append(classes("container", variant)).append("\" data-alert data-alert-variant=\"").append(variant).append("\">");
            out.append(rewriteInside(html.substring(m.end(), end), variant));
            cursor = end;
        }
        return out.append(html.substring(cursor)).toString();
    }

    /** The index just past the {@code </div>} that closes the Alert whose opening tag ends at {@code from}. */
    private static int closingIndex(String html, int from) {
        Matcher tags = DIV_TAG.matcher(html);
        int depth = 1;
        int at = from;
        while (tags.find(at)) {
            depth += tags.group(1).isEmpty() ? 1 : -1;
            at = tags.end();
            if (depth == 0) {
                return html.indexOf('>', at) + 1;
            }
        }
        throw new IllegalStateException("an alert's closing </div> was not found");
    }

    private String rewriteInside(String inner, String variant) {
        String s = inner;
        s = replaceOnce(s, "ash-icon ash-alert__icon", "inline-block shrink-0 " + classes("icon", variant));
        s = replaceOnce(s, "<div class=\"ash-alert__content\">", "<div class=\"" + classes("content", variant) + "\">");
        s = replaceOnce(s, "<span class=\"ash-sr-only\">", "<span class=\"" + classes("label", variant) + "\">");
        if (s.contains("<h3 class=\"ash-alert__title\">")) {
            s = replaceOnce(s, "<h3 class=\"ash-alert__title\">", "<h3 class=\"" + classes("title", variant) + "\">");
            Matcher after = Pattern.compile("(</h3>\\s*)<div class=\"ash-alert__body\">").matcher(s);
            if (after.find()) {
                s = after.replaceFirst(Matcher.quoteReplacement(after.group(1) + "<div class=\"" + classes("body-titled", variant) + "\">"));
            }
        }
        if (s.contains("<div class=\"ash-alert__body\">")) {
            s = replaceOnce(s, "<div class=\"ash-alert__body\">", "<div class=\"" + classes("body-plain", variant) + "\">");
        }
        if (s.contains("ash-alert")) {
            throw new IllegalStateException("an alert was not fully rewritten to its legacy classes: " + s.replaceAll("\\s+", " "));
        }
        return s;
    }

    private static String replaceOnce(String s, String target, String replacement) {
        int at = s.indexOf(target);
        if (at < 0) {
            throw new IllegalStateException("expected " + target + " inside an alert");
        }
        return s.substring(0, at) + replacement + s.substring(at + target.length());
    }
}
