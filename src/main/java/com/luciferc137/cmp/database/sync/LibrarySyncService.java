package com.luciferc137.cmp.database.sync;

import com.luciferc137.cmp.database.DatabaseManager;
import com.luciferc137.cmp.database.dao.MusicDao;
import com.luciferc137.cmp.database.dao.SyncLogDao;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.SyncLogEntity;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Music library synchronization service.
 * Synchronizes files from a folder with the database.
 * Synchronization is never automatic, it must be triggered
 * manually by the user to avoid overloading disk reads.
 */
public class LibrarySyncService {

    private final MusicDao musicDao;
    private final SyncLogDao syncLogDao;
    private final AudioMetadataExtractor metadataExtractor;
    private final DatabaseManager dbManager;
    private final ExecutorService executorService;

    private volatile boolean isSyncing = false;
    private volatile boolean cancelRequested = false;

    public LibrarySyncService() {
        this.musicDao = new MusicDao();
        this.syncLogDao = new SyncLogDao();
        this.metadataExtractor = new AudioMetadataExtractor();
        this.dbManager = DatabaseManager.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Checks if a synchronization is in progress.
     *
     * @return true if sync is in progress
     */
    public boolean isSyncing() {
        return isSyncing;
    }

    /**
     * Cancels the current synchronization.
     */
    public void cancelSync() {
        cancelRequested = true;
    }

    /**
     * Synchronizes a folder with the database synchronously.
     *
     * @param folderPath The folder path to synchronize
     * @return The synchronization result
     */
    public SyncResult syncFolder(String folderPath) {
        return syncFolder(folderPath, new SyncProgressListener.Empty());
    }

    /**
     * Synchronizes a folder with the database synchronously.
     *
     * @param folderPath The folder path to synchronize
     * @param listener The listener to track progress
     * @return The synchronization result
     */
    public SyncResult syncFolder(String folderPath, SyncProgressListener listener) {
        if (isSyncing) {
            return new SyncResult(0, 0, 0, 0, 0, 0, "already_syncing");
        }

        isSyncing = true;
        cancelRequested = false;
        long startTime = System.currentTimeMillis();

        int filesAdded = 0;
        int filesUpdated = 0;
        int filesRemoved = 0;
        int filesSkipped = 0;
        int errors = 0;

        try {
            Path folder = Paths.get(folderPath);
            if (!Files.exists(folder) || !Files.isDirectory(folder)) {
                return new SyncResult(0, 0, 0, 0, 1, 0, "folder_not_found");
            }

            // 1. Scan all audio files in the folder
            List<File> audioFiles = scanAudioFiles(folder);
            listener.onSyncStarted(audioFiles.size());

            // 2. Get all existing paths in database
            Set<String> existingPaths = new HashSet<>(musicDao.findAllPaths());
            Set<String> processedPaths = new HashSet<>();

            // 3. Create a sync log entry
            SyncLogEntity syncLog = new SyncLogEntity(folderPath);
            syncLogDao.insert(syncLog);

            // 4. Process each audio file
            dbManager.beginTransaction();
            try {
                int current = 0;
                for (File file : audioFiles) {
                    if (cancelRequested) {
                        dbManager.rollback();
                        return new SyncResult(filesAdded, filesUpdated, filesRemoved,
                                             filesSkipped, errors,
                                             System.currentTimeMillis() - startTime, "cancelled");
                    }

                    current++;
                    String path = file.getAbsolutePath();
                    processedPaths.add(path);
                    listener.onFileProcessed(current, audioFiles.size(), file.getName());

                    try {
                        Optional<MusicEntity> existing = musicDao.findByPath(path);

                        AudioMetadataExtractor.ExtractedMetadata metadata = metadataExtractor.extract(file);
                        if (existing.isPresent()) {
                            // File already exists, check if update needed

                            if (!existing.get().getHash().equals(metadata.getHash())) {
                                // File has changed, update it
                                MusicEntity entity = existing.get();
                                updateEntityFromMetadata(entity, metadata);
                                musicDao.update(entity);
                                filesUpdated++;
                                listener.onFileUpdated(path);
                            } else {
                                filesSkipped++;
                            }
                        } else {
                            // New file, add it
                            MusicEntity entity = createEntityFromMetadata(path, metadata);
                            musicDao.insert(entity);
                            filesAdded++;
                            listener.onFileAdded(path);
                        }
                    } catch (IOException e) {
                        errors++;
                        listener.onError(path, e.getMessage());
                    }
                }

                // 5. Remove entries for files that no longer exist
                for (String existingPath : existingPaths) {
                    // Only delete if file was in the synced folder
                    if (existingPath.startsWith(folderPath) && !processedPaths.contains(existingPath)) {
                        musicDao.deleteByPath(existingPath);
                        filesRemoved++;
                        listener.onFileRemoved(existingPath);
                    }
                }

                dbManager.commit();

                // 6. Update sync log
                syncLog.setFilesAdded(filesAdded);
                syncLog.setFilesUpdated(filesUpdated);
                syncLog.setFilesRemoved(filesRemoved);
                syncLog.setStatus("completed");
                syncLogDao.update(syncLog);

            } catch (SQLException e) {
                dbManager.rollback();
                throw e;
            }

            long duration = System.currentTimeMillis() - startTime;
            SyncResult result = new SyncResult(filesAdded, filesUpdated, filesRemoved,
                                               filesSkipped, errors, duration, "completed");
            listener.onSyncCompleted(result);
            return result;

        } catch (Exception e) {
            System.err.println("Error during synchronization: " + e.getMessage());
            long duration = System.currentTimeMillis() - startTime;
            return new SyncResult(filesAdded, filesUpdated, filesRemoved,
                                  filesSkipped, errors + 1, duration, "error");
        } finally {
            isSyncing = false;
        }
    }

    /**
     * Synchronizes a folder asynchronously.
     *
     * @param folderPath The folder path
     * @return A CompletableFuture with the result
     */
    public CompletableFuture<SyncResult> syncFolderAsync(String folderPath) {
        return syncFolderAsync(folderPath, new SyncProgressListener.Empty());
    }

    /**
     * Synchronizes a folder asynchronously.
     *
     * @param folderPath The folder path
     * @param listener The progress listener
     * @return A CompletableFuture with the result
     */
    public CompletableFuture<SyncResult> syncFolderAsync(String folderPath, SyncProgressListener listener) {
        return CompletableFuture.supplyAsync(() -> syncFolder(folderPath, listener), executorService);
    }

    /**
     * Recursively scans a folder to find audio files.
     */
    private List<File> scanAudioFiles(Path folder) throws IOException {
        List<File> audioFiles = new ArrayList<>();

        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                File f = file.toFile();
                if (metadataExtractor.isAudioFile(f)) {
                    audioFiles.add(f);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exc) {
                // Ignore inaccessible files
                return FileVisitResult.CONTINUE;
            }
        });

        return audioFiles;
    }

    /**
     * Creates a MusicEntity from metadata.
     */
    private MusicEntity createEntityFromMetadata(String path, AudioMetadataExtractor.ExtractedMetadata metadata) {
        return new MusicEntity(
            path,
            metadata.getTitle(),
            metadata.getArtist(),
            metadata.getAlbum(),
            metadata.getDuration(),
            metadata.getHash()
        );
    }

    /**
     * Updates a MusicEntity with new metadata.
     */
    private void updateEntityFromMetadata(MusicEntity entity, AudioMetadataExtractor.ExtractedMetadata metadata) {
        entity.setTitle(metadata.getTitle());
        entity.setArtist(metadata.getArtist());
        entity.setAlbum(metadata.getAlbum());
        entity.setDuration(metadata.getDuration());
        entity.setHash(metadata.getHash());
    }

    /**
     * Shuts down the service and releases resources.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}

