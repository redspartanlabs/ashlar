package dev.redspartanlabs.ashlar.tailwind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated class vocabulary is how a consumer's Tailwind build learns
 * about classes that live in Ashlar's Java and JavaScript instead of its
 * templates. It only works if the file is packaged under the {@code ashlar/}
 * prefix (the part of the jar a consumer already unpacks) and is not lossy,
 * so this checks both, using classes that exist nowhere else.
 */
class TailwindVocabularyTest {

    private static final String RESOURCE = "/ashlar/tailwind/classes.txt";

    private static List<String> lines;

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = TailwindVocabularyTest.class.getResourceAsStream(RESOURCE)) {
            assertNotNull(in, RESOURCE + " must be on the classpath, i.e. packaged in the jar under ashlar/");
            lines = new ArrayList<>(List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n", -1)));
        }
    }

    @Test
    void isNotEmptyAndEndsWithANewline() {
        assertTrue(lines.size() > 100, "expected a few hundred tokens, got " + lines.size());
        assertEquals("", lines.get(lines.size() - 1), "file should end with a newline");
    }

    @Test
    void isSortedAndFreeOfDuplicatesAndBlankLines() {
        List<String> tokens = lines.subList(0, lines.size() - 1);
        Set<String> sorted = new TreeSet<>(tokens);
        assertEquals(sorted.size(), tokens.size(), "tokens must be unique");
        assertEquals(new ArrayList<>(sorted), tokens, "tokens must be sorted so output is deterministic");
        assertFalse(tokens.contains(""), "no blank lines");
    }

    @ParameterizedTest(name = "Java-held class {0}")
    @ValueSource(strings = {
            "bg-red-600",               // ButtonStyles: a variant a templates-only scan loses
            "hover:bg-blue-700",        // ButtonStyles: state variant
            "aria-disabled:opacity-60", // ButtonStyles: loading state
            "-translate-x-full",        // negative utility; a "looks like a class" filter dropped these
    })
    void capturesJavaHeldClasses(String token) {
        assertTrue(lines.contains(token), token + " is missing from the vocabulary");
    }

    @ParameterizedTest(name = "JavaScript-held class {0}")
    @ValueSource(strings = {
            "opacity-100",   // drawer.js open state
            "translate-x-0", // drawer.js open state
            "bg-blue-50/60", // table.js selected row
    })
    void capturesJavaScriptHeldClasses(String token) {
        assertTrue(lines.contains(token), token + " is missing from the vocabulary");
    }

    @Test
    void preservesDataVariantsContainingEqualsSigns() {
        assertTrue(lines.stream().anyMatch(t -> t.startsWith("data-[toast-state=") && t.endsWith("]:translate-x-0")),
                "data-[...=...] variants must survive extraction untouched");
    }
}
