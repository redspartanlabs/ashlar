package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cleanup of the throwaway Chrome profile that {@link DevToolsSession#close()} runs. Chrome's helper processes delete and write files in that profile while it is
 * being removed, and an entry that vanished mid-sweep used to escape as an {@code UncheckedIOException}, failing a whole parity class after every measurement had
 * succeeded. These tests drive the sweep itself, with no browser, and make the race deterministic: the sweep reports each entry to a callback just before it deletes it,
 * and the tests use that moment to make files and directories disappear or appear.
 */
class DevToolsSessionCleanupTest {

    @TempDir
    Path temp;

    /** A profile-shaped tree: nested directories, an empty directory and files at several depths. Returns the root. */
    private Path populated(String name) throws IOException {
        Path root = Files.createDirectories(temp.resolve(name));
        Files.writeString(root.resolve("Local State"), "{}");
        Files.createDirectories(root.resolve("Default/Cache/Cache_Data"));
        Files.writeString(root.resolve("Default/Preferences"), "{}");
        Files.writeString(root.resolve("Default/Cache/Cache_Data/f_000001"), "x");
        Files.writeString(root.resolve("Default/Cache/Cache_Data/f_000002"), "y");
        Files.createDirectories(root.resolve("Crashpad/attachments"));
        Files.createDirectories(root.resolve("Empty"));
        return root;
    }

    private static int count(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return (int) walk.count();
        }
    }

    @Test
    void aPopulatedDirectoryIsRemovedCompletelyWithNoFailures() throws IOException {
        Path root = populated("profile");
        assertTrue(count(root) > 10);
        assertEquals(List.of(), DevToolsSession.deleteTree(root));
        assertFalse(Files.exists(root), "the directory itself is gone, not only its contents");
    }

    @Test
    void aDirectoryThatIsAlreadyGoneIsNotAFailure() {
        Path missing = temp.resolve("never-created");
        assertEquals(List.of(), DevToolsSession.deleteTree(missing));
        assertFalse(Files.exists(missing));
    }

    @Test
    void cleaningUpRepeatedlyIsSafeEachTimeAndAfterwards() throws IOException {
        Path root = populated("profile");
        assertEquals(List.of(), DevToolsSession.deleteTree(root));
        assertEquals(List.of(), DevToolsSession.deleteTree(root), "a second sweep of the same, now missing, directory");
        assertEquals(List.of(), DevToolsSession.deleteTree(root), "and a third");
        Path again = populated("profile2");
        assertEquals(List.of(), DevToolsSession.deleteTree(again));
        assertFalse(Files.exists(again));
    }

    @Test
    void filesThatDisappearDuringTheSweepAreExpectedAndNotFailures() throws IOException {
        Path root = populated("profile");
        AtomicInteger vanished = new AtomicInteger();
        // At the first file the sweep is about to delete, every other file in the tree is deleted by "someone else": the entries the sweep has not reached yet are gone
        // when it gets to them, and the one it is about to delete is gone when it deletes it.
        AtomicBoolean fired = new AtomicBoolean();
        // A single pass (sweep), not deleteTree: the bounded repetition would clear a wrongly recorded failure on its next pass and hide the very thing being tested.
        List<IOException> failures = DevToolsSession.sweep(root, path -> {
            if (Files.isRegularFile(path) && fired.compareAndSet(false, true)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    for (Path p : walk.filter(Files::isRegularFile).toList()) {
                        Files.deleteIfExists(p);
                        vanished.incrementAndGet();
                    }
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }
        });
        assertTrue(fired.get() && vanished.get() >= 4, "the race really happened: " + vanished);
        assertEquals(List.of(), failures);
        assertFalse(Files.exists(root));
    }

    @Test
    void aDirectoryThatDisappearsJustBeforeItsOwnDeletionIsExpectedToo() throws IOException {
        Path root = populated("profile");
        AtomicInteger raced = new AtomicInteger();
        List<IOException> failures = DevToolsSession.sweep(root, path -> {
            if (Files.isDirectory(path)) {
                try {
                    // By now the sweep has emptied it, so another process removing it first is exactly the race.
                    Files.delete(path);
                    raced.incrementAndGet();
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }
        });
        assertTrue(raced.get() >= 4, "every directory was removed from under the sweep: " + raced);
        assertEquals(List.of(), failures);
        assertFalse(Files.exists(root));
    }

    @Test
    void anUnexpectedFailureIsKeptNotSuppressedAndTheRestOfTheTreeIsStillRemoved() throws IOException {
        Path root = populated("profile");
        // A new file appears in the root every time the sweep is about to delete it (a helper process still writing): the root can never be removed.
        AtomicInteger late = new AtomicInteger();
        List<IOException> failures = DevToolsSession.deleteTree(root, path -> {
            if (path.equals(root)) {
                try {
                    Files.writeString(root.resolve("late-" + late.incrementAndGet()), "z");
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }
        });
        assertFalse(failures.isEmpty(), "the failure is reported, not turned into a success");
        assertInstanceOf(DirectoryNotEmptyException.class, failures.get(0));
        assertTrue(Files.exists(root), "the root could not be removed");
        assertFalse(Files.exists(root.resolve("Default")) || Files.exists(root.resolve("Crashpad")) || Files.exists(root.resolve("Local State")),
                "but everything else in the tree was deleted: the sweep goes on after a failure");
        assertTrue(late.get() >= 2, "and the sweep was repeated, a bounded number of times: " + late);
        assertTrue(late.get() <= 3, "bounded: " + late);
    }

    @Test
    void aFailureThatClearsOnTheNextSweepIsNotReported() throws IOException {
        Path root = populated("profile");
        AtomicBoolean once = new AtomicBoolean();
        List<IOException> failures = DevToolsSession.deleteTree(root, path -> {
            if (path.equals(root) && once.compareAndSet(false, true)) {
                try {
                    Files.writeString(root.resolve("late"), "z");
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }
        });
        assertEquals(List.of(), failures, "a helper that wrote once, while exiting, does not fail the run");
        assertFalse(Files.exists(root));
    }

    // ---- making a deletion genuinely fail ----

    /**
     * How a test makes a deletion fail. POSIX permissions are used wherever a POSIX view exists. The DOS read-only flag is used only where there is none (Windows): Linux
     * also offers a DOS view, but there the flag is metadata and does not stop a file being deleted, so the presence of that view says nothing about enforcement.
     */
    private enum Mechanism {
        POSIX, DOS;

        /** The mechanism for the filesystem {@code file} is on, or null when it offers neither. */
        static Mechanism of(Path file) {
            if (Files.getFileAttributeView(file, PosixFileAttributeView.class) != null) {
                return POSIX;
            }
            return Files.getFileAttributeView(file, DosFileAttributeView.class) != null ? DOS : null;
        }
    }

    /**
     * A restriction that makes the deletion of one file fail, and its undo. It is closed by try-with-resources, so it is lifted whether or not an assertion failed; an undo
     * that fails is then added as suppressed to the failure already in flight and never replaces it. An entry that is already gone needs no undo, and closing twice is safe.
     */
    private static final class Restriction implements AutoCloseable {
        @FunctionalInterface
        interface Undo {
            void run() throws IOException;
        }

        private final Undo undo;
        private boolean closed;

        Restriction(Undo undo) {
            this.undo = undo;
        }

        static Restriction apply(Mechanism mechanism, Path file) throws IOException {
            if (mechanism == Mechanism.POSIX) {
                // Deleting an entry needs write permission on its directory, so the directory is what is made read-only.
                Path parent = file.getParent();
                Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("r-xr-xr-x"));
                return new Restriction(() -> {
                    try {
                        Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("rwxr-xr-x"));
                    } catch (NoSuchFileException alreadyGone) {
                        // Nothing left to restore.
                    }
                });
            }
            Files.setAttribute(file, "dos:readonly", true);
            return new Restriction(() -> {
                try {
                    Files.setAttribute(file, "dos:readonly", false);
                } catch (NoSuchFileException alreadyGone) {
                    // Nothing left to restore.
                }
            });
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                undo.run();
            }
        }
    }

    /**
     * Establishes, with a disposable entry and before the cleanup under test is involved, that the mechanism really makes a deletion fail on this machine, for this user.
     * The decision is never inferred from what the cleanup does. If the restriction cannot be enforced (a superuser, or a filesystem that does not honour it) the test aborts
     * and says so.
     */
    private void assumeDeletionCanBeBlocked(Mechanism mechanism) throws IOException {
        Path dir = Files.createDirectories(temp.resolve("probe-" + mechanism));
        Path probe = Files.writeString(dir.resolve("probe"), "x");
        boolean blocked;
        try (Restriction restriction = Restriction.apply(mechanism, probe)) {
            try {
                Files.delete(probe);
                blocked = false;
            } catch (FileSystemException denied) {
                blocked = !(denied instanceof NoSuchFileException);
            }
        }
        Files.deleteIfExists(probe);
        Files.deleteIfExists(dir);
        Assumptions.assumeTrue(blocked, mechanism + " cannot make a deletion fail for this user on this filesystem (a superuser, or a filesystem that does not enforce it), "
                + "so there is no persistent failure to test here");
    }

    @Test
    void aPersistentPermissionFailureIsReportedAfterEveryAttemptAndNeverSuppressed(TestReporter reporter) throws IOException {
        Path root = populated("profile");
        Path locked = Files.writeString(root.resolve("Default/locked"), "x");
        Mechanism mechanism = Mechanism.of(locked);
        Assumptions.assumeTrue(mechanism != null, "this filesystem has neither POSIX nor DOS attributes, so a deletion cannot be made to fail");
        assumeDeletionCanBeBlocked(mechanism);
        reporter.publishEntry("persistent failure made with", mechanism.name());
        try (Restriction restriction = Restriction.apply(mechanism, locked)) {
            AtomicInteger sweeps = new AtomicInteger();
            List<IOException> failures = DevToolsSession.deleteTree(root, path -> {
                if (path.equals(root)) {
                    sweeps.incrementAndGet();
                }
            });
            assertFalse(failures.isEmpty(), "the failure is reported, not retried or filtered away");
            assertEquals(3, sweeps.get(), "the sweep was repeated to the bound, and the failure was still there: it is reported, not retried away");
            assertTrue(failures.stream().anyMatch(f -> f instanceof FileSystemException && !(f instanceof DirectoryNotEmptyException) && !(f instanceof NoSuchFileException)),
                    "the permission/IO failure itself is among the failures, not hidden behind a directory-not-empty: " + failures);
            assertTrue(Files.exists(locked), "the entry that could not be deleted is still there, which the failure honestly says");
            assertFalse(Files.exists(root.resolve("Local State")) || Files.exists(root.resolve("Crashpad")), "everything that could be deleted was");
            IllegalStateException report = DevToolsSession.cleanupFailure(root, failures);
            assertTrue(report.getMessage().contains(root.toString()) && report.getSuppressed().length == failures.size(), report.getMessage());
        }
    }

    @Test
    void theRestrictionIsLiftedWhenAnAssertionFailsAndTheAssertionStaysTheOnlyFailure() throws IOException {
        Path file = Files.writeString(Files.createDirectories(temp.resolve("lift")).resolve("entry"), "x");
        Mechanism mechanism = Mechanism.of(file);
        Assumptions.assumeTrue(mechanism != null, "no attribute view to restrict a deletion with");
        AssertionError failure = assertThrows(AssertionError.class, () -> {
            try (Restriction restriction = Restriction.apply(mechanism, file)) {
                throw new AssertionError("the primary failure");
            }
        });
        assertEquals("the primary failure", failure.getMessage());
        assertEquals(0, failure.getSuppressed().length, "lifting the restriction succeeded, so it adds nothing");
        Files.delete(file);
        assertFalse(Files.exists(file), "and the restriction really was lifted, so the entry can be deleted again");
    }

    @Test
    void anUndoThatFailsIsAddedAsSuppressedUnderTheOriginalFailureAndNeverReplacesIt() {
        AssertionError failure = assertThrows(AssertionError.class, () -> {
            try (Restriction restriction = new Restriction(() -> {
                throw new IOException("the undo failed");
            })) {
                throw new AssertionError("the primary failure");
            }
        });
        assertEquals("the primary failure", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("the undo failed", failure.getSuppressed()[0].getMessage());
        // With nothing else wrong, the same failure is not swallowed either.
        IOException alone = assertThrows(IOException.class, () -> {
            try (Restriction restriction = new Restriction(() -> {
                throw new IOException("the undo failed");
            })) {
                // nothing fails in the body
            }
        });
        assertEquals("the undo failed", alone.getMessage());
    }

    @Test
    void anEntryThatIsAlreadyGoneNeedsNoUndoAndClosingTwiceIsSafe() throws IOException {
        Path dir = Files.createDirectories(temp.resolve("gone"));
        Path file = Files.writeString(dir.resolve("entry"), "x");
        Mechanism mechanism = Mechanism.of(file);
        Assumptions.assumeTrue(mechanism != null, "no attribute view to restrict a deletion with");
        Restriction restriction = Restriction.apply(mechanism, file);
        // Someone else lifts it and removes the entry (and, for a directory permission, the directory) before the undo runs.
        if (mechanism == Mechanism.POSIX) {
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.delete(file);
            Files.delete(dir);
        } else {
            Files.setAttribute(file, "dos:readonly", false);
            Files.delete(file);
        }
        restriction.close();
        restriction.close();
        assertFalse(Files.exists(file));
    }

    @Test
    void aSymbolicLinkIsRemovedItselfAndNeverFollowed() throws IOException {
        Path outside = Files.createDirectories(temp.resolve("outside"));
        Path precious = Files.writeString(outside.resolve("precious.txt"), "keep");
        Path root = populated("profile");
        try {
            Files.createSymbolicLink(root.resolve("SingletonSocket"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("symbolic links cannot be created here: " + e);
        }
        assertEquals(List.of(), DevToolsSession.deleteTree(root));
        assertFalse(Files.exists(root));
        assertTrue(Files.exists(precious), "what the link pointed at is untouched");
    }

    @Test
    void theExceptionForAnUnexpectedFailureNamesTheProfileAndCarriesEveryFailure() {
        Path profile = temp.resolve("profile");
        IOException first = new DirectoryNotEmptyException(profile.resolve("a").toString());
        IOException second = new NoSuchFileException("ignored-type-but-still-carried");
        IllegalStateException failure = DevToolsSession.cleanupFailure(profile, List.of(first, second));
        assertTrue(failure.getMessage().contains(profile.toString()), failure.getMessage());
        assertTrue(failure.getMessage().contains("2 unexpected failure(s)") && failure.getMessage().contains("DirectoryNotEmptyException"), failure.getMessage());
        assertEquals(List.of(first, second), new ArrayList<>(List.of(failure.getSuppressed())));
    }
}
