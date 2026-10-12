package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Badge's pre-migration Tailwind classes, read out of {@code badge-legacy.html} (the same file
 * Tailwind compiles for the oracle stylesheet), and a rewrite that puts them back onto migrated markup
 * so the SAME page can be rendered both ways in one browser run.
 */
final class BadgeLegacyMarkup {

    private static final Pattern VARIANT = Pattern.compile("data-legacy-variant=\"([a-z]+)\"\\s+class=\"([^\"]+)\"");
    private static final Pattern ICON = Pattern.compile("data-legacy-icon\\s+class=\"inline-block shrink-0 ([^\"]+)\"");
    private static final Pattern MIGRATED_SPAN = Pattern.compile("(<span\\s+)class=\"ash-badge ash-badge--([a-z]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");

    private final Map<String, String> variants = new LinkedHashMap<>();
    private final String iconSizing;

    BadgeLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = BadgeLegacyMarkup.class.getResourceAsStream("/parity/badge/badge-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/badge/badge-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher v = VARIANT.matcher(html);
        while (v.find()) {
            variants.put(v.group(1), v.group(2));
        }
        Matcher i = ICON.matcher(html);
        if (variants.size() != 5 || !i.find()) {
            throw new IllegalStateException("badge-legacy.html must define the five variants and the icon sizing, found " + variants.keySet());
        }
        iconSizing = i.group(1);
    }

    /** The legacy class string of a variant (lower case): base, ring and the variant's light and dark colours. */
    String variantClasses(String variant) {
        return variants.get(variant);
    }

    Map<String, String> variants() {
        return variants;
    }

    /** The legacy icon sizing tokens (for example {@code h-3 w-3}). */
    String iconSizing() {
        return iconSizing;
    }

    /**
     * Rewrites migrated Badge markup in {@code html} to its legacy classes: the span's class attribute and
     * the {@code ash-badge__icon} token. Everything else is untouched.
     */
    String rewrite(String html) {
        String spans = MIGRATED_SPAN.matcher(html).replaceAll(m -> {
            String legacy = variants.get(m.group(2));
            if (legacy == null) {
                throw new IllegalStateException("no legacy classes for the variant " + m.group(2));
            }
            return Matcher.quoteReplacement(m.group(1) + "class=\"" + legacy + "\"");
        });
        return CLASS_ATTRIBUTE.matcher(spans).replaceAll(m -> Matcher.quoteReplacement(
                "class=\"" + m.group(1).replaceAll("\\bash-badge__icon\\b", iconSizing) + "\""));
    }
}
