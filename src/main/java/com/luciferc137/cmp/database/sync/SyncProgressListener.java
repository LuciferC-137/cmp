package com.luciferc137.cmp.database.sync;

/**
 * Callback interface to track synchronization progress.
 */
public interface SyncProgressListener {

    /**
     * Called when synchronization starts.
     *
     * @param totalFiles The total number of files to process
     */
    void onSyncStarted(int totalFiles);

    /**
     * Called for each processed file.
     *
     * @param currentFile The current file number
     * @param totalFiles The total number of files
     * @param fileName The current file name
     */
    void onFileProcessed(int currentFile, int totalFiles, String fileName);

    /**
     * Called when a file is added to the library.
     *
     * @param path The path of the added file
     */
    void onFileAdded(String path);

    /**
     * Called when a file is updated.
     *
     * @param path The path of the updated file
     */
    void onFileUpdated(String path);

    /**
     * Called when a file is removed from the library.
     *
     * @param path The path of the removed file
     */
    void onFileRemoved(String path);

    /**
     * Called when an error occurs.
     *
     * @param path The path of the file with error
     * @param error The error message
     */
    void onError(String path, String error);

    /**
     * Called when synchronization is completed.
     *
     * @param result The synchronization result
     */
    void onSyncCompleted(SyncResult result);

    /**
     * Empty implementation for when callbacks are not needed.
     */
    class Empty implements SyncProgressListener {
        @Override
        public void onSyncStarted(int totalFiles) {}

        @Override
        public void onFileProcessed(int currentFile, int totalFiles, String fileName) {}

        @Override
        public void onFileAdded(String path) {}

        @Override
        public void onFileUpdated(String path) {}

        @Override
        public void onFileRemoved(String path) {}

        @Override
        public void onError(String path, String error) {}

        @Override
        public void onSyncCompleted(SyncResult result) {}
    }
}

