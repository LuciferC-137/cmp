package com.luciferc137.cmp.database.model;

import java.time.LocalDateTime;

/**
 * Entité représentant un enregistrement de synchronisation.
 */
public class SyncLogEntity {

    private Long id;
    private LocalDateTime syncDate;
    private String folderPath;
    private int filesAdded;
    private int filesUpdated;
    private int filesRemoved;
    private String status;

    public SyncLogEntity() {
    }

    public SyncLogEntity(String folderPath) {
        this.folderPath = folderPath;
        this.status = "in_progress";
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getSyncDate() {
        return syncDate;
    }

    public void setSyncDate(LocalDateTime syncDate) {
        this.syncDate = syncDate;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public int getFilesAdded() {
        return filesAdded;
    }

    public void setFilesAdded(int filesAdded) {
        this.filesAdded = filesAdded;
    }

    public int getFilesUpdated() {
        return filesUpdated;
    }

    public void setFilesUpdated(int filesUpdated) {
        this.filesUpdated = filesUpdated;
    }

    public int getFilesRemoved() {
        return filesRemoved;
    }

    public void setFilesRemoved(int filesRemoved) {
        this.filesRemoved = filesRemoved;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SyncLogEntity{" +
                "id=" + id +
                ", syncDate=" + syncDate +
                ", folderPath='" + folderPath + '\'' +
                ", filesAdded=" + filesAdded +
                ", filesUpdated=" + filesUpdated +
                ", filesRemoved=" + filesRemoved +
                ", status='" + status + '\'' +
                '}';
    }
}

