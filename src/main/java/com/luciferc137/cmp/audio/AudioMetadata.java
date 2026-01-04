package com.luciferc137.cmp.audio;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified class representing audio file metadata.
 * Provides read/write operations for metadata using JAudioTagger.
 * This class serves as a single source of truth for audio metadata operations.
 */
public class AudioMetadata {

    static {
        // Disable JAudioTagger verbose logging
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    // Basic metadata
    private String title;
    private String artist;
    private String album;
    private String albumArtist;
    private String genre;
    private String year;
    private String trackNumber;
    private String discNumber;
    private String composer;
    private String lyrics;
    private String comment;
    
    // Technical metadata
    private long duration; // in milliseconds
    private int bitrate; // in kbps
    private int sampleRate; // in Hz
    private int channels;
    private String encodingType;
    
    // File info
    private String filePath;
    private AudioFormat format;
    
    // Cover art
    private byte[] coverArt;
    private String coverArtMimeType;

    public AudioMetadata() {
    }

    /**
     * Creates an AudioMetadata instance by reading from an audio file.
     *
     * @param file The audio file to read
     * @return AudioMetadata with populated fields
     * @throws IOException if the file cannot be read
     */
    public static AudioMetadata fromFile(File file) throws IOException {
        AudioMetadata metadata = new AudioMetadata();
        metadata.filePath = file.getAbsolutePath();
        metadata.format = AudioFormat.fromFile(file);
        
        // Default title = filename without extension
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        metadata.title = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            
            // Extract technical info from header
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                metadata.duration = header.getTrackLength() * 1000L;
                metadata.bitrate = (int) header.getBitRateAsNumber();
                metadata.sampleRate = header.getSampleRateAsNumber();
                metadata.channels = parseChannels(header.getChannels());
                metadata.encodingType = header.getEncodingType();
            }
            
            // Extract metadata from tags
            Tag tag = audioFile.getTag();
            if (tag != null) {
                metadata.title = getTagValueOrDefault(tag, FieldKey.TITLE, metadata.title);
                metadata.artist = getTagValue(tag, FieldKey.ARTIST);
                metadata.album = getTagValue(tag, FieldKey.ALBUM);
                metadata.albumArtist = getTagValue(tag, FieldKey.ALBUM_ARTIST);
                metadata.genre = getTagValue(tag, FieldKey.GENRE);
                metadata.year = getTagValue(tag, FieldKey.YEAR);
                metadata.trackNumber = getTagValue(tag, FieldKey.TRACK);
                metadata.discNumber = getTagValue(tag, FieldKey.DISC_NO);
                metadata.composer = getTagValue(tag, FieldKey.COMPOSER);
                metadata.lyrics = getTagValue(tag, FieldKey.LYRICS);
                metadata.comment = getTagValue(tag, FieldKey.COMMENT);
                
                // Extract cover art
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    metadata.coverArt = artwork.getBinaryData();
                    metadata.coverArtMimeType = artwork.getMimeType();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error reading metadata from: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
        
        return metadata;
    }

    /**
     * Saves the current metadata to the audio file.
     *
     * @throws IOException if the file cannot be written
     */
    public void saveToFile() throws IOException {
        if (filePath == null) {
            throw new IOException("No file path specified");
        }
        saveToFile(new File(filePath));
    }

    /**
     * Saves the current metadata to the specified audio file.
     *
     * @param file The file to save to
     * @throws IOException if the file cannot be written
     */
    public void saveToFile(File file) throws IOException {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            
            setTagValue(tag, FieldKey.TITLE, title);
            setTagValue(tag, FieldKey.ARTIST, artist);
            setTagValue(tag, FieldKey.ALBUM, album);
            setTagValue(tag, FieldKey.ALBUM_ARTIST, albumArtist);
            setTagValue(tag, FieldKey.GENRE, genre);
            setTagValue(tag, FieldKey.YEAR, year);
            setTagValue(tag, FieldKey.TRACK, trackNumber);
            setTagValue(tag, FieldKey.DISC_NO, discNumber);
            setTagValue(tag, FieldKey.COMPOSER, composer);
            setTagValue(tag, FieldKey.LYRICS, lyrics);
            setTagValue(tag, FieldKey.COMMENT, comment);
            
            // Save cover art if present
            if (coverArt != null && coverArt.length > 0) {
                try {
                    // Remove existing artwork
                    tag.deleteArtworkField();

                    // Create and add new artwork
                    Artwork artwork = ArtworkFactory.createArtworkFromFile(file);
                    artwork.setBinaryData(coverArt);
                    if (coverArtMimeType != null) {
                        artwork.setMimeType(coverArtMimeType);
                    }
                    tag.setField(artwork);
                } catch (Exception e) {
                    System.err.println("Could not save cover art: " + e.getMessage());
                }
            }

            audioFile.commit();
            
        } catch (Exception e) {
            throw new IOException("Failed to save metadata: " + e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    private static String getTagValue(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getTagValueOrDefault(Tag tag, FieldKey key, String defaultValue) {
        String value = getTagValue(tag, key);
        return value != null ? value : defaultValue;
    }

    private static void setTagValue(Tag tag, FieldKey key, String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                tag.setField(key, value.trim());
            } else {
                tag.deleteField(key);
            }
        } catch (Exception e) {
            // Field might not be supported for this format
            System.err.println("Could not set field " + key + ": " + e.getMessage());
        }
    }

    private static int parseChannels(String channelString) {
        if (channelString == null) return 2;
        try {
            // Handle cases like "Stereo", "Mono", "2", etc.
            if (channelString.toLowerCase().contains("stereo")) return 2;
            if (channelString.toLowerCase().contains("mono")) return 1;
            return Integer.parseInt(channelString.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    // ==================== Getters and Setters ====================

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

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(String trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(String discNumber) {
        this.discNumber = discNumber;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public String getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public void setFormat(AudioFormat format) {
        this.format = format;
    }

    public byte[] getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(byte[] coverArt) {
        this.coverArt = coverArt;
    }

    public String getCoverArtMimeType() {
        return coverArtMimeType;
    }

    public void setCoverArtMimeType(String coverArtMimeType) {
        this.coverArtMimeType = coverArtMimeType;
    }

    /**
     * Returns a formatted duration string (MM:SS or HH:MM:SS).
     */
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Returns a formatted bitrate string.
     */
    public String getFormattedBitrate() {
        return bitrate + " kbps";
    }

    /**
     * Returns a formatted sample rate string.
     */
    public String getFormattedSampleRate() {
        return String.format("%.1f kHz", sampleRate / 1000.0);
    }

    @Override
    public String toString() {
        return "AudioMetadata{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", duration=" + duration +
                ", format=" + format +
                '}';
    }
}

