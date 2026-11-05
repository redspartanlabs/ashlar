package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
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

    @Test
    void aPersistentPermissionFailureIsReportedAfterEveryAttemptAndNeverSuppressed() throws IOException {
        Path root = populated("profile");
        Path locked = Files.writeString(root.resolve("Default/locked"), "x");
        Path parent = locked.getParent();
        // A failure that does not go away: a read-only file where DOS attributes exist, otherwise a directory the owner cannot write to.
        boolean dos = Files.getFileAttributeView(locked, DosFileAttributeView.class) != null;
        boolean posix = Files.getFileAttributeView(locked, PosixFileAttributeView.class) != null;
        Assumptions.assumeTrue(dos || posix, "no file attribute view to make a deletion fail with");
        if (dos) {
            Files.setAttribute(locked, "dos:readonly", true);
        } else {
            Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("r-xr-xr-x"));
            // Whether the permission is enforced is decided here, from the environment, and not from what the cleanup returns: otherwise a cleanup that hid the failure
            // would turn this into a skipped test instead of a failing one. A superuser can write to the directory anyway, and then nothing can fail.
            if (Files.isWritable(parent)) {
                Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("rwxr-xr-x"));
                Assumptions.abort("the permission is not enforced for this user (a superuser), so nothing can fail here");
            }
        }
        try {
            AtomicInteger sweeps = new AtomicInteger();
            List<IOException> failures = DevToolsSession.deleteTree(root, path -> {
                if (path.equals(root)) {
                    sweeps.incrementAndGet();
                }
            });
            assertFalse(failures.isEmpty(), "the failure is reported, not retried or filtered away");
            assertEquals(3, sweeps.get(), "the sweep was repeated to the bound, and the failure was still there: it is reported, not retried away");
            assertTrue(failures.stream().anyMatch(f -> f instanceof FileSystemException && !(f instanceof DirectoryNotEmptyException) && !(f instanceof java.nio.file.NoSuchFileException)),
                    "the permission/IO failure itself is among the failures, not hidden behind a directory-not-empty: " + failures);
            assertTrue(Files.exists(locked), "the entry that could not be deleted is still there, which the failure honestly says");
            assertFalse(Files.exists(root.resolve("Local State")) || Files.exists(root.resolve("Crashpad")), "everything that could be deleted was");
            IllegalStateException report = DevToolsSession.cleanupFailure(root, failures);
            assertTrue(report.getMessage().contains(root.toString()) && report.getSuppressed().length == failures.size(), report.getMessage());
        } finally {
            // Undo it, so the test framework can remove the temporary directory.
            if (dos) {
                Files.setAttribute(locked, "dos:readonly", false);
            } else {
                Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("rwxr-xr-x"));
            }
        }
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
