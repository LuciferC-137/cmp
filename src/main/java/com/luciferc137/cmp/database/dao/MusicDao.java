package com.luciferc137.cmp.database.dao;

import com.luciferc137.cmp.database.DatabaseManager;
import com.luciferc137.cmp.database.model.MusicEntity;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) for music management in database.
 */
public class MusicDao {

    private final DatabaseManager dbManager;

    public MusicDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Insert new music into the database.
     *
     * @param music Music entity to insert
     * @return generated ID of the inserted music
     */
    public long insert(MusicEntity music) throws SQLException {
        String sql = """
            INSERT INTO music (path, title, artist, album, duration, hash)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, music.getPath());
            stmt.setString(2, music.getTitle());
            stmt.setString(3, music.getArtist());
            stmt.setString(4, music.getAlbum());
            stmt.setLong(5, music.getDuration());
            stmt.setString(6, music.getHash());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    music.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Update data on existing music.
     *
     * @param music Music entity with updated data
     */
    public void update(MusicEntity music) throws SQLException {
        String sql = """
            UPDATE music SET
                path = ?,
                title = ?,
                artist = ?,
                album = ?,
                duration = ?,
                hash = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, music.getPath());
            stmt.setString(2, music.getTitle());
            stmt.setString(3, music.getArtist());
            stmt.setString(4, music.getAlbum());
            stmt.setLong(5, music.getDuration());
            stmt.setString(6, music.getHash());
            stmt.setLong(7, music.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a music by its ID.
     *
     * @param id The ID of the music to delete
     */
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM music WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Delete a music by its file path.
     *
     * @param path Path of the music file
     */
    public void deleteByPath(String path) throws SQLException {
        String sql = "DELETE FROM music WHERE path = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, path);
            stmt.executeUpdate();
        }
    }

    /**
     * Search a music by its ID.
     *
     * @param id ID of music
     * @return Music entity if found
     */
    public Optional<MusicEntity> findById(long id) throws SQLException {
        String sql = "SELECT * FROM music WHERE id = ?";

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
     * Search music by its file path.
     *
     * @param path File path of the music
     * @return Music entity if found
     */
    public Optional<MusicEntity> findByPath(String path) throws SQLException {
        String sql = "SELECT * FROM music WHERE path = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, path);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Search music by its file hash.
     *
     * @param hash File hash of the music
     * @return Music entity if found
     */
    public Optional<MusicEntity> findByHash(String hash) throws SQLException {
        String sql = "SELECT * FROM music WHERE hash = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, hash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all musics in the database.
     *
     * @return List of all musics
     */
    public List<MusicEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM music ORDER BY artist, album, title";
        List<MusicEntity> result = new ArrayList<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        }
        return result;
    }

    /**
     * Searches for music by artist.
     *
     * @param artist The artist name
     * @return List of music by the artist
     */
    public List<MusicEntity> findByArtist(String artist) throws SQLException {
        String sql = "SELECT * FROM music WHERE artist LIKE ? ORDER BY album, title";
        List<MusicEntity> result = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + artist + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSet(rs));
                }
            }
        }
        return result;
    }

    /**
     * Search for music by album.
     *
     * @param album The name of the album
     * @return List of musics in the album
     */
    public List<MusicEntity> findByAlbum(String album) throws SQLException {
        String sql = "SELECT * FROM music WHERE album LIKE ? ORDER BY title";
        List<MusicEntity> result = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + album + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSet(rs));
                }
            }
        }
        return result;
    }

    /**
     * Global search in title, artist, and album.
     *
     * @param query Text to search
     * @return List of corresponding musics
     */
    public List<MusicEntity> search(String query) throws SQLException {
        String sql = """
            SELECT * FROM music 
            WHERE title LIKE ? OR artist LIKE ? OR album LIKE ?
            ORDER BY artist, album, title
        """;
        List<MusicEntity> result = new ArrayList<>();
        String searchPattern = "%" + query + "%";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSet(rs));
                }
            }
        }
        return result;
    }

    /**
     * Returns the list of all music file paths.
     *
     * @return List of music file paths
     */
    public List<String> findAllPaths() throws SQLException {
        String sql = "SELECT path FROM music";
        List<String> result = new ArrayList<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(rs.getString("path"));
            }
        }
        return result;
    }

    /**
     * Count the total number of musics in the database.
     *
     * @return Number of musics
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM music";

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Returns the list of all unique artists.
     *
     * @return List of artists
     */
    public List<String> findAllArtists() throws SQLException {
        String sql = "SELECT DISTINCT artist FROM music WHERE artist IS NOT NULL ORDER BY artist";
        List<String> result = new ArrayList<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(rs.getString("artist"));
            }
        }
        return result;
    }

    /**
     * Returns the list of all unique albums.
     *
     * @return List of albums
     */
    public List<String> findAllAlbums() throws SQLException {
        String sql = "SELECT DISTINCT album FROM music WHERE album IS NOT NULL ORDER BY album";
        List<String> result = new ArrayList<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(rs.getString("album"));
            }
        }
        return result;
    }

    /**
     * Converts a ResultSet to a MusicEntity.
     */
    private MusicEntity mapResultSet(ResultSet rs) throws SQLException {
        MusicEntity music = new MusicEntity();
        music.setId(rs.getLong("id"));
        music.setPath(rs.getString("path"));
        music.setTitle(rs.getString("title"));
        music.setArtist(rs.getString("artist"));
        music.setAlbum(rs.getString("album"));
        music.setDuration(rs.getLong("duration"));
        music.setHash(rs.getString("hash"));
        music.setRating(rs.getInt("rating"));

        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            music.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }

        String updatedAt = rs.getString("updated_at");
        if (updatedAt != null) {
            music.setUpdatedAt(LocalDateTime.parse(updatedAt.replace(" ", "T")));
        }

        return music;
    }

    /**
     * Updates the rating for a music track.
     *
     * @param musicId The music ID
     * @param rating The new rating (0-5)
     */
    public void updateRating(long musicId, int rating) throws SQLException {
        String sql = "UPDATE music SET rating = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, Math.max(0, Math.min(5, rating)));
            stmt.setLong(2, musicId);
            stmt.executeUpdate();
        }
    }
}

