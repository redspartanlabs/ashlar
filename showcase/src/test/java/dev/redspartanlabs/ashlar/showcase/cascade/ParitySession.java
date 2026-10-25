package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Assumptions;

import java.nio.file.Path;

/**
 * The environment every parity suite built on the shared harness needs: the Gradle-provided locations of the compiled
 * oracle stylesheets, the boot jar and the generated Ashlar stylesheet, and a real Chrome.
 *
 * <p>This is the one place the "Chrome is required here, optional there" policy lives. When Chrome cannot be found
 * the suite is ABORTED (reported as skipped, never as passed), unless {@code CI} or {@code ASHLAR_REQUIRE_BROWSER=true}
 * is set, in which case a missing Chrome is a failure. The older per-component suites carry their own copies of
 * this logic and are not changed by the harness.
 */
final class ParitySession {

    private final Path generatedDir;
    private final Path bootJar;
    private final HeadlessChrome chrome;

    private ParitySession(Path generatedDir, Path bootJar, HeadlessChrome chrome) {
        this.generatedDir = generatedDir;
        this.bootJar = bootJar;
        this.chrome = chrome;
    }

    /** Checks the environment for the suite called {@code name} and returns it, or aborts the suite when Chrome is absent. */
    static ParitySession open(String name) {
        String generatedDir = System.getProperty("ashlar.cascade.dir");
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (generatedDir == null || bootJar == null || System.getProperty("ashlar.css.generated") == null) {
            throw new IllegalStateException("ashlar.cascade.dir, ashlar.showcase.jar and ashlar.css.generated must be set; run through Gradle (:showcase:test)");
        }
        var chromePath = HeadlessChrome.find();
        if (chromePath.isEmpty()) {
            if (System.getenv("CI") != null || "true".equalsIgnoreCase(System.getenv("ASHLAR_REQUIRE_BROWSER"))) {
                throw new IllegalStateException("Chrome is required (CI or ASHLAR_REQUIRE_BROWSER set) but was not found.");
            }
            System.out.println("[" + name + "] Chrome not found - " + name + " SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the " + name + " checks were not run");
        }
        return new ParitySession(Path.of(generatedDir), Path.of(bootJar), new HeadlessChrome(chromePath.get()));
    }

    /** The directory holding the stylesheets the Tailwind CLI compiled for the tests (served at {@code /generated/}). */
    Path generatedDir() {
        return generatedDir;
    }

    Path bootJar() {
        return bootJar;
    }

    HeadlessChrome chrome() {
        return chrome;
    }
}
