package com.luciferc137.cmp.database.sync;

/**
 * Callback pour suivre la progression de la synchronisation.
 */
public interface SyncProgressListener {

    /**
     * Appelé lorsque la synchronisation démarre.
     *
     * @param totalFiles Le nombre total de fichiers à traiter
     */
    void onSyncStarted(int totalFiles);

    /**
     * Appelé pour chaque fichier traité.
     *
     * @param currentFile Le numéro du fichier actuel
     * @param totalFiles Le nombre total de fichiers
     * @param fileName Le nom du fichier en cours
     */
    void onFileProcessed(int currentFile, int totalFiles, String fileName);

    /**
     * Appelé lorsqu'un fichier est ajouté à la bibliothèque.
     *
     * @param path Le chemin du fichier ajouté
     */
    void onFileAdded(String path);

    /**
     * Appelé lorsqu'un fichier est mis à jour.
     *
     * @param path Le chemin du fichier mis à jour
     */
    void onFileUpdated(String path);

    /**
     * Appelé lorsqu'un fichier est supprimé de la bibliothèque.
     *
     * @param path Le chemin du fichier supprimé
     */
    void onFileRemoved(String path);

    /**
     * Appelé lorsqu'une erreur survient.
     *
     * @param path Le chemin du fichier en erreur
     * @param error Le message d'erreur
     */
    void onError(String path, String error);

    /**
     * Appelé lorsque la synchronisation est terminée.
     *
     * @param result Le résultat de la synchronisation
     */
    void onSyncCompleted(SyncResult result);

    /**
     * Implémentation vide pour quand on n'a pas besoin de callbacks.
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

