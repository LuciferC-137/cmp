package com.luciferc137.cmp.database.sync;

import com.luciferc137.cmp.audio.AudioFormat;
import com.luciferc137.cmp.audio.AudioMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Metadata extractor for audio files during library synchronization.
 * Uses the centralized AudioMetadata class for reading metadata.
 * Adds file hash computation for tracking file changes.
 */
public class AudioMetadataExtractor {

    private static final int HASH_BUFFER_SIZE = 8192;
    private static final int HASH_MAX_BYTES = 1024 * 1024; // 1 MB for hash (beginning of file)

    /**
     * Metadata result from extraction, including file hash.
     */
    public static class ExtractedMetadata {
        private String title;
        private String artist;
        private String album;
        private long duration; // in milliseconds
        private String hash;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        @Override
        public String toString() {
            return "ExtractedMetadata{" +
                    "title='" + title + '\'' +
                    ", artist='" + artist + '\'' +
                    ", album='" + album + '\'' +
                    ", duration=" + duration +
                    ", hash='" + hash + '\'' +
                    '}';
        }
    }

    /**
     * Extracts metadata from an audio file.
     *
     * @param file The audio file
     * @return The extracted metadata with hash
     */
    public ExtractedMetadata extract(File file) throws IOException {
        ExtractedMetadata result = new ExtractedMetadata();

        // Compute file hash for change detection
        result.setHash(computeFileHash(file));

        // Use centralized AudioMetadata class for reading
        AudioMetadata metadata = AudioMetadata.fromFile(file);

        result.setTitle(metadata.getTitle());
        result.setArtist(metadata.getArtist());
        result.setAlbum(metadata.getAlbum());
        result.setDuration(metadata.getDuration());

        return result;
    }

    /**
     * Computes an MD5 hash of the beginning of the file for identification.
     * Only hashes the first 1 MB for performance.
     *
     * @param file The file
     * @return The hash in hexadecimal
     */
    private String computeFileHash(File file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[HASH_BUFFER_SIZE];
                int bytesRead;
                int totalBytesRead = 0;

                while ((bytesRead = is.read(buffer)) != -1 && totalBytesRead < HASH_MAX_BYTES) {
                    md.update(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            // Convert to hexadecimal
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Checks if a file is a supported audio file.
     *
     * @param file The file to check
     * @return true if it's a supported audio file
     */
    public boolean isAudioFile(File file) {
        return AudioFormat.isSupported(file);
    }
}
