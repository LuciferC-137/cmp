package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.importer.AimpPlaylistImporter;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.database.sync.SyncProgressListener;
import com.luciferc137.cmp.database.sync.SyncResult;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.settings.SettingsManager;
import com.luciferc137.cmp.ui.Coordinator;
import com.luciferc137.cmp.ui.utils.ThemeManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class SettingsHandler implements Handler {
    private ListView<String> categoryList;
    private ProgressBar syncProgressBar;
    private Label syncStatusLabel;
    private Label importStatusLabel;
    private ListView<PlaylistEntity> playlistListView;
    private ListView<TagEntity> tagListView;
    private TextField musicFolderField;
    private VBox libraryPane;
    private VBox playlistsPane;
    private VBox tagManagementPane;
    private VBox backgroundSettingsPane;
    private Button importAimpButton;
    private Button resyncButton;
    private Button browseButton;

    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private final LibraryService libraryService = LibraryService.getInstance();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();

    @Override
    public void initialize() {
        // Initialize the category list
        categoryList.getItems().addAll("Library", "Playlists", "Tags", "Background");
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

    public void bindUIComponents(
            ListView<String> categoryList,
            ProgressBar syncProgressBar,
            Label syncStatusLabel,
            Label importStatusLabel,
            ListView<PlaylistEntity> playlistListView,
            ListView<TagEntity> tagListView,
            TextField musicFolderField,
            VBox libraryPane,
            VBox playlistsPane,
            VBox tagManagementPane,
            VBox backgroundSettingsPane,
            Button importAimpButton,
            Button resyncButton,
            Button browseButton
    ) {
        this.categoryList = categoryList;
        this.syncProgressBar = syncProgressBar;
        this.syncStatusLabel = syncStatusLabel;
        this.importStatusLabel = importStatusLabel;
        this.playlistListView = playlistListView;
        this.tagListView = tagListView;
        this.musicFolderField = musicFolderField;
        this.libraryPane = libraryPane;
        this.playlistsPane = playlistsPane;
        this.tagManagementPane = tagManagementPane;
        this.backgroundSettingsPane = backgroundSettingsPane;
        this.importAimpButton = importAimpButton;
        this.resyncButton = resyncButton;
        this.browseButton = browseButton;
    }

    /**
     * Sets up the playlist list view with custom cell factory.
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

    public void refreshTagList() {
        List<TagEntity> tags = libraryService.getAllTags();
        tagListView.getItems().setAll(tags);
    }

    /**
     * Notifies that playlists have changed.
     */
    private void notifyPlaylistsChanged() {
        Coordinator.playlistPanelHandler().refreshPlaylistTabs();
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
        backgroundSettingsPane.setVisible(false);
        backgroundSettingsPane.setManaged(false);

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
        } else if ("Background".equals(category)) {
            backgroundSettingsPane.setVisible(true);
            backgroundSettingsPane.setManaged(true);
        }
    }

    public void onCreatePlaylist() {
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

    public void onDeletePlaylists() {
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
     * Opens a file chooser to import an AIMP .xspf playlist.
     */
    public void onImportAimpPlaylist() {
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

    public void onResync() {
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
}
