package com.luciferc137.cmp.database.dao;

import com.luciferc137.cmp.database.DatabaseManager;
import com.luciferc137.cmp.database.model.SyncLogEntity;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) pour la gestion des logs de synchronisation.
 */
public class SyncLogDao {

    private final DatabaseManager dbManager;

    public SyncLogDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Insère un nouveau log de synchronisation.
     *
     * @param syncLog L'entité de log à insérer
     * @return L'ID généré
     */
    public long insert(SyncLogEntity syncLog) throws SQLException {
        String sql = """
            INSERT INTO sync_log (folder_path, files_added, files_updated, files_removed, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, syncLog.getFolderPath());
            stmt.setInt(2, syncLog.getFilesAdded());
            stmt.setInt(3, syncLog.getFilesUpdated());
            stmt.setInt(4, syncLog.getFilesRemoved());
            stmt.setString(5, syncLog.getStatus());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    syncLog.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Met à jour un log de synchronisation existant.
     *
     * @param syncLog L'entité de log à mettre à jour
     */
    public void update(SyncLogEntity syncLog) throws SQLException {
        String sql = """
            UPDATE sync_log SET
                files_added = ?,
                files_updated = ?,
                files_removed = ?,
                status = ?
            WHERE id = ?
        """;

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, syncLog.getFilesAdded());
            stmt.setInt(2, syncLog.getFilesUpdated());
            stmt.setInt(3, syncLog.getFilesRemoved());
            stmt.setString(4, syncLog.getStatus());
            stmt.setLong(5, syncLog.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Recherche un log par son ID.
     *
     * @param id L'ID du log
     * @return L'entité log si trouvée
     */
    public Optional<SyncLogEntity> findById(long id) throws SQLException {
        String sql = "SELECT * FROM sync_log WHERE id = ?";

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
     * Retourne le dernier log de synchronisation.
     *
     * @return Le dernier log si existant
     */
    public Optional<SyncLogEntity> findLast() throws SQLException {
        String sql = "SELECT * FROM sync_log ORDER BY sync_date DESC LIMIT 1";

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Retourne tous les logs de synchronisation.
     *
     * @return Liste de tous les logs ordonnés par date décroissante
     */
    public List<SyncLogEntity> findAll() throws SQLException {
        String sql = "SELECT * FROM sync_log ORDER BY sync_date DESC";
        List<SyncLogEntity> result = new ArrayList<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapResultSet(rs));
            }
        }
        return result;
    }

    /**
     * Returns the N most recent synchronization logs.
     *
     * @param limit The maximum number of logs to return
     * @return List of logs
     */
    public List<SyncLogEntity> findRecent(int limit) throws SQLException {
        String sql = "SELECT * FROM sync_log ORDER BY sync_date DESC LIMIT ?";
        List<SyncLogEntity> result = new ArrayList<>();

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSet(rs));
                }
            }
        }
        return result;
    }

    /**
     * Supprime les logs plus anciens qu'une certaine date.
     *
     * @param olderThan La date limite
     * @return Le nombre de logs supprimés
     */
    public int deleteOlderThan(LocalDateTime olderThan) throws SQLException {
        String sql = "DELETE FROM sync_log WHERE sync_date < ?";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, olderThan.toString().replace("T", " "));
            return stmt.executeUpdate();
        }
    }

    /**
     * Convertit un ResultSet en entité SyncLogEntity.
     */
    private SyncLogEntity mapResultSet(ResultSet rs) throws SQLException {
        SyncLogEntity syncLog = new SyncLogEntity();
        syncLog.setId(rs.getLong("id"));
        syncLog.setFolderPath(rs.getString("folder_path"));
        syncLog.setFilesAdded(rs.getInt("files_added"));
        syncLog.setFilesUpdated(rs.getInt("files_updated"));
        syncLog.setFilesRemoved(rs.getInt("files_removed"));
        syncLog.setStatus(rs.getString("status"));

        String syncDate = rs.getString("sync_date");
        if (syncDate != null) {
            syncLog.setSyncDate(LocalDateTime.parse(syncDate.replace(" ", "T")));
        }

        return syncLog;
    }
}

