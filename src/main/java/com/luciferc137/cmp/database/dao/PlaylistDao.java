package com.luciferc137.cmp.database.dao;

import com.luciferc137.cmp.database.DatabaseManager;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.PlaylistEntity;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) for managing playlists in the database.
 */
public class PlaylistDao {

    private final DatabaseManager dbManager;

    public PlaylistDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Inserts a new playlist into the database.
     *
     * @param playlist The playlist entity to insert
     * @return The generated ID
     */
    public long insert(PlaylistEntity playlist) throws SQLException {
        String sql = "INSERT INTO playlist (name) VALUES (?)";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, playlist.getName());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    playlist.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Updates an existing playlist.
     *
     * @param playlist The playlist entity to update
     */
    public void update(PlaylistEntity playlist) throws SQLException {
        String sql = "UPDATE playlist SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playlist.getName());
            stmt.setLong(2, playlist.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a playlist by its ID.
     *
     * @param id The ID of the playlist to delete
     */
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM playlist WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Finds a playlist by its ID.
     *
     * @param id The playlist ID
     * @return The playlist entity if found
     */
    public Optional<PlaylistEntity> findById(long id) throws SQLException {
        String sql = "SELECT * FROM playlist WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a playlist by its name.
     *
     * @param name The playlist name
     * @return The playlist entity if found
     */
    public Optional<PlaylistEntity> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM playlist WHERE name = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all playlists.
     *
     * @return List of all playlists
     */
    public List<PlaylistEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM playlist ORDER BY name";
        List<PlaylistEntity> result = new ArrayList<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        }
        return result;
    }

    /**
     * Adds a music to a playlist.
     *
     * @param playlistId The playlist ID
     * @param musicId The music ID
     */
    public void addMusic(long playlistId, long musicId) throws SQLException {
        // Get the current maximum position
        int maxPosition = getMaxPosition(playlistId);

        String sql = "INSERT OR IGNORE INTO playlist_music (playlist_id, music_id, position) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, playlistId);
            stmt.setLong(2, musicId);
            stmt.setInt(3, maxPosition + 1);
            stmt.executeUpdate();
        }
    }

    /**
     * Removes a music from a playlist.
     *
     * @param playlistId The playlist ID
     * @param musicId The music ID
     */
    public void removeMusic(long playlistId, long musicId) throws SQLException {
        String sql = "DELETE FROM playlist_music WHERE playlist_id = ? AND music_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, playlistId);
            stmt.setLong(2, musicId);
            stmt.executeUpdate();
        }
    }

    /**
     * Returns all music from a playlist ordered by position.
     *
     * @param playlistId The playlist ID
     * @return List of music in the playlist
     */
    public List<MusicEntity> getMusicsByPlaylist(long playlistId) throws SQLException {
        String sql = """
            SELECT m.* FROM music m
            INNER JOIN playlist_music pm ON m.id = pm.music_id
            WHERE pm.playlist_id = ?
            ORDER BY pm.position
        """;
        List<MusicEntity> result = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, playlistId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapMusicResultSet(rs));
                }
            }
        }
        return result;
    }

    /**
     * Changes the position of a music in a playlist.
     *
     * @param playlistId The playlist ID
     * @param musicId The music ID
     * @param newPosition The new position
     */
    public void updateMusicPosition(long playlistId, long musicId, int newPosition) throws SQLException {
        String sql = "UPDATE playlist_music SET position = ? WHERE playlist_id = ? AND music_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, newPosition);
            stmt.setLong(2, playlistId);
            stmt.setLong(3, musicId);
            stmt.executeUpdate();
        }
    }

    /**
     * Counts the number of music in a playlist.
     *
     * @param playlistId The playlist ID
     * @return The number of music
     */
    public int countMusics(long playlistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM playlist_music WHERE playlist_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, playlistId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Gets the maximum position in a playlist.
     */
    private int getMaxPosition(long playlistId) throws SQLException {
        String sql = "SELECT MAX(position) FROM playlist_music WHERE playlist_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, playlistId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Converts a ResultSet to a PlaylistEntity.
     */
    private PlaylistEntity mapResultSet(ResultSet rs) throws SQLException {
        PlaylistEntity playlist = new PlaylistEntity();
        playlist.setId(rs.getLong("id"));
        playlist.setName(rs.getString("name"));

        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            playlist.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }

        String updatedAt = rs.getString("updated_at");
        if (updatedAt != null) {
            playlist.setUpdatedAt(LocalDateTime.parse(updatedAt.replace(" ", "T")));
        }

        return playlist;
    }

    /**
     * Converts a ResultSet to a MusicEntity.
     */
    private MusicEntity mapMusicResultSet(ResultSet rs) throws SQLException {
        MusicEntity music = new MusicEntity();
        music.setId(rs.getLong("id"));
        music.setPath(rs.getString("path"));
        music.setTitle(rs.getString("title"));
        music.setArtist(rs.getString("artist"));
        music.setAlbum(rs.getString("album"));
        music.setDuration(rs.getLong("duration"));
        music.setHash(rs.getString("hash"));
        return music;
    }
}

