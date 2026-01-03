package com.luciferc137.cmp.database;

import com.luciferc137.cmp.database.dao.MusicDao;
import com.luciferc137.cmp.database.dao.PlaylistDao;
import com.luciferc137.cmp.database.dao.TagDao;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.database.sync.LibrarySyncService;
import com.luciferc137.cmp.database.sync.SyncProgressListener;
import com.luciferc137.cmp.database.sync.SyncResult;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main service for managing the music library.
 * Provides a simplified interface for accessing various features.
 */
public class LibraryService {

    private static LibraryService instance;

    private final MusicDao musicDao;
    private final PlaylistDao playlistDao;
    private final TagDao tagDao;
    private final LibrarySyncService syncService;

    private LibraryService() {
        // Initialize the database
        DatabaseManager.getInstance();

        this.musicDao = new MusicDao();
        this.playlistDao = new PlaylistDao();
        this.tagDao = new TagDao();
        this.syncService = new LibrarySyncService();
    }

    /**
     * Returns the unique instance of LibraryService.
     *
     * @return The LibraryService instance
     */
    public static synchronized LibraryService getInstance() {
        if (instance == null) {
            instance = new LibraryService();
        }
        return instance;
    }

    // ==================== Synchronization ====================

    /**
     * Synchronizes a folder with the library (synchronous).
     *
     * @param folderPath The folder path
     * @return The synchronization result
     */
    public SyncResult syncFolder(String folderPath) {
        return syncService.syncFolder(folderPath);
    }

    /**
     * Synchronizes a folder with the library (synchronous with listener).
     *
     * @param folderPath The folder path
     * @param listener The progress listener
     * @return The synchronization result
     */
    public SyncResult syncFolder(String folderPath, SyncProgressListener listener) {
        return syncService.syncFolder(folderPath, listener);
    }

    /**
     * Synchronizes a folder with the library (asynchronous).
     *
     * @param folderPath The folder path
     * @return A CompletableFuture with the result
     */
    public CompletableFuture<SyncResult> syncFolderAsync(String folderPath) {
        return syncService.syncFolderAsync(folderPath);
    }

    /**
     * Synchronizes a folder with the library (asynchronous with listener).
     *
     * @param folderPath The folder path
     * @param listener The progress listener
     * @return A CompletableFuture with the result
     */
    public CompletableFuture<SyncResult> syncFolderAsync(String folderPath, SyncProgressListener listener) {
        return syncService.syncFolderAsync(folderPath, listener);
    }

    /**
     * Checks if a synchronization is in progress.
     *
     * @return true if a sync is in progress
     */
    public boolean isSyncing() {
        return syncService.isSyncing();
    }

    /**
     * Cancels the ongoing synchronization.
     */
    public void cancelSync() {
        syncService.cancelSync();
    }

    // ==================== Music ====================

    /**
     * Returns all music from the library.
     *
     * @return List of all music
     */
    public List<MusicEntity> getAllMusics() {
        try {
            return musicDao.findAll();
        } catch (SQLException e) {
            System.err.println("Error retrieving music: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches for music by its ID.
     *
     * @param id The music ID
     * @return The music if found
     */
    public Optional<MusicEntity> getMusicById(long id) {
        try {
            return musicDao.findById(id);
        } catch (SQLException e) {
            System.err.println("Error searching for music: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Searches for music by text (title, artist, album).
     *
     * @param query The text to search for
     * @return List of matching music
     */
    public List<MusicEntity> searchMusics(String query) {
        try {
            return musicDao.search(query);
        } catch (SQLException e) {
            System.err.println("Error during search: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns music by an artist.
     *
     * @param artist The artist name
     * @return List of music by the artist
     */
    public List<MusicEntity> getMusicsByArtist(String artist) {
        try {
            return musicDao.findByArtist(artist);
        } catch (SQLException e) {
            System.err.println("Error searching by artist: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns music from an album.
     *
     * @param album The album name
     * @return List of music from the album
     */
    public List<MusicEntity> getMusicsByAlbum(String album) {
        try {
            return musicDao.findByAlbum(album);
        } catch (SQLException e) {
            System.err.println("Error searching by album: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns the list of all artists.
     *
     * @return List of artists
     */
    public List<String> getAllArtists() {
        try {
            return musicDao.findAllArtists();
        } catch (SQLException e) {
            System.err.println("Error retrieving artists: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns the list of all albums.
     *
     * @return List of albums
     */
    public List<String> getAllAlbums() {
        try {
            return musicDao.findAllAlbums();
        } catch (SQLException e) {
            System.err.println("Error retrieving albums: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Counts the total number of music.
     *
     * @return The number of music
     */
    public int getMusicCount() {
        try {
            return musicDao.count();
        } catch (SQLException e) {
            System.err.println("Error counting: " + e.getMessage());
            return 0;
        }
    }

    // ==================== Playlists ====================

    /**
     * Creates a new playlist.
     *
     * @param name The playlist name
     * @return The created playlist
     */
    public Optional<PlaylistEntity> createPlaylist(String name) {
        try {
            PlaylistEntity playlist = new PlaylistEntity(name);
            playlistDao.insert(playlist);
            return Optional.of(playlist);
        } catch (SQLException e) {
            System.err.println("Error creating playlist: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns all playlists.
     *
     * @return List of playlists
     */
    public List<PlaylistEntity> getAllPlaylists() {
        try {
            return playlistDao.findAll();
        } catch (SQLException e) {
            System.err.println("Error retrieving playlists: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Deletes a playlist.
     *
     * @param playlistId The playlist ID
     */
    public void deletePlaylist(long playlistId) {
        try {
            playlistDao.delete(playlistId);
        } catch (SQLException e) {
            System.err.println("Error deleting playlist: " + e.getMessage());
        }
    }

    /**
     * Adds music to a playlist.
     *
     * @param playlistId The playlist ID
     * @param musicId The music ID
     */
    public void addMusicToPlaylist(long playlistId, long musicId) {
        try {
            playlistDao.addMusic(playlistId, musicId);
        } catch (SQLException e) {
            System.err.println("Error adding to playlist: " + e.getMessage());
        }
    }

    /**
     * Removes music from a playlist.
     *
     * @param playlistId The playlist ID
     * @param musicId The music ID
     */
    public void removeMusicFromPlaylist(long playlistId, long musicId) {
        try {
            playlistDao.removeMusic(playlistId, musicId);
        } catch (SQLException e) {
            System.err.println("Error removing from playlist: " + e.getMessage());
        }
    }

    /**
     * Returns music from a playlist.
     *
     * @param playlistId The playlist ID
     * @return List of music in the playlist
     */
    public List<MusicEntity> getPlaylistMusics(long playlistId) {
        try {
            return playlistDao.getMusicsByPlaylist(playlistId);
        } catch (SQLException e) {
            System.err.println("Error retrieving music: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== Tags ====================

    /**
     * Creates a new tag.
     *
     * @param name The tag name
     * @return The created tag
     */
    public Optional<TagEntity> createTag(String name) {
        return createTag(name, "#808080");
    }

    /**
     * Creates a new tag with a color.
     *
     * @param name The tag name
     * @param color The tag color (hex)
     * @return The created tag
     */
    public Optional<TagEntity> createTag(String name, String color) {
        try {
            TagEntity tag = new TagEntity(name, color);
            tagDao.insert(tag);
            return Optional.of(tag);
        } catch (SQLException e) {
            System.err.println("Error creating tag: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns all tags.
     *
     * @return List of tags
     */
    public List<TagEntity> getAllTags() {
        try {
            return tagDao.findAll();
        } catch (SQLException e) {
            System.err.println("Error retrieving tags: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Deletes a tag.
     *
     * @param tagId The tag ID
     */
    public void deleteTag(long tagId) {
        try {
            tagDao.delete(tagId);
        } catch (SQLException e) {
            System.err.println("Error deleting tag: " + e.getMessage());
        }
    }

    /**
     * Adds a tag to music.
     *
     * @param musicId The music ID
     * @param tagId The tag ID
     */
    public void addTagToMusic(long musicId, long tagId) {
        try {
            tagDao.addTagToMusic(musicId, tagId);
        } catch (SQLException e) {
            System.err.println("Error adding tag: " + e.getMessage());
        }
    }

    /**
     * Removes a tag from music.
     *
     * @param musicId The music ID
     * @param tagId The tag ID
     */
    public void removeTagFromMusic(long musicId, long tagId) {
        try {
            tagDao.removeTagFromMusic(musicId, tagId);
        } catch (SQLException e) {
            System.err.println("Error removing tag: " + e.getMessage());
        }
    }

    /**
     * Returns the tags of a music.
     *
     * @param musicId The music ID
     * @return List of tags for the music
     */
    public List<TagEntity> getMusicTags(long musicId) {
        try {
            return tagDao.getTagsByMusic(musicId);
        } catch (SQLException e) {
            System.err.println("Error retrieving tags: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns music tracks with a specific tag.
     *
     * @param tagId The tag ID
     * @return List of music with this tag
     */
    public List<MusicEntity> getMusicsByTag(long tagId) {
        try {
            return tagDao.getMusicsByTag(tagId);
        } catch (SQLException e) {
            System.err.println("Error searching by tag: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Updates the rating for a music track.
     *
     * @param musicId The music ID
     * @param rating The new rating (0-5)
     */
    public void updateMusicRating(long musicId, int rating) {
        try {
            musicDao.updateRating(musicId, rating);
        } catch (SQLException e) {
            System.err.println("Error updating rating: " + e.getMessage());
        }
    }

    /**
     * Gets all tag IDs for a music track.
     *
     * @param musicId The music ID
     * @return Set of tag IDs
     */
    public java.util.Set<Long> getMusicTagIds(long musicId) {
        try {
            return tagDao.getTagsByMusic(musicId).stream()
                    .map(TagEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (SQLException e) {
            System.err.println("Error getting music tags: " + e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Gets tag names for a music track.
     *
     * @param musicId The music ID
     * @return List of tag names
     */
    public List<String> getMusicTagNames(long musicId) {
        try {
            return tagDao.getTagsByMusic(musicId).stream()
                    .map(TagEntity::getName)
                    .collect(java.util.stream.Collectors.toList());
        } catch (SQLException e) {
            System.err.println("Error getting music tag names: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== Utilities ====================

    /**
     * Closes service resources.
     */
    public void shutdown() {
        syncService.shutdown();
        DatabaseManager.getInstance().close();
    }
}

