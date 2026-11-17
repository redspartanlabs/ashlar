// TEST-ONLY FIXTURE for the Tailwind vocabulary extraction (ashlar/build.gradle.kts,
// generateTailwindVocabularyFixture). It is scanned, never compiled and never
// published: it sits outside every source set. TailwindVocabularyTest requires each
// negative utility below to come out of the extraction whole.
final class NegativeUtilities {

    // An ordinary string literal: a negative utility with a fraction, one with a scale value, one under a variant.
    static final String PLAIN = "-translate-x-1/2 -mt-2 hover:-translate-y-px";

    // A text block, with a negative utility under a data variant and an arbitrary value.
    static final String BLOCK = """
            data-[state=open]:-translate-x-full
            -top-[3px]
            """;
}
