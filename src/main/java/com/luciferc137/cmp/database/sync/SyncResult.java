package com.luciferc137.cmp.database.sync;

/**
 * Result of a music library synchronization.
 */
public class SyncResult {

    private final int filesAdded;
    private final int filesUpdated;
    private final int filesRemoved;
    private final int filesSkipped;
    private final int errors;
    private final long durationMs;
    private final String status;

    public SyncResult(int filesAdded, int filesUpdated, int filesRemoved,
                      int filesSkipped, int errors, long durationMs, String status) {
        this.filesAdded = filesAdded;
        this.filesUpdated = filesUpdated;
        this.filesRemoved = filesRemoved;
        this.filesSkipped = filesSkipped;
        this.errors = errors;
        this.durationMs = durationMs;
        this.status = status;
    }

    public int getFilesAdded() {
        return filesAdded;
    }

    public int getFilesUpdated() {
        return filesUpdated;
    }

    public int getFilesRemoved() {
        return filesRemoved;
    }

    public int getFilesSkipped() {
        return filesSkipped;
    }

    public int getErrors() {
        return errors;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalProcessed() {
        return filesAdded + filesUpdated + filesRemoved;
    }

    @Override
    public String toString() {
        return String.format(
            "SyncResult{added=%d, updated=%d, removed=%d, skipped=%d, errors=%d, duration=%dms, status='%s'}",
            filesAdded, filesUpdated, filesRemoved, filesSkipped, errors, durationMs, status
        );
    }
}

