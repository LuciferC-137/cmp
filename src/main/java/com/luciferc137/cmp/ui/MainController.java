package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.VlcAudioPlayer;
import com.luciferc137.cmp.audio.WaveformExtractor;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.library.*;
import com.luciferc137.cmp.settings.SettingsManager;
import com.luciferc137.cmp.ui.handlers.*;
import com.luciferc137.cmp.ui.settings.SettingsWindow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    @FXML private ListView<Music> playlistView;
    @FXML private HBox playlistTabsContainer;
    @FXML private ScrollPane playlistTabsScrollPane;
    @FXML private Label currentPlaylistLabel;
    @FXML private Label playlistInfoLabel;

    @FXML private Button shuffleButton;
    @FXML private Button loopButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button addPlaylistButton;

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
                volumeSlider
        );

        playlistPanelHandler.bindUIComponents(
                playlistView,
                playlistTabsContainer,
                currentPlaylistLabel,
                playlistInfoLabel
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
            }

            @Override
            public void onSessionNeedsSave() {
                saveSession();
            }
        });

        // Playlist panel events
        playlistPanelHandler.setEventListener(new PlaylistPanelHandler.PlaylistEventListener() {
            @Override
            public void onPlaylistTrackSelected(Music music, Long playlistId, List<Music> playlistContent) {
                if (playlistId == null) {
                    playbackQueue.setLocalQueue(playlistContent, music);
                } else {
                    PlaylistEntity playlist = playlistPanelHandler.getAvailablePlaylists().stream()
                            .filter(p -> p.getId().equals(playlistId))
                            .findFirst().orElse(null);
                    if (playlist != null) {
                        playbackQueue.loadPlaylist(playlist.getId(), playlist.getName(), playlistContent);
                    }
                    playbackQueue.playTrack(music);
                }
                playbackHandler.playTrack(music);
                playlistPanelHandler.updatePlaylistTabStyles();
            }

            @Override
            public void onPlaylistTabsNeedRefresh() {
                playlistPanelHandler.refreshPlaylistTabs();
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
            public void onDisplayedPlaylistRestored(Long playlistId) {
                playlistPanelHandler.setDisplayedPlaylistId(playlistId);
                playlistPanelHandler.updatePlaylistTabStyles();

                if (playlistId == null) {
                    if (currentPlaylistLabel != null) {
                        currentPlaylistLabel.setText("Local");
                    }
                    playlistPanelHandler.getDisplayedPlaylistContent().setAll(playbackQueue.getQueue());
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

    private void saveSession() {
        sessionHandler.saveSession(
                playlistPanelHandler.getDisplayedPlaylistId(),
                playbackHandler.getCurrentPosition()
        );
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
        playbackHandler.playFromTable(selected, new ArrayList<>(musicLibrary.getMusicList()));
        playlistPanelHandler.updatePlaylistTabStyles();
    }

    @FXML
    private void onPause() {
        playbackHandler.pause();
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
    private void onAddPlaylist() {
        playlistPanelHandler.showCreatePlaylistDialog();
    }

    @FXML
    private void onSettings() {
        SettingsWindow.show(musicTable.getScene().getWindow());
    }
}

