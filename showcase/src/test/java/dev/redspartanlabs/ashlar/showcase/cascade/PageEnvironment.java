package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The controlled environment for rendering a page WITHOUT Tailwind (environment control E1).
 *
 * <p>Tailwind's preflight gives the page its font and line height on {@code html}. Take Tailwind away and
 * every inherited font differs, which says nothing about a component. So the harness supplies exactly those
 * page-level declarations, read out of the compiled Tailwind stylesheet rather than retyped, and then
 * compares only what the component itself owns. It does not touch the component.
 */
final class PageEnvironment {

    private static final Pattern DEFAULT_FONT_FAMILY = Pattern.compile("--default-font-family:([^;}]+)");
    private static final Pattern FONT_SANS = Pattern.compile("--font-sans:([^;}]+)");
    private static final Pattern HTML_RULE = Pattern.compile("html,:host\\{([^}]*)\\}");
    private static final Pattern HTML_FONT_FAMILY = Pattern.compile("(?<![-\\w])font-family:([^;]+);");
    private static final Pattern HTML_LINE_HEIGHT = Pattern.compile("(?<![-\\w])line-height:([^;]+);");

    private PageEnvironment() {
    }

    /** CSS to append to the page when Tailwind's stylesheet is replaced by Ashlar's alone. */
    static String tailwindRemovedRules(Path compiledTailwindCss) throws IOException {
        String css = Files.readString(compiledTailwindCss, StandardCharsets.UTF_8);
        String defaultFamily = find(DEFAULT_FONT_FAMILY, css, "--default-font-family");
        String fontSans = find(FONT_SANS, css, "--font-sans");
        String html = find(HTML_RULE, css, "the html rule");
        String fontFamily = find(HTML_FONT_FAMILY, html, "html font-family");
        String lineHeight = find(HTML_LINE_HEIGHT, html, "html line-height");
        // :root has lower specificity than a skin's :root[data-skin], so a skin's --font-sans still wins, as it does with Tailwind.
        return ":root{--font-sans:" + fontSans + ";--default-font-family:" + defaultFamily + "}"
                + "html{font-family:" + fontFamily + ";line-height:" + lineHeight + "}\n";
    }

    private static String find(Pattern pattern, String text, String what) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) {
            throw new IllegalStateException("could not find " + what + " in the compiled Tailwind stylesheet");
        }
        return m.group(1).trim();
    }
}
