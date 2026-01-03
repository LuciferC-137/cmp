package com.luciferc137.cmp.library;

/**
 * Enum representing sort options for the music library.
 */
public enum SortOrder {
    /**
     * Sort by title (A-Z).
     */
    TITLE_ASC("Title (A-Z)"),

    /**
     * Sort by title (Z-A).
     */
    TITLE_DESC("Title (Z-A)"),

    /**
     * Sort by artist then album then title.
     */
    ARTIST_ALBUM("Artist / Album"),

    /**
     * Sort by album then title.
     */
    ALBUM_TITLE("Album / Title"),

    /**
     * Sort by duration (shortest first).
     */
    DURATION_ASC("Duration (Short to Long)"),

    /**
     * Sort by duration (longest first).
     */
    DURATION_DESC("Duration (Long to Short)"),

    /**
     * Sort by date added (newest first).
     */
    DATE_ADDED_DESC("Recently Added"),

    /**
     * Sort by date added (oldest first).
     */
    DATE_ADDED_ASC("Oldest First");

    private final String displayName;

    SortOrder(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

