package com.luciferc137.cmp.ui.settings;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.importer.AimpPlaylistImporter;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.database.sync.SyncProgressListener;
import com.luciferc137.cmp.database.sync.SyncResult;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.settings.SettingsManager;
import com.luciferc137.cmp.ui.ThemeManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the settings window.
 */
public class SettingsController {

    // Library management pane
    @FXML private ListView<String> categoryList;
    @FXML private StackPane contentPane;
    @FXML private VBox libraryPane;
    @FXML private TextField musicFolderField;
    @FXML private Button browseButton;
    @FXML private Button resyncButton;
    @FXML private ProgressBar syncProgressBar;
    @FXML private Label syncStatusLabel;

    // Playlist management pane
    @FXML private VBox playlistsPane;
    @FXML private ListView<PlaylistEntity> playlistListView;
    @FXML private Button createPlaylistButton;
    @FXML private Button deletePlaylistButton;
    @FXML private Button importAimpButton;
    @FXML private Label importStatusLabel;

    // Tag management pane
    @FXML private VBox tagManagementPane;
    @FXML private ListView<TagEntity> tagListView;
    @FXML private Button createTagButton;
    @FXML private Button refactorTagButton;
    @FXML private Button deleteTagButton;

    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private final LibraryService libraryService = LibraryService.getInstance();

    // Callback for when playlists change (to refresh MainController)
    private Runnable onPlaylistsChangedCallback;

    @FXML
    public void initialize() {
        // Initialize the category list
        categoryList.getItems().addAll("Library", "Playlists", "Tags");
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
        importStatusLabel.setText("");

        // Setup playlist list view
        setupPlaylistListView();

        // Setup tag list view
        setupTagListView();
    }

    /**
     * Sets the callback to be called when playlists change.
     */
    public void setOnPlaylistsChangedCallback(Runnable callback) {
        this.onPlaylistsChangedCallback = callback;
    }

    /**
     * Selects a specific category in the settings.
     *
     * @param category The category name to select (e.g., "Library", "Playlists")
     */
    public void selectCategory(String category) {
        if (categoryList.getItems().contains(category)) {
            categoryList.getSelectionModel().select(category);
        }
    }

    /**
     * Notifies that playlists have changed.
     */
    private void notifyPlaylistsChanged() {
        if (onPlaylistsChangedCallback != null) {
            onPlaylistsChangedCallback.run();
        }
    }

    /**
     * Setup the playlist list view with custom cell factory.
     */
    private void setupPlaylistListView() {
        // Enable multiple selection
        playlistListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Custom cell factory to display playlist names
        playlistListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PlaylistEntity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        // Load playlists
        refreshPlaylistList();
    }

    private void setupTagListView() {
        // Disable multiple selection
        tagListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Custom cell factory to display tag names
        tagListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TagEntity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        // Load tags
        refreshTagList();
    }

    /**
     * Refreshes the playlist list from the database.
     */
    private void refreshPlaylistList() {
        List<PlaylistEntity> playlists = libraryService.getAllPlaylists();
        playlistListView.getItems().setAll(playlists);
    }

    private void refreshTagList() {
        List<TagEntity> tags = libraryService.getAllTags();
        tagListView.getItems().setAll(tags);
    }

    /**
     * Shows content for the selected category.
     */
    private void showCategoryContent(String category) {
        // Hide all panels
        libraryPane.setVisible(false);
        libraryPane.setManaged(false);
        playlistsPane.setVisible(false);
        playlistsPane.setManaged(false);
        tagManagementPane.setVisible(false);
        tagManagementPane.setManaged(false);

        // Show the corresponding panel
        if ("Library".equals(category)) {
            libraryPane.setVisible(true);
            libraryPane.setManaged(true);
        } else if ("Playlists".equals(category)) {
            playlistsPane.setVisible(true);
            playlistsPane.setManaged(true);
            refreshPlaylistList();
        } else if ("Tags".equals(category)) {
            tagManagementPane.setVisible(true);
            tagManagementPane.setManaged(true);
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
     * Creates a new playlist.
     */
    @FXML
    private void onCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Enter the name for the new playlist:");
        dialog.setContentText("Name:");
        ThemeManager.applyDarkTheme(dialog);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Optional<PlaylistEntity> created = libraryService.createPlaylist(name.trim());
                if (created.isPresent()) {
                    refreshPlaylistList();
                    notifyPlaylistsChanged();
                } else {
                    showAlert("Error", "Failed to create playlist.");
                }
            }
        });
    }

    /**
     * Deletes the selected playlists with confirmation.
     */
    @FXML
    private void onDeletePlaylists() {
        List<PlaylistEntity> selected = playlistListView.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert("No Selection", "Please select one or more playlists to delete.");
            return;
        }

        // Build confirmation message
        Alert confirmDialog = confirmDeletePlaylist(selected);
        ThemeManager.applyDarkTheme(confirmDialog);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete all selected playlists
            for (PlaylistEntity playlist : selected) {
                libraryService.deletePlaylist(playlist.getId());
            }
            refreshPlaylistList();
            notifyPlaylistsChanged();
        }
    }

    private static @NotNull Alert confirmDeletePlaylist(List<PlaylistEntity> selected) {
        String message;
        if (selected.size() == 1) {
            message = "Are you sure you want to delete the playlist \""
                    + selected.getFirst().getName() + "\"?";
        } else {
            message = "Are you sure you want to delete " + selected.size() + " playlists?";
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete Playlist(s)");
        confirmDialog.setContentText(message);
        return confirmDialog;
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
                            result.filesAdded(),
                            result.filesUpdated(),
                            result.filesRemoved()
                    );
                    syncStatusLabel.setText("Completed: " + result.filesAdded() + " added, " +
                            result.filesUpdated() + " updated, " +
                            result.filesRemoved() + " removed");

                    showAlert("Synchronization Complete", message);
                });
            }
        });
    }

    /**
     * Opens a file chooser to import an AIMP .xspf playlist.
     */
    @FXML
    private void onImportAimpPlaylist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select AIMP Playlist");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("AIMP Playlists", "*.xspf", "*.aimppl4"),
                new FileChooser.ExtensionFilter("XSPF Playlists", "*.xspf"),
                new FileChooser.ExtensionFilter("AIMPPL4 Playlists", "*.aimppl4"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Set initial directory to common playlist locations
        String userHome = System.getProperty("user.home");
        File initialDir = new File(userHome);
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        Stage stage = (Stage) importAimpButton.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        importStatusLabel.setText("Importing...");
        importAimpButton.setDisable(true);

        // Run import in background
        new Thread(() -> {
            AimpPlaylistImporter importer = new AimpPlaylistImporter();
            StringBuilder resultMessage = new StringBuilder();
            int successCount = 0;
            int totalTracks = 0;
            int importedTracks = 0;

            for (File file : selectedFiles) {
                AimpPlaylistImporter.ImportResult result = importer.importPlaylist(file);
                if (result.isSuccess()) {
                    successCount++;
                    totalTracks += result.getTotalTracks();
                    importedTracks += result.getImportedTracks();
                    resultMessage.append("✓ ").append(result.getPlaylistName())
                            .append(": ").append(result.getImportedTracks())
                            .append("/").append(result.getTotalTracks()).append(" tracks\n");

                    if (result.getMissingCount() > 0) {
                        resultMessage.append("  (").append(result.getMissingCount())
                                .append(" tracks not found in library)\n");
                    }
                } else {
                    resultMessage.append("✗ ").append(file.getName())
                            .append(": ").append(result.getError()).append("\n");
                }
            }

            int finalSuccessCount = successCount;
            int finalTotalTracks = totalTracks;
            int finalImportedTracks = importedTracks;

            Platform.runLater(() -> {
                importAimpButton.setDisable(false);

                String statusText = String.format("%d playlist(s) imported, %d/%d tracks added",
                        finalSuccessCount, finalImportedTracks, finalTotalTracks);
                importStatusLabel.setText(statusText);

                // Refresh the playlist list and notify MainController
                refreshPlaylistList();
                notifyPlaylistsChanged();

                // Refresh the music library to update playlists
                musicLibrary.refresh();

                showAlert("Import Complete",
                        String.format("Imported %d playlist(s):\n\n%s",
                                finalSuccessCount, resultMessage.toString()));
            });
        }).start();
    }

    @FXML
    public void onCreateTag(ActionEvent actionEvent) {
        showCreateTagDialog(this::refreshTagList);
    }

    @FXML
    public void onRefactorTag(ActionEvent actionEvent) {
        TagEntity selectedTag = tagListView.getSelectionModel().getSelectedItem();
        if (selectedTag == null) {
            showAlert("No Selection", "Please select a tag to refactor.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selectedTag.getName());
        dialog.setTitle("Refactor Tag");
        dialog.setHeaderText("Refactor the selected tag");
        dialog.setContentText("New tag name:");
        ThemeManager.applyDarkTheme(dialog);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                selectedTag.setName(newName.trim());
                libraryService.refactorTag(selectedTag.getId(),
                        newName.trim(), selectedTag.getColor());
                refreshTagList();
                musicLibrary.refresh(); // Refresh the library to update tag associations
            }
        });
    }

    @FXML
    public void onDeleteTags(ActionEvent actionEvent) {
        List<TagEntity> selected = tagListView.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert("No Selection", "Please select one or more tags to delete.");
            return;
        }

        // Build confirmation message
        Alert confirmDialog = confirmDeleteTag(selected);
        ThemeManager.applyDarkTheme(confirmDialog);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete all selected tags
            for (TagEntity tag : selected) {
                libraryService.deleteTag(tag.getId());
            }
            refreshTagList();
            musicLibrary.refresh(); // Refresh the library to update tag associations
        }
    }

    private static @NotNull Alert confirmDeleteTag(List<TagEntity> selected) {
        String message;
        if (selected.size() == 1) {
            message = "Are you sure you want to delete the tag \""
                    + selected.getFirst().getName() + "\"?";
        } else {
            message = "Are you sure you want to delete " + selected.size() + " tags?\n" +
                    "This action will permanently remove the tags from all associated music files.";
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete Tag");
        confirmDialog.setContentText(message);
        return confirmDialog;
    }

    /**
     * Shows a dialog to create a new tag.
     */
    public static void showCreateTagDialog(@Nullable Runnable onTagCreated) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Tag");
        dialog.setHeaderText("Create a new tag");
        dialog.setContentText("Tag name:");
        ThemeManager.applyDarkTheme(dialog);

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                MusicLibrary.getInstance().createTag(name.trim(), "#808080");
                if (onTagCreated != null) {
                    onTagCreated.run();
                }
            }
        });
    }

    /**
     * Shows a dialog to create a new tag.
     */
    public static void showCreateTagDialog() {
        showCreateTagDialog(null);
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
