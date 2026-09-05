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
    @FXML public HBox customTabHeader;
    @FXML public ToggleButton queueTabButton;
    @FXML public ToggleButton playlistsTabButton;

    @FXML public TabPane tabPane;
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
    @FXML public ComboBox<String> queueSortComboBox;
    @FXML public Label playlistInfoLabel;
    @FXML public Label queueInfoLabel;
    @FXML public Button orderButton;
    @FXML public Button loopModeButton;
    @FXML public Button createNewPlaylistButton;
    @FXML public Button addAllToQueueButton;

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
                queueRatingColumn,
                syncQueueButton,
                loopModeButton,
                orderButton,
                queueSortComboBox,
                queueTabButton,
                playlistsTabButton,
                tabPane,
                queueInfoLabel
        );

        configureHandlerListeners();
        configureCrossController();

        Coordinator.queuePanelHandler().updateOrderButtonStyle();
        Coordinator.getInstance().onPlaylistControllerReady();

        clearQueueButton.setFocusTraversable(false);
        shuffleQueueButton.setFocusTraversable(false);
        syncQueueButton.setFocusTraversable(false);
        orderButton.setFocusTraversable(false);
        loopModeButton.setFocusTraversable(false);

        clearQueueButton.getStyleClass().add("delete-button");
    }

    private void configureCrossController() {
        Coordinator.setPlaylistTableRefreshAction(this::refreshAllTables);
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
            public void onPlaylistContextMenuRequested(List<Integer> selectedIndices, double screenX, double screenY, Long
                    playlistId){
                Coordinator.contextMenuHandler().showMusicContextMenuForPlaylist(
                        selectedIndices,
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
            public void onQueueItemSelected(Integer index) {
                Coordinator.playbackQueue().setCurrentIndex(index);
            }

            @Override
            public void onContextMenuRequested(List<Integer> indices, double screenX, double screenY) {
                Coordinator.contextMenuHandler().showMusicContextMenuForQueue(
                        indices,
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

    private void refreshAllTables() {
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
        Coordinator.queuePanelHandler().updatesyncQueueButtonStyle();
        if (Coordinator.queuePanelHandler().isSyncEnabled()) {
            Coordinator.queuePanelHandler().scrollToCurrentTrack();
        }
    }

    @FXML
    public void onSwitchOrder(ActionEvent actionEvent) {
        Coordinator.queuePanelHandler().switchSortOrder();
    }

    @FXML
    public void onLoopMode(ActionEvent actionEvent) {
        Coordinator.playbackHandler().cycleLoopMode();
    }

    @FXML
    public void onCreateNewPlaylist(ActionEvent actionEvent) {
        Coordinator.queuePanelHandler().createNewPlaylist();
    }

    @FXML
    public void onAddAllToQueue(ActionEvent actionEvent) {
        Coordinator.playlistPanelHandler().addCurrentPlaylistToQueue();
    }
}