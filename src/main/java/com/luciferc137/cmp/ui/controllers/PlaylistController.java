package com.luciferc137.cmp.ui.controllers;

import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.Coordinator;
import com.luciferc137.cmp.ui.handlers.PlaylistPanelHandler;
import com.luciferc137.cmp.ui.settings.SettingsWindow;
import javafx.application.Platform;
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
    @FXML public Label currentPlaylistLabel;
    @FXML public Button syncScrollButton;
    @FXML public TableView<Music> playlistTable;
    @FXML public TableColumn<Music, String> playlistTitleColumn;
    @FXML public TableColumn<Music, String> playlistRatingColumn;
    @FXML private Label playlistInfoLabel;

    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private final PlaybackQueue playbackQueue = PlaybackQueue.getInstance();

    @FXML
    public void initialize() {

        musicLibrary.setOnRatingChanged(() -> {
            playlistTable.refresh();
        });

        Coordinator.playlistPanelHandler().bindUIComponents(
                playlistTable,
                playlistTitleColumn,
                playlistRatingColumn,
                playlistTabsContainer,
                currentPlaylistLabel,
                playlistInfoLabel,
                syncScrollButton
        );

        configureHandlerListeners();
        configureCrossController();

        Coordinator.getInstance().onPlaylistControllerReady();
    }

    private void configureCrossController() {
        Coordinator.setPlaylistTableRefreshAction(this::refreshAllViews);
    }

    private void configureHandlerListeners() {
        Coordinator.playlistPanelHandler().setEventListener(new PlaylistPanelHandler.PlaylistEventListener() {
            @Override
            public void onPlaylistTrackSelected(Music music, Long playlistId, List < Music > playlistContent,
            boolean isFromSavedPlaylist){
                if (playlistId == null) {
                    // Playing from Local playlist
                    Coordinator.playbackHandler().playbackQueue.setLocalQueue(playlistContent, music);
                } else {
                    // Playing from a saved playlist - don't modify Local
                    Coordinator.playlistPanelHandler().getAvailablePlaylists().stream()
                            .filter(p -> p.getId().equals(playlistId))
                            .findFirst().ifPresent(playlist
                                    -> Coordinator.playbackHandler().playbackQueue.loadPlaylist(playlist.getId(),
                                    playlist.getName(), playlistContent));
                    Coordinator.playbackHandler().playbackQueue.playTrack(music);
                }
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
    }

    private void refreshAllViews() {
        playlistTable.refresh();
    }

    @FXML
    private void onManagePlaylists() {
        // Open settings window on the Playlists tab
        SettingsWindow.setOnPlaylistsChangedCallback(() -> {
            Platform.runLater(() -> Coordinator.playlistPanelHandler().refreshPlaylistTabs());
        });
        SettingsWindow.show(this.managePlaylistsButton.getScene().getWindow(), "Playlists");
    }
}
