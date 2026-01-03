package com.luciferc137.cmp.library;

/**
 * Enum representing different filter types for the music library.
 */
public enum FilterType {
    /**
     * No filter - show all music.
     */
    ALL,

    /**
     * Filter by artist name.
     */
    ARTIST,

    /**
     * Filter by album name.
     */
    ALBUM,

    /**
     * Filter by tag.
     */
    TAG,

    /**
     * Filter by playlist.
     */
    PLAYLIST,

    /**
     * Filter by search query (searches in title, artist, album).
     */
    SEARCH
}

