package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.PlaylistManagerDialog;
import com.luciferc137.cmp.ui.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the playlist panel functionality including:
 * - Playlist tabs management
 * - Playlist view display
 * - Loading and switching between playlists
 * - Playlist CRUD operations
 */
public class PlaylistPanelHandler {

    private final LibraryService libraryService;
    private final PlaybackQueue playbackQueue;

    // UI Components
    private ListView<Music> playlistView;
    private HBox playlistTabsContainer;
    private Label currentPlaylistLabel;
    private Label playlistInfoLabel;

    // State
    private Long displayedPlaylistId = null; // null = Local, otherwise playlist ID
    private List<PlaylistEntity> availablePlaylists = new ArrayList<>();
    private final ObservableList<Music> displayedPlaylistContent = FXCollections.observableArrayList();

    // Event listener
    private PlaylistEventListener eventListener;

    /**
     * Listener interface for playlist panel events.
     */
    public interface PlaylistEventListener {
        void onPlaylistTrackSelected(Music music, Long playlistId, List<Music> playlistContent);
        void onPlaylistTabsNeedRefresh();
    }

    public PlaylistPanelHandler() {
        this.libraryService = LibraryService.getInstance();
        this.playbackQueue = PlaybackQueue.getInstance();
    }

    /**
     * Binds UI components to this handler.
     */
    public void bindUIComponents(
            ListView<Music> playlistView,
            HBox playlistTabsContainer,
            Label currentPlaylistLabel,
            Label playlistInfoLabel
    ) {
        this.playlistView = playlistView;
        this.playlistTabsContainer = playlistTabsContainer;
        this.currentPlaylistLabel = currentPlaylistLabel;
        this.playlistInfoLabel = playlistInfoLabel;
    }

    public void setEventListener(PlaylistEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Initializes the playlist panel.
     */
    public void initialize() {
        if (playlistView == null) return;

        // Bind playlist view to displayed content
        playlistView.setItems(displayedPlaylistContent);

        // Custom cell factory to highlight current track
        playlistView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Music item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.title + " - " + (item.artist != null ? item.artist : "Unknown"));
                    if (item.equals(playbackQueue.getCurrentTrack())) {
                        setStyle("-fx-font-weight: bold; -fx-background-color: #1E90FF; -fx-text-fill: white;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Double-click to play from playlist
        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                Music selected = playlistView.getSelectionModel().getSelectedItem();
                if (selected != null && eventListener != null) {
                    eventListener.onPlaylistTrackSelected(selected, displayedPlaylistId,
                            new ArrayList<>(displayedPlaylistContent));
                }
            }
        });

        // Update current track highlighting
        playbackQueue.currentTrackProperty().addListener((obs, old, newTrack) -> {
            playlistView.refresh();
        });

        // Update playlist info when content changes
        displayedPlaylistContent.addListener((ListChangeListener<Music>) c -> updatePlaylistInfo());

        // Sync Local view with playback queue changes
        playbackQueue.getQueue().addListener((ListChangeListener<Music>) c -> {
            if (displayedPlaylistId == null) {
                displayedPlaylistContent.setAll(playbackQueue.getQueue());
            }
        });

        // Load tabs
        refreshPlaylistTabs();
    }

    /**
     * Refreshes the playlist tabs.
     */
    public void refreshPlaylistTabs() {
        if (playlistTabsContainer == null) return;

        playlistTabsContainer.getChildren().clear();
        availablePlaylists = libraryService.getAllPlaylists();

        // Add "Local" tab first
        Button localTab = createPlaylistTab("Local", null);
        playlistTabsContainer.getChildren().add(localTab);

        // Add tabs for each playlist
        for (PlaylistEntity playlist : availablePlaylists) {
            Button tab = createPlaylistTab(playlist.getName(), playlist.getId());
            playlistTabsContainer.getChildren().add(tab);
        }

        updatePlaylistTabStyles();
    }

    private Button createPlaylistTab(String name, Long playlistId) {
        Button tab = new Button(name);
        tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #3C3C3C; -fx-text-fill: #B0B0B0;");
        tab.setOnAction(e -> {
            displayedPlaylistId = playlistId;
            loadPlaylistIntoView(playlistId, name);
            updatePlaylistTabStyles();
        });

        // Context menu for playlist management
        if (playlistId != null) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Playlist");
            deleteItem.setOnAction(e -> deletePlaylist(playlistId, name));
            contextMenu.getItems().add(deleteItem);
            tab.setContextMenu(contextMenu);
        }

        return tab;
    }

    /**
     * Updates tab styles to highlight active and playing playlists.
     */
    public void updatePlaylistTabStyles() {
        if (playlistTabsContainer == null) return;

        for (int i = 0; i < playlistTabsContainer.getChildren().size(); i++) {
            Button tab = (Button) playlistTabsContainer.getChildren().get(i);
            Long tabPlaylistId = (i == 0) ? null : availablePlaylists.get(i - 1).getId();

            boolean isActive = (displayedPlaylistId == null && tabPlaylistId == null) ||
                    (displayedPlaylistId != null && displayedPlaylistId.equals(tabPlaylistId));

            boolean isPlaying = playbackQueue.getCurrentPlaylistId() == (tabPlaylistId != null ? tabPlaylistId : -1);

            if (isActive) {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #1E90FF; -fx-text-fill: white;");
            } else if (isPlaying) {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #2E7D32; -fx-text-fill: white;");
            } else {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #3C3C3C; -fx-text-fill: #B0B0B0;");
            }
        }
    }

    /**
     * Loads a playlist into the view.
     */
    public void loadPlaylistIntoView(Long playlistId, String name) {
        displayedPlaylistId = playlistId;

        if (currentPlaylistLabel != null) {
            currentPlaylistLabel.setText(name);
        }

        displayedPlaylistContent.clear();

        if (playlistId == null) {
            // "Local" playlist - show the current playback queue
            displayedPlaylistContent.addAll(playbackQueue.getQueue());
        } else {
            // Saved playlist - load from database
            List<com.luciferc137.cmp.database.model.MusicEntity> playlistMusics =
                    libraryService.getPlaylistMusics(playlistId);

            for (var entity : playlistMusics) {
                Music music = Music.fromEntity(entity);
                List<String> tagNames = libraryService.getMusicTagNames(entity.getId());
                music.setTags(tagNames);
                displayedPlaylistContent.add(music);
            }
        }

        updatePlaylistTabStyles();
        updatePlaylistInfo();
    }

    /**
     * Refreshes the currently displayed playlist.
     */
    public void refreshDisplayedPlaylist() {
        if (displayedPlaylistId == null) {
            displayedPlaylistContent.setAll(playbackQueue.getQueue());
        } else {
            String name = currentPlaylistLabel != null ? currentPlaylistLabel.getText() : "Playlist";
            loadPlaylistIntoView(displayedPlaylistId, name);
        }
    }

    private void updatePlaylistInfo() {
        if (playlistInfoLabel != null) {
            int count = displayedPlaylistContent.size();
            long totalDuration = displayedPlaylistContent.stream()
                    .mapToLong(m -> m.duration)
                    .sum();
            playlistInfoLabel.setText(count + " tracks • " + formatTime(totalDuration));
        }
    }

    private void deletePlaylist(Long playlistId, String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Playlist");
        confirm.setHeaderText("Delete \"" + name + "\"?");
        confirm.setContentText("This action cannot be undone.");
        ThemeManager.applyDarkTheme(confirm);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                libraryService.deletePlaylist(playlistId);
                refreshPlaylistTabs();
            }
        });
    }

    /**
     * Shows the create playlist dialog.
     */
    public void showCreatePlaylistDialog() {
        PlaylistManagerDialog.showCreatePlaylistDialog(this::refreshPlaylistTabs)
                .ifPresent(playlist -> refreshPlaylistTabs());
    }

    // ==================== Session Persistence Support ====================

    public Long getDisplayedPlaylistId() {
        return displayedPlaylistId;
    }

    public void setDisplayedPlaylistId(Long playlistId) {
        this.displayedPlaylistId = playlistId;
    }

    public ObservableList<Music> getDisplayedPlaylistContent() {
        return displayedPlaylistContent;
    }

    public List<PlaylistEntity> getAvailablePlaylists() {
        return availablePlaylists;
    }

    // ==================== Utilities ====================

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}

