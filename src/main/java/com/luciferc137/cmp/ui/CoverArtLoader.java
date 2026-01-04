package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Utility class for loading and managing cover art images.
 * Provides consistent cover art loading with fallback to default icon.
 */
public class CoverArtLoader {

    public static final int DEFAULT_SIZE = 100;
    public static final int SMALL_SIZE = 50;
    private static final String DEFAULT_COVER_PATH = "/icons/track.png";

    // Cached default image for performance
    private static Image defaultCoverSmall;
    private static Image defaultCoverDefault;

    /**
     * Loads cover art from AudioMetadata at the default size (100x100).
     * Falls back to default icon if no cover art is available.
     *
     * @param metadata The audio metadata (can be null)
     * @return The cover art image or default icon
     */
    public static Image loadCoverArt(AudioMetadata metadata) {
        return loadCoverArt(metadata, DEFAULT_SIZE);
    }

    /**
     * Loads cover art from AudioMetadata at the specified size.
     * Falls back to default icon if no cover art is available.
     *
     * @param metadata The audio metadata (can be null)
     * @param size The desired size (width and height)
     * @return The cover art image or default icon
     */
    public static Image loadCoverArt(AudioMetadata metadata, int size) {
        if (metadata != null && metadata.getCoverArt() != null && metadata.getCoverArt().length > 0) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(metadata.getCoverArt());
                // Load at requested size, preserve ratio, disable smoothing for sharpness
                Image coverImage = new Image(bais, size, size, true, false);
                if (!coverImage.isError()) {
                    return coverImage;
                }
            } catch (Exception e) {
                System.err.println("Error loading cover art: " + e.getMessage());
            }
        }
        return getDefaultCover(size);
    }

    /**
     * Loads cover art from a file path at the default size.
     * Falls back to default icon if the file doesn't exist or has no cover art.
     *
     * @param filePath Path to the audio file
     * @return The cover art image or default icon
     */
    public static Image loadCoverArt(String filePath) {
        return loadCoverArt(filePath, DEFAULT_SIZE);
    }

    /**
     * Loads cover art from a file path at the specified size.
     * Falls back to default icon if the file doesn't exist or has no cover art.
     *
     * @param filePath Path to the audio file
     * @param size The desired size (width and height)
     * @return The cover art image or default icon
     */
    public static Image loadCoverArt(String filePath, int size) {
        if (filePath == null || filePath.isEmpty()) {
            return getDefaultCover(size);
        }

        try {
            File file = new File(filePath);
            if (file.exists()) {
                AudioMetadata metadata = AudioMetadata.fromFile(file);
                return loadCoverArt(metadata, size);
            }
        } catch (IOException e) {
            System.err.println("Error loading cover art from file: " + e.getMessage());
        }

        return getDefaultCover(size);
    }

    /**
     * Loads cover art from raw byte data at the specified size.
     * Falls back to default icon if the data is invalid.
     *
     * @param coverData The raw image data
     * @param size The desired size (width and height)
     * @return The cover art image or default icon
     */
    public static Image loadCoverArt(byte[] coverData, int size) {
        if (coverData != null && coverData.length > 0) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(coverData);
                // Load at requested size, preserve ratio, disable smoothing for sharpness
                Image coverImage = new Image(bais, size, size, true, false);
                if (!coverImage.isError()) {
                    return coverImage;
                }
            } catch (Exception e) {
                System.err.println("Error loading cover art from bytes: " + e.getMessage());
            }
        }
        return getDefaultCover(size);
    }

    /**
     * Gets the default cover art icon at the default size.
     *
     * @return The default cover art image
     */
    public static Image getDefaultCover() {
        return getDefaultCover(DEFAULT_SIZE);
    }

    /**
     * Gets the default cover art icon at the specified size.
     * Uses cached images for common sizes.
     *
     * @param size The desired size (width and height)
     * @return The default cover art image
     */
    public static Image getDefaultCover(int size) {
        // Return cached versions for common sizes
        if (size == SMALL_SIZE) {
            if (defaultCoverSmall == null) {
                defaultCoverSmall = loadDefaultCoverFromResource(SMALL_SIZE);
            }
            return defaultCoverSmall;
        }

        if (size == DEFAULT_SIZE) {
            if (defaultCoverDefault == null) {
                defaultCoverDefault = loadDefaultCoverFromResource(DEFAULT_SIZE);
            }
            return defaultCoverDefault;
        }

        // For other sizes, load fresh
        return loadDefaultCoverFromResource(size);
    }

    /**
     * Loads the default cover art from resources.
     */
    private static Image loadDefaultCoverFromResource(int size) {
        try {
            InputStream inputStream = CoverArtLoader.class.getResourceAsStream(DEFAULT_COVER_PATH);
            if (inputStream != null) {
                return new Image(inputStream, size, size, true, false);
            }
        } catch (Exception e) {
            System.err.println("Could not load default cover: " + e.getMessage());
        }
        // Return an empty image as last resort
        return null;
    }

    /**
     * Shows a file chooser dialog to select a new cover art image.
     *
     * @param owner The owner window for the dialog
     * @return The selected image file, or null if cancelled
     */
    public static File showCoverArtChooser(Window owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Cover Art Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("PNG Images", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Images", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        return fileChooser.showOpenDialog(owner);
    }

    /**
     * Loads an image file as bytes for embedding in audio metadata.
     *
     * @param imageFile The image file to load
     * @return The image data as bytes, or null if failed
     */
    public static byte[] loadImageAsBytes(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            return null;
        }

        try {
            return Files.readAllBytes(imageFile.toPath());
        } catch (IOException e) {
            System.err.println("Error loading image file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the MIME type for an image file based on its extension.
     *
     * @param imageFile The image file
     * @return The MIME type string
     */
    public static String getMimeType(File imageFile) {
        if (imageFile == null) {
            return "image/png";
        }

        String name = imageFile.getName().toLowerCase();
        if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "image/png";
    }
}

