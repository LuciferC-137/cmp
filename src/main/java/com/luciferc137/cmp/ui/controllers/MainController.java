package com.luciferc137.cmp.ui.controllers;

import com.luciferc137.cmp.MainApp;
import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.library.*;
import com.luciferc137.cmp.ui.Coordinator;
import com.luciferc137.cmp.ui.dialog.BatchCoverArtDialog;
import com.luciferc137.cmp.ui.dialog.MetadataEditorDialog;
import com.luciferc137.cmp.ui.utils.WaveformProgressBar;
import com.luciferc137.cmp.ui.handlers.*;
import com.luciferc137.cmp.ui.lyrics.LyricsWindow;
import com.luciferc137.cmp.ui.settings.SettingsController;
import com.luciferc137.cmp.ui.settings.SettingsWindow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;

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

    @FXML private Label musicTableInfoLabel;

    @FXML private Button prevButton;
    @FXML private Button nextButton;

    @FXML public Button stopButton;
    @FXML public Button playButton;
    @FXML public Button pauseButton;
    @FXML public Button lyricsButton;

    @FXML private Label volumePercentLabel;
    

    // ==================== Initialization ====================

    @FXML
    public void initialize() {
        bindHandlerUIComponents();
        configureHandlerListeners();

        // Setup table selection and bindings
        musicTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        musicTable.setItems(Coordinator.musicLibrary().getMusicList());

        // Load music from database
        Coordinator.musicLibrary().refresh();

        // Table click handlers
        setupTableClickHandlers();

        // Setup state change listeners (after session restore)
        setupStateListeners();

        // Setup window close handler
        setupWindowCloseHandler();

        // Setup click handler to deselect when clicking outside table
        setupDeselectOnClickOutside();

        // Setup periodic session save (every 10 seconds when playing)
        setupPeriodicSessionSave();

        Coordinator.musicLibrary().setOnRatingChanged(() -> {
            musicTable.refresh();
        });
        Coordinator.musicLibrary().setController(this);

        // Link volume slider to label
        if (volumeSlider != null && volumePercentLabel != null) {
            updateVolumePercentLabel((int) volumeSlider.getValue());
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateVolumePercentLabel(newVal.intValue());
            });
        }

        updateMusicTableInfoLabel();

        configureCrossController();

        Coordinator.getInstance().onMainControllerReady();
    }

    private void bindHandlerUIComponents() {
        Coordinator.playbackHandler().bindUIComponents(
                waveformProgressBar,
                currentTitleLabel,
                currentArtistLabel,
                elapsedTimeLabel,
                totalTimeLabel,
                volumeSlider,
                currentCoverArt
        );

        Coordinator.tableHandler().bindUIComponents(
                musicTable,
                titleColumn,
                artistColumn,
                albumColumn,
                durationColumn,
                tagsColumn,
                ratingColumn
        );
    }

    private void configureCrossController() {
        Coordinator.setMainTableRefreshAction(musicTable::refresh);
    }

    private void configureHandlerListeners() {
        // Playback handler events
        Coordinator.playbackHandler().setEventListener(new PlaybackHandler.PlaybackEventListener() {
            @Override
            public void onTrackChanged(Music music) {
                Coordinator.playlistPanelHandler().updatePlaylistTabStyles();
                // Update lyrics window if it's open
                LyricsWindow.updateCurrentTrack(music);
                MainApp.mprisMediaKeyService.setNowPlaying(music);
            }

            @Override
            public void onSessionNeedsSave() {
                saveSession();
            }

            @Override
            public void onPlaybackStatusChanged(boolean isPlaying) {
                MainApp.mprisMediaKeyService.setPlaybackStatus(isPlaying);
            }
        });
        Coordinator.playbackHandler().initialize();

        // Table handler events
        Coordinator.tableHandler().setEventListener(new TableHandler.TableEventListener() {
            @Override
            public void onShowTagFilterPopup() {
                Coordinator.filterPopupHandler().showTagFilterPopup(
                        musicTable.getScene().getWindow(),
                        musicTable.getScene().getWindow().getX(),
                        musicTable.getScene().getWindow().getY()
                );
            }

            @Override
            public void onShowRatingFilterPopup() {
                Coordinator.filterPopupHandler().showRatingFilterPopup(
                        musicTable.getScene().getWindow(),
                        musicTable.getScene().getWindow().getX(),
                        musicTable.getScene().getWindow().getY()
                );
            }

            @Override
            public void onRatingChanged() {
                // Sync playlist view when rating is changed in main table
                Coordinator.playlistPanelHandler().refreshPlaylistTabs();
            }
        });
        Coordinator.tableHandler().initialize();

        // Context menu events
        Coordinator.contextMenuHandler().setEventListener(new ContextMenuHandler.ContextMenuEventListener() {
            @Override
            public void onPlayRequested(Music music) {
                musicTable.getSelectionModel().clearSelection();
                musicTable.getSelectionModel().select(music);
                onPlay();
            }

            @Override
            public void onShowCreateTagDialog() {
                SettingsController.showCreateTagDialog();
            }

            @Override
            public void onPlaylistRefreshNeeded() {
                Coordinator.playlistPanelHandler().refreshPlaylistTabs();
            }

            @Override
            public void onDisplayedPlaylistRefreshNeeded(Long playlistId) {
                Coordinator.playlistPanelHandler().refreshDisplayedPlaylist();
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

                LibraryService libraryService = LibraryService.getInstance();
                for (Music music : musicList) {
                    if (music.getId() != null) {
                        libraryService.removeMusicFromPlaylist(playlistId, music.getId());
                    }
                }

                // Refresh the displayed playlist
                Coordinator.playlistPanelHandler().refreshDisplayedPlaylist();
            }
        });
        Coordinator.contextMenuHandler().initialize();

        // Session restore events
        Coordinator.sessionHandler().setRestoreListener(new SessionHandler.SessionRestoreListener() {

            @Override
            public void onLoopModeRestored(PlaybackQueue.LoopMode mode) {
                Coordinator.queuePanelHandler().setLoopMode(mode);
            }

            @Override
            public void onCurrentTrackRestored(Music music) {
                Coordinator.playbackHandler().displayTrackInfo(music);
            }

            @Override
            public void onPlaybackPositionRestored(long position) {
                Coordinator.playbackHandler().setRestoredPosition(position);
            }

            @Override
            public void onDisplayedPlaylistRestored(Long playlistId) {
                Coordinator.playlistPanelHandler().setDisplayedPlaylistId(playlistId);
                Coordinator.playlistPanelHandler().updatePlaylistTabStyles();
                Coordinator.playlistPanelHandler().loadPlaylistIntoView(playlistId);
                Coordinator.refreshPlaylistTable();
            }

            @Override
            public void onSessionRestoreComplete() {
                // Refresh the displayed playlist to show tracks in playback order
                Coordinator.playlistPanelHandler().refreshDisplayedPlaylist();
            }
        });
        Coordinator.sessionHandler().initialize();
    }

    private void setupTableClickHandlers() {
        musicTable.setOnMouseClicked(event -> {
            Coordinator.contextMenuHandler().hideActiveMenu();
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
                    Coordinator.contextMenuHandler().showMusicContextMenu(
                            selectedItems,
                            event.getScreenX(),
                            event.getScreenY(),
                            musicTable
                    );
                }
            });
            return row;
        });
    }

    private void setupStateListeners() {

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
                            if (Coordinator.audioPlayer().isPlaying()) {
                                saveSession();
                            }
                        }
                )
        );
        periodicSave.setCycleCount(javafx.animation.Animation.INDEFINITE);
        periodicSave.play();
    }

    private void saveSession() {
        Coordinator.sessionHandler().saveSession(
                Coordinator.playlistPanelHandler().getDisplayedPlaylistId(),
                Coordinator.playbackHandler().getCurrentPosition()
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
        Coordinator.playlistPanelHandler().refreshDisplayedPlaylist();

        // Refresh current track display if the current track was affected
        if (editedMusic != null) {
            // Synchronize metadata if the edited music matches the current track
            Coordinator.playbackHandler().refreshCurrentTrackIfMatches(editedMusic);
        } else {
            // Batch operation or unknown - just refresh the display
            Coordinator.playbackHandler().refreshCurrentTrackDisplay();
        }

        // Refresh lyrics window if it's open
        LyricsWindow.refreshCurrentTrack();
    }

    // ==================== FXML Action Handlers ====================

    @FXML
    private void onSearch() {
        String query = searchField.getText();
        Coordinator.musicLibrary().search(query);
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
        Coordinator.musicLibrary().clearSearch();
    }

    @FXML
    private void onPlay() {
        Music selected = musicTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Play selected track from table
            Coordinator.playbackHandler().playFromTable(selected, new ArrayList<>(Coordinator.musicLibrary().getMusicList()));
        } else {
            // No selection - resume current track or play from queue
            Coordinator.playbackHandler().resumeOrPlayCurrent();
        }
        Coordinator.playlistPanelHandler().updatePlaylistTabStyles();
    }

    @FXML
    private void onPause() {
        // If there's a restored position and nothing is playing yet, resume at that position
        if (Coordinator.playbackHandler().hasRestoredPosition() && !Coordinator.playbackHandler().getAudioPlayer().isPlaying()) {
            Coordinator.playbackHandler().resumeAtSavedPosition();
        } else {
            Coordinator.playbackHandler().pause();
        }
    }

    @FXML
    private void onStop() {
        Coordinator.playbackHandler().stop();
    }

    @FXML
    private void onPrevious() {
        Coordinator.playbackHandler().previous();
    }

    @FXML
    private void onNext() {
        Coordinator.playbackHandler().next();
    }

    @FXML
    private void onSettings() {
        // Set callback to refresh playlist tabs when playlists change in settings
        SettingsWindow.setOnPlaylistsChangedCallback(() -> {
            Platform.runLater(() -> Coordinator.playlistPanelHandler().refreshPlaylistTabs());
        });
        SettingsWindow.show(musicTable.getScene().getWindow());
    }

    @FXML
    private void onShowLyrics() {
        Music currentMusic = Coordinator.playbackHandler().getCurrentMusic();
        LyricsWindow.show(
                musicTable.getScene().getWindow(),
                currentMusic,
                this::refreshAllViews,
                Coordinator.playbackHandler().getAudioPlayer()::getPosition,
                Coordinator.playbackHandler().getAudioPlayer()::getDuration
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

    public void updateMusicTableInfoLabel() {
        int displayedTracks = musicTable.getItems().size();
        long totalDuration = musicTable.getItems().stream()
                .mapToLong(m -> m.duration).sum();
        musicTableInfoLabel.setText(displayedTracks
                + " tracks • " + formatTime(totalDuration));
        Coordinator.tableHandler().updateColumnHeaders();
    }

    public static String formatTime(long millis) {
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

    // ==================== Keyboard Shortcut Handlers ====================

    public void openSettingsFromShortcut() {
        onSettings();
    }

    public void onNextFromShortcut() {
        onNext();
    }

    public void onPreviousFromShortcut() {
        onPrevious();
    }

    public void onPlayFromShortcut() { onPlay();}

    public void onPauseFromShortcut() { onPause(); }

    public void fiveSecondForwardFromShortcut() {
        Coordinator.playbackHandler().fiveSecondsForward();
    }

    public void fiveSecondBackwardFromShortcut() {
        Coordinator.playbackHandler().fiveSecondsBack();
    }

    public void onStopFromShortcut() {
        Coordinator.playbackHandler().stop();
    }
}
