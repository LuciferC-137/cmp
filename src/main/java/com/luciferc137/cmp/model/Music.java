package com.luciferc137.cmp.model;

import com.luciferc137.cmp.database.model.MusicEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Music model for the UI layer.
 * Represents a playable music track with its metadata.
 */
public class Music {

    private Long id;
    public String title;
    public String artist;
    public String album;
    public String filePath;
    public long duration; // in milliseconds
    private int rating; // 0-5
    private List<String> tags; // Tag names for display

    public Music(String title, String artist, String album, String filePath) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
        this.tags = new ArrayList<>();
    }

    public Music(Long id, String title, String artist, String album, String filePath, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
        this.duration = duration;
        this.tags = new ArrayList<>();
    }

    /**
     * Creates a Music instance from a MusicEntity.
     *
     * @param entity The database entity
     * @return A new Music instance
     */
    public static Music fromEntity(MusicEntity entity) {
        Music music = new Music(
                entity.getId(),
                entity.getTitle(),
                entity.getArtist(),
                entity.getAlbum(),
                entity.getPath(),
                entity.getDuration()
        );
        music.setRating(entity.getRating());
        return music;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = Math.max(0, Math.min(5, rating));
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public void addTag(String tag) {
        if (tag != null && !tag.isEmpty() && !tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * Returns tags as a comma-separated string for display.
     */
    public String getTagsAsString() {
        return String.join(", ", tags);
    }

    /**
     * Returns rating as star characters for display.
     */
    public String getRatingAsStars() {
        if (rating == 0) return "";
        return "★".repeat(rating) + "☆".repeat(5 - rating);
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

    @Override
    public String toString() {
        return title + " - " + artist + " (" + album + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Music music = (Music) o;
        if (id != null && music.id != null) {
            return id.equals(music.id);
        }
        return filePath != null && filePath.equals(music.filePath);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (filePath != null ? filePath.hashCode() : 0);
    }
}
