package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.VlcAudioPlayer;
import com.luciferc137.cmp.audio.WaveformExtractor;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.library.*;
import com.luciferc137.cmp.ui.handlers.*;
import com.luciferc137.cmp.ui.settings.SettingsWindow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Main controller for the music player UI.
 * Acts as a coordinator between specialized handlers and FXML components.
 */
public class MainController {

    // ==================== FXML Components ====================

    @FXML private TableView<Music> musicTable;
    @FXML private TableColumn<Music, String> titleColumn;
    @FXML private TableColumn<Music, String> artistColumn;
    @FXML private TableColumn<Music, String> albumColumn;
    @FXML private TableColumn<Music, String> durationColumn;
    @FXML private TableColumn<Music, String> tagsColumn;
    @FXML private TableColumn<Music, String> ratingColumn;

    @FXML private TextField searchField;
    @FXML private Slider volumeSlider;
    @FXML private WaveformProgressBar waveformProgressBar;

    @FXML private Label currentTitleLabel;
    @FXML private Label currentArtistLabel;
    @FXML private Label elapsedTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private ImageView currentCoverArt;

    @FXML private TableView<Music> playlistTable;
    @FXML private TableColumn<Music, String> playlistTitleColumn;
    @FXML private TableColumn<Music, String> playlistRatingColumn;
    @FXML private HBox playlistTabsContainer;
    @FXML private ScrollPane playlistTabsScrollPane;
    @FXML private Label currentPlaylistLabel;
    @FXML private Label playlistInfoLabel;
    @FXML private Button syncScrollButton;

    @FXML private Button shuffleButton;
    @FXML private Button loopButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button managePlaylistsButton;

    @FXML private Label volumePercentLabel;

    // ==================== Core Services ====================

    private final VlcAudioPlayer audioPlayer = new VlcAudioPlayer();
    private final WaveformExtractor waveformExtractor = new WaveformExtractor();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private final PlaybackQueue playbackQueue = PlaybackQueue.getInstance();

    // ==================== Handlers ====================

    private PlaybackHandler playbackHandler;
    private PlaylistPanelHandler playlistPanelHandler;
    private TableHandler tableHandler;
    private ContextMenuHandler contextMenuHandler;
    private FilterPopupHandler filterPopupHandler;
    private SessionHandler sessionHandler;
    private ShuffleLoopHandler shuffleLoopHandler;

    // ==================== Initialization ====================

    @FXML
    public void initialize() {
        initializeHandlers();
        bindHandlerUIComponents();
        configureHandlerListeners();

        // Setup table selection and bindings
        musicTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        musicTable.setItems(musicLibrary.getMusicList());

        // Load music from database
        musicLibrary.refresh();

        // Table click handlers
        setupTableClickHandlers();

        // Initialize all handlers
        tableHandler.initialize();
        playbackHandler.initialize();
        playlistPanelHandler.initialize();

        // Restore session
        sessionHandler.restoreSession();

        // Update button styles after restore
        shuffleLoopHandler.updateAllButtonStyles();

        // Setup state change listeners (after session restore)
        setupStateListeners();

        // Setup window close handler
        setupWindowCloseHandler();

        // Setup click handler to deselect when clicking outside table
        setupDeselectOnClickOutside();

        // Setup periodic session save (every 10 seconds when playing)
        setupPeriodicSessionSave();

        musicLibrary.setOnRatingChanged(() -> {
            musicTable.refresh();
            playlistTable.refresh();
        });

        // Link volume slider to label
        if (volumeSlider != null && volumePercentLabel != null) {
            updateVolumePercentLabel((int) volumeSlider.getValue());
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateVolumePercentLabel(newVal.intValue());
            });
        }
    }

    private void initializeHandlers() {
        playbackHandler = new PlaybackHandler(audioPlayer, waveformExtractor);
        playlistPanelHandler = new PlaylistPanelHandler();
        tableHandler = new TableHandler();
        contextMenuHandler = new ContextMenuHandler();
        filterPopupHandler = new FilterPopupHandler();
        sessionHandler = new SessionHandler();
        shuffleLoopHandler = new ShuffleLoopHandler();
    }

    private void bindHandlerUIComponents() {
        playbackHandler.bindUIComponents(
                waveformProgressBar,
                currentTitleLabel,
                currentArtistLabel,
                elapsedTimeLabel,
                totalTimeLabel,
                volumeSlider,
                currentCoverArt
        );

        playlistPanelHandler.bindUIComponents(
                playlistTable,
                playlistTitleColumn,
                playlistRatingColumn,
                playlistTabsContainer,
                currentPlaylistLabel,
                playlistInfoLabel,
                syncScrollButton
        );

        tableHandler.bindUIComponents(
                musicTable,
                titleColumn,
                artistColumn,
                albumColumn,
                durationColumn,
                tagsColumn,
                ratingColumn
        );

        shuffleLoopHandler.bindUIComponents(shuffleButton, loopButton);
    }

    private void configureHandlerListeners() {
        // Playback handler events
        playbackHandler.setEventListener(new PlaybackHandler.PlaybackEventListener() {
            @Override
            public void onTrackChanged(Music music) {
                playlistPanelHandler.updatePlaylistTabStyles();
                // Update lyrics window if it's open
                LyricsWindow.updateCurrentTrack(music);
            }

            @Override
            public void onSessionNeedsSave() {
                saveSession();
            }
        });

        // Playlist panel events
        playlistPanelHandler.setEventListener(new PlaylistPanelHandler.PlaylistEventListener() {
            @Override
            public void onPlaylistTrackSelected(Music music, Long playlistId, List<Music> playlistContent, boolean isFromSavedPlaylist) {
                if (playlistId == null) {
                    // Playing from Local playlist
                    playbackQueue.setLocalQueue(playlistContent, music);
                } else {
                    // Playing from a saved playlist - don't modify Local
                    playlistPanelHandler.getAvailablePlaylists().stream()
                            .filter(p -> p.getId().equals(playlistId))
                            .findFirst().ifPresent(playlist
                                    -> playbackQueue.loadPlaylist(playlist.getId(),
                                    playlist.getName(), playlistContent));
                    playbackQueue.playTrack(music);
                }
                playbackHandler.playTrack(music);
                playlistPanelHandler.updatePlaylistTabStyles();
            }

            @Override
            public void onPlaylistTabsNeedRefresh() {
                playlistPanelHandler.refreshPlaylistTabs();
            }

            @Override
            public void onPlaylistContextMenuRequested(List<Music> selectedMusic, double screenX, double screenY, Long playlistId) {
                contextMenuHandler.showMusicContextMenuForPlaylist(
                        selectedMusic,
                        screenX,
                        screenY,
                        playlistTable,
                        playlistId
                );
            }

            @Override
            public void onRatingChanged() {
                // Sync main table when rating is changed in playlist view
                musicTable.refresh();
            }
        });

        // Table handler events
        tableHandler.setEventListener(new TableHandler.TableEventListener() {
            @Override
            public void onShowTagFilterPopup() {
                filterPopupHandler.showTagFilterPopup(
                        musicTable.getScene().getWindow(),
                        musicTable.getScene().getWindow().getX(),
                        musicTable.getScene().getWindow().getY()
                );
            }

            @Override
            public void onShowRatingFilterPopup() {
                filterPopupHandler.showRatingFilterPopup(
                        musicTable.getScene().getWindow(),
                        musicTable.getScene().getWindow().getX(),
                        musicTable.getScene().getWindow().getY()
                );
            }

            @Override
            public void onRatingChanged() {
                // Sync playlist view when rating is changed in main table
                playlistTable.refresh();
            }
        });

        // Context menu events
        contextMenuHandler.setEventListener(new ContextMenuHandler.ContextMenuEventListener() {
            @Override
            public void onPlayRequested(Music music) {
                musicTable.getSelectionModel().clearSelection();
                musicTable.getSelectionModel().select(music);
                onPlay();
            }

            @Override
            public void onShowCreateTagDialog() {
                filterPopupHandler.showCreateTagDialog();
            }

            @Override
            public void onPlaylistRefreshNeeded() {
                playlistPanelHandler.refreshPlaylistTabs();
            }

            @Override
            public void onDisplayedPlaylistRefreshNeeded(Long playlistId) {
                playlistPanelHandler.refreshDisplayedPlaylist();
            }

            @Override
            public void onEditMetadataRequested(Music music) {
                boolean saved = MetadataEditorDialog.show(music);
                if (saved) {
                    onMetadataChanged(music);
                }
            }

            @Override
            public void onBatchChangeCoverArtRequested(List<Music> musicList) {
                BatchCoverArtDialog.show(musicList, () -> onMetadataChanged(null));
            }

            @Override
            public void onMetadataChanged(Music editedMusic) {
                refreshAllViews(editedMusic);
            }

            @Override
            public void onRemoveFromPlaylistRequested(List<Music> musicList, Long playlistId) {
                if (playlistId == null) return;

                com.luciferc137.cmp.database.LibraryService libraryService = com.luciferc137.cmp.database.LibraryService.getInstance();
                for (Music music : musicList) {
                    if (music.getId() != null) {
                        libraryService.removeMusicFromPlaylist(playlistId, music.getId());
                    }
                }

                // Refresh the displayed playlist
                playlistPanelHandler.refreshDisplayedPlaylist();
            }
        });

        // Session restore events
        sessionHandler.setRestoreListener(new SessionHandler.SessionRestoreListener() {
            @Override
            public void onShuffleStateRestored(boolean enabled) {
                shuffleLoopHandler.updateShuffleButtonStyle();
            }

            @Override
            public void onLoopModeRestored(PlaybackQueue.LoopMode mode) {
                shuffleLoopHandler.updateLoopButtonStyle();
            }

            @Override
            public void onCurrentTrackRestored(Music music) {
                playbackHandler.displayTrackInfo(music);
            }

            @Override
            public void onPlaybackPositionRestored(long position) {
                playbackHandler.setRestoredPosition(position);
            }

            @Override
            public void onDisplayedPlaylistRestored(Long playlistId) {
                playlistPanelHandler.setDisplayedPlaylistId(playlistId);
                playlistPanelHandler.updatePlaylistTabStyles();

                if (playlistId == null) {
                    if (currentPlaylistLabel != null) {
                        currentPlaylistLabel.setText("Local");
                    }
                    // Use localPlaylistContent instead of queue for Local playlist display
                    playlistPanelHandler.getDisplayedPlaylistContent().setAll(playbackQueue.getLocalPlaylistContent());
                } else {
                    String playlistName = playlistPanelHandler.getAvailablePlaylists().stream()
                            .filter(p -> p.getId().equals(playlistId))
                            .map(PlaylistEntity::getName)
                            .findFirst().orElse("Playlist");
                    playlistPanelHandler.loadPlaylistIntoView(playlistId, playlistName);
                }
            }

            @Override
            public void onSessionRestoreComplete() {
                // Nothing additional needed
            }
        });
    }

    private void setupTableClickHandlers() {
        musicTable.setOnMouseClicked(event -> {
            contextMenuHandler.hideActiveMenu();
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                onPlay();
            }
        });

        musicTable.setRowFactory(tv -> {
            TableRow<Music> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    List<Music> selectedItems = new ArrayList<>(musicTable.getSelectionModel().getSelectedItems());
                    if (selectedItems.isEmpty()) {
                        selectedItems.add(row.getItem());
                    } else if (!selectedItems.contains(row.getItem())) {
                        selectedItems = List.of(row.getItem());
                    }
                    contextMenuHandler.showMusicContextMenu(
                            selectedItems,
                            event.getScreenX(),
                            event.getScreenY(),
                            musicTable,
                            playlistPanelHandler.getDisplayedPlaylistId()
                    );
                }
            });
            return row;
        });
    }

    private void setupStateListeners() {
        playbackQueue.shuffleEnabledProperty().addListener((obs, old, enabled) -> {
            shuffleLoopHandler.updateShuffleButtonStyle();
            if (!sessionHandler.isRestoringSession()) {
                saveSession();
            }
        });

        playbackQueue.loopModeProperty().addListener((obs, old, mode) -> {
            shuffleLoopHandler.updateLoopButtonStyle();
            if (!sessionHandler.isRestoringSession()) {
                saveSession();
            }
        });
    }

    private void setupWindowCloseHandler() {
        Platform.runLater(() -> {
            if (musicTable.getScene() != null && musicTable.getScene().getWindow() != null) {
                musicTable.getScene().getWindow().setOnCloseRequest(event -> saveSession());
            }
        });
    }

    private void setupDeselectOnClickOutside() {
        Platform.runLater(() -> {
            if (musicTable.getScene() != null) {
                // Add a filter on the scene to detect clicks outside the table
                musicTable.getScene().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
                    // Check if the click is outside the music table
                    if (!isClickInsideNode(event, musicTable)) {
                        // Clear selection if clicking outside table
                        musicTable.getSelectionModel().clearSelection();
                    }
                });
            }
        });
    }

    /**
     * Checks if a mouse event occurred inside a given node.
     */
    private boolean isClickInsideNode(javafx.scene.input.MouseEvent event, javafx.scene.Node node) {
        javafx.geometry.Bounds boundsInScene = node.localToScene(node.getBoundsInLocal());
        return boundsInScene.contains(event.getSceneX(), event.getSceneY());
    }

    private void setupPeriodicSessionSave() {
        // Save session every 10 seconds when playing to preserve playback position
        javafx.animation.Timeline periodicSave = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(10),
                        event -> {
                            if (audioPlayer.isPlaying()) {
                                saveSession();
                            }
                        }
                )
        );
        periodicSave.setCycleCount(javafx.animation.Animation.INDEFINITE);
        periodicSave.play();
    }

    private void saveSession() {
        sessionHandler.saveSession(
                playlistPanelHandler.getDisplayedPlaylistId(),
                playbackHandler.getCurrentPosition()
        );
    }

    /**
     * Refreshes all views after metadata changes.
     * This includes the music table, playlist view, current track display, and lyrics window.
     *
     * @param editedMusic The music that was edited (can be null for batch operations)
     */
    private void refreshAllViews(Music editedMusic) {
        // Refresh main table
        musicTable.refresh();

        // Refresh playlist content (reload from database if needed) and refresh the table
        playlistPanelHandler.refreshDisplayedPlaylist();
        playlistTable.refresh();

        // Refresh current track display if the current track was affected
        if (editedMusic != null) {
            // Synchronize metadata if the edited music matches the current track
            playbackHandler.refreshCurrentTrackIfMatches(editedMusic);
        } else {
            // Batch operation or unknown - just refresh the display
            playbackHandler.refreshCurrentTrackDisplay();
        }

        // Refresh lyrics window if it's open
        LyricsWindow.refreshCurrentTrack();
    }

    // ==================== FXML Action Handlers ====================

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
        if (selected != null) {
            // Play selected track from table
            playbackHandler.playFromTable(selected, new ArrayList<>(musicLibrary.getMusicList()));
        } else {
            // No selection - resume current track or play from queue
            playbackHandler.resumeOrPlayCurrent();
        }
        playlistPanelHandler.updatePlaylistTabStyles();
    }

    @FXML
    private void onPause() {
        // If there's a restored position and nothing is playing yet, resume at that position
        if (playbackHandler.hasRestoredPosition() && !playbackHandler.getAudioPlayer().isPlaying()) {
            playbackHandler.resumeAtSavedPosition();
        } else {
            playbackHandler.pause();
        }
    }

    @FXML
    private void onStop() {
        playbackHandler.stop();
    }

    @FXML
    private void onPrevious() {
        playbackHandler.previous();
    }

    @FXML
    private void onNext() {
        playbackHandler.next();
    }

    @FXML
    private void onShuffle() {
        playbackHandler.toggleShuffle();
    }

    @FXML
    private void onToggleLoop() {
        playbackHandler.cycleLoopMode();
    }

    @FXML
    private void onManagePlaylists() {
        // Open settings window on the Playlists tab
        SettingsWindow.setOnPlaylistsChangedCallback(() -> {
            Platform.runLater(() -> playlistPanelHandler.refreshPlaylistTabs());
        });
        SettingsWindow.show(musicTable.getScene().getWindow(), "Playlists");
    }

    @FXML
    private void onSettings() {
        // Set callback to refresh playlist tabs when playlists change in settings
        SettingsWindow.setOnPlaylistsChangedCallback(() -> {
            Platform.runLater(() -> playlistPanelHandler.refreshPlaylistTabs());
        });
        SettingsWindow.show(musicTable.getScene().getWindow());
    }

    @FXML
    private void onShowLyrics() {
        Music currentMusic = playbackHandler.getCurrentMusic();
        LyricsWindow.show(
                musicTable.getScene().getWindow(),
                currentMusic,
                this::refreshAllViews
        );
    }

    private void updateVolumePercentLabel(int value) {
        // Add spaces for alignment
        if (value < 10) {
            volumePercentLabel.setText("  " + value + "%");
        } else if (value < 100) {
            volumePercentLabel.setText(" " + value + "%");
        } else {
            volumePercentLabel.setText(value + "%");
        }
    }
}
