package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Locates a real Chrome/Chromium and loads a page in it, returning the serialized DOM. */
final class HeadlessChrome {

    private final Path executable;

    HeadlessChrome(Path executable) {
        this.executable = executable;
    }

    Path executable() {
        return executable;
    }

    /**
     * Order: system property {@code ashlar.chrome}, env {@code ASHLAR_CHROME},
     * env {@code CHROME_BIN}, well-known install paths, then PATH.
     */
    static Optional<Path> find() {
        List<String> candidates = new ArrayList<>();
        candidates.add(System.getProperty("ashlar.chrome"));
        candidates.add(System.getenv("ASHLAR_CHROME"));
        candidates.add(System.getenv("CHROME_BIN"));
        for (String base : new String[]{System.getenv("PROGRAMFILES"), System.getenv("PROGRAMFILES(X86)"), System.getenv("LOCALAPPDATA")}) {
            if (base != null) {
                candidates.add(base + "\\Google\\Chrome\\Application\\chrome.exe");
            }
        }
        candidates.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && Files.isExecutable(Path.of(candidate))) {
                return Optional.of(Path.of(candidate));
            }
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(java.io.File.pathSeparator)) {
                for (String name : new String[]{"google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "chrome", "chrome.exe"}) {
                    Path candidate = Path.of(dir, name);
                    if (Files.isExecutable(candidate) && !Files.isDirectory(candidate)) {
                        return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Loads {@code url} in headless Chrome and returns the DOM after the load event. */
    String dumpDom(String url) throws Exception {
        return dumpDom(url, List.of());
    }

    /** As {@link #dumpDom(String)}, with extra Chrome command-line flags. */
    String dumpDom(String url, List<String> extraFlags) throws Exception {
        return dumpDom(url, extraFlags, DUMP_TIMEOUT);
    }

    /** How long a page may take to load and be dumped: the load, the 5 s of virtual time and the dump of the heaviest page the suites use finish in a few seconds. */
    static final Duration DUMP_TIMEOUT = Duration.ofSeconds(90);

    /**
     * As {@link #dumpDom(String, List)}, with the time the browser is given stated. When it runs out, the browser and everything it started are terminated (not only the
     * browser process, whose helpers would otherwise outlive it) before the failure is thrown.
     */
    String dumpDom(String url, List<String> extraFlags, Duration timeout) throws Exception {
        Path profile = Files.createTempDirectory("ashlar-cascade-chrome");
        try {
            List<String> command = new ArrayList<>(List.of(
                    executable.toString(),
                    "--headless=new",
                    "--disable-gpu",
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--user-data-dir=" + profile,
                    "--virtual-time-budget=5000"));
            if (System.getenv("CI") != null) {
                command.add("--no-sandbox");
            }
            command.addAll(extraFlags);
            command.add("--dump-dom");
            command.add(url);

            Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            CompletableFuture<byte[]> stdout = CompletableFuture.supplyAsync(() -> {
                try {
                    return p.getInputStream().readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            if (!p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                DevToolsSession.Termination termination = DevToolsSession.terminate(p);
                stdout.cancel(true);
                if (termination.interrupted()) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("Chrome did not finish within " + (timeout.toMillis() % 1000 == 0 ? timeout.toSeconds() + "s" : timeout.toMillis() + " ms") + " for " + url
                        + " (terminated " + termination.processes() + " browser process(es))");
            }
            return new String(stdout.get(15, TimeUnit.SECONDS), StandardCharsets.UTF_8);
        } finally {
            // Best-effort cleanup of the throwaway profile: the failures are not reported here, but the sweep copes with entries that vanish while it runs and is repeated.
            DevToolsSession.deleteTree(profile);
        }
    }
}
