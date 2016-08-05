package org.esa.snap.s2tbx.cep.util;

import org.esa.snap.s2tbx.cep.Constants;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for various operations.
 *
 * @author Cosmin Cara
 */
public class Utilities {

    private static Boolean supportsPosix;


    /**
     * Makes sure that the given path exists on the file system.
     *
     * @param folder        The path to be verified / created.
     *
     * @throws IOException
     */
    public static Path ensureExists(Path folder) throws IOException {
        if (folder != null && !Files.exists(folder)) {
            if (isPosixFileSystem()) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString(Constants.DEFAULT_PERMISSIONS);
                FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(perms);
                folder = Files.createDirectory(folder, attrs);
                ensurePermissions(folder);
            } else {
                folder = Files.createDirectory(folder);
            }
        }
        return folder;
    }

    /**
     * On Unix file systems, ensures that the given file (or folder) has proper permissions.
     * @param file          The file or folder to set permissions to
     *
     * @throws IOException
     */
    public static Path ensurePermissions(Path file) throws IOException {
        if (file != null && Files.exists(file)) {
            if (isPosixFileSystem()) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString(Constants.DEFAULT_PERMISSIONS);
                file = Files.setPosixFilePermissions(file, perms);
            }
        }
        return file;
    }

    /**
     * Finds first file of the given extension in the given folder (not recursive).
     *
     * @param folder        The folder in which to search
     * @param extension     The extension of the file
     *
     * @throws IOException
     */
    public static Optional<Path> findFirst(Path folder, String extension) throws IOException {
        List<Path> files = listFiles(folder, 1);
        return files.stream().filter(p -> p.toString().endsWith(extension)).findFirst();
    }

    /**
     * Traverses the given path up to the given depth and returns the list of files and folders.
     * @param basePath      The parent path
     * @param depth         The depth of the traversal
     *
     * @throws IOException
     */
    public static List<Path> listFiles(Path basePath, int depth) throws IOException {
        if (basePath == null)
            return null;
        depth = depth <= 0 ? 255 : depth;
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(basePath,
                EnumSet.noneOf(FileVisitOption.class),
                depth,
                new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        files.add(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        files.add(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
        return files;
    }

    private static boolean isPosixFileSystem() {
        if (supportsPosix == null) {
            supportsPosix = Boolean.FALSE;
            FileSystem fileSystem = FileSystems.getDefault();
            Iterable<FileStore> fileStores = fileSystem.getFileStores();
            for (FileStore fs : fileStores) {
                supportsPosix = fs.supportsFileAttributeView(PosixFileAttributeView.class);
                if (supportsPosix) {
                    break;
                }
            }
        }
        return supportsPosix;
    }
}