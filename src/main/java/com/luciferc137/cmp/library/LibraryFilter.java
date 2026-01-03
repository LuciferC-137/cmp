package com.luciferc137.cmp.library;

/**
 * Represents a filter to apply to the music library.
 */
public class LibraryFilter {

    private final FilterType type;
    private final String value;
    private final Long entityId;

    private LibraryFilter(FilterType type, String value, Long entityId) {
        this.type = type;
        this.value = value;
        this.entityId = entityId;
    }

    /**
     * Creates a filter that shows all music.
     */
    public static LibraryFilter all() {
        return new LibraryFilter(FilterType.ALL, null, null);
    }

    /**
     * Creates a filter by artist name.
     *
     * @param artist The artist name
     */
    public static LibraryFilter byArtist(String artist) {
        return new LibraryFilter(FilterType.ARTIST, artist, null);
    }

    /**
     * Creates a filter by album name.
     *
     * @param album The album name
     */
    public static LibraryFilter byAlbum(String album) {
        return new LibraryFilter(FilterType.ALBUM, album, null);
    }

    /**
     * Creates a filter by tag ID.
     *
     * @param tagId The tag ID
     * @param tagName The tag name (for display purposes)
     */
    public static LibraryFilter byTag(Long tagId, String tagName) {
        return new LibraryFilter(FilterType.TAG, tagName, tagId);
    }

    /**
     * Creates a filter by playlist ID.
     *
     * @param playlistId The playlist ID
     * @param playlistName The playlist name (for display purposes)
     */
    public static LibraryFilter byPlaylist(Long playlistId, String playlistName) {
        return new LibraryFilter(FilterType.PLAYLIST, playlistName, playlistId);
    }

    /**
     * Creates a search filter.
     *
     * @param query The search query
     */
    public static LibraryFilter search(String query) {
        return new LibraryFilter(FilterType.SEARCH, query, null);
    }

    public FilterType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Long getEntityId() {
        return entityId;
    }

    /**
     * Returns a human-readable description of the filter.
     */
    public String getDescription() {
        return switch (type) {
            case ALL -> "All Music";
            case ARTIST -> "Artist: " + value;
            case ALBUM -> "Album: " + value;
            case TAG -> "Tag: " + value;
            case PLAYLIST -> "Playlist: " + value;
            case SEARCH -> "Search: " + value;
        };
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryFilter that = (LibraryFilter) o;
        if (type != that.type) return false;
        if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        return result;
    }
}

