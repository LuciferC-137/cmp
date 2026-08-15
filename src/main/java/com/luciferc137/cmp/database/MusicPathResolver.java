package com.luciferc137.cmp.database;

import com.luciferc137.cmp.settings.SettingsManager;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Converts between absolute file paths and paths stored relative to the
 * music library root folder. Storing relative paths keeps the database
 * valid even if the user moves, renames, or copies the library folder
 * to another machine.
 *
 * The library root is read from SettingsManager on every call rather than
 * cached, so a change of music folder during the session is always
 * reflected immediately.
 */
public final class MusicPathResolver {

    public static String toRelative(Path absolutePath, Path root) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalized = absolutePath.toAbsolutePath().normalize();

        if (!normalized.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(
                    "Path " + normalized + " is outside the given root " + normalizedRoot);
        }
        return normalizedRoot.relativize(normalized).toString().replace('\\', '/');
    }

    public static Path toAbsolute(String relativePath, Path root) {
        return root.toAbsolutePath().normalize().resolve(relativePath).normalize();
    }

    public static String toRelative(Path absolutePath) {
        return toRelative(absolutePath, requestLibraryRoot());
    }

    public static Path toAbsolute(String relativePath) {
        return toAbsolute(relativePath, requestLibraryRoot());
    }

    private static Path requestLibraryRoot() {
        String configured = SettingsManager.getInstance().getMusicFolderPath();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("No music folder configured in settings.");
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }
}