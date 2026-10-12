package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Starts the REAL packaged application: the Spring Boot jar, as a separate
 * process, on a free loopback port. Nothing is mocked - templates, resources,
 * the Ashlar jar nested in BOOT-INF/lib and the compiled stylesheet are exactly
 * what a deployment would run.
 */
final class RunningShowcase implements AutoCloseable {

    private final Process process;
    private final Path log;
    private final int port;

    private RunningShowcase(Process process, Path log, int port) {
        this.process = process;
        this.log = log;
        this.port = port;
    }

    static RunningShowcase start(Path bootJar) throws Exception {
        if (!Files.isRegularFile(bootJar)) {
            throw new IllegalStateException("the boot jar must have been built: " + bootJar);
        }
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        Path log = Files.createTempFile("ashlar-showcase", ".log");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-jar", bootJar.toString(), "--server.port=" + port)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        RunningShowcase showcase = new RunningShowcase(process, log, port);
        showcase.awaitReady();
        return showcase;
    }

    int port() {
        return port;
    }

    private void awaitReady() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/ashlar/"))
                .timeout(Duration.ofSeconds(3)).GET().build();
        long deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("the showcase exited early:\n" + Files.readString(log));
            }
            try {
                if (client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (IOException notYet) {
                Thread.sleep(500);
            }
        }
        close();
        throw new IllegalStateException("the showcase did not become ready within 120s");
    }

    @Override
    public void close() {
        process.destroyForcibly();
        try {
            Files.deleteIfExists(log);
        } catch (IOException ignored) {
            // Best-effort cleanup of the throwaway log.
        }
    }
}
