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
            "pointer-events-auto",     // ToastStyles: a class held only in Java, which a templates-only scan loses
            "hover:bg-blue-700",        // ButtonStyles: state variant
            "data-[toast-state=hidden]:hidden", // ToastStyles: a state variant held only in Java
    })
    void capturesJavaHeldClasses(String token) {
        assertTrue(lines.contains(token), token + " is missing from the vocabulary");
    }

    @ParameterizedTest(name = "JavaScript-held class {0}")
    @ValueSource(strings = {
            "opacity-100",   // select.js and combobox.js open state
            "scale-100",     // select.js and combobox.js open state
    })
    void capturesJavaScriptHeldClasses(String token) {
        assertTrue(lines.contains(token), token + " is missing from the vocabulary");
    }

    /**
     * Negative utilities are exactly what a "looks like a class" filter dropped in an earlier attempt. No library source
     * holds one any more (TooltipStyles held the last), so they are checked on test-only fixtures
     * ({@code src/test/tailwind-vocabulary}) run through the SAME extraction function the published vocabulary is built
     * with (ashlar/build.gradle.kts): every string form of both languages, plain and under a variant.
     */
    @ParameterizedTest(name = "negative utility {0}")
    @ValueSource(strings = {
            "-translate-x-1/2",                    // Java string literal, with a fraction
            "-mt-2",                               // Java string literal
            "hover:-translate-y-px",               // Java string literal, under a variant
            "data-[state=open]:-translate-x-full", // Java text block, under a data variant
            "-top-[3px]",                          // Java text block, arbitrary value
            "-translate-y-1/2",                    // JavaScript double-quoted string
            "-ml-4",                               // JavaScript double-quoted string
            "focus:-rotate-45",                    // JavaScript single-quoted string, under a variant
            "-inset-x-2",                          // JavaScript backtick string, first line
            "sm:-bottom-1",                        // JavaScript backtick string, second line, under a variant
    })
    void capturesNegativeUtilitiesInEveryStringForm(String token) throws IOException {
        assertTrue(fixtureVocabulary().contains(token), token + " is missing from the fixture vocabulary");
    }

    @Test
    void theFixtureVocabularyIsATestResourceAndNeverPartOfThePublishedOne() throws IOException {
        // The fixture tokens that no library source holds must not leak into the published vocabulary.
        for (String token : List.of("-mt-2", "hover:-translate-y-px", "data-[state=open]:-translate-x-full", "-top-[3px]", "-ml-4", "focus:-rotate-45", "-inset-x-2", "sm:-bottom-1")) {
            assertFalse(lines.contains(token), token + " is a fixture token and must not be in the published vocabulary");
        }
        assertEquals(null, TailwindVocabularyTest.class.getResource("/ashlar/tailwind-vocabulary-fixture/classes.txt"), "the fixture output is not under ashlar/");
    }

    private static List<String> fixtureVocabulary() throws IOException {
        try (InputStream in = TailwindVocabularyTest.class.getResourceAsStream("/tailwind-vocabulary-fixture/classes.txt")) {
            assertNotNull(in, "the fixture vocabulary must be generated into the test resources (generateTailwindVocabularyFixture)");
            return List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n"));
        }
    }

    @Test
    void preservesDataVariantsContainingEqualsSigns() {
        assertTrue(lines.stream().anyMatch(t -> t.startsWith("data-[toast-state=") && t.endsWith("]:translate-x-0")),
                "data-[...=...] variants must survive extraction untouched");
    }
}
