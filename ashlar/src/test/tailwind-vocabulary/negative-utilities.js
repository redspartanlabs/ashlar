// TEST-ONLY FIXTURE for the Tailwind vocabulary extraction (ashlar/build.gradle.kts,
// generateTailwindVocabularyFixture). It is scanned, never run and never published.
// TailwindVocabularyTest requires each negative utility below to come out whole.

// A double-quoted string, a single-quoted string and a backtick string spanning lines.
const doubleQuoted = "-translate-y-1/2 -ml-4";
const singleQuoted = 'focus:-rotate-45';
const multiLine = `-inset-x-2
    sm:-bottom-1`;
