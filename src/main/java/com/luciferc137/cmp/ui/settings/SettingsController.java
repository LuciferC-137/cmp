package com.luciferc137.cmp.ui.settings;

import com.luciferc137.cmp.database.sync.SyncProgressListener;
import com.luciferc137.cmp.database.sync.SyncResult;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.settings.SettingsManager;
import com.luciferc137.cmp.ui.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Controller for the settings window.
 */
public class SettingsController {

    @FXML
    private ListView<String> categoryList;

    @FXML
    private StackPane contentPane;

    @FXML
    private VBox libraryPane;

    @FXML
    private TextField musicFolderField;

    @FXML
    private Button browseButton;

    @FXML
    private Button resyncButton;

    @FXML
    private ProgressBar syncProgressBar;

    @FXML
    private Label syncStatusLabel;

    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();

    @FXML
    public void initialize() {
        // Initialize the category list
        categoryList.getItems().addAll("Library");
        categoryList.getSelectionModel().selectFirst();

        // Load saved music folder path
        String savedPath = settingsManager.getMusicFolderPath();
        if (savedPath != null && !savedPath.isEmpty()) {
            musicFolderField.setText(savedPath);
        }

        // Handle category selection
        categoryList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showCategoryContent(newVal));

        // Show initial content
        showCategoryContent("Library");

        // Hide progress bar initially
        syncProgressBar.setVisible(false);
        syncStatusLabel.setText("");
    }

    /**
     * Shows content for the selected category.
     */
    private void showCategoryContent(String category) {
        // Hide all panels
        libraryPane.setVisible(false);
        libraryPane.setManaged(false);

        // Show the corresponding panel
        if ("Library".equals(category)) {
            libraryPane.setVisible(true);
            libraryPane.setManaged(true);
        }
    }

    /**
     * Opens a folder chooser dialog.
     */
    @FXML
    private void onBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Music Folder");

        // Set initial directory if path exists
        String currentPath = musicFolderField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        }

        Stage stage = (Stage) browseButton.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            String path = selectedDirectory.getAbsolutePath();
            musicFolderField.setText(path);
            settingsManager.setMusicFolderPath(path);
        }
    }

    /**
     * Starts the library synchronization.
     */
    @FXML
    private void onResync() {
        String folderPath = musicFolderField.getText();

        if (folderPath == null || folderPath.trim().isEmpty()) {
            showAlert("Error", "Please select a music folder.");
            return;
        }

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            showAlert("Error", "The selected folder does not exist.");
            return;
        }

        // Save the path
        settingsManager.setMusicFolderPath(folderPath);

        // Disable buttons during synchronization
        resyncButton.setDisable(true);
        browseButton.setDisable(true);
        syncProgressBar.setVisible(true);
        syncProgressBar.setProgress(0);
        syncStatusLabel.setText("Starting synchronization...");

        // Run synchronization in background
        musicLibrary.syncFolder(folderPath, new SyncProgressListener() {
            @Override
            public void onSyncStarted(int totalFiles) {
                Platform.runLater(() ->
                        syncStatusLabel.setText("Scanning " + totalFiles + " files..."));
            }

            @Override
            public void onFileProcessed(int currentFile, int totalFiles, String fileName) {
                Platform.runLater(() -> {
                    double progress = (double) currentFile / totalFiles;
                    syncProgressBar.setProgress(progress);
                    syncStatusLabel.setText("Processing: " + fileName);
                });
            }

            @Override
            public void onFileAdded(String path) {
                // Can be used for logging
            }

            @Override
            public void onFileUpdated(String path) {
                // Can be used for logging
            }

            @Override
            public void onFileRemoved(String path) {
                // Can be used for logging
            }

            @Override
            public void onError(String path, String error) {
                Platform.runLater(() ->
                        System.err.println("Error on " + path + ": " + error));
            }

            @Override
            public void onSyncCompleted(SyncResult result) {
                Platform.runLater(() -> {
                    syncProgressBar.setProgress(1);
                    syncProgressBar.setVisible(false);
                    resyncButton.setDisable(false);
                    browseButton.setDisable(false);

                    String message = String.format(
                            "Synchronization completed:\n• %d files added\n• %d files updated\n• %d files removed",
                            result.getFilesAdded(),
                            result.getFilesUpdated(),
                            result.getFilesRemoved()
                    );
                    syncStatusLabel.setText("Completed: " + result.getFilesAdded() + " added, " +
                            result.getFilesUpdated() + " updated, " +
                            result.getFilesRemoved() + " removed");

                    showAlert("Synchronization Complete", message);
                });
            }
        });
    }

    /**
     * Shows an alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.applyDarkTheme(alert);
        alert.showAndWait();
    }

    /**
     * Closes the settings window.
     */
    @FXML
    private void onClose() {
        Stage stage = (Stage) categoryList.getScene().getWindow();
        stage.close();
    }
}
