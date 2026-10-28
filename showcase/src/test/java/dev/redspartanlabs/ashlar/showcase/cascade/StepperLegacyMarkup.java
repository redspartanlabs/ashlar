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
 * The Stepper's and the Stepper Item's pre-migration Tailwind class strings, read from {@code /parity/stepper/stepper-legacy.html} (the one
 * source of truth for what "legacy" means), and the rewrite that turns migrated Stepper markup back into the markup the components rendered
 * before.
 *
 * <p>The migrated Stepper Item says its state once, as a modifier on the {@code <li>}, and its parts carry no state of their own; the legacy
 * parts each had a class string per state. So the rewrite works one {@code <li>} at a time: the state of the {@code <li>} picks the legacy
 * indicator, line and label of every part inside it. The visually hidden status text ({@code ash-sr-only}, used elsewhere by Alert, Spinner
 * and Timeline too) is rewritten only inside a Stepper Item, so another component's own rewrite is left alone. Everything else on the page,
 * including the Icons' own markup (which the Icon rewrite handles), is left exactly as served. It fails loudly when an {@code ash-stepper}
 * class value is not one of the known parts, rather than leaving some of it migrated.
 */
final class StepperLegacyMarkup {

    static final List<String> STATES = List.of("completed", "current", "upcoming");

    static final List<String> ROLES = List.of("stepper", "list", "item", "connector", "line-completed", "line-current", "line-upcoming",
            "indicator-completed", "indicator-current", "indicator-upcoming", "text", "label-completed", "label-current", "label-upcoming", "status");

    /** The migrated class value of each part that does not depend on a step's state, exactly as the templates write it. */
    static final Map<String, String> MIGRATED = Map.of(
            "stepper", "ash-stepper",
            "list", "ash-stepper__list",
            "connector", "ash-stepper-item__connector",
            "text", "ash-stepper-item__text");

    /** The migrated class value of the {@code <li>}, per state. */
    static String migratedItem(String state) {
        return "ash-stepper-item ash-stepper-item--" + state;
    }

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern STEPPER_TOKEN = Pattern.compile("(?<![\\w-])ash-stepper");
    private static final Pattern ITEM = Pattern.compile("(<li\\b[^>]*?class=\")(ash-stepper-item ash-stepper-item--(completed|current|upcoming))(\"[^>]*>)(.*?)(</li>)", Pattern.DOTALL);

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    StepperLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = StepperLegacyMarkup.class.getResourceAsStream("/parity/stepper/stepper-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/stepper/stepper-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("stepper-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("stepper-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** How many migrated Steppers {@code html} contains. */
    static int count(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).trim().equals("ash-stepper")) {
                n++;
            }
        }
        return n;
    }

    private String rewriteItem(String state, String body) {
        Map<String, String> byMigrated = new LinkedHashMap<>();
        byMigrated.put(MIGRATED.get("connector"), classes("connector"));
        byMigrated.put("ash-stepper-item__line", classes("line-" + state));
        byMigrated.put("ash-stepper-item__indicator", classes("indicator-" + state));
        byMigrated.put(MIGRATED.get("text"), classes("text"));
        byMigrated.put("ash-stepper-item__label", classes("label-" + state));
        String withStatus = body.replace("<span class=\"ash-sr-only\">", "<span class=\"" + classes("status") + "\">");
        return replaceClasses(withStatus, byMigrated);
    }

    /** Replaces each class value that carries an ash-stepper token by its entry in {@code byMigrated}, or fails. */
    private static String replaceClasses(String html, Map<String, String> byMigrated) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!STEPPER_TOKEN.matcher(value).find()) {
                continue;
            }
            String legacy = byMigrated.get(value);
            if (legacy == null) {
                throw new IllegalStateException("an ash-stepper class value that is not a known Stepper or Stepper Item part: " + value);
            }
            out.append(html, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }

    /** Rewrites every migrated Stepper and Stepper Item in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher items = ITEM.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (items.find()) {
            String state = items.group(3);
            out.append(html, cursor, items.start()).append(items.group(1)).append(classes("item")).append(items.group(4))
                    .append(rewriteItem(state, items.group(5))).append(items.group(6));
            cursor = items.end();
        }
        String itemsRewritten = out.append(html.substring(cursor)).toString();
        Map<String, String> outer = new LinkedHashMap<>();
        outer.put(MIGRATED.get("stepper"), classes("stepper"));
        outer.put(MIGRATED.get("list"), classes("list"));
        return replaceClasses(itemsRewritten, outer);
    }
}
