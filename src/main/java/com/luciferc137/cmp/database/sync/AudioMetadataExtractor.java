package com.luciferc137.cmp.database.sync;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Metadata extractor for audio files.
 * Uses JAudioTagger library for broad format support (MP3, M4A, FLAC, OGG, WAV, etc.).
 */
public class AudioMetadataExtractor {

    private static final int HASH_BUFFER_SIZE = 8192;
    private static final int HASH_MAX_BYTES = 1024 * 1024; // 1 MB for hash (beginning of file)

    static {
        // Disable JAudioTagger verbose logging
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    /**
     * Metadata extracted from an audio file.
     */
    public static class AudioMetadata {
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
            return "AudioMetadata{" +
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
     * @return The extracted metadata
     */
    public AudioMetadata extract(File file) throws IOException {
        AudioMetadata metadata = new AudioMetadata();

        // Extract file hash
        metadata.setHash(computeFileHash(file));

        // Default title = filename without extension
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String defaultTitle = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        metadata.setTitle(defaultTitle);

        try {
            AudioFile audioFile = AudioFileIO.read(file);

            // Extract duration from audio header
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                // Duration is in seconds, convert to milliseconds
                metadata.setDuration(header.getTrackLength() * 1000L);
            }

            // Extract metadata from tags
            Tag tag = audioFile.getTag();
            if (tag != null) {
                // Extract title
                String title = tag.getFirst(FieldKey.TITLE);
                if (title != null && !title.trim().isEmpty()) {
                    metadata.setTitle(title.trim());
                }

                // Extract artist
                String artist = tag.getFirst(FieldKey.ARTIST);
                if (artist != null && !artist.trim().isEmpty()) {
                    metadata.setArtist(artist.trim());
                }

                // Extract album
                String album = tag.getFirst(FieldKey.ALBUM);
                if (album != null && !album.trim().isEmpty()) {
                    metadata.setAlbum(album.trim());
                }
            }

        } catch (Exception e) {
            // Format not supported or error reading file, keep default values
            System.err.println("Error reading metadata for: " + file.getAbsolutePath() + " - " + e.getMessage());
        }

        return metadata;
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
        if (!file.isFile()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") ||
               name.endsWith(".wav") ||
               name.endsWith(".flac") ||
               name.endsWith(".ogg") ||
               name.endsWith(".m4a") ||
               name.endsWith(".aac") ||
               name.endsWith(".wma") ||
               name.endsWith(".aiff") ||
               name.endsWith(".aif");
    }
}
