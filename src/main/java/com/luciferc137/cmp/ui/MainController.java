package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.VlcAudioPlayer;
import com.luciferc137.cmp.audio.WaveformExtractor;
import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.library.*;
import com.luciferc137.cmp.settings.PlaybackSession;
import com.luciferc137.cmp.settings.SettingsManager;
import com.luciferc137.cmp.ui.settings.SettingsWindow;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main controller for the music player UI.
 */
public class MainController {

    @FXML
    private TableView<Music> musicTable;

    @FXML
    private TableColumn<Music, String> titleColumn;

    @FXML
    private TableColumn<Music, String> artistColumn;

    @FXML
    private TableColumn<Music, String> albumColumn;

    @FXML
    private TableColumn<Music, String> durationColumn;

    @FXML
    private TableColumn<Music, String> tagsColumn;

    @FXML
    private TableColumn<Music, String> ratingColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Slider volumeSlider;

    @FXML
    private WaveformProgressBar waveformProgressBar;

    @FXML
    private Label currentTitleLabel;

    @FXML
    private Label currentArtistLabel;

    @FXML
    private Label elapsedTimeLabel;

    @FXML
    private Label totalTimeLabel;

    // Playlist panel elements
    @FXML
    private ListView<Music> playlistView;

    @FXML
    private HBox playlistTabsContainer;

    @FXML
    private ScrollPane playlistTabsScrollPane;

    @FXML
    private Label currentPlaylistLabel;

    @FXML
    private Label playlistInfoLabel;

    @FXML
    private Button shuffleButton;

    @FXML
    private Button loopButton;

    @FXML
    private Button prevButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button addPlaylistButton;

    private final VlcAudioPlayer audioPlayer = new VlcAudioPlayer();
    private final WaveformExtractor waveformExtractor = new WaveformExtractor();
    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private final PlaybackQueue playbackQueue = PlaybackQueue.getInstance();
    private final LibraryService libraryService = LibraryService.getInstance();

    private AnimationTimer progressTimer;
    private Music currentMusic;
    private Long displayedPlaylistId = null; // null = Local, otherwise playlist ID
    private List<PlaylistEntity> availablePlaylists = new ArrayList<>();
    private boolean isRestoringSession = false; // Flag to prevent saving during restore
    private ContextMenu activeContextMenu = null; // Track active context menu to close it
    private final javafx.collections.ObservableList<Music> displayedPlaylistContent =
            javafx.collections.FXCollections.observableArrayList(); // Content shown in playlist view

    // Track column sort states
    private final Map<SortableColumn, ColumnSortState> columnSortStates = new HashMap<>();

    @FXML
    public void initialize() {
        // Initialize column sort states
        for (SortableColumn col : SortableColumn.values()) {
            columnSortStates.put(col, ColumnSortState.NONE);
        }

        // Setup table columns
        setupTableColumns();

        // Enable multiple selection (Ctrl+Click, Shift+Click, Ctrl+A)
        musicTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // Bind the TableView to the MusicLibrary's observable list
        musicTable.setItems(musicLibrary.getMusicList());

        // Load music from the database
        musicLibrary.refresh();

        // Double-click to play
        musicTable.setOnMouseClicked(event -> {
            // Close context menu on any click
            if (activeContextMenu != null && activeContextMenu.isShowing()) {
                activeContextMenu.hide();
            }
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                onPlay();
            }
        });

        // Context menu for multiple selection
        musicTable.setRowFactory(tv -> {
            TableRow<Music> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    // Get all selected items
                    List<Music> selectedItems = new ArrayList<>(musicTable.getSelectionModel().getSelectedItems());
                    if (selectedItems.isEmpty()) {
                        selectedItems.add(row.getItem());
                    } else if (!selectedItems.contains(row.getItem())) {
                        // Right-clicked on unselected item, use only that one
                        selectedItems = List.of(row.getItem());
                    }
                    showMusicContextMenu(selectedItems, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        // Initialize volume from saved settings
        if (volumeSlider != null) {
            int savedVolume = settingsManager.getLastVolume();
            volumeSlider.setValue(savedVolume);
            audioPlayer.setVolume(savedVolume);

            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                    audioPlayer.setVolume(newVal.intValue()));

            volumeSlider.setOnMouseReleased(event ->
                    settingsManager.setLastVolume((int) volumeSlider.getValue()));
        }

        // Initialize timer to update progress bar
        initProgressTimer();

        // Click handler on progress bar for track navigation
        if (waveformProgressBar != null) {
            waveformProgressBar.setOnMouseClicked(this::onWaveformClicked);
        }

        // Setup playlist panel
        setupPlaylistPanel();

        // Setup track end detection
        setupTrackEndDetection();

        // Restore previous session BEFORE adding save listeners
        restoreSession();

        // Update button styles after restore
        updateShuffleButtonStyle();
        updateLoopButtonStyle();

        // NOW add listeners that save session (after restore is complete)
        playbackQueue.shuffleEnabledProperty().addListener((obs, old, enabled) -> {
            updateShuffleButtonStyle();
            if (!isRestoringSession) {
                saveSession();
            }
        });

        playbackQueue.loopModeProperty().addListener((obs, old, mode) -> {
            updateLoopButtonStyle();
            if (!isRestoringSession) {
                saveSession();
            }
        });

        // Setup window close handler to save session
        Platform.runLater(() -> {
            if (musicTable.getScene() != null && musicTable.getScene().getWindow() != null) {
                musicTable.getScene().getWindow().setOnCloseRequest(event -> {
                    saveSession();
                });
            }
        });
    }

    /**
     * Updates the shuffle button style based on state.
     */
    private void updateShuffleButtonStyle() {
        if (shuffleButton != null) {
            boolean enabled = playbackQueue.isShuffleEnabled();
            shuffleButton.setText(enabled ? "Shuf ✓" : "Shuf");
            shuffleButton.setStyle(enabled ?
                    "-fx-font-size: 11px; -fx-background-color: #4CAF50; -fx-text-fill: white;" :
                    "-fx-font-size: 11px;");
        }
    }

    /**
     * Updates the loop button style based on mode.
     */
    private void updateLoopButtonStyle() {
        if (loopButton != null) {
            PlaybackQueue.LoopMode mode = playbackQueue.getLoopMode();
            String text = switch (mode) {
                case NONE -> "Loop";
                case PLAYLIST -> "All";
                case SINGLE -> "One";
            };
            loopButton.setText(text);
            loopButton.setStyle(mode != PlaybackQueue.LoopMode.NONE ?
                    "-fx-font-size: 11px; -fx-background-color: #2196F3; -fx-text-fill: white;" :
                    "-fx-font-size: 11px;");
        }
    }

    /**
     * Sets up detection for when a track ends to auto-advance.
     */
    private void setupTrackEndDetection() {
        // Use a separate timer to check for track completion
        AnimationTimer endDetector = new AnimationTimer() {
            private boolean wasPlaying = false;
            private long lastPosition = 0;
            private int stuckCount = 0;

            @Override
            public void handle(long now) {
                if (currentMusic == null) {
                    wasPlaying = false;
                    return;
                }

                long duration = audioPlayer.getDuration();
                long position = audioPlayer.getPosition();

                // Check if track has ended (position near or at end, or stopped after playing)
                if (duration > 0 && position > 0) {
                    // Track ended: position >= duration - 500ms
                    if (position >= duration - 500) {
                        onTrackEnded();
                        return;
                    }

                    // Also detect if position is stuck at end (player stopped)
                    if (wasPlaying && !audioPlayer.isPlaying() && position > duration - 2000) {
                        stuckCount++;
                        if (stuckCount > 5) {
                            onTrackEnded();
                            stuckCount = 0;
                            return;
                        }
                    } else {
                        stuckCount = 0;
                    }
                }

                wasPlaying = audioPlayer.isPlaying();
                lastPosition = position;
            }
        };
        endDetector.start();
    }

    /**
     * Called when the current track has ended.
     */
    private void onTrackEnded() {
        Platform.runLater(() -> {
            Music next = playbackQueue.nextAuto();
            if (next != null) {
                playTrack(next);
            } else {
                // No next track, stop playback
                audioPlayer.stop();
            }
        });
    }

    /**
     * Saves the current playback session for later restoration.
     */
    private void saveSession() {
        try {
            PlaybackSession session = settingsManager.getSession();

            // Always save shuffle and loop states
            session.setShuffleEnabled(playbackQueue.isShuffleEnabled());
            session.setLoopMode(playbackQueue.getLoopMode().name());

            // Save current playlist info
            session.setPlayingPlaylistId(playbackQueue.getCurrentPlaylistId());
            session.setPlayingPlaylistName(playbackQueue.getCurrentPlaylistName());

            // Save current track info
            Music current = playbackQueue.getCurrentTrack();
            if (current != null && current.getId() != null) {
                session.setCurrentTrackId(current.getId());
                session.setCurrentTrackIndex(playbackQueue.getCurrentIndex());
                session.setPlaybackPosition(audioPlayer.getPosition());
            } else {
                session.setCurrentTrackId(-1);
                session.setCurrentTrackIndex(-1);
                session.setPlaybackPosition(0);
            }

            // Save shuffle order
            session.setShuffleOrder(playbackQueue.getShuffleOrder());
            session.setShufflePosition(playbackQueue.getShufflePosition());

            // Save queue track IDs
            session.setQueueTrackIds(playbackQueue.getQueueTrackIds());

            // Save displayed playlist
            session.setDisplayedPlaylistId(displayedPlaylistId != null ? displayedPlaylistId : -1);

            settingsManager.saveSession();
            System.out.println("Session saved: shuffle=" + session.isShuffleEnabled() +
                             ", loop=" + session.getLoopMode() +
                             ", tracks=" + session.getQueueTrackIds().size());
        } catch (Exception e) {
            System.err.println("Error saving session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restores the playback session from saved state.
     */
    private void restoreSession() {
        PlaybackSession session = settingsManager.getSession();
        isRestoringSession = true;

        try {
            System.out.println("Restoring session from file...");
            System.out.println("  Session shuffle: " + session.isShuffleEnabled());
            System.out.println("  Session loop: " + session.getLoopMode());
            System.out.println("  Session tracks: " + session.getQueueTrackIds().size());

            // Always restore loop mode (even without queue)
            try {
                PlaybackQueue.LoopMode loopMode = PlaybackQueue.LoopMode.valueOf(session.getLoopMode());
                playbackQueue.setLoopMode(loopMode);
                System.out.println("Restored loop mode: " + loopMode);
            } catch (IllegalArgumentException e) {
                playbackQueue.setLoopMode(PlaybackQueue.LoopMode.PLAYLIST);
            }

            // Always restore shuffle state (even without queue)
            playbackQueue.setShuffleEnabled(session.isShuffleEnabled());
            System.out.println("Restored shuffle: " + session.isShuffleEnabled());

            // Restore queue from track IDs
            List<Long> trackIds = session.getQueueTrackIds();
            if (!trackIds.isEmpty()) {
                List<Music> tracks = new ArrayList<>();
                for (Long trackId : trackIds) {
                    libraryService.getMusicById(trackId).ifPresent(entity -> {
                        Music music = Music.fromEntity(entity);
                        // Load tags
                        List<String> tagNames = libraryService.getMusicTagNames(trackId);
                        music.setTags(tagNames);
                        tracks.add(music);
                    });
                }

                if (!tracks.isEmpty()) {
                    playbackQueue.restoreQueue(tracks,
                            session.getPlayingPlaylistName(),
                            session.getPlayingPlaylistId());

                    // Restore shuffle order if shuffle is enabled
                    if (session.isShuffleEnabled() && !session.getShuffleOrder().isEmpty()) {
                        playbackQueue.restoreShuffleState(
                                session.getShuffleOrder(),
                                session.getShufflePosition());
                    }

                    // Restore current track
                    int trackIndex = session.getCurrentTrackIndex();
                    if (trackIndex >= 0 && trackIndex < tracks.size()) {
                        playbackQueue.restoreCurrentIndex(trackIndex);

                        // Update UI with current track info (but don't play)
                        Music current = playbackQueue.getCurrentTrack();
                        if (current != null) {
                            currentMusic = current;
                            updateCurrentSongLabels(current);
                            loadWaveform(current);
                        }
                    }

                    System.out.println("Restored queue with " + tracks.size() + " tracks, current index: " + trackIndex);
                }
            }

            // Restore displayed playlist tab
            long displayedId = session.getDisplayedPlaylistId();
            displayedPlaylistId = displayedId == -1 ? null : displayedId;
            updatePlaylistTabStyles();

            // Load the displayed playlist content
            if (displayedPlaylistId == null) {
                // For Local, sync with the queue we just restored
                if (currentPlaylistLabel != null) {
                    currentPlaylistLabel.setText("Local");
                }
                displayedPlaylistContent.setAll(playbackQueue.getQueue());
            } else {
                // For saved playlists, load from database
                String playlistName = availablePlaylists.stream()
                        .filter(p -> p.getId().equals(displayedPlaylistId))
                        .map(PlaylistEntity::getName)
                        .findFirst().orElse("Playlist");
                loadPlaylistIntoView(displayedPlaylistId, playlistName);
            }

            // Update playlist info
            updatePlaylistInfo();

        } catch (Exception e) {
            System.err.println("Error restoring session: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isRestoringSession = false;
        }
    }

    /**
     * Sets up the playlist panel with tabs and list view.
     */
    private void setupPlaylistPanel() {
        // Bind playlist view to the displayed playlist content (not the playback queue)
        if (playlistView != null) {
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
                        // Highlight current track if it's in the currently playing queue
                        if (item.equals(playbackQueue.getCurrentTrack())) {
                            setStyle("-fx-font-weight: bold; -fx-background-color: #e3f2fd;");
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
                    if (selected != null) {
                        // If viewing Local, set queue from displayedPlaylistContent
                        // If viewing a saved playlist, load that playlist into queue and play
                        if (displayedPlaylistId == null) {
                            // Local - use displayed content as queue
                            playbackQueue.setLocalQueue(new ArrayList<>(displayedPlaylistContent), selected);
                        } else {
                            // Saved playlist - load and play
                            PlaylistEntity playlist = availablePlaylists.stream()
                                    .filter(p -> p.getId().equals(displayedPlaylistId))
                                    .findFirst().orElse(null);
                            if (playlist != null) {
                                playbackQueue.loadPlaylist(playlist.getId(), playlist.getName(),
                                        new ArrayList<>(displayedPlaylistContent));
                            }
                            playbackQueue.playTrack(selected);
                        }
                        playTrack(selected);
                        updatePlaylistTabStyles();
                    }
                }
            });

            // Update current track listener - refresh the view to update highlighting
            playbackQueue.currentTrackProperty().addListener((obs, old, newTrack) -> {
                playlistView.refresh();
            });
        }

        // Update playlist info label when displayed content changes
        displayedPlaylistContent.addListener((javafx.collections.ListChangeListener<Music>) c -> {
            updatePlaylistInfo();
        });

        // Sync Local view with playback queue changes
        playbackQueue.getQueue().addListener((javafx.collections.ListChangeListener<Music>) c -> {
            if (displayedPlaylistId == null) {
                // We're viewing Local, sync the displayed content
                displayedPlaylistContent.setAll(playbackQueue.getQueue());
            }
        });

        // Load available playlists and create tabs
        refreshPlaylistTabs();

        // Note: The actual playlist content will be loaded by restoreSession()
        // or set to Local by default if no session exists
    }

    /**
     * Refreshes the playlist tabs based on available playlists.
     */
    private void refreshPlaylistTabs() {
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

    /**
     * Creates a playlist tab button.
     */
    private Button createPlaylistTab(String name, Long playlistId) {
        Button tab = new Button(name);
        tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        tab.setOnAction(e -> {
            displayedPlaylistId = playlistId;
            loadPlaylistIntoView(playlistId, name);
            updatePlaylistTabStyles();
        });

        // Context menu for playlist management
        if (playlistId != null) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete Playlist");
            deleteItem.setOnAction(e -> {
                deletePlaylist(playlistId, name);
            });
            contextMenu.getItems().add(deleteItem);
            tab.setContextMenu(contextMenu);
        }

        return tab;
    }

    /**
     * Updates the styles of playlist tabs to highlight the active one.
     */
    private void updatePlaylistTabStyles() {
        for (int i = 0; i < playlistTabsContainer.getChildren().size(); i++) {
            Button tab = (Button) playlistTabsContainer.getChildren().get(i);
            Long tabPlaylistId = (i == 0) ? null : availablePlaylists.get(i - 1).getId();

            boolean isActive = (displayedPlaylistId == null && tabPlaylistId == null) ||
                              (displayedPlaylistId != null && displayedPlaylistId.equals(tabPlaylistId));

            boolean isPlaying = playbackQueue.getCurrentPlaylistId() == (tabPlaylistId != null ? tabPlaylistId : -1);

            if (isActive) {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #2196F3; -fx-text-fill: white;");
            } else if (isPlaying) {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #81C784;");
            } else {
                tab.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
            }
        }
    }

    /**
     * Loads a playlist into the playlist view (for display).
     */
    private void loadPlaylistIntoView(Long playlistId, String name) {
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
                // Load tags for each music
                List<String> tagNames = libraryService.getMusicTagNames(entity.getId());
                music.setTags(tagNames);
                displayedPlaylistContent.add(music);
            }
        }

        updatePlaylistTabStyles();
        updatePlaylistInfo();
    }

    /**
     * Refreshes the currently displayed playlist view.
     */
    private void refreshDisplayedPlaylist() {
        if (displayedPlaylistId == null) {
            // Local - sync with queue
            displayedPlaylistContent.setAll(playbackQueue.getQueue());
        } else {
            // Reload from database
            String name = currentPlaylistLabel != null ? currentPlaylistLabel.getText() : "Playlist";
            loadPlaylistIntoView(displayedPlaylistId, name);
        }
    }

    /**
     * Updates the playlist info label.
     */
    private void updatePlaylistInfo() {
        if (playlistInfoLabel != null) {
            int count = displayedPlaylistContent.size();
            long totalDuration = displayedPlaylistContent.stream()
                    .mapToLong(m -> m.duration)
                    .sum();
            playlistInfoLabel.setText(count + " tracks • " + formatTime(totalDuration));
        }
    }

    /**
     * Deletes a playlist.
     */
    private void deletePlaylist(Long playlistId, String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Playlist");
        confirm.setHeaderText("Delete \"" + name + "\"?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                libraryService.deletePlaylist(playlistId);
                refreshPlaylistTabs();
            }
        });
    }

    /**
     * Sets up the table columns with cell factories and click handlers.
     */
    private void setupTableColumns() {
        // Title column
        titleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().title != null ? data.getValue().title : ""));
        setupSortableColumn(titleColumn, SortableColumn.TITLE);

        // Artist column
        artistColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().artist != null ? data.getValue().artist : ""));
        setupSortableColumn(artistColumn, SortableColumn.ARTIST);

        // Album column
        albumColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().album != null ? data.getValue().album : ""));
        setupSortableColumn(albumColumn, SortableColumn.ALBUM);

        // Duration column
        durationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFormattedDuration()));
        setupSortableColumn(durationColumn, SortableColumn.DURATION);

        // Tags column - shows comma-separated tags, click opens filter popup
        tagsColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTagsAsString()));
        tagsColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setWrapText(false);
                }
            }
        });
        // Click on header opens tag filter popup
        tagsColumn.setGraphic(createFilterableHeader("Tags", this::showTagFilterPopup));
        tagsColumn.setText("");
        tagsColumn.setSortable(false);

        // Rating column - shows stars, click cycles rating or opens filter
        ratingColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRatingAsStars()));
        ratingColumn.setCellFactory(col -> new TableCell<>() {
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
        // Click on header opens rating filter popup
        ratingColumn.setGraphic(createFilterableHeader("Rating", this::showRatingFilterPopup));
        ratingColumn.setText("");
        ratingColumn.setSortable(false);

        // Disable default sorting behavior (we handle it manually)
        musicTable.setSortPolicy(table -> false);
    }

    /**
     * Sets up a sortable column with click handler.
     */
    private void setupSortableColumn(TableColumn<Music, String> column, SortableColumn sortCol) {
        column.setSortable(false); // Disable default sort

        // Create header label with sort indicator
        Label header = new Label(sortCol.getDisplayName());
        header.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onColumnHeaderClicked(sortCol);
                updateColumnHeaders();
            }
        });
        column.setGraphic(header);
        column.setText("");
    }

    /**
     * Creates a filterable header with a label and filter icon.
     */
    private Label createFilterableHeader(String text, Runnable onFilterClick) {
        Label header = new Label(text + " ▼");
        header.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onFilterClick.run();
            }
        });
        return header;
    }

    /**
     * Updates column headers to show sort indicators.
     */
    private void updateColumnHeaders() {
        AdvancedFilter filter = musicLibrary.getAdvancedFilter();
        SortableColumn activeCol = filter.getSortColumn();
        ColumnSortState activeState = filter.getSortState();

        updateSortableColumnHeader(titleColumn, SortableColumn.TITLE, activeCol, activeState);
        updateSortableColumnHeader(artistColumn, SortableColumn.ARTIST, activeCol, activeState);
        updateSortableColumnHeader(albumColumn, SortableColumn.ALBUM, activeCol, activeState);
        updateSortableColumnHeader(durationColumn, SortableColumn.DURATION, activeCol, activeState);
    }

    private void updateSortableColumnHeader(TableColumn<Music, String> column, SortableColumn sortCol,
                                             SortableColumn activeCol, ColumnSortState activeState) {
        String text = sortCol.getDisplayName();
        if (sortCol == activeCol && activeState != ColumnSortState.NONE) {
            text += activeState.getSymbol();
        }
        ((Label) column.getGraphic()).setText(text);
    }

    /**
     * Handles column header click for sorting.
     */
    private void onColumnHeaderClicked(SortableColumn column) {
        musicLibrary.cycleSort(column);
    }

    /**
     * Creates interactive rating stars for a music item.
     */
    private HBox createRatingStars(Music music) {
        HBox stars = new HBox(2);
        stars.setAlignment(Pos.CENTER_LEFT);

        for (int i = 1; i <= 5; i++) {
            final int rating = i;
            Label star = new Label(i <= music.getRating() ? "★" : "☆");
            star.setStyle("-fx-cursor: hand; -fx-font-size: 14px;");
            star.setOnMouseClicked(e -> {
                e.consume();
                int newRating = (music.getRating() == rating) ? 0 : rating;
                musicLibrary.updateRating(music, newRating);
                musicTable.refresh();
            });
            stars.getChildren().add(star);
        }

        return stars;
    }

    /**
     * Shows the tag filter popup.
     */
    private void showTagFilterPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(5);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5;");

        Label title = new Label("Filter by Tags");
        title.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(title);

        ObservableList<TagEntity> tags = musicLibrary.getAvailableTags();
        AdvancedFilter filter = musicLibrary.getAdvancedFilter();

        if (tags.isEmpty()) {
            content.getChildren().add(new Label("No tags available"));
        } else {
            for (TagEntity tag : tags) {
                HBox row = createTagFilterRow(tag, filter);
                content.getChildren().add(row);
            }
        }

        // Add new tag button
        Button addTagBtn = new Button("+ New Tag");
        addTagBtn.setOnAction(e -> {
            popup.hide();
            showCreateTagDialog();
        });
        content.getChildren().add(new Separator());
        content.getChildren().add(addTagBtn);

        // Clear filters button
        if (filter.hasActiveTagFilters()) {
            Button clearBtn = new Button("Clear Tag Filters");
            clearBtn.setOnAction(e -> {
                filter.getActiveTagFilters().keySet().forEach(id ->
                        filter.setTagFilterState(id, TagFilterState.IRRELEVANT));
                musicLibrary.applyFilterAndSort();
                popup.hide();
            });
            content.getChildren().add(clearBtn);
        }

        popup.getContent().add(content);

        // Show popup near the tags column header
        popup.show(tagsColumn.getGraphic(),
                tagsColumn.getGraphic().getScene().getWindow().getX() + 400,
                tagsColumn.getGraphic().getScene().getWindow().getY() + 100);
    }

    /**
     * Creates a row for a tag filter with tri-state checkbox.
     */
    private HBox createTagFilterRow(TagEntity tag, AdvancedFilter filter) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TagFilterState state = filter.getTagFilterState(tag.getId());
        Label stateLabel = new Label(state.getSymbol());
        stateLabel.setStyle("-fx-font-size: 14px; -fx-min-width: 20px;");

        Label nameLabel = new Label(tag.getName());

        row.getChildren().addAll(stateLabel, nameLabel);
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> {
            TagFilterState newState = musicLibrary.cycleTagFilter(tag.getId());
            stateLabel.setText(newState.getSymbol());
        });

        return row;
    }

    /**
     * Shows the rating filter popup.
     */
    private void showRatingFilterPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(5);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5;");

        Label title = new Label("Filter by Rating");
        title.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(title);

        AdvancedFilter filter = musicLibrary.getAdvancedFilter();

        for (int rating = 5; rating >= 0; rating--) {
            HBox row = createRatingFilterRow(rating, filter);
            content.getChildren().add(row);
        }

        // Clear filters button
        if (filter.hasActiveRatingFilters()) {
            content.getChildren().add(new Separator());
            Button clearBtn = new Button("Clear Rating Filters");
            clearBtn.setOnAction(e -> {
                filter.getActiveRatingFilters().keySet().forEach(r ->
                        filter.setRatingFilterState(r, TagFilterState.IRRELEVANT));
                musicLibrary.applyFilterAndSort();
                popup.hide();
            });
            content.getChildren().add(clearBtn);
        }

        popup.getContent().add(content);

        popup.show(ratingColumn.getGraphic(),
                ratingColumn.getGraphic().getScene().getWindow().getX() + 600,
                ratingColumn.getGraphic().getScene().getWindow().getY() + 100);
    }

    /**
     * Creates a row for a rating filter with tri-state toggle.
     */
    private HBox createRatingFilterRow(int rating, AdvancedFilter filter) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TagFilterState state = filter.getRatingFilterState(rating);
        Label stateLabel = new Label(state.getSymbol());
        stateLabel.setStyle("-fx-font-size: 14px; -fx-min-width: 20px;");

        String stars = rating == 0 ? "No rating" : "★".repeat(rating) + "☆".repeat(5 - rating);
        Label ratingLabel = new Label(stars);

        row.getChildren().addAll(stateLabel, ratingLabel);
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> {
            TagFilterState newState = musicLibrary.cycleRatingFilter(rating);
            stateLabel.setText(newState.getSymbol());
        });

        return row;
    }

    /**
     * Shows a dialog to create a new tag.
     */
    private void showCreateTagDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Tag");
        dialog.setHeaderText("Create a new tag");
        dialog.setContentText("Tag name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                musicLibrary.createTag(name.trim(), "#808080");
            }
        });
    }

    /**
     * Shows context menu for one or more music items.
     */
    private void showMusicContextMenu(List<Music> selectedMusic, double screenX, double screenY) {
        if (selectedMusic.isEmpty()) return;

        // Close any previously open context menu
        if (activeContextMenu != null) {
            activeContextMenu.hide();
        }

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true); // Auto-hide when clicking elsewhere
        activeContextMenu = contextMenu;

        boolean isMultiple = selectedMusic.size() > 1;
        String itemsLabel = isMultiple ? selectedMusic.size() + " tracks" : "\"" + selectedMusic.get(0).title + "\"";

        // Add tags submenu (only for single selection)
        if (!isMultiple) {
            Music music = selectedMusic.getFirst();
            Menu addTagMenu = new Menu("Add Tag");
            ObservableList<TagEntity> availableTags = musicLibrary.getAvailableTags();

            if (availableTags.isEmpty()) {
                MenuItem noTags = new MenuItem("No tags available");
                noTags.setDisable(true);
                addTagMenu.getItems().add(noTags);
            } else {
                for (TagEntity tag : availableTags) {
                    CheckMenuItem tagItem = new CheckMenuItem(tag.getName());
                    tagItem.setSelected(music.getTags().contains(tag.getName()));
                    tagItem.setOnAction(e -> {
                        if (tagItem.isSelected()) {
                            musicLibrary.addTagToMusic(music, tag);
                        } else {
                            musicLibrary.removeTagFromMusic(music, tag);
                        }
                        musicTable.refresh();
                    });
                    addTagMenu.getItems().add(tagItem);
                }
            }

            // Create new tag option
            addTagMenu.getItems().add(new SeparatorMenuItem());
            MenuItem createTag = new MenuItem("+ Create New Tag...");
            createTag.setOnAction(e -> showCreateTagDialog());
            addTagMenu.getItems().add(createTag);

            contextMenu.getItems().add(addTagMenu);

            // Rating submenu (only for single selection)
            Menu ratingMenu = new Menu("Set Rating");
            for (int i = 0; i <= 5; i++) {
                final int rating = i;
                String label = i == 0 ? "No rating" : "★".repeat(i) + "☆".repeat(5 - rating);
                CheckMenuItem ratingItem = new CheckMenuItem(label);
                ratingItem.setSelected(music.getRating() == i);
                ratingItem.setOnAction(e -> {
                    musicLibrary.updateRating(music, rating);
                    musicTable.refresh();
                });
                ratingMenu.getItems().add(ratingItem);
            }
            contextMenu.getItems().add(ratingMenu);
        } else {
            // For multiple selection, add batch tag menu
            Menu addTagMenu = new Menu("Add Tag to All");
            ObservableList<TagEntity> availableTags = musicLibrary.getAvailableTags();

            if (availableTags.isEmpty()) {
                MenuItem noTags = new MenuItem("No tags available");
                noTags.setDisable(true);
                addTagMenu.getItems().add(noTags);
            } else {
                for (TagEntity tag : availableTags) {
                    MenuItem tagItem = new MenuItem(tag.getName());
                    tagItem.setOnAction(e -> {
                        for (Music music : selectedMusic) {
                            musicLibrary.addTagToMusic(music, tag);
                        }
                        musicTable.refresh();
                    });
                    addTagMenu.getItems().add(tagItem);
                }
            }
            contextMenu.getItems().add(addTagMenu);
        }

        // Play option (first selected track)
        contextMenu.getItems().add(new SeparatorMenuItem());
        MenuItem playItem = new MenuItem(isMultiple ? "Play First" : "Play");
        playItem.setOnAction(e -> {
            musicTable.getSelectionModel().clearSelection();
            musicTable.getSelectionModel().select(selectedMusic.get(0));
            onPlay();
        });
        contextMenu.getItems().add(playItem);

        // Add to current queue option
        MenuItem addToQueueItem = new MenuItem(isMultiple ? "Add All to Current Queue" : "Add to Current Queue");
        addToQueueItem.setOnAction(e -> {
            for (Music music : selectedMusic) {
                playbackQueue.addToQueue(music);
            }
        });
        contextMenu.getItems().add(addToQueueItem);

        // Add to playlist submenu
        Menu addToPlaylistMenu = new Menu(isMultiple ? "Add All to Playlist" : "Add to Playlist");
        List<PlaylistEntity> playlists = libraryService.getAllPlaylists();

        if (playlists.isEmpty()) {
            MenuItem noPlaylists = new MenuItem("No playlists available");
            noPlaylists.setDisable(true);
            addToPlaylistMenu.getItems().add(noPlaylists);
        } else {
            for (PlaylistEntity playlist : playlists) {
                MenuItem playlistItem = new MenuItem(playlist.getName());
                playlistItem.setOnAction(e -> {
                    addMusicsToPlaylist(selectedMusic, playlist);
                });
                addToPlaylistMenu.getItems().add(playlistItem);
            }
        }

        addToPlaylistMenu.getItems().add(new SeparatorMenuItem());
        MenuItem createPlaylistItem = new MenuItem("+ New Playlist...");
        createPlaylistItem.setOnAction(e -> {
            PlaylistManagerDialog.showCreatePlaylistDialog(() -> {})
                    .ifPresent(newPlaylist -> {
                        addMusicsToPlaylist(selectedMusic, newPlaylist);
                        refreshPlaylistTabs();
                    });
        });
        addToPlaylistMenu.getItems().add(createPlaylistItem);

        contextMenu.getItems().add(addToPlaylistMenu);

        contextMenu.show(musicTable, screenX, screenY);
    }

    /**
     * Adds multiple music tracks to a playlist.
     */
    private void addMusicsToPlaylist(List<Music> musics, PlaylistEntity playlist) {
        if (playlist.getId() == null) return;

        int addedCount = 0;
        for (Music music : musics) {
            if (music.getId() != null) {
                libraryService.addMusicToPlaylist(playlist.getId(), music.getId());
                addedCount++;
            }
        }

        // Refresh the view if we added to the currently displayed playlist
        if (displayedPlaylistId != null && displayedPlaylistId.equals(playlist.getId())) {
            refreshDisplayedPlaylist();
        }

        // Show confirmation
        final int count = addedCount;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Added to Playlist");
        alert.setHeaderText(null);
        alert.setContentText(count + " track(s) added to \"" + playlist.getName() + "\"");
        alert.show();

        // Auto-hide after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(alert::close);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    /**
     * Adds a single music track to a playlist.
     */
    private void addMusicToPlaylist(Music music, PlaylistEntity playlist) {
        addMusicsToPlaylist(List.of(music), playlist);
    }

    /**
     * Initializes the AnimationTimer to update progress.
     */
    private void initProgressTimer() {
        progressTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 33_000_000) {
                    updateProgress();
                    lastUpdate = now;
                }
            }
        };
        progressTimer.start();
    }

    /**
     * Updates the progress bar and time labels based on playback position.
     */
    private void updateProgress() {
        long duration = audioPlayer.getDuration();
        long position = audioPlayer.getPosition();

        // Update progress bar
        if (waveformProgressBar != null && audioPlayer.isPlaying()) {
            if (duration > 0) {
                double progress = (double) position / duration;
                waveformProgressBar.setProgress(progress);
            }
        }

        // Update time labels
        if (elapsedTimeLabel != null) {
            elapsedTimeLabel.setText(formatTime(position));
        }
        if (totalTimeLabel != null && duration > 0) {
            totalTimeLabel.setText(formatTime(duration));
        }
    }

    /**
     * Formats time in milliseconds to MM:SS or HH:MM:SS.
     */
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

    /**
     * Click handler on waveform bar for navigation.
     */
    private void onWaveformClicked(MouseEvent event) {
        if (currentMusic == null) return;

        double clickX = event.getX();
        double width = waveformProgressBar.getWidth();
        double progress = clickX / width;

        long duration = audioPlayer.getDuration();
        if (duration > 0) {
            long seekPosition = (long) (duration * progress);
            audioPlayer.seek(seekPosition);
            waveformProgressBar.setProgress(progress);
        }
    }

    /**
     * Loads and displays the waveform of an audio file.
     */
    private void loadWaveform(Music music) {
        if (waveformProgressBar == null) return;

        waveformProgressBar.clear();

        int numSamples = 200;

        waveformExtractor.extractAsync(music.filePath, numSamples)
                .thenAccept(data -> Platform.runLater(() ->
                        waveformProgressBar.setWaveformData(data)));
    }

    /**
     * Updates the current song information labels.
     */
    private void updateCurrentSongLabels(Music music) {
        if (currentTitleLabel != null) {
            currentTitleLabel.setText(music.title != null ? music.title : "Unknown Title");
        }
        if (currentArtistLabel != null) {
            String artistAlbum = "";
            if (music.artist != null && !music.artist.isEmpty()) {
                artistAlbum = music.artist;
            }
            if (music.album != null && !music.album.isEmpty()) {
                if (!artistAlbum.isEmpty()) {
                    artistAlbum += " — ";
                }
                artistAlbum += music.album;
            }
            currentArtistLabel.setText(artistAlbum);
        }
        if (totalTimeLabel != null) {
            totalTimeLabel.setText(music.getFormattedDuration());
        }
        if (elapsedTimeLabel != null) {
            elapsedTimeLabel.setText("0:00");
        }
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText();
        musicLibrary.search(query);
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
        musicLibrary.clearSearch();
    }

    @FXML
    private void onPlay() {
        Music selected = musicTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            // If nothing selected in table, try to resume current track
            if (currentMusic != null) {
                audioPlayer.resume();
            }
            return;
        }

        // When playing from table, set up "Local" playlist with current filtered content
        List<Music> currentTableContent = new ArrayList<>(musicLibrary.getMusicList());
        playbackQueue.setLocalQueue(currentTableContent, selected);

        playTrack(selected);
        updatePlaylistTabStyles();
        saveSession();
    }

    /**
     * Plays a specific track (from table or playlist).
     */
    private void playTrack(Music music) {
        if (music == null) return;

        if (currentMusic == null || !currentMusic.equals(music)) {
            currentMusic = music;
            loadWaveform(music);
            updateCurrentSongLabels(music);
        }

        audioPlayer.play(music);
        if (waveformProgressBar != null) {
            waveformProgressBar.setProgress(0);
        }

        // Save session when track changes
        saveSession();
    }

    @FXML
    private void onPause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
        } else {
            audioPlayer.resume();
        }
    }

    @FXML
    private void onStop() {
        audioPlayer.stop();
    }

    @FXML
    private void onPrevious() {
        Music prev = playbackQueue.previous();
        if (prev != null) {
            playTrack(prev);
        }
    }

    @FXML
    private void onNext() {
        Music next = playbackQueue.next();
        if (next != null) {
            playTrack(next);
        }
    }

    @FXML
    private void onShuffle() {
        playbackQueue.toggleShuffle();
    }

    @FXML
    private void onToggleLoop() {
        playbackQueue.cycleLoopMode();
    }

    @FXML
    private void onAddPlaylist() {
        PlaylistManagerDialog.showCreatePlaylistDialog(this::refreshPlaylistTabs)
                .ifPresent(playlist -> {
                    refreshPlaylistTabs();
                });
    }

    @FXML
    private void onSettings() {
        SettingsWindow.show(musicTable.getScene().getWindow());
    }
}
