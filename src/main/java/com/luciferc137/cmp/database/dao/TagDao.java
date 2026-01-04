package com.luciferc137.cmp.database.dao;

import com.luciferc137.cmp.database.DatabaseManager;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.TagEntity;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) pour la gestion des tags en base de données.
 */
public class TagDao {

    private final DatabaseManager dbManager;

    public TagDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Insert new tag in the database.
     *
     * @param tag Tag entity to insert
     * @return Generated ID
     */
    public long insert(TagEntity tag) throws SQLException {
        String sql = "INSERT INTO tag (name, color) VALUES (?, ?)";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, tag.getName());
            stmt.setString(2, tag.getColor());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    tag.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Update existing tag.
     *
     * @param tag Tag entity to update
     */
    public void update(TagEntity tag) throws SQLException {
        String sql = "UPDATE tag SET name = ?, color = ? WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, tag.getName());
            stmt.setString(2, tag.getColor());
            stmt.setLong(3, tag.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Delete a tag by its ID.
     *
     * @param id Tag to delete
     */
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM tag WHERE id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Search a tag by its ID.
     *
     * @param id tag ID
     * @return tag entity if found
     */
    public Optional<TagEntity> findById(long id) throws SQLException {
        String sql = "SELECT * FROM tag WHERE id = ?";

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
     * Finds a tag by its name.
     *
     * @param name Name of tag
     * @return Tag entity if found
     */
    public Optional<TagEntity> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM tag WHERE name = ?";

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
     * Returns all tags
     *
     * @return List of all tags
     */
    public List<TagEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM tag ORDER BY name";
        List<TagEntity> result = new ArrayList<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        }
        return result;
    }

    /**
     * Add a tag to a track
     *
     * @param musicId Music ID
     * @param tagId Tag ID
     */
    public void addTagToMusic(long musicId, long tagId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO music_tag (music_id, tag_id) VALUES (?, ?)";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, musicId);
            stmt.setLong(2, tagId);
            stmt.executeUpdate();
        }
    }

    /**
     * Remove a tag from a music
     *
     * @param musicId Music ID
     * @param tagId Tag ID
     */
    public void removeTagFromMusic(long musicId, long tagId) throws SQLException {
        String sql = "DELETE FROM music_tag WHERE music_id = ? AND tag_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, musicId);
            stmt.setLong(2, tagId);
            stmt.executeUpdate();
        }
    }

    /**
     * Returns all tags for a specific music.
     *
     * @param musicId Music ID
     * @return List of tags for this music
     */
    public List<TagEntity> getTagsByMusic(long musicId) throws SQLException {
        String sql = """
            SELECT t.* FROM tag t
            INNER JOIN music_tag mt ON t.id = mt.tag_id
            WHERE mt.music_id = ?
            ORDER BY t.name
        """;
        List<TagEntity> result = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, musicId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSet(rs));
                }
            }
        }
        return result;
    }

    /**
     * Returns all music with a specific tag.
     *
     * @param tagId The tag ID
     * @return Liste des musiques avec ce tag
     */
    public List<MusicEntity> getMusicsByTag(long tagId) throws SQLException {
        String sql = """
            SELECT m.* FROM music m
            INNER JOIN music_tag mt ON m.id = mt.music_id
            WHERE mt.tag_id = ?
            ORDER BY m.artist, m.album, m.title
        """;
        List<MusicEntity> result = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, tagId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapMusicResultSet(rs));
                }
            }
        }
        return result;
    }

    /**
     * Count number of music with a specific tag.
     *
     * @param tagId tag ID
     * @return Number of music with this tag
     */
    public int countMusics(long tagId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM music_tag WHERE tag_id = ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, tagId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Converts a ResultSet to a TagEntity.
     */
    private TagEntity mapResultSet(ResultSet rs) throws SQLException {
        TagEntity tag = new TagEntity();
        tag.setId(rs.getLong("id"));
        tag.setName(rs.getString("name"));
        tag.setColor(rs.getString("color"));

        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            tag.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        }

        return tag;
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

