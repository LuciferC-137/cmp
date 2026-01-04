package com.luciferc137.cmp.audio;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Enum representing supported audio formats with their characteristics.
 */
public enum AudioFormat {
    MP3("mp3", "MPEG Audio Layer III", true, true),
    FLAC("flac", "Free Lossless Audio Codec", true, true),
    OGG("ogg", "Ogg Vorbis", true, true),
    WAV("wav", "Waveform Audio", true, false),
    M4A("m4a", "MPEG-4 Audio", true, true),
    AAC("aac", "Advanced Audio Coding", true, true),
    WMA("wma", "Windows Media Audio", true, true),
    AIFF("aiff", "Audio Interchange File Format", true, false),
    AIF("aif", "Audio Interchange File Format", true, false),
    OPUS("opus", "Opus Audio", true, true),
    UNKNOWN("", "Unknown Format", false, false);

    private final String extension;
    private final String description;
    private final boolean supportsMetadata;
    private final boolean supportsEmbeddedArt;

    AudioFormat(String extension, String description, boolean supportsMetadata, boolean supportsEmbeddedArt) {
        this.extension = extension;
        this.description = description;
        this.supportsMetadata = supportsMetadata;
        this.supportsEmbeddedArt = supportsEmbeddedArt;
    }

    public String getExtension() {
        return extension;
    }

    public String getDescription() {
        return description;
    }

    public boolean supportsMetadata() {
        return supportsMetadata;
    }

    public boolean supportsEmbeddedArt() {
        return supportsEmbeddedArt;
    }

    /**
     * Detects the audio format from a file path.
     */
    public static AudioFormat fromFile(File file) {
        return fromFileName(file.getName());
    }

    /**
     * Detects the audio format from a file name.
     */
    public static AudioFormat fromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return UNKNOWN;
        }
        
        String lowerName = fileName.toLowerCase();
        int dotIndex = lowerName.lastIndexOf('.');
        if (dotIndex < 0) {
            return UNKNOWN;
        }
        
        String ext = lowerName.substring(dotIndex + 1);
        
        for (AudioFormat format : values()) {
            if (format.extension.equals(ext)) {
                return format;
            }
        }
        
        return UNKNOWN;
    }

    /**
     * Returns all supported file extensions.
     */
    public static List<String> getSupportedExtensions() {
        return Arrays.stream(values())
                .filter(f -> f != UNKNOWN)
                .map(f -> "." + f.extension)
                .toList();
    }

    /**
     * Checks if a file is a supported audio file.
     */
    public static boolean isSupported(File file) {
        return fromFile(file) != UNKNOWN;
    }

    /**
     * Checks if a file name represents a supported audio file.
     */
    public static boolean isSupported(String fileName) {
        return fromFileName(fileName) != UNKNOWN;
    }
}

