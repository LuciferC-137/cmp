package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.MainApp;
import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.controllers.MainController;
import com.luciferc137.cmp.ui.dialog.PlaylistManagerDialog;
import com.luciferc137.cmp.ui.utils.ThemeManager;
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
import java.util.logging.Level;

/**
 * Handles the playlist panel functionality including:
 * - Playlist tabs management
 * - Playlist view display (as TableView with rating column)
 * - Loading and switching between playlists
 * - Playlist CRUD operations
 */
public class PlaylistPanelHandler implements Handler {

    private final LibraryService libraryService;
    private final MusicLibrary musicLibrary;
    private final PlaybackQueue playbackQueue;

    // UI Components
    private TableView<Music> playlistTable;
    private TableColumn<Music, String> playlistTitleColumn;
    private TableColumn<Music, String> playlistRatingColumn;
    private HBox playlistTabsContainer;
    private Label playlistInfoLabel;

    // State
    private Long displayedPlaylistId = null;
    private List<PlaylistEntity> availablePlaylists = new ArrayList<>();
    private final ObservableList<Music> displayedPlaylistContent = FXCollections.observableArrayList();

    // Event listener
    private PlaylistEventListener eventListener;

    /**
     * Listener interface for playlist panel events.
     */
    public interface PlaylistEventListener {
        /**
         * Called when a track is selected from a playlist.
         * @param music The selected music
         * @param playlistId The playlist ID
         * @param playlistContent The content of the playlist
         */
        void onPlaylistTrackSelected(Music music, Long playlistId, List<Music> playlistContent);
        void onPlaylistTabsNeedRefresh();
        /**
         * Called when a context menu is requested on playlist items.
         * @param selectedMusic The list of selected music items
         * @param screenX The screen X position for the context menu
         * @param screenY The screen Y position for the context menu
         * @param playlistId The playlist ID
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
            Label playlistInfoLabel
    ) {
        this.playlistTable = playlistTable;
        this.playlistTitleColumn = playlistTitleColumn;
        this.playlistRatingColumn = playlistRatingColumn;
        this.playlistTabsContainer = playlistTabsContainer;
        this.playlistInfoLabel = playlistInfoLabel;
    }

    public void setEventListener(PlaylistEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public void initialize() {
        if (playlistTable == null) return;

        // Enable multiple selection
        playlistTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Bind playlist table to displayed content
        playlistTable.setItems(displayedPlaylistContent);

        // Setup columns
        setupTableColumns();

        // Double-click to play from playlist
        playlistTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                Music selected = playlistTable.getSelectionModel().getSelectedItem();
                if (selected != null && eventListener != null) {
                    eventListener.onPlaylistTrackSelected(selected, displayedPlaylistId,
                            new ArrayList<>(displayedPlaylistContent));
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

        // Update playlist info when content changes
        displayedPlaylistContent.addListener((ListChangeListener<Music>) c -> updatePlaylistInfo());

        // Refresh displayed playlist when playback order changes (shuffle regenerated, session restored, etc.)
        // This is the main listener for keeping the playlist view in sync with playback order
        playbackQueue.playbackOrderVersionProperty().addListener((obs, oldVersion, newVersion) -> {
            // Only refresh if we're displaying the currently playing playlist
            long currentlyPlayingId = playbackQueue.playbackOrderVersionProperty().getValue();
            boolean isCurrentlyPlayingPlaylist = (displayedPlaylistId == null && currentlyPlayingId == -1) ||
                    (displayedPlaylistId != null && displayedPlaylistId == currentlyPlayingId);

            if (isCurrentlyPlayingPlaylist) {
                refreshDisplayedPlaylist();
            }
        });

        // Load tabs
        refreshPlaylistTabs();
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
            loadPlaylistIntoView(playlistId);
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
            Long tabPlaylistId = availablePlaylists.get(i).getId();

            boolean isActive = displayedPlaylistId != null && displayedPlaylistId.equals(tabPlaylistId);

            if (isActive) {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #1E90FF; -fx-text-fill: white;");
            } else {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #3C3C3C; -fx-text-fill: #B0B0B0;");
            }
        }
    }

    /**
     * Loads a playlist into the view.
     */
    public void loadPlaylistIntoView(Long playlistId) {
        if (playlistId == null) {
            MainApp.logger.log(Level.SEVERE, "Could not load playlist: playlistId is null");
            return;
        }
        displayedPlaylistId = playlistId;
        displayedPlaylistContent.clear();

        List<MusicEntity> playlistMusics =
                libraryService.getPlaylistMusics(playlistId);

        for (MusicEntity entity : playlistMusics) {
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

        updatePlaylistTabStyles();
        updatePlaylistInfo();
    }

    /**
     * Refreshes the currently displayed playlist.
     */
    public void refreshDisplayedPlaylist() {
        loadPlaylistIntoView(displayedPlaylistId);
    }

    /**
     * Returns the name of the playlist with the given ID.
     */
    public String getPlaylistName(Long playlistId) {
        return availablePlaylists.stream()
                .filter(p -> p.getId().equals(playlistId))
                .map(PlaylistEntity::getName)
                .findFirst()
                .orElse("Playlist");
    }

    private void updatePlaylistInfo() {
        if (playlistInfoLabel != null) {
            int count = displayedPlaylistContent.size();
            long totalDuration = displayedPlaylistContent.stream()
                    .mapToLong(m -> m.duration)
                    .sum();
            playlistInfoLabel.setText(count + " tracks • "
                    + MainController.formatTime(totalDuration));
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

    public List<PlaylistEntity> getAvailablePlaylists() {
        return availablePlaylists;
    }

}