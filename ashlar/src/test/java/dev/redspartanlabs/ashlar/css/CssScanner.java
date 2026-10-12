package dev.redspartanlabs.ashlar.css;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A deliberately small CSS scanner for the structural checks on Ashlar's
 * generated stylesheet. It understands comments, strings, braces and
 * semicolons - enough to split a stylesheet into top-level items and a block
 * into rules - and nothing else. It is test support, not a general parser.
 */
final class CssScanner {

    /** A top-level or nested item: {@code body} is null for a statement ending in a semicolon. */
    record Item(String prelude, String body) {
    }

    private static final Pattern ASH_SUBLAYER = Pattern.compile("^@layer ash\\.[a-z][a-z0-9-]*$");
    private static final Pattern FORBIDDEN_IN_BODY = Pattern.compile("^@(import|layer|charset)\\b");

    private CssScanner() {
    }

    /** Splits {@code css} into its items at nesting depth zero. Comments are dropped. */
    static List<Item> items(String css) {
        List<Item> items = new ArrayList<>();
        StringBuilder prelude = new StringBuilder();
        int i = 0;
        int n = css.length();
        while (i < n) {
            char c = css.charAt(i);
            if (c == '/' && i + 1 < n && css.charAt(i + 1) == '*') {
                i = skipComment(css, i);
            } else if (c == '"' || c == '\'') {
                int end = skipString(css, i);
                prelude.append(css, i, end);
                i = end;
            } else if (c == ';') {
                items.add(new Item(prelude.toString().trim(), null));
                prelude.setLength(0);
                i++;
            } else if (c == '{') {
                int end = matchingBrace(css, i);
                items.add(new Item(prelude.toString().trim(), css.substring(i + 1, end)));
                prelude.setLength(0);
                i = end + 1;
            } else if (c == '}') {
                items.add(new Item("}", null));
                i++;
            } else {
                prelude.append(c);
                i++;
            }
        }
        if (!prelude.toString().isBlank()) {
            items.add(new Item(prelude.toString().trim(), null));
        }
        return items;
    }

    /** Splits a declaration block into name to value, in order. */
    static Map<String, String> declarations(String block) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Item item : items(block + ";")) {
            if (item.body() != null || item.prelude().isEmpty()) {
                continue;
            }
            int colon = item.prelude().indexOf(':');
            if (colon > 0) {
                result.put(item.prelude().substring(0, colon).trim(), item.prelude().substring(colon + 1).trim());
            }
        }
        return result;
    }

    /** Selectors of a comma-separated selector list, split at the top level only. */
    static List<String> selectors(String selectorList) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < selectorList.length(); i++) {
            char c = selectorList.charAt(i);
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                result.add(selectorList.substring(start, i).trim());
                start = i + 1;
            }
        }
        result.add(selectorList.substring(start).trim());
        return result;
    }

    /**
     * Reports every way {@code css} breaks the packaging contract: the first
     * item must be a layer-order statement; every other top-level item must be a
     * block in a direct {@code ash.*} sublayer that the statement names; no
     * block may contain {@code @import}, {@code @layer} or {@code @charset}; and
     * nothing else (an unlayered rule, a rule in a Tailwind-owned layer, a rule
     * in the parent {@code ash} layer) may appear.
     */
    static List<String> violations(String css) {
        List<String> problems = new ArrayList<>();
        List<Item> items = items(css);
        if (items.isEmpty()) {
            return List.of("stylesheet is empty");
        }
        Item first = items.get(0);
        List<String> declared = List.of();
        if (first.body() != null || !first.prelude().startsWith("@layer ")) {
            problems.add("first item is not the layer-order statement: " + first.prelude());
        } else {
            declared = List.of(first.prelude().substring("@layer ".length()).split("\\s*,\\s*"));
        }
        for (Item item : items.subList(1, items.size())) {
            if (item.body() == null) {
                problems.add("unexpected top-level statement: " + item.prelude());
            } else if (!ASH_SUBLAYER.matcher(item.prelude()).matches()) {
                problems.add("rule or at-rule outside a named ash.* sublayer: " + item.prelude());
            } else {
                String name = item.prelude().substring("@layer ".length());
                if (!declared.contains(name)) {
                    problems.add("sublayer not named in the leading statement: " + name);
                }
                if (containsForbiddenAtRule(item.body())) {
                    problems.add("block " + name + " contains @import, @layer or @charset");
                }
            }
        }
        return problems;
    }

    /** True if an {@code @import}, {@code @layer} or {@code @charset} at-rule appears anywhere in {@code body}. */
    private static boolean containsForbiddenAtRule(String body) {
        for (Item item : items(body)) {
            if (FORBIDDEN_IN_BODY.matcher(item.prelude()).lookingAt()) {
                return true;
            }
            if (item.body() != null && containsForbiddenAtRule(item.body())) {
                return true;
            }
        }
        return false;
    }

    /** The rules (selector list to declaration block) directly inside a layer block body. */
    static List<Item> rules(String blockBody) {
        return items(blockBody).stream().filter(i -> i.body() != null).toList();
    }

    /**
     * A style rule with the media condition it sits under ({@code ""} when unconditional).
     * {@code media} is the at-rule prelude as written, for example {@code @media (min-width:40rem)}.
     */
    record Rule(String media, String selector, String block) {
    }

    /**
     * Every style rule in {@code blockBody}, in source order, with rules nested in
     * {@code @media} or {@code @supports} flattened and tagged with their condition.
     * Other at-rules that carry a body of declarations (such as {@code @font-face}) are
     * not style rules and are skipped.
     */
    static List<Rule> flatRules(String blockBody) {
        List<Rule> result = new ArrayList<>();
        collect("", blockBody, result);
        return result;
    }

    private static void collect(String condition, String body, List<Rule> into) {
        for (Item item : items(body)) {
            if (item.body() == null) {
                continue;
            }
            if (item.prelude().startsWith("@media") || item.prelude().startsWith("@supports")) {
                collect(condition.isEmpty() ? item.prelude() : condition + " and " + item.prelude(), item.body(), into);
            } else if (!item.prelude().startsWith("@")) {
                into.add(new Rule(condition, item.prelude(), item.body()));
            }
        }
    }

    private static int skipComment(String css, int start) {
        int end = css.indexOf("*/", start + 2);
        return end < 0 ? css.length() : end + 2;
    }

    private static int skipString(String css, int start) {
        char quote = css.charAt(start);
        int i = start + 1;
        while (i < css.length() && css.charAt(i) != quote) {
            i += css.charAt(i) == '\\' ? 2 : 1;
        }
        return Math.min(i + 1, css.length());
    }

    private static int matchingBrace(String css, int open) {
        int depth = 0;
        int i = open;
        while (i < css.length()) {
            char c = css.charAt(i);
            if (c == '/' && i + 1 < css.length() && css.charAt(i + 1) == '*') {
                i = skipComment(css, i);
                continue;
            }
            if (c == '"' || c == '\'') {
                i = skipString(css, i);
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        throw new IllegalArgumentException("unbalanced braces starting at " + open);
    }
}
