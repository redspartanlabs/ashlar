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
 * The Icon's pre-migration Tailwind class strings, read from {@code /parity/icon/icon-legacy.html} (the one source of
 * truth for what "legacy" means), and the rewrite that turns migrated Icon markup back into the markup the component
 * rendered before.
 *
 * <p>The legacy Icon wrote {@code inline-block shrink-0} and then the caller's {@code classes}, which defaulted to
 * {@code h-5 w-5} and REPLACED that default when given. The migrated Icon writes {@code ash-icon} and then the caller's
 * {@code classes}, which are ADDED to Ashlar's default size. So the rewrite is:
 * <ul>
 *   <li>{@code ash-icon} alone becomes the legacy default ({@code inline-block shrink-0 h-5 w-5});</li>
 *   <li>{@code ash-icon} followed by classes becomes {@code inline-block shrink-0} followed by the same classes, with a
 *       size modifier ({@code ash-icon--xs} to {@code --xl}) replaced by the Tailwind size utilities the same size was
 *       written with. A caller class that carries no size is NOT given one: that is the one place the two
 *       contracts differ, and it is a documented behaviour change rather than something to paper over.</li>
 * </ul>
 * It fails loudly on an {@code ash-icon*} token it does not know, rather than leaving some of it migrated.
 */
final class IconLegacyMarkup {

    static final List<String> MODIFIERS = List.of("xs", "sm", "md", "lg", "xl");

    /**
     * The showcase's Icon page defines one class of its own to demonstrate --ash-icon-size ({@code .hero-icon { --ash-icon-size: 3rem }}).
     * Its legacy equivalent is the size the legacy Icon would have been given for the same 3rem: h-12 w-12.
     */
    static final Map<String, String> SHOWCASE_SIZE_CLASSES = Map.of("hero-icon", "size-12");

    private static final Pattern LEGACY = Pattern.compile("<svg data-legacy-role=\"([a-z0-9-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final String BASE = "inline-block shrink-0";

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    IconLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = IconLegacyMarkup.class.getResourceAsStream("/parity/icon/icon-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/icon/icon-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("icon-legacy.html defines " + m.group(1) + " twice");
            }
        }
        for (String required : List.of("base", "default", "size-10", "size-12")) {
            if (!legacyByRole.containsKey(required)) {
                throw new IllegalStateException("icon-legacy.html must define " + required);
            }
        }
        for (String modifier : MODIFIERS) {
            if (!legacyByRole.containsKey(modifier)) {
                throw new IllegalStateException("icon-legacy.html must define " + modifier);
            }
            if (!legacyByRole.get(modifier).startsWith(BASE + " ")) {
                throw new IllegalStateException("every legacy Icon starts with " + BASE + ": " + modifier);
            }
        }
    }

    /** The legacy classes of a role (a modifier name, {@code default}, {@code base} or {@code size-10}). */
    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** The Tailwind size utilities a modifier's size was written with (for example {@code h-3 w-3} for {@code xs}). */
    String sizeUtilities(String modifier) {
        return classes(modifier).substring(BASE.length() + 1);
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** The legacy class value for a migrated class value, or {@code null} when the value carries no {@code ash-icon} token. */
    String legacyFor(String migrated) {
        String[] tokens = migrated.trim().split("\\s+");
        boolean isIcon = false;
        for (String token : tokens) {
            if (token.equals("ash-icon")) {
                isIcon = true;
            } else if (token.startsWith("ash-icon")) {
                String modifier = token.startsWith("ash-icon--") ? token.substring("ash-icon--".length()) : "";
                if (!MODIFIERS.contains(modifier)) {
                    throw new IllegalStateException("an ash-icon class that is not a known Icon class: " + token + " in " + migrated);
                }
            }
        }
        if (!isIcon) {
            for (String token : tokens) {
                if (token.startsWith("ash-icon--")) {
                    throw new IllegalStateException("an Icon modifier without ash-icon: " + migrated);
                }
            }
            return null;
        }
        if (tokens.length == 1) {
            return classes("default");
        }
        StringBuilder out = new StringBuilder(BASE);
        for (String token : tokens) {
            if (token.equals("ash-icon")) {
                continue;
            }
            String showcaseSize = SHOWCASE_SIZE_CLASSES.get(token);
            out.append(' ').append(token.startsWith("ash-icon--") ? sizeUtilities(token.substring("ash-icon--".length()))
                    : showcaseSize != null ? classes(showcaseSize).substring(BASE.length() + 1) : token);
        }
        return out.toString();
    }

    /** How many class attributes in {@code html} carry the {@code ash-icon} token. */
    static int count(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (Arrays.asList(m.group(1).trim().split("\\s+")).contains("ash-icon")) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Icon class attribute in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String legacy = legacyFor(m.group(1));
            if (legacy == null) {
                continue;
            }
            out.append(html, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
