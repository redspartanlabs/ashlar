package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Card used to be styled by Tailwind utility classes, and nine skins selected its header, title and footer through two of them
 * ({@code .border-b} and {@code .border-t}). Those classes are gone, so the skins select the Card's own hooks instead
 * ({@code data-card-header}, {@code data-card-footer}). This pins that replacement: exactly the 17 known selectors, in exactly the nine skins
 * that had them, with no Tailwind class left in a Card selector, with the structural pseudo-classes kept so the selectors' specificity is
 * identical to what it was, and with the Stat Card's structural value selector untouched. It reads the skin sources only.
 */
class CardSkinSelectorsTest {

    private static final Path SKINS = Path.of("src/main/resources/static/css");

    /** Header selectors (the header itself and the title inside it) and footer selectors, per skin. Ledger has none. */
    private static final Map<String, int[]> EXPECTED = Map.of(
            "axiom", new int[] {2, 1}, "civic", new int[] {2, 1}, "fathom", new int[] {1, 0}, "foundry", new int[] {2, 0}, "harbor", new int[] {2, 0},
            "keystone", new int[] {1, 0}, "sterling", new int[] {1, 0}, "summit", new int[] {2, 1}, "veridian", new int[] {1, 0}, "ledger", new int[] {0, 0});

    private static String skin(String name) throws IOException {
        return Files.readString(SKINS.resolve("theme-" + name + ".css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    void noSkinSelectsACardThroughATailwindClassAnymore() throws IOException {
        Pattern tailwindInACardSelector = Pattern.compile("\\[data-card\\]\\s*>\\s*\\.[a-z]");
        for (String name : EXPECTED.keySet()) {
            assertTrue(!tailwindInACardSelector.matcher(skin(name)).find(), name + " still selects a Card child through a class");
            assertEquals(0, count(skin(name), ".border-b:first-child"), name);
            assertEquals(0, count(skin(name), ".border-t:last-child"), name);
        }
    }

    @Test
    void exactlySeventeenSelectorsUseTheHooksInExactlyTheNineSkinsThatNeededThem() throws IOException {
        int header = 0;
        int footer = 0;
        Map<String, String> found = new TreeMap<>();
        for (Map.Entry<String, int[]> e : EXPECTED.entrySet()) {
            String css = skin(e.getKey());
            int h = count(css, "[data-card] > [data-card-header]:first-child");
            int f = count(css, "[data-card] > [data-card-footer]:last-child");
            found.put(e.getKey(), h + "/" + f);
            assertEquals(e.getValue()[0], h, e.getKey() + " header selectors");
            assertEquals(e.getValue()[1], f, e.getKey() + " footer selectors");
            header += h;
            footer += f;
        }
        assertEquals(14, header, found.toString());
        assertEquals(3, footer, found.toString());
        assertEquals(17, header + footer);
    }

    @Test
    void theTitleSelectorsKeepTheirH3AndTheStructuralValueSelectorIsUntouchedInEverySkin() throws IOException {
        for (String name : EXPECTED.keySet()) {
            String css = skin(name);
            assertEquals(List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "sterling", "summit", "veridian")
                    .contains(name) ? 1 : 0, count(css, "[data-card] > [data-card-header]:first-child > h3"), name + ": one title selector in each skin that styled the title");
            assertEquals(1, count(css, "[data-stat-card] dd > span:first-child"), name + ": the Stat Card value selector is structural and unchanged");
        }
    }

    /** Specificity (classes, attributes and pseudo-classes) of a compound selector: what changed must not change it. */
    private static int specificity(String selector) {
        int n = 0;
        Matcher m = Pattern.compile("\\.[A-Za-z_-]+|\\[[^\\]]+\\]|(?<!:):[a-z-]+").matcher(selector);
        while (m.find()) {
            n++;
        }
        return n;
    }

    @Test
    void replacingTheClassWithTheAttributeHookDoesNotChangeASelectorsSpecificity() {
        assertEquals(specificity("[data-card] > .border-b:first-child"), specificity("[data-card] > [data-card-header]:first-child"));
        assertEquals(specificity("[data-card] > .border-t:last-child"), specificity("[data-card] > [data-card-footer]:last-child"));
        assertEquals(specificity("[data-card] > .border-b:first-child > h3"), specificity("[data-card] > [data-card-header]:first-child > h3"));
        // The comparison can fail: dropping the pseudo-class (the tempting simplification) lowers it.
        assertTrue(specificity("[data-card] > .border-b:first-child") != specificity("[data-card] > [data-card-header]"));
    }
}
