package com.luciferc137.cmp.database.sync;

import org.jetbrains.annotations.NotNull;

/**
 * Result of a music library synchronization.
 */
public record SyncResult(int filesAdded, int filesUpdated, int filesRemoved,
                         int filesSkipped, int errors,
                         long durationMs, String status) {

    public int getTotalProcessed() {
        return filesAdded + filesUpdated + filesRemoved;
    }

    @Override
    public @NotNull String toString() {
        return String.format(
                "SyncResult{added=%d, updated=%d, removed=%d, skipped=%d, errors=%d, duration=%dms, status='%s'}",
                filesAdded, filesUpdated, filesRemoved, filesSkipped, errors, durationMs, status
        );
    }
}

