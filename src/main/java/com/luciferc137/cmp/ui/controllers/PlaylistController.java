package com.luciferc137.cmp.ui.controllers;

import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.Coordinator;
import com.luciferc137.cmp.ui.handlers.PlaylistPanelHandler;
import com.luciferc137.cmp.ui.handlers.QueuePanelHandler;
import com.luciferc137.cmp.ui.settings.SettingsWindow;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class PlaylistController {
    @FXML public VBox queuePane;
    @FXML public ScrollPane playlistTabsScrollPane;
    @FXML public HBox playlistTabsContainer;
    @FXML public Button managePlaylistsButton;
    @FXML public TableView<Music> playlistTable;
    @FXML public TableColumn<Music, String> playlistTitleColumn;
    @FXML public TableColumn<Music, String> playlistRatingColumn;
    @FXML public Button clearQueueButton;
    @FXML public Button shuffleQueueButton;
    @FXML public Button syncQueueButton;
    @FXML private Label playlistInfoLabel;

    @FXML public TableView<Music> queueTable;
    @FXML public TableColumn<Music, String> queueTitleColumn;
    @FXML public TableColumn<Music, String> queueRatingColumn;


    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private final PlaybackQueue playbackQueue = PlaybackQueue.getInstance();

    @FXML
    public void initialize() {

        musicLibrary.setOnRatingChanged(() -> {
            playlistTable.refresh();
            queueTable.refresh();
        });

        Coordinator.playlistPanelHandler().bindUIComponents(
                playlistTable,
                playlistTitleColumn,
                playlistRatingColumn,
                playlistTabsContainer,
                playlistInfoLabel
        );

        Coordinator.queuePanelHandler().bindUIComponents(
                queueTable,
                queueTitleColumn,
                queueRatingColumn
        );

        configureHandlerListeners();
        configureCrossController();

        setupsyncQueueButton();

        Coordinator.getInstance().onPlaylistControllerReady();
    }

    private void configureCrossController() {
        Coordinator.setPlaylistTableRefreshAction(this::refreshAllViews);
    }

    private void configureHandlerListeners() {
        Coordinator.playlistPanelHandler().setEventListener(new PlaylistPanelHandler.PlaylistEventListener() {
            @Override
            public void onPlaylistTrackSelected(Music music, Long playlistId, List<Music> playlistContent){
                Coordinator.playbackHandler().playbackQueue.setQueue(playlistContent);
                Coordinator.playbackHandler().playTrack(music);
                Coordinator.playlistPanelHandler().updatePlaylistTabStyles();
            }

            @Override
            public void onPlaylistTabsNeedRefresh () {
                Coordinator.playlistPanelHandler().refreshPlaylistTabs();
            }

            @Override
            public void onPlaylistContextMenuRequested(List<Music> selectedMusic, double screenX, double screenY, Long
                    playlistId){
                Coordinator.contextMenuHandler().showMusicContextMenuForPlaylist(
                        selectedMusic,
                        screenX,
                        screenY,
                        playlistTable,
                        playlistId
                );
            }

            @Override
            public void onRatingChanged () {
                Coordinator.refreshMainTable();
            }
        });
        Coordinator.playlistPanelHandler().initialize();

        Coordinator.queuePanelHandler().setEventListener(new QueuePanelHandler.QueueEventListener() {

            @Override
            public void onQueueItemRemoved(int index) {
                Coordinator.playbackHandler().playbackQueue.removeFromQueue(index);
            }

            @Override
            public void onQueueItemSelected(Music music) {
                Coordinator.playbackHandler().playTrack(music);
            }

            @Override
            public void onContextMenuRequested(List<Music> musics, double screenX, double screenY) {
                Coordinator.contextMenuHandler().showMusicContextMenuForQueue(
                        musics,
                        screenX,
                        screenY,
                        queueTable
                );
            }

            @Override
            public void onRatingChanged() {
                Coordinator.refreshMainTable();
            }
        });
        Coordinator.queuePanelHandler().initialize();
    }

    /**
     * Sets up the sync scroll button and scroll listeners.
     */
    private void setupsyncQueueButton() {
        if (syncQueueButton == null) return;

        // Disable sync when user manually scrolls with mouse wheel
        if (queueTable != null) {
            queueTable.setOnScroll(event -> {
                Coordinator.queuePanelHandler().disableSync();
                updatesyncQueueButtonStyle();
            });

            // Disable sync when user interacts with the scrollbar
            queueTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) {
                    queueTable.lookupAll(".scroll-bar").forEach(node -> {
                        if (node instanceof ScrollBar scrollBar &&
                                scrollBar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                            scrollBar.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                                Coordinator.queuePanelHandler().disableSync();
                                updatesyncQueueButtonStyle();
                            });
                        }
                    });
                }
            });
        }
    }

    /**
     * Updates the sync button style based on current state.
     */
    private void updatesyncQueueButtonStyle() {
        if (syncQueueButton == null) return;

        if (Coordinator.queuePanelHandler().isSyncEnabled()) {
            syncQueueButton.setStyle("-fx-font-size: 14px; -fx-background-color: #1E90FF; -fx-text-fill: white;");
            syncQueueButton.setText("⇅");
        } else {
            syncQueueButton.setStyle("-fx-font-size: 14px; -fx-background-color: #3C3C3C; -fx-text-fill: #808080;");
            syncQueueButton.setText("⇅");
        }
    }

    private void refreshAllViews() {
        playlistTable.refresh();
        queueTable.refresh();
    }

    @FXML
    private void onManagePlaylists() {
        // Open settings window on the Playlists tab
        SettingsWindow.setOnPlaylistsChangedCallback(() -> {
            Platform.runLater(() -> Coordinator.playlistPanelHandler().refreshPlaylistTabs());
        });
        SettingsWindow.show(this.managePlaylistsButton.getScene().getWindow(), "Playlists");
    }

    @FXML
    public void onClearQueue(ActionEvent actionEvent) {
        Coordinator.queuePanelHandler().onClearQueue();
    }

    @FXML
    public void onShuffleQueue(ActionEvent actionEvent) {
        Coordinator.queuePanelHandler().onShuffleQueue();
    }

    @FXML
    public void toggleSyncQueue(ActionEvent actionEvent) {
        Coordinator.queuePanelHandler().toggleSync();
        updatesyncQueueButtonStyle();
        if (Coordinator.queuePanelHandler().isSyncEnabled()) {
            Coordinator.queuePanelHandler().scrollToCurrentTrack();
        }
    }
}