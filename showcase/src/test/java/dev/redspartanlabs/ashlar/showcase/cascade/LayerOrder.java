package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes the effective cascade-layer order of a stylesheet using the CSS
 * rule: a layer takes its position at its first mention, whether in a
 * {@code @layer a, b;} statement or in a {@code @layer a { }} block. A dotted
 * name such as {@code ash.tokens} is a sublayer of {@code ash}; the parent
 * takes its position at the first mention of any of its sublayers.
 *
 * <p>Only top-level {@code @layer} rules are scanned (nested layer blocks and
 * layers inside {@code @import}/{@code @media} are not), which is sufficient
 * for the compiled Tailwind output examined here.
 *
 * <p>This is a static aid. It is not proof of browser behaviour; the browser
 * test is.
 */
final class LayerOrder {

    static final List<String> ASH_SUBLAYERS = List.of(
            "ash.tokens", "ash.base", "ash.layout", "ash.components", "ash.skin");

    private final Set<String> names = new LinkedHashSet<>();

    static LayerOrder of(String css) {
        LayerOrder order = new LayerOrder();
        int depth = 0;
        int i = 0;
        int n = css.length();
        while (i < n) {
            char c = css.charAt(i);
            if (c == '/' && i + 1 < n && css.charAt(i + 1) == '*') {
                int end = css.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
            } else if (c == '"' || c == '\'') {
                i = skipString(css, i);
            } else if (c == '{') {
                depth++;
                i++;
            } else if (c == '}') {
                depth--;
                i++;
            } else if (depth == 0 && c == '@' && css.startsWith("@layer", i)
                    && i + 6 < n && !Character.isLetterOrDigit(css.charAt(i + 6))) {
                int start = i + 6;
                int end = start;
                while (end < n && css.charAt(end) != ';' && css.charAt(end) != '{') {
                    end++;
                }
                for (String name : css.substring(start, end).split(",")) {
                    order.register(name.trim());
                }
                i = end;
            } else {
                i++;
            }
        }
        return order;
    }

    private static int skipString(String css, int start) {
        char quote = css.charAt(start);
        int i = start + 1;
        while (i < css.length() && css.charAt(i) != quote) {
            i += css.charAt(i) == '\\' ? 2 : 1;
        }
        return i + 1;
    }

    private void register(String name) {
        if (name.isEmpty()) {
            return;
        }
        StringBuilder prefix = new StringBuilder();
        for (String part : name.split("\\.")) {
            if (prefix.length() > 0) {
                prefix.append('.');
            }
            prefix.append(part);
            names.add(prefix.toString());
        }
    }

    /** Top-level layers, lowest first. */
    List<String> topLevel() {
        return names.stream().filter(n -> !n.contains(".")).toList();
    }

    /** Direct sublayers of {@code parent}, lowest first, as full dotted names. */
    List<String> children(String parent) {
        List<String> result = new ArrayList<>();
        for (String name : names) {
            if (name.startsWith(parent + ".") && name.indexOf('.', parent.length() + 1) < 0) {
                result.add(name);
            }
        }
        return result;
    }

    /**
     * The intended order, ignoring Tailwind's {@code properties} layer (an
     * accepted extra layer): theme, base, ash, components, utilities, with
     * ash's sublayers in the order tokens, base, layout, components, skin.
     */
    boolean matchesIntendedOrder() {
        List<String> top = new ArrayList<>(topLevel());
        top.remove("properties");
        return top.equals(List.of("theme", "base", "ash", "components", "utilities"))
                && children("ash").equals(ASH_SUBLAYERS);
    }

    @Override
    public String toString() {
        return names.toString();
    }
}
