package com.luciferc137.cmp.ui.settings;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.settings.SettingsManager;
import com.luciferc137.cmp.ui.Coordinator;
import com.luciferc137.cmp.ui.handlers.SettingsHandler;
import javafx.event.ActionEvent;
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

    // Background settings pane
    @FXML private VBox backgroundSettingsPane;
    @FXML private ComboBox<String> renderModeComboBox;

    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private final LibraryService libraryService = LibraryService.getInstance();

    @FXML
    public void initialize() {
        Coordinator.settingsHandler().bindUIComponents(categoryList,
                syncProgressBar,
                syncStatusLabel,
                importStatusLabel,
                playlistListView,
                tagListView,
                musicFolderField,
                libraryPane,
                playlistsPane,
                tagManagementPane,
                backgroundSettingsPane,
                importAimpButton,
                resyncButton,
                browseButton
        );

        Coordinator.settingsHandler().initialize();
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

    @FXML
    private void onCreatePlaylist() {
        Coordinator.settingsHandler().onCreatePlaylist();
    }

    @FXML
    private void onDeletePlaylists() {
        Coordinator.settingsHandler().onDeletePlaylists();
    }

    /**
     * Starts the library synchronization.
     */
    @FXML
    private void onResync() {
        Coordinator.settingsHandler().onResync();
    }

    @FXML
    private void onImportAimpPlaylist() {
        Coordinator.settingsHandler().onImportAimpPlaylist();
    }

    @FXML
    public void onCreateTag(ActionEvent actionEvent) {
        SettingsHandler.showCreateTagDialog(Coordinator.settingsHandler()::refreshTagList);
    }

    @FXML
    public void onRefactorTag(ActionEvent actionEvent) {
        Coordinator.settingsHandler().onRefactorTag(actionEvent);
    }

    @FXML
    public void onDeleteTags(ActionEvent actionEvent) {
        Coordinator.settingsHandler().onDeleteTags(actionEvent);
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
