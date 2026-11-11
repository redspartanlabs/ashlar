package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A stand-in for Chrome, started as a separate JVM by {@link DevToolsSessionStartupTest}, so that the startup and cleanup of {@link DevToolsSession} and {@link HeadlessChrome}
 * can be tested with real processes and no real browser. It writes the id of each process it is (atomically, so a reader never sees half a number) into the directory it is
 * given, which the test reads to see what is alive afterwards. Whatever happens it ends itself after two minutes, so a failed test cannot leave one running.
 *
 * <p>Usage: {@code FakeBrowser <markerDirectory> <mode> [browser flags...]}, where the mode is
 * <ul>
 *   <li>{@code tree}: a browser that never publishes a DevTools port and has a helper process of its own; it records the profile it was given ({@code --user-data-dir=...});</li>
 *   <li>{@code helper}: the helper that {@code tree} starts;</li>
 *   <li>{@code exit}: a browser that exits at once;</li>
 *   <li>{@code bystander}: an unrelated process that must never be terminated by the code under test.</li>
 * </ul>
 */
public final class FakeBrowser {

    private FakeBrowser() {
    }

    public static void main(String[] args) throws Exception {
        Timer selfDestruct = new Timer(true);
        selfDestruct.schedule(new TimerTask() {
            @Override
            public void run() {
                Runtime.getRuntime().halt(9);
            }
        }, 120_000);
        Path markers = Path.of(args[0]);
        String mode = args[1];
        switch (mode) {
            case "tree" -> {
                for (int i = 2; i < args.length; i++) {
                    if (args[i].startsWith("--user-data-dir=")) {
                        write(markers, "profile.path", args[i].substring("--user-data-dir=".length()));
                    }
                }
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                String classes = Path.of(FakeBrowser.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
                new ProcessBuilder(java, "-Xmx32m", "-XX:TieredStopAtLevel=1", "-cp", classes, FakeBrowser.class.getName(), markers.toString(), "helper")
                        .redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                // The browser reports itself only once its helper has, so a test that finds both files knows the whole tree existed.
                long give = System.nanoTime() + 20_000_000_000L;
                while (!Files.exists(markers.resolve("helper.pid")) && System.nanoTime() < give) {
                    Thread.sleep(20);
                }
                write(markers, "browser.pid", Long.toString(ProcessHandle.current().pid()));
                sleepUntilKilled();
            }
            case "helper" -> {
                write(markers, "helper.pid", Long.toString(ProcessHandle.current().pid()));
                sleepUntilKilled();
            }
            case "bystander" -> {
                write(markers, "bystander.pid", Long.toString(ProcessHandle.current().pid()));
                sleepUntilKilled();
            }
            case "exit" -> {
                write(markers, "browser.pid", Long.toString(ProcessHandle.current().pid()));
                System.exit(3);
            }
            default -> throw new IllegalArgumentException("unknown mode " + mode);
        }
    }

    private static void sleepUntilKilled() throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void write(Path directory, String name, String content) throws IOException {
        Path temporary = Files.writeString(directory.resolve(name + ".tmp"), content);
        Files.move(temporary, directory.resolve(name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
