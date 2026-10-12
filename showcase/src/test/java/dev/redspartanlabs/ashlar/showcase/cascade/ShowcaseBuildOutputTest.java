package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase C, static half: what the real production build produced. Needs no
 * browser. Checks the packaged boot jar, the compiled production stylesheet and
 * that the production Tailwind entry follows the measured consumer contract.
 *
 * <p>These are structural checks. {@link ShowcaseRuntimeTest} is what proves the
 * browser behaves as these files say it should.
 */
class ShowcaseBuildOutputTest {

    private static final String CSS_ENTRY = "META-INF/resources/ashlar/css/ashlar.css";

    private static final List<String> ASH_SUBLAYERS = List.of(
            "ash.tokens", "ash.base", "ash.layout", "ash.components", "ash.skin", "ash.utilities");

    private static Path property(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, name + " must be set by the Gradle test task");
        return Path.of(value);
    }

    private static String productionCss() throws IOException {
        try (InputStream in = ShowcaseBuildOutputTest.class.getResourceAsStream("/static/css/app.css")) {
            assertNotNull(in, "compiled production app.css must be on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void theBootJarCarriesTheAshlarJarWhichCarriesTheGeneratedStylesheet() throws IOException {
        byte[] generated = Files.readAllBytes(property("ashlar.css.generated"));
        try (ZipFile boot = new ZipFile(property("ashlar.showcase.jar").toFile())) {
            ZipEntry nested = boot.getEntry("BOOT-INF/lib/ashlar-0.1.0.jar");
            assertNotNull(nested, "the Ashlar jar must be nested in the boot jar");
            byte[] found = null;
            try (ZipInputStream inner = new ZipInputStream(new ByteArrayInputStream(boot.getInputStream(nested).readAllBytes()))) {
                for (ZipEntry e = inner.getNextEntry(); e != null; e = inner.getNextEntry()) {
                    if (e.getName().equals(CSS_ENTRY)) {
                        found = inner.readAllBytes();
                    }
                }
            }
            assertNotNull(found, CSS_ENTRY + " is missing from the Ashlar jar inside the boot jar");
            assertArrayEquals(generated, found);
        }
    }

    @Test
    void theBootJarServesTheCompiledStylesheetThatContainsAshlarsCss() throws IOException {
        try (ZipFile boot = new ZipFile(property("ashlar.showcase.jar").toFile())) {
            ZipEntry app = boot.getEntry("BOOT-INF/classes/static/css/app.css");
            assertNotNull(app);
            String css = new String(boot.getInputStream(app).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(css.contains("--ash-color-bg"), "Ashlar's tokens are bundled into app.css");
            assertTrue(css.contains(".ash-sr-only"), "Ashlar's utility is bundled into app.css");
        }
    }

    @Test
    void theCompiledProductionStylesheetHasTheIntendedEffectiveLayerOrder() throws IOException {
        LayerOrder order = LayerOrder.of(productionCss());
        assertEquals(List.of("properties", "theme", "base", "ash", "components", "utilities"), order.topLevel(), order.toString());
        assertEquals(ASH_SUBLAYERS, order.children("ash"), order.toString());
    }

    @Test
    void theProductionEntryImportsAshlarBeforeTailwindAsTheMeasuredContractRequires() throws IOException {
        String source = Files.readString(Path.of("src/main/tailwind/app.css"), StandardCharsets.UTF_8);
        int ashlar = source.indexOf("@import \"../../../build/ashlar-css/ashlar.css\";");
        int tailwind = source.indexOf("@import \"tailwindcss\";");
        assertTrue(ashlar >= 0 && tailwind >= 0 && ashlar < tailwind,
                "Ashlar's self-ordering stylesheet must be the first import");
    }

    @Test
    void theBaselineEntryDiffersFromTheProductionEntryOnlyByTheAshlarImport() throws IOException {
        Set<String> production = codeLines(Files.readString(Path.of("src/main/tailwind/app.css"), StandardCharsets.UTF_8));
        Set<String> baseline = codeLines(Files.readString(Path.of("src/test/tailwind/phase-c-baseline-entry.css"), StandardCharsets.UTF_8));
        Set<String> expected = new LinkedHashSet<>(production);
        expected.remove("@import \"../../../build/ashlar-css/ashlar.css\";");
        assertEquals(expected, baseline);
    }

    private static Set<String> codeLines(String css) {
        return List.of(css.replaceAll("(?s)/\\*.*?\\*/", "").split("\\R")).stream()
                .map(String::trim).filter(l -> !l.isEmpty()).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
