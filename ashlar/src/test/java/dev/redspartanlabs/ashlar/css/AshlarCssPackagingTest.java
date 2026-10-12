package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the stylesheet in the ACTUAL built jar, not in the source tree or the
 * resources directory: the file is there, byte-identical to the generated
 * output, starts with the layer-order statement, keeps every rule inside a named
 * ash.* sublayer, and the rest of what the jar already carried is still there.
 *
 * <p>Repeated-clean-build determinism is a property of whole builds and is
 * checked by running them; here the output is checked for the properties that
 * make it deterministic (UTF-8, LF only, no BOM, no timestamp).
 */
class AshlarCssPackagingTest {

    private static final String CSS_ENTRY = "META-INF/resources/ashlar/css/ashlar.css";

    private static Path jar;
    private static byte[] packaged;

    @BeforeAll
    static void openTheJar() throws IOException {
        String path = System.getProperty("ashlar.jar");
        assertNotNull(path, "ashlar.jar must be set by the Gradle test task");
        jar = Path.of(path);
        assertTrue(Files.isRegularFile(jar), "the jar must have been built: " + jar);
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(CSS_ENTRY);
            assertNotNull(entry, CSS_ENTRY + " is missing from " + jar.getFileName());
            try (InputStream in = zip.getInputStream(entry)) {
                packaged = in.readAllBytes();
            }
        }
    }

    private static String css() {
        return new String(packaged, StandardCharsets.UTF_8);
    }

    private static List<String> entries() throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            return zip.stream().map(ZipEntry::getName).toList();
        }
    }

    @Test
    void theStylesheetIsPackagedAtTheExpectedPathExactlyOnce() throws IOException {
        List<String> css = entries().stream().filter(n -> n.endsWith(".css")).toList();
        assertEquals(List.of(CSS_ENTRY), css);
    }

    @Test
    void theJarCopyIsByteIdenticalToTheGeneratedFile() throws IOException {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        assertArrayEquals(Files.readAllBytes(Path.of(generated)), packaged);
    }

    @Test
    void theLayerOrderStatementComesFirstAsTheContractRequires() {
        assertTrue(css().startsWith(
                        "@layer properties, theme, base, ash.tokens, ash.base, ash.layout, ash.components, ash.skin, ash.utilities, components, utilities;\n"),
                css().substring(0, Math.min(160, css().length())));
    }

    @Test
    void noRuleEscapesTheNamedAshSublayers() {
        assertEquals(List.of(), CssScanner.violations(css()));
    }

    @Test
    void theJarCopyCarriesEveryPublicClassAndConfigurationProperty() {
        String text = css();
        for (String cls : AshlarCssFoundationTest.CONTRACT_CLASSES) {
            assertTrue(text.contains(cls), cls + " is missing from the jar's ashlar.css");
        }
        for (String cls : AshlarCssFoundationTest.UTILITY_CLASSES) {
            assertTrue(text.contains(cls), cls + " is missing from the jar's ashlar.css");
        }
        AshlarCssFoundationTest.RESET_PROPERTIES.values().forEach(props -> props.forEach(
                p -> assertTrue(text.contains(p + ": initial"), p + " must be reset in the jar's ashlar.css")));
        AshlarCssFoundationTest.SPACING_TOKENS.forEach(
                t -> assertTrue(text.contains(t + ":"), t + " is missing from the jar's ashlar.css"));
    }

    @Test
    void theOutputHasTheStructuralPropertiesDeterminismDependsOn() {
        byte[] bytes = packaged;
        assertFalse(bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF, "no BOM");
        String text = css();
        assertFalse(text.contains("\r"), "LF line endings only");
        assertTrue(text.endsWith("\n") && !text.endsWith("\n\n"), "exactly one trailing newline");
        assertFalse(text.matches("(?s).*\\b20\\d\\d-\\d\\d-\\d\\d.*"), "no date stamp");
        assertFalse(text.toLowerCase().contains("generated at"), "no generation timestamp");
    }

    @Test
    void templatesJavaScriptAndTheTailwindVocabularyAreStillPackaged() throws IOException {
        List<String> names = entries();
        for (String required : List.of(
                "ashlar/components/button.jte",
                "ashlar/utilities/footer.jte",
                "META-INF/resources/ashlar/js/button.js",
                "META-INF/resources/ashlar/js/theme.js",
                "ashlar/tailwind/classes.txt")) {
            assertTrue(names.contains(required), required + " is missing from the jar");
        }
        assertTrue(names.stream().filter(n -> n.startsWith("ashlar/") && n.endsWith(".jte")).count() >= 70,
                "the jar carried 70 templates before the stylesheet was added");
        assertTrue(names.stream().filter(n -> n.startsWith("META-INF/resources/ashlar/js/") && n.endsWith(".js")).count() >= 21,
                "the jar carried 21 JavaScript modules before the stylesheet was added");
    }
}
