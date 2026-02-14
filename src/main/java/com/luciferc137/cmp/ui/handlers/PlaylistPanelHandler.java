package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.PlaylistManagerDialog;
import com.luciferc137.cmp.ui.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handles the playlist panel functionality including:
 * - Playlist tabs management
 * - Playlist view display (as TableView with rating column)
 * - Loading and switching between playlists
 * - Playlist CRUD operations
 */
public class PlaylistPanelHandler {

    private final LibraryService libraryService;
    private final MusicLibrary musicLibrary;
    private final PlaybackQueue playbackQueue;

    // UI Components
    private TableView<Music> playlistTable;
    private TableColumn<Music, String> playlistTitleColumn;
    private TableColumn<Music, String> playlistRatingColumn;
    private HBox playlistTabsContainer;
    private Label currentPlaylistLabel;
    private Label playlistInfoLabel;
    private Button syncScrollButton;

    // State
    private Long displayedPlaylistId = null; // null = Local, otherwise playlist ID
    private List<PlaylistEntity> availablePlaylists = new ArrayList<>();
    private final ObservableList<Music> displayedPlaylistContent = FXCollections.observableArrayList();

    // Scroll sync state
    private boolean scrollSyncEnabled = false;

    // Event listener
    private PlaylistEventListener eventListener;

    /**
     * Listener interface for playlist panel events.
     */
    public interface PlaylistEventListener {
        /**
         * Called when a track is selected from the playlist.
         * @param music The selected music
         * @param playlistId The playlist ID (null for Local)
         * @param playlistContent The content of the playlist
         * @param isFromSavedPlaylist true if the track was selected from a saved playlist (not Local)
         */
        void onPlaylistTrackSelected(Music music, Long playlistId, List<Music> playlistContent, boolean isFromSavedPlaylist);
        void onPlaylistTabsNeedRefresh();
        /**
         * Called when a context menu is requested on playlist items.
         * @param selectedMusic The list of selected music items
         * @param screenX The screen X position for the context menu
         * @param screenY The screen Y position for the context menu
         * @param playlistId The playlist ID (null for Local)
         */
        void onPlaylistContextMenuRequested(List<Music> selectedMusic, double screenX, double screenY, Long playlistId);
        /**
         * Called when a rating is changed in the playlist view.
         */
        void onRatingChanged();
    }

    public PlaylistPanelHandler() {
        this.libraryService = LibraryService.getInstance();
        this.musicLibrary = MusicLibrary.getInstance();
        this.playbackQueue = PlaybackQueue.getInstance();
    }

    /**
     * Binds UI components to this handler.
     */
    public void bindUIComponents(
            TableView<Music> playlistTable,
            TableColumn<Music, String> playlistTitleColumn,
            TableColumn<Music, String> playlistRatingColumn,
            HBox playlistTabsContainer,
            Label currentPlaylistLabel,
            Label playlistInfoLabel,
            Button syncScrollButton
    ) {
        this.playlistTable = playlistTable;
        this.playlistTitleColumn = playlistTitleColumn;
        this.playlistRatingColumn = playlistRatingColumn;
        this.playlistTabsContainer = playlistTabsContainer;
        this.currentPlaylistLabel = currentPlaylistLabel;
        this.playlistInfoLabel = playlistInfoLabel;
        this.syncScrollButton = syncScrollButton;
    }

    public void setEventListener(PlaylistEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Initializes the playlist panel.
     */
    public void initialize() {
        if (playlistTable == null) return;

        // Enable multiple selection
        playlistTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Bind playlist table to displayed content
        playlistTable.setItems(displayedPlaylistContent);

        // Setup columns
        setupTableColumns();

        // Custom row factory to highlight current track
        playlistTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Music item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    getStyleClass().removeAll("playlist-current-track", "playlist-selected-track");
                } else {
                    boolean isCurrentTrack = item.equals(playbackQueue.getCurrentTrack());
                    if (isCurrentTrack) {
                        if (!getStyleClass().contains("playlist-current-track")) {
                            getStyleClass().add("playlist-current-track");
                        }
                        getStyleClass().remove("playlist-selected-track");
                    } else {
                        getStyleClass().remove("playlist-current-track");
                    }
                }
            }
        });

        // Double-click to play from playlist
        playlistTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                Music selected = playlistTable.getSelectionModel().getSelectedItem();
                if (selected != null && eventListener != null) {
                    // Pass true if this is from a saved playlist (not Local)
                    boolean isFromSavedPlaylist = displayedPlaylistId != null;
                    eventListener.onPlaylistTrackSelected(selected, displayedPlaylistId,
                            new ArrayList<>(displayedPlaylistContent), isFromSavedPlaylist);
                }
            }
        });

        // Right-click context menu on playlist items
        playlistTable.setOnContextMenuRequested(event -> {
            List<Music> selectedItems = new ArrayList<>(playlistTable.getSelectionModel().getSelectedItems());
            if (!selectedItems.isEmpty() && eventListener != null) {
                eventListener.onPlaylistContextMenuRequested(
                        selectedItems,
                        event.getScreenX(),
                        event.getScreenY(),
                        displayedPlaylistId
                );
            }
        });

        // Update current track highlighting and scroll sync
        playbackQueue.currentTrackProperty().addListener((obs, old, newTrack) -> {
            playlistTable.refresh();
            // Scroll to current track if sync is enabled
            if (scrollSyncEnabled && newTrack != null) {
                scrollToCurrentTrack();
            }
        });

        // Update playlist info when content changes
        displayedPlaylistContent.addListener((ListChangeListener<Music>) c -> updatePlaylistInfo());

        // Sync Local view with Local playlist content changes (not the playback queue)
        playbackQueue.getLocalPlaylistContent().addListener((ListChangeListener<Music>) c -> {
            if (displayedPlaylistId == null) {
                displayedPlaylistContent.setAll(playbackQueue.getLocalPlaylistContent());
            }
        });

        // Refresh playlist tabs order when the playing playlist changes
        playbackQueue.currentPlaylistIdProperty().addListener((obs, oldId, newId) -> {
            refreshPlaylistTabs();
            // Also refresh displayed playlist order if we're viewing the new playing playlist
            refreshDisplayedPlaylist();
        });

        // Refresh displayed playlist order when shuffle is toggled
        playbackQueue.shuffleEnabledProperty().addListener((obs, wasEnabled, isEnabled) -> {
            // Refresh to show tracks in new order (shuffled or unshuffled)
            refreshDisplayedPlaylist();
        });

        // Setup sync scroll button
        setupSyncScrollButton();

        // Load tabs
        refreshPlaylistTabs();
    }

    /**
     * Sets up the sync scroll button and scroll listeners.
     */
    private void setupSyncScrollButton() {
        if (syncScrollButton == null) return;

        // Initial style (disabled)
        updateSyncButtonStyle();

        // Toggle sync on button click
        syncScrollButton.setOnAction(e -> {
            scrollSyncEnabled = !scrollSyncEnabled;
            updateSyncButtonStyle();
            if (scrollSyncEnabled) {
                scrollToCurrentTrack();
            }
        });

        // Disable sync when user manually scrolls with mouse wheel
        playlistTable.setOnScroll(event -> {
            if (scrollSyncEnabled) {
                scrollSyncEnabled = false;
                updateSyncButtonStyle();
            }
        });

        // Disable sync when user interacts with the scrollbar
        // We need to find the vertical scrollbar and add a listener
        playlistTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                // Find the vertical scrollbar
                playlistTable.lookupAll(".scroll-bar").forEach(node -> {
                    if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                        // Disable sync when user drags the scrollbar
                        scrollBar.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                            if (scrollSyncEnabled) {
                                scrollSyncEnabled = false;
                                updateSyncButtonStyle();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Updates the sync button style based on current state.
     */
    private void updateSyncButtonStyle() {
        if (syncScrollButton == null) return;

        if (scrollSyncEnabled) {
            syncScrollButton.setStyle("-fx-font-size: 12px; -fx-padding: 2; -fx-background-color: #1E90FF; -fx-text-fill: white;");
            syncScrollButton.setText("⇅");
        } else {
            syncScrollButton.setStyle("-fx-font-size: 12px; -fx-padding: 2; -fx-background-color: #3C3C3C; -fx-text-fill: #808080;");
            syncScrollButton.setText("⇅");
        }
    }

    /**
     * Scrolls the playlist table to show the current track at the top.
     */
    private void scrollToCurrentTrack() {
        Music currentTrack = playbackQueue.getCurrentTrack();
        if (currentTrack == null || playlistTable == null) return;

        int index = displayedPlaylistContent.indexOf(currentTrack);
        if (index >= 0) {
            playlistTable.scrollTo(index);
        }
    }

    /**
     * Sets up the table columns for title and rating.
     */
    private void setupTableColumns() {
        // Title column - displays title and artist
        playlistTitleColumn.setCellValueFactory(data -> {
            Music music = data.getValue();
            String display = music.title + " - " + (music.artist != null ? music.artist : "Unknown");
            return new SimpleStringProperty(display);
        });

        // Rating column with interactive stars
        playlistRatingColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRatingAsStars()));
        playlistRatingColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Music music = getTableView().getItems().get(getIndex());
                    HBox stars = createRatingStars(music);
                    setGraphic(stars);
                    setText(null);
                }
            }
        });

        // Disable default sorting behavior
        playlistTable.setSortPolicy(table -> false);
        playlistTitleColumn.setSortable(false);
        playlistRatingColumn.setSortable(false);
    }

    /**
     * Creates interactive rating stars for a music item.
     * Rating changes are synchronized with the main table.
     */
    private HBox createRatingStars(Music music) {
        HBox stars = new HBox(2);
        stars.setAlignment(Pos.CENTER_LEFT);

        for (int i = 1; i <= 5; i++) {
            final int rating = i;
            Label star = new Label(i <= music.getRating() ? "★" : "☆");
            star.setStyle("-fx-cursor: hand; -fx-font-size: 12px;");
            star.setOnMouseClicked(e -> {
                e.consume();
                int newRating = (music.getRating() == rating) ? 0 : rating;
                // Use MusicLibrary to update rating - this syncs with main table
                musicLibrary.updateRating(music, newRating);
                // Refresh playlist view
                playlistTable.refresh();
                // Notify listener to sync other views (main table)
                if (eventListener != null) {
                    eventListener.onRatingChanged();
                }
            });
            stars.getChildren().add(star);
        }

        return stars;
    }

    /**
     * Refreshes the playlist tabs.
     * Playlists are sorted alphabetically.
     */
    public void refreshPlaylistTabs() {
        if (playlistTabsContainer == null) return;

        playlistTabsContainer.getChildren().clear();
        availablePlaylists = libraryService.getAllPlaylists();

        // Add "Local" tab first
        Button localTab = createPlaylistTab("Local", null);
        playlistTabsContainer.getChildren().add(localTab);

        // Sort playlists alphabetically (case-insensitive)
        List<PlaylistEntity> sortedPlaylists = new ArrayList<>(availablePlaylists);
        sortedPlaylists.sort(Comparator.comparing(p -> p.getName().toLowerCase()));

        // Update availablePlaylists to match the sorted order (for updatePlaylistTabStyles)
        availablePlaylists = sortedPlaylists;

        // Add tabs for each playlist in sorted order
        for (PlaylistEntity playlist : sortedPlaylists) {
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
     * If the playlist being displayed is the currently playing playlist,
     * tracks are shown in playback order (respecting shuffle if enabled).
     * Otherwise, tracks are shown in their stored order.
     */
    public void loadPlaylistIntoView(Long playlistId, String name) {
        displayedPlaylistId = playlistId;

        if (currentPlaylistLabel != null) {
            currentPlaylistLabel.setText(name);
        }

        displayedPlaylistContent.clear();

        // Check if this playlist is currently playing
        long currentlyPlayingId = playbackQueue.getCurrentPlaylistId();
        boolean isCurrentlyPlayingPlaylist = (playlistId == null && currentlyPlayingId == -1) ||
                (playlistId != null && playlistId == currentlyPlayingId);

        if (isCurrentlyPlayingPlaylist && !playbackQueue.getQueue().isEmpty()) {
            // Display tracks in playback order (respects shuffle)
            displayedPlaylistContent.addAll(playbackQueue.getTracksInPlaybackOrder());
        } else if (playlistId == null) {
            // "Local" playlist not currently playing - show stored Local content
            displayedPlaylistContent.addAll(playbackQueue.getLocalPlaylistContent());
        } else {
            // Saved playlist not currently playing - load from database in stored order
            List<com.luciferc137.cmp.database.model.MusicEntity> playlistMusics =
                    libraryService.getPlaylistMusics(playlistId);

            for (var entity : playlistMusics) {
                // Try to get the Music from the central cache first
                Music music = musicLibrary.getMusicById(entity.getId());
                if (music == null) {
                    // If not in cache, create new instance (fallback)
                    music = Music.fromEntity(entity);
                    List<String> tagNames = libraryService.getMusicTagNames(entity.getId());
                    music.setTags(tagNames);
                }
                displayedPlaylistContent.add(music);
            }
        }

        updatePlaylistTabStyles();
        updatePlaylistInfo();
    }

    /**
     * Refreshes the currently displayed playlist.
     * If the displayed playlist is the currently playing one,
     * tracks are shown in playback order (respecting shuffle).
     */
    public void refreshDisplayedPlaylist() {
        // Check if the displayed playlist is currently playing
        long currentlyPlayingId = playbackQueue.getCurrentPlaylistId();
        boolean isCurrentlyPlayingPlaylist = (displayedPlaylistId == null && currentlyPlayingId == -1) ||
                (displayedPlaylistId != null && displayedPlaylistId == currentlyPlayingId);

        if (isCurrentlyPlayingPlaylist && !playbackQueue.getQueue().isEmpty()) {
            // Display tracks in playback order (respects shuffle)
            displayedPlaylistContent.setAll(playbackQueue.getTracksInPlaybackOrder());
        } else if (displayedPlaylistId == null) {
            // Local playlist not currently playing
            displayedPlaylistContent.setAll(playbackQueue.getLocalPlaylistContent());
        } else {
            // Saved playlist - reload from database
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
