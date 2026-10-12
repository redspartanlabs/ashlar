package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Component CSS reads Tailwind's palette as {@code var(--color-slate-100, <fallback>)}. The fallback is a COPY of
 * Tailwind's value, so it can drift. This compares every fallback in the component sources with the installed
 * Tailwind theme, which is the source of truth, and fails on any difference.
 *
 * <p>It lives in the showcase module because only the showcase has Node and {@code node_modules}; the Ashlar
 * library module deliberately has neither. It checks component CSS only (not {@code tokens.css}, whose fallbacks
 * are a separate matter).
 */
class ComponentPaletteFallbackTest {

    private static final Pattern PALETTE_VAR = Pattern.compile("var\\(\\s*(--color-[a-z]+(?:-\\d+)?)\\s*(,)?");
    private static final Pattern OKLCH = Pattern.compile("^oklch\\(\\s*([0-9.]+)%\\s+([0-9.]+)\\s+([0-9.]+)\\s*\\)$");

    private static Path moduleDir() {
        String generated = System.getProperty("ashlar.css.generated");
        assertTrue(generated != null, "ashlar.css.generated must be set by the Gradle test task");
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                return p;
            }
        }
        throw new IllegalStateException("could not find the ashlar module above " + generated);
    }

    private static Map<String, String> tailwindPalette() throws IOException {
        Path theme = moduleDir().getParent().resolve("node_modules/tailwindcss/theme.css");
        assertTrue(Files.isRegularFile(theme), "Tailwind's theme.css must be installed (npm ci): " + theme);
        Map<String, String> palette = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(--color-[a-z]+(?:-\\d+)?)\\s*:\\s*([^;]+);").matcher(Files.readString(theme, StandardCharsets.UTF_8));
        while (m.find()) {
            palette.put(m.group(1), m.group(2).trim());
        }
        return palette;
    }

    /** Every palette reference in the component sources: variable, fallback (null when missing), and the file. */
    private record Reference(String file, String variable, String fallback) {
    }

    private static List<Reference> componentReferences() throws IOException {
        List<Reference> references = new ArrayList<>();
        try (Stream<Path> files = Files.list(moduleDir().resolve("src/main/css/components"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".css")).sorted().toList()) {
                String css = Files.readString(file, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
                Matcher m = PALETTE_VAR.matcher(css);
                while (m.find()) {
                    String fallback = null;
                    if (m.group(2) != null) {
                        int start = m.end();
                        int depth = 1;
                        int i = start;
                        while (i < css.length() && depth > 0) {
                            char c = css.charAt(i);
                            depth += c == '(' ? 1 : c == ')' ? -1 : 0;
                            i++;
                        }
                        fallback = css.substring(start, i - 1).trim();
                    }
                    references.add(new Reference(file.getFileName().toString(), m.group(1), fallback));
                }
            }
        }
        return references;
    }

    /** Equal as colours: oklch components compared numerically (so 0.007 equals .007), anything else as text. */
    static boolean sameColour(String a, String b) {
        Matcher x = OKLCH.matcher(a);
        Matcher y = OKLCH.matcher(b);
        if (x.matches() && y.matches()) {
            for (int g = 1; g <= 3; g++) {
                if (Math.abs(Double.parseDouble(x.group(g)) - Double.parseDouble(y.group(g))) > 1e-9) {
                    return false;
                }
            }
            return true;
        }
        return a.replaceAll("\\s+", "").equalsIgnoreCase(b.replaceAll("\\s+", ""));
    }

    @Test
    void everyPaletteFallbackInComponentCssEqualsTailwindsOwnValue() throws IOException {
        Map<String, String> palette = tailwindPalette();
        List<Reference> references = componentReferences();
        assertTrue(references.size() >= 30, "expected the Badge's palette references to be found, got " + references.size());
        List<String> problems = new ArrayList<>();
        for (Reference r : references) {
            String tailwind = palette.get(r.variable());
            if (tailwind == null) {
                problems.add(r.file() + ": " + r.variable() + " does not exist in Tailwind's theme");
            } else if (r.fallback() == null) {
                problems.add(r.file() + ": " + r.variable() + " is read without a fallback");
            } else if (!sameColour(r.fallback(), tailwind)) {
                problems.add(r.file() + ": " + r.variable() + " fallback " + r.fallback() + " != Tailwind " + tailwind);
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void theComparisonCanFail() {
        assertTrue(sameColour("oklch(96.8% 0.007 247.896)", "oklch(96.8% .007 247.896)"));
        assertTrue(!sameColour("oklch(96.8% 0.007 247.896)", "oklch(96.8% 0.007 247.897)"));
        assertTrue(!sameColour("#2563eb", "oklch(54.6% .245 262.881)"));
        assertTrue(sameColour("#FFF", "#fff"));
    }
}
