package dev.redspartanlabs.ashlar.showcase.cascade;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starting and stopping a {@link DevToolsSession}: what happens when the startup fails, and that a good start still ends cleanly. The failures are driven with
 * {@link FakeBrowser}, a stand-in for Chrome that is a real process with a real helper process of its own, so they need no browser; one test starts a real Chrome (and says so
 * when there is none).
 *
 * <p>The failing start is run once for the tests that look at it (it takes the whole startup deadline, which is 5 s here): each of them checks one thing about that one run.
 */
class DevToolsSessionStartupTest {

    /** The startup deadline the failing run is given; the browser it starts never publishes a port, so the run takes about this long. */
    private static final Duration DEADLINE = Duration.ofSeconds(5);

    private static final String JAVA = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    @TempDir
    static Path shared;

    private static Path markers;
    private static IllegalStateException failure;
    private static long elapsedMillis;
    private static Path profile;
    private static long bystander;
    private static long browser;
    private static long helper;

    /** The command that starts the stand-in browser in {@code mode}, given the profile directory it is to use. */
    private static Function<Path, List<String>> fake(Path markers, String mode) {
        return profile -> command(markers, mode, "--user-data-dir=" + profile);
    }

    private static List<String> command(Path markers, String mode, String... flags) {
        List<String> command = new ArrayList<>(List.of(JAVA, "-Xmx32m", "-XX:TieredStopAtLevel=1", "-cp", classes(), FakeBrowser.class.getName(), markers.toString(), mode));
        command.addAll(List.of(flags));
        return command;
    }

    private static String classes() {
        try {
            return Path.of(FakeBrowser.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The process id a stand-in wrote; the test fails (rather than passing vacuously) when the stand-in never got far enough to write it. */
    private static long pidOf(Path pidFile, String what) throws IOException {
        assertTrue(Files.exists(pidFile), what + " never started (the machine may be too slow for the stand-in), so there is nothing to check");
        return Long.parseLong(Files.readString(pidFile).trim());
    }

    /** Whether the process with this id exists and is running (a process that has exited and been reaped has no handle at all). */
    private static boolean alive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private static void awaitFile(Path file) throws InterruptedException {
        long give = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!Files.exists(file) && System.nanoTime() < give) {
            Thread.sleep(20);
        }
    }

    @BeforeAll
    static void runTheFailingStart() throws Exception {
        markers = Files.createDirectories(shared.resolve("markers"));
        Path bystanderMarkers = Files.createDirectories(shared.resolve("bystander"));
        // An unrelated process that is not the browser or anything it started: it must survive the cleanup.
        new ProcessBuilder(command(bystanderMarkers, "bystander")).redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        awaitFile(bystanderMarkers.resolve("bystander.pid"));
        bystander = pidOf(bystanderMarkers.resolve("bystander.pid"), "the bystander");

        DevToolsSession.Timing timing = new DevToolsSession.Timing(DEADLINE, DEADLINE, DEADLINE, DEADLINE);
        long began = System.nanoTime();
        failure = assertThrows(IllegalStateException.class, () -> new DevToolsSession(fake(markers, "tree"), timing, Files::readAllLines));
        elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began);

        // The profile the browser was given, and the two processes it reported, are read from what it wrote.
        profile = Path.of(Files.readString(markers.resolve("profile.path")).trim());
        browser = pidOf(markers.resolve("browser.pid"), "the browser");
        helper = pidOf(markers.resolve("helper.pid"), "the browser's helper");
    }

    @AfterAll
    static void noStrayProcesses() {
        for (long pid : new long[]{bystander, browser, helper}) {
            ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
        }
    }

    // ---- a startup that fails ----

    @Test
    void aPermanentStartupFailureEndsAtTheDeadlineNotLater() {
        assertTrue(elapsedMillis >= DEADLINE.toMillis() - 200, "it waited for the deadline, not less: " + elapsedMillis + " ms");
        assertTrue(elapsedMillis < DEADLINE.toMillis() + 15_000, "and ended soon after it, cleanup included: " + elapsedMillis + " ms");
    }

    @Test
    void theFailureSaysWhichPhaseFailedWhyHowLongItTookWhatWasTerminatedAndWhetherTheProfileWasRemoved() {
        String message = failure.getMessage();
        assertTrue(message.contains("phase 'port-file'"), message);
        assertTrue(message.contains("did not publish its DevTools port within " + DEADLINE.toMillis() + " ms"), message);
        assertTrue(message.matches("(?s).*after \\d+ ms.*"), message);
        // At least the browser and its helper; the platform may add its own (on Windows each console process has a console host), so the count is not pinned to two.
        assertTrue(message.matches("(?s).*terminated ([2-9]|\\d\\d+) browser process\\(es\\).*"), message);
        assertTrue(message.contains("profile removed"), message);
        assertTrue(message.contains("infrastructure failure, not a parity result"), message);
        assertNotNull(failure.getCause(), "the original failure is the cause");
        assertEquals(0, failure.getSuppressed().length, "the cleanup had nothing to add");
    }

    @Test
    void theMessageNamesNoLocalPaths() {
        String message = failure.getMessage();
        assertFalse(message.contains(profile.toString()), message);
        assertFalse(message.contains(profile.getParent().toString()), message);
    }

    @Test
    void aFailedStartTerminatesTheBrowserAndTheHelperItStarted() {
        assertFalse(alive(browser), "the browser process was terminated");
        assertFalse(alive(helper), "and so was the helper process it started, which would otherwise have outlived it");
    }

    @Test
    void aFailedStartRemovesTheTemporaryProfile() {
        assertFalse(Files.exists(profile), "the profile directory is gone");
    }

    @Test
    void aFailedStartTerminatesNothingElse() {
        assertTrue(alive(bystander), "a process that is neither the browser nor anything it started is left alone");
    }

    // ---- a browser that exits at once ----

    @Test
    @Timeout(60)
    void aBrowserThatExitsEndsTheStartupAtOnceAndLeavesNothingBehind() throws Exception {
        Path exitMarkers = Files.createDirectories(shared.resolve("exit"));
        DevToolsSession.Timing timing = new DevToolsSession.Timing(Duration.ofSeconds(30), Duration.ofSeconds(30), Duration.ofSeconds(10), Duration.ofSeconds(30));
        List<Path> used = new ArrayList<>();
        long began = System.nanoTime();
        IllegalStateException exited = assertThrows(IllegalStateException.class, () -> new DevToolsSession(p -> {
            used.add(p);
            return command(exitMarkers, "exit");
        }, timing, Files::readAllLines));
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began);
        assertTrue(elapsed < 20_000, "it did not wait for the deadline: " + elapsed + " ms");
        assertTrue(exited.getMessage().contains("phase 'port-file'") && exited.getMessage().contains("exited before it published its DevTools port"), exited.getMessage());
        assertEquals(1, used.size());
        assertFalse(Files.exists(used.get(0)), "the profile was removed");
    }

    @Test
    @Timeout(60)
    void aBrowserThatCannotBeLaunchedAtAllLeavesNoProfileBehind() {
        List<Path> used = new ArrayList<>();
        IllegalStateException failed = assertThrows(IllegalStateException.class, () -> new DevToolsSession(p -> {
            used.add(p);
            return List.of(shared.resolve("no-such-browser").toString());
        }, DevToolsSession.Timing.DEFAULT, Files::readAllLines));
        assertTrue(failed.getMessage().contains("phase 'launch'") && failed.getMessage().contains("terminated 0 browser process(es)"), failed.getMessage());
        assertInstanceOf(IOException.class, failed.getCause());
        assertFalse(Files.exists(used.get(0)), "the profile was removed");
    }

    // ---- the port file ----

    private static Path portFile(String content) throws IOException {
        Path dir = Files.createTempDirectory(shared, "port");
        return Files.writeString(dir.resolve("DevToolsActivePort"), content, StandardCharsets.UTF_8);
    }

    @Test
    @Timeout(30)
    void aReadThatFailsAtFirstBecauseTheFileIsBusyIsRetriedUntilItWorks() throws Exception {
        Path file = portFile("9222\n/devtools/browser/abc\n");
        AtomicInteger reads = new AtomicInteger();
        int port = DevToolsSession.awaitPort(file, () -> true, Duration.ofSeconds(10), f -> {
            if (reads.incrementAndGet() <= 5) {
                throw new FileSystemException(f.toString(), null, "The process cannot access the file because it is being used by another process");
            }
            return Files.readAllLines(f);
        });
        assertEquals(9222, port);
        assertEquals(6, reads.get(), "five failed reads, then the one that worked");
    }

    @Test
    @Timeout(30)
    void aPortFileThatIsNotYetCompleteIsWaitedFor() throws Exception {
        Path file = portFile("9222");
        AtomicInteger reads = new AtomicInteger();
        int port = DevToolsSession.awaitPort(file, () -> true, Duration.ofSeconds(10), f -> {
            if (reads.incrementAndGet() == 3) {
                Files.writeString(f, "9222\n/devtools/browser/abc\n");
            }
            return Files.readAllLines(f);
        });
        assertEquals(9222, port);
        assertTrue(reads.get() >= 3);
    }

    @Test
    @Timeout(30)
    void aReadThatNeverWorksEndsAtTheDeadlineAndCarriesTheLastFailure() throws Exception {
        Path file = portFile("9222\n/devtools/browser/abc\n");
        AtomicInteger reads = new AtomicInteger();
        long began = System.nanoTime();
        IllegalStateException failed = assertThrows(IllegalStateException.class, () -> DevToolsSession.awaitPort(file, () -> true, Duration.ofMillis(600), f -> {
            throw new FileSystemException(f.toString(), null, "read " + reads.incrementAndGet());
        }));
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began);
        assertTrue(elapsed >= 550 && elapsed < 5_000, "a permanent failure is not retried for ever: " + elapsed + " ms");
        assertTrue(failed.getMessage().contains("within 600 ms") && failed.getMessage().contains(reads.get() + " read(s) of the port file failed"), failed.getMessage());
        assertInstanceOf(FileSystemException.class, failed.getCause());
        assertTrue(failed.getCause().getMessage().contains("read " + reads.get()), "the cause is the LAST failure: " + failed.getCause().getMessage());
        assertTrue(reads.get() >= 3, "and it was retried meanwhile: " + reads);
    }

    @Test
    @Timeout(30)
    void aBrowserThatHasExitedEndsTheWaitWithoutWaitingForTheDeadline() {
        long began = System.nanoTime();
        IllegalStateException failed = assertThrows(IllegalStateException.class,
                () -> DevToolsSession.awaitPort(Path.of("no-such-port-file"), () -> false, Duration.ofSeconds(25), Files::readAllLines));
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began) < 5_000);
        assertTrue(failed.getMessage().contains("exited before it published its DevTools port"), failed.getMessage());
    }

    @Test
    @Timeout(30)
    void aPortFileThatCannotBeAPortFailsAtOnceNotAtTheDeadline() throws Exception {
        Path file = portFile("not-a-port\n/devtools/browser/abc\n");
        long began = System.nanoTime();
        IllegalStateException failed = assertThrows(IllegalStateException.class, () -> DevToolsSession.awaitPort(file, () -> true, Duration.ofSeconds(25), Files::readAllLines));
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began) < 5_000, "a permanent problem is not waited out");
        assertInstanceOf(NumberFormatException.class, failed.getCause());
    }

    // ---- the /json/list request ----

    private record Endpoint(HttpServer server, CountDownLatch release) implements AutoCloseable {
        int port() {
            return server.getAddress().getPort();
        }

        @Override
        public void close() {
            release.countDown();
            server.stop(0);
        }
    }

    /** A loopback endpoint that answers {@code /json/list} with {@code body}, or does not answer at all (until closed) when {@code body} is null. */
    private static Endpoint endpoint(String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/json/list", exchange -> {
            if (body == null) {
                try {
                    release.await(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                exchange.close();
                return;
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return new Endpoint(server, release);
    }

    @Test
    @Timeout(30)
    void aTargetLookupThatIsNeverAnsweredTimesOutPredictablyAndCarriesTheTimeout() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        try (Endpoint silent = endpoint(null)) {
            long began = System.nanoTime();
            IllegalStateException failed = assertThrows(IllegalStateException.class,
                    () -> DevToolsSession.awaitPageTarget(http, silent.port(), Duration.ofMillis(1_500), Duration.ofMillis(400)));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began);
            assertTrue(elapsed >= 1_400 && elapsed < 6_000, "it ended at the deadline: " + elapsed + " ms");
            assertTrue(failed.getMessage().contains("no page target within 1500 ms") && failed.getMessage().contains("bounded to 400 ms"), failed.getMessage());
            assertInstanceOf(HttpTimeoutException.class, failed.getCause());
        } finally {
            http.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    void aSingleLookupIsNeverGivenMoreTimeThanIsLeftOfTheDeadline() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        try (Endpoint silent = endpoint(null)) {
            long began = System.nanoTime();
            // The request bound (20 s) is far longer than the deadline (700 ms): the lookup ends at the deadline.
            assertThrows(IllegalStateException.class, () -> DevToolsSession.awaitPageTarget(http, silent.port(), Duration.ofMillis(700), Duration.ofSeconds(20)));
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began) < 6_000);
        } finally {
            http.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    void aTargetLookupFindsThePageAmongTheTargets() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        String body = "[{\"id\":\"1\",\"type\":\"service_worker\",\"webSocketDebuggerUrl\":\"ws://127.0.0.1:1/devtools/page/sw\"},"
                + "{\"id\":\"2\",\"type\": \"page\",\"title\":\"\",\"webSocketDebuggerUrl\": \"ws://127.0.0.1:1/devtools/page/ABC\"}]";
        try (Endpoint chrome = endpoint(body)) {
            assertEquals("ws://127.0.0.1:1/devtools/page/ABC", DevToolsSession.awaitPageTarget(http, chrome.port(), Duration.ofSeconds(5), Duration.ofSeconds(2)));
        } finally {
            http.shutdownNow();
        }
    }

    // ---- a cleanup that itself fails ----

    /** Never touched: the failure report only scrubs it from messages. */
    private static final Path SOME_PROFILE = Path.of("some", "profile");

    @AfterEach
    void aTestMustNotLeaveTheInterruptFlagSet() {
        Thread.interrupted();
    }

    private static IllegalStateException theRealFailure() {
        return new IllegalStateException("the real startup failure");
    }

    private static IllegalStateException report(Throwable failure, Supplier<DevToolsSession.Termination> terminator, Supplier<List<IOException>> sweeper) {
        return DevToolsSession.startupFailure("port-file", failure, System.nanoTime(), SOME_PROFILE, terminator, sweeper);
    }

    @Test
    void aTerminationThatThrowsNeverReplacesTheOriginalFailureAndTheProfileIsStillCleaned() {
        IllegalStateException original = theRealFailure();
        UnsupportedOperationException broken = new UnsupportedOperationException("cannot enumerate the processes");
        AtomicInteger sweeps = new AtomicInteger();
        IllegalStateException result = report(original, () -> {
            throw broken;
        }, () -> {
            sweeps.incrementAndGet();
            return List.of();
        });
        assertSame(original, result.getCause(), "the startup failure is still THE cause");
        assertEquals(1, sweeps.get(), "the profile cleanup was attempted although the termination threw");
        assertEquals(List.of(broken), List.of(result.getSuppressed()), "and the cleanup failure is carried, not lost");
        assertTrue(result.getMessage().contains("phase 'port-file'") && result.getMessage().contains("the real startup failure"), result.getMessage());
        assertTrue(result.getMessage().contains("termination FAILED (UnsupportedOperationException)") && result.getMessage().contains("profile removed"), result.getMessage());
        assertFalse(result.getMessage().contains("cannot enumerate"), "only the type of a cleanup failure is in the message: " + result.getMessage());
    }

    @Test
    void aProfileCleanupThatThrowsNeverReplacesTheOriginalFailureAndTheTerminationWasStillAttempted() {
        IllegalStateException original = theRealFailure();
        SecurityException broken = new SecurityException("denied");
        AtomicInteger terminations = new AtomicInteger();
        IllegalStateException result = report(original, () -> {
            terminations.incrementAndGet();
            return new DevToolsSession.Termination(3, false);
        }, () -> {
            throw broken;
        });
        assertSame(original, result.getCause());
        assertEquals(1, terminations.get());
        assertEquals(List.of(broken), List.of(result.getSuppressed()));
        assertTrue(result.getMessage().contains("terminated 3 browser process(es)") && result.getMessage().contains("profile cleanup FAILED (SecurityException)"), result.getMessage());
    }

    @Test
    void whenBothCleanupStepsThrowBothAreCarriedAndTheOriginalFailureIsStillTheCause() {
        IllegalStateException original = theRealFailure();
        RuntimeException first = new UnsupportedOperationException("one");
        RuntimeException second = new IllegalArgumentException("two");
        IllegalStateException result = report(original, () -> {
            throw first;
        }, () -> {
            throw second;
        });
        assertSame(original, result.getCause());
        assertEquals(List.of(first, second), List.of(result.getSuppressed()));
        assertTrue(result.getMessage().contains("termination FAILED (UnsupportedOperationException)") && result.getMessage().contains("profile cleanup FAILED (IllegalArgumentException)"),
                result.getMessage());
    }

    @Test
    void filesThatCouldNotBeDeletedAreCarriedAsSuppressedAndReportedInTheMessage() {
        IOException a = new java.nio.file.DirectoryNotEmptyException("a");
        IOException b = new FileSystemException("b");
        IllegalStateException result = report(theRealFailure(), () -> new DevToolsSession.Termination(2, false), () -> List.of(a, b));
        assertEquals(List.of(a, b), List.of(result.getSuppressed()));
        assertTrue(result.getMessage().contains("terminated 2 browser process(es); profile NOT fully removed (2 failure(s))"), result.getMessage());
    }

    @Test
    void whenNoBrowserWasLaunchedNothingIsTerminatedAndTheProfileIsStillCleaned() {
        AtomicInteger sweeps = new AtomicInteger();
        IllegalStateException result = report(theRealFailure(), null, () -> {
            sweeps.incrementAndGet();
            return List.of();
        });
        assertEquals(1, sweeps.get());
        assertTrue(result.getMessage().contains("terminated 0 browser process(es)") && result.getMessage().contains("profile removed"), result.getMessage());
    }

    @Test
    void anInterruptedStartupLeavesTheThreadInterruptedAfterTheCleanup() {
        IllegalStateException result = report(new InterruptedException("interrupted while waiting"), () -> new DevToolsSession.Termination(1, false), List::of);
        assertTrue(Thread.interrupted(), "the interrupt that ended the startup is not swallowed by the cleanup");
        assertTrue(result.getMessage().contains("InterruptedException"), result.getMessage());
    }

    @Test
    void anInterruptThatArrivedBeforeTheCleanupDoesNotCutTheCleanupShortAndIsRestoredAfterIt() {
        AtomicBoolean interruptedDuringTermination = new AtomicBoolean(true);
        AtomicBoolean interruptedDuringSweep = new AtomicBoolean(true);
        Thread.currentThread().interrupt();
        report(theRealFailure(), () -> {
            interruptedDuringTermination.set(Thread.currentThread().isInterrupted());
            return new DevToolsSession.Termination(1, false);
        }, () -> {
            interruptedDuringSweep.set(Thread.currentThread().isInterrupted());
            return List.of();
        });
        assertFalse(interruptedDuringTermination.get() || interruptedDuringSweep.get(), "the waits in the cleanup run uninterrupted, so they are not cut short");
        assertTrue(Thread.interrupted(), "and the interrupt is restored afterwards");
    }

    @Test
    void anInterruptDuringTheTerminationOrTheSweepIsRestored() {
        report(theRealFailure(), () -> new DevToolsSession.Termination(1, true), List::of);
        assertTrue(Thread.interrupted(), "reported by the termination");
        report(theRealFailure(), () -> new DevToolsSession.Termination(1, false), () -> {
            Thread.currentThread().interrupt();
            return List.of();
        });
        assertTrue(Thread.interrupted(), "set by the sweep, as deleteTree does when it is interrupted");
    }

    @Test
    void anInterruptIsRestoredEvenWhenACleanupStepThrows() {
        Thread.currentThread().interrupt();
        IllegalStateException original = theRealFailure();
        IllegalStateException result = report(original, () -> {
            throw new UnsupportedOperationException();
        }, () -> {
            throw new SecurityException();
        });
        assertSame(original, result.getCause());
        assertTrue(Thread.interrupted());
    }

    @Test
    void withoutAnInterruptTheFlagStaysClear() {
        report(theRealFailure(), () -> new DevToolsSession.Termination(1, false), List::of);
        assertFalse(Thread.currentThread().isInterrupted());
    }

    // ---- a start that works ----

    private static boolean isChrome(ProcessHandle handle, Path executable) {
        return handle.info().command().map(c -> Path.of(c).getFileName().toString().equalsIgnoreCase(executable.getFileName().toString())).orElse(false);
    }

    @Test
    @Timeout(120)
    void aGoodStartWorksAndItsCloseTerminatesTheWholeBrowserRemovesTheProfileAndLeavesOtherBrowsersAlone() throws Exception {
        var chrome = HeadlessChrome.find();
        if (chrome.isEmpty()) {
            if (System.getenv("CI") != null || "true".equalsIgnoreCase(System.getenv("ASHLAR_REQUIRE_BROWSER"))) {
                throw new IllegalStateException("Chrome is required (CI or ASHLAR_REQUIRE_BROWSER set) but was not found.");
            }
            Assumptions.abort("Chrome not found; the real-browser start was not tested");
        }
        // Browsers that are running before this session starts (a person's own, for one): none of them may be touched.
        Set<Long> others = ProcessHandle.allProcesses().filter(h -> isChrome(h, chrome.get())).map(ProcessHandle::pid).collect(Collectors.toSet());
        DevToolsSession session = new DevToolsSession(chrome.get());
        List<ProcessHandle> ours = new ArrayList<>();
        Path profile = session.profile();
        try {
            assertEquals("ok", session.evaluate("'ok'"), "the session works");
            ours.add(session.browser());
            session.browser().descendants().forEach(ours::add);
            assertTrue(ours.size() > 1, "a real browser has helper processes: " + ours.size());
            assertTrue(Files.isDirectory(profile));
        } finally {
            Set<Long> stillRunning = others.stream().filter(pid -> ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).collect(Collectors.toSet());
            session.close();
            assertFalse(Files.exists(profile), "the profile was removed");
            for (ProcessHandle handle : ours) {
                assertFalse(handle.isAlive(), "process " + handle.pid() + " of the session's own browser is gone");
            }
            for (long pid : stillRunning) {
                assertTrue(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false), "a browser that was not this session's (" + pid + ") was left running");
            }
        }
    }

    // ---- HeadlessChrome.dumpDom, whose launch has the same shape ----

    /** A launcher script that starts the stand-in browser (and passes it the flags it was given), because {@link HeadlessChrome} runs an executable, not a command line. */
    private static Path launcher(Path directory, Path markers) throws IOException {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
        Path script = directory.resolve(windows ? "fake-chrome.cmd" : "fake-chrome.sh");
        String start = "\"" + JAVA + "\" -Xmx32m -XX:TieredStopAtLevel=1 -cp \"" + classes() + "\" " + FakeBrowser.class.getName() + " \"" + markers + "\" tree ";
        Files.writeString(script, windows ? "@echo off\r\n" + start + "%*\r\n" : "#!/bin/sh\n" + start + "\"$@\"\n");
        script.toFile().setExecutable(true);
        return script;
    }

    @Test
    @Timeout(120)
    void aPageDumpThatRunsOutOfTimeTerminatesTheWholeBrowserAndRemovesItsProfile() throws Exception {
        Path dumpMarkers = Files.createDirectories(shared.resolve("dump"));
        HeadlessChrome fake = new HeadlessChrome(launcher(Files.createDirectories(shared.resolve("launcher")), dumpMarkers));
        IllegalStateException timedOut = assertThrows(IllegalStateException.class, () -> fake.dumpDom("about:blank", List.of(), Duration.ofSeconds(6)));
        assertTrue(timedOut.getMessage().contains("did not finish within 6s") && timedOut.getMessage().contains("browser process(es)"), timedOut.getMessage());
        long dumpBrowser = pidOf(dumpMarkers.resolve("browser.pid"), "the browser");
        long dumpHelper = pidOf(dumpMarkers.resolve("helper.pid"), "the browser's helper");
        try {
            assertFalse(alive(dumpBrowser), "the browser was terminated");
            assertFalse(alive(dumpHelper), "and so was its helper, which killing only the browser process would have left running");
            assertFalse(Files.exists(Path.of(Files.readString(dumpMarkers.resolve("profile.path")).trim())), "and its profile was removed");
        } finally {
            ProcessHandle.of(dumpBrowser).ifPresent(ProcessHandle::destroyForcibly);
            ProcessHandle.of(dumpHelper).ifPresent(ProcessHandle::destroyForcibly);
        }
    }
}
