package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.ui.Coordinator;
import com.luciferc137.cmp.ui.controllers.MainController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QueuePanelHandler implements Handler {
    public static final String[] QUEUE_SORT_OPTIONS = {
            "Title",
            "Artist",
            "Album",
            "Duration",
            "Rating"
    };

    private final MusicLibrary musicLibrary;
    private final PlaybackQueue playbackQueue;

    // UI Components
    public TableView<Music> queueTable;
    public TableColumn<Music, String> queueTitleColumn;
    public TableColumn<Music, String> queueRatingColumn;
    public Button syncQueueButton;
    public Button loopModeButton;
    public Button orderButton;
    public ComboBox<String> queueSortComboBox;
    public ToggleButton queueTabButton;
    public ToggleButton playlistsTabButton;
    public TabPane tabPane;
    public Label queueInfoLabel;

    private QueuePanelHandler.QueueEventListener eventListener;

    private boolean isSyncEnabled = false;
    private boolean isAscendingSort = true;

    // Drag-and-drop support
    private List<Integer> draggedTracks;

    public interface QueueEventListener {
        void onQueueItemSelected(Integer index);
        void onQueueItemRemoved(int index);
        void onRatingChanged();
        void onContextMenuRequested(List<Integer> indices, double screenX, double screenY);
    }

    public QueuePanelHandler() {
        this.musicLibrary = MusicLibrary.getInstance();
        this.playbackQueue = PlaybackQueue.getInstance();
    }

    public void bindUIComponents(TableView<Music> queueTable,
                                 TableColumn<Music, String> queueTitleColumn,
                                 TableColumn<Music, String> queueRatingColumn,
                                 Button syncQueueButton,
                                 Button loopModeButton,
                                 Button orderButton,
                                 ComboBox<String> queueSortComboBox,
                                 ToggleButton queueTabButton,
                                 ToggleButton playlistsTabButton,
                                 TabPane tabPane,
                                 Label queueInfoLabel) {
        this.queueInfoLabel = queueInfoLabel;
        this.queueSortComboBox = queueSortComboBox;
        this.syncQueueButton = syncQueueButton;
        this.loopModeButton = loopModeButton;
        this.orderButton = orderButton;
        this.queueTable = queueTable;
        this.queueTitleColumn = queueTitleColumn;
        this.queueRatingColumn = queueRatingColumn;
        this.queueTabButton = queueTabButton;
        this.playlistsTabButton = playlistsTabButton;
        this.tabPane = tabPane;
    }

    public void setEventListener(QueueEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public void initialize() {
        if (queueTable == null) return;
        queueTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        queueTable.setItems(playbackQueue.getQueue());

        setupTableColumns();

        // Double-click to play from playlist
        queueTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                int selected = queueTable.getSelectionModel().getSelectedIndex();
                eventListener.onQueueItemSelected(selected);
            }
        });

        queueTable.setOnContextMenuRequested(event -> {
            if (eventListener != null) {
                List<Integer> selected = queueTable.getSelectionModel().getSelectedIndices();
                if (!selected.isEmpty()) {
                    eventListener.onContextMenuRequested(selected, event.getScreenX(), event.getScreenY());
                }
            }
        });

        queueTable.setOnKeyReleased(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                List<Music> selected = queueTable.getSelectionModel().getSelectedItems();
                if (!selected.isEmpty() && eventListener != null) {
                    for (Music music : selected) {
                        int index = playbackQueue.getQueue().indexOf(music);
                        if (index >= 0) {
                            eventListener.onQueueItemRemoved(index);
                        }
                    }
                }
            }
        });

        // Drag-and-drop support for reordering tracks
        queueTable.setRowFactory(tableView -> {
            TableRow<Music> row = new TableRow<>();

            row.itemProperty().addListener((obs, oldMusic, newMusic) -> {
                updateCurrentTrackStyle(row);
            });

            // Drag detected event for initiating drag-and-drop
            row.setOnDragDetected(event -> {
                if (row.isEmpty()) return;

                int index = row.getIndex();

                if (!row.isSelected()) {
                    queueTable.getSelectionModel().clearSelection();
                    queueTable.getSelectionModel().select(index);
                }

                List<Integer> selectedTracks = new ArrayList<>();

                for (Music music : queueTable.getSelectionModel().getSelectedItems()) {
                    selectedTracks.add(playbackQueue.getQueue().indexOf(music));
                }

                if (selectedTracks.isEmpty()) return;

                Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);

                ClipboardContent content = new ClipboardContent();
                content.putString("queue-reorder");

                dragboard.setContent(content);

                draggedTracks = selectedTracks;

                event.consume();
            });

            // Drag over event to allow dropping
            row.setOnDragOver(event -> {
                if (draggedTracks != null && !draggedTracks.isEmpty()) {
                    if (!row.isEmpty() && !draggedTracks.contains(row.getIndex())) {
                        // Display visual feedback for valid drop target
                        if (!row.getStyleClass().contains("drag-over-row")) {
                            row.getStyleClass().add("drag-over-row");
                        }
                        event.acceptTransferModes(TransferMode.MOVE);
                    } else {
                        row.getStyleClass().remove("drag-over-row");
                    }
                    event.consume();
                }
            });

            row.setOnDragExited(event -> row.getStyleClass().remove("drag-over-row"));

            // Drop
            row.setOnDragDropped(event -> {
                if (draggedTracks == null || draggedTracks.isEmpty()) {
                    return;
                }

                int targetIndex = row.getIndex();

                playbackQueue.moveBatch(draggedTracks, targetIndex);

                draggedTracks = null;

                row.getStyleClass().remove("drag-over-row");

                queueTable.getSelectionModel().clearSelection();

                event.setDropCompleted(true);
                event.consume();
            });

            // Drag done event to clear the dragged tracks
            row.setOnDragDone(event -> {
                draggedTracks = null;
                row.getStyleClass().remove("drag-over-row");
                event.consume();
            });

            return row;
        });

        playbackQueue.addCurrentTrackListener((musicObservable, oldMusic, newMusic) -> {
            queueTable.refresh();
            onTrackChange();
        });

        playbackQueue.loopModeProperty().addListener((obs, oldMode, newMode) -> {
            updateLoopModeButtonStyle();
        });

            playbackQueue.addQueueListener((musicObservable, oldMusic, newMusic) -> {
            updateQueueInfo();
        });

        configureQueueSortComboBox();
        configureTabPane();
        setupsyncQueueButton();
    }

    private void configureTabPane() {
        ToggleGroup group = new ToggleGroup();
        queueTabButton.setToggleGroup(group);
        playlistsTabButton.setToggleGroup(group);
        queueTabButton.setSelected(true);

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                group.selectToggle(oldToggle);
                return;
            }
            if (newToggle == queueTabButton) {
                tabPane.getSelectionModel().select(0);
            } else if (newToggle == playlistsTabButton) {
                tabPane.getSelectionModel().select(1);
                Coordinator.playlistPanelHandler().refreshDisplayedPlaylist();
            }
        });

        for (Tab tab : tabPane.getTabs()) {
            tab.setClosable(false);
        }

        // Remove the default header
        tabPane.setFocusTraversable(false);
        Platform.runLater(() -> {
            Region overflowButton = (Region)  tabPane.lookup(".tab-header-area");
            if (overflowButton != null) {
                overflowButton.setVisible(false);
                overflowButton.setManaged(false);
                overflowButton.setMaxWidth(0);
                overflowButton.setMinWidth(0);
                overflowButton.setPrefWidth(0);
            }
        });
    }

    private void configureQueueSortComboBox() {
        queueSortComboBox.setPromptText("Sort by");
        queueSortComboBox.getItems().addAll(QUEUE_SORT_OPTIONS);

        queueSortComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText("Sort by");
            }
        });

        queueSortComboBox.setOnAction(event -> {
            String selected = queueSortComboBox.getValue();

            if (selected != null) {
                Coordinator.queuePanelHandler().sortQueue(selected);
                Platform.runLater(() -> queueSortComboBox.getSelectionModel().clearSelection());
            }
        });
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
                Coordinator.queuePanelHandler().updatesyncQueueButtonStyle();
            });

            // Disable sync when user interacts with the scrollbar
            queueTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) {
                    queueTable.lookupAll(".scroll-bar").forEach(node -> {
                        if (node instanceof ScrollBar scrollBar &&
                                scrollBar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                            scrollBar.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                                Coordinator.queuePanelHandler().disableSync();
                                Coordinator.queuePanelHandler().updatesyncQueueButtonStyle();
                            });
                        }
                    });
                }
            });
        }
    }

    public void onClearQueue() {
        playbackQueue.clear();
    }

    public void onShuffleQueue() {
        playbackQueue.shuffle();
    }

    public void onTrackChange() {
        for (Node row : queueTable.lookupAll(".table-row-cell")) {
            if (row instanceof TableRow<?> tableRow) {
                @SuppressWarnings("unchecked")
                TableRow<Music> musicRow = (TableRow<Music>) tableRow;

                updateCurrentTrackStyle(musicRow);
            }
        }
    }

    private void updateCurrentTrackStyle(TableRow<Music> row) {
        int current = row.getIndex();

        row.getStyleClass().remove("playlist-current-track");

        if (current == playbackQueue.getCurrentIndex()) {
            row.getStyleClass().add("playlist-current-track");
        }

        if (isSyncEnabled) scrollToCurrentTrack();

    }

    public void disableSync() {
        this.isSyncEnabled = false;
    }

    public void toggleSync() {
        this.isSyncEnabled = !this.isSyncEnabled;
    }

    public boolean isSyncEnabled() {
        return this.isSyncEnabled;
    }

    public void scrollToCurrentTrack() {
        if (playbackQueue.getCurrentIndex() >= 0) {
            queueTable.scrollTo(playbackQueue.getCurrentIndex());
        }
    }

    public boolean isAscendingSort() {
        return isAscendingSort;
    }

    public void switchSortOrder() {
        isAscendingSort = !isAscendingSort;
        updateOrderButtonStyle();
    }

    public void setLoopMode(PlaybackQueue.LoopMode loopMode) {
        playbackQueue.setLoopMode(loopMode);
    }

    /**
     * Sorts the playback queue based on the specified column and current internal order.
     *
     * @param sortBy    The column to sort by ("Title", "Artist", "Album", "Duration", "Rating").
     */
    public void sortQueue(String sortBy) {
        Comparator<Music> comparator = switch (sortBy) {
            case "Title" -> Comparator.comparing(m -> m.title != null ? m.title.toLowerCase() : "");
            case "Artist" -> Comparator.comparing(m -> m.artist != null ? m.artist.toLowerCase() : "");
            case "Album" -> Comparator.comparing(m -> m.album != null ? m.album.toLowerCase() : "");
            case "Duration" -> Comparator.comparingLong(m -> m.duration);
            case "Rating" -> Comparator.comparingInt(Music::getRating);
            default -> null;
        };
        if (comparator != null) {
            if (!isAscendingSort) {
                comparator = comparator.reversed();
            }
            playbackQueue.sortQueue(comparator);
        }
        queueTable.refresh();
    }

    public void createNewPlaylist() {
        Coordinator.playlistPanelHandler().showCreatePlaylistDialog(queueTable.getItems().stream().toList());
    }

    /**
     * Updates the sync button style based on current state.
     */
    public void updatesyncQueueButtonStyle() {
        if (syncQueueButton == null) return;

        if (Coordinator.queuePanelHandler().isSyncEnabled()) {
            syncQueueButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white;");
        } else {
            syncQueueButton.setStyle("-fx-background-color: #3C3C3C; -fx-text-fill: #808080;");
        }
    }

    /**
     * Updates the loop mode button style based on current loop mode.
     */
    public void updateOrderButtonStyle() {
        orderButton.setText(!Coordinator.queuePanelHandler().isAscendingSort() ? "⬆" : "⬇");
    }

    public void updateLoopModeButtonStyle() {
        if (loopModeButton == null) return;

        PlaybackQueue.LoopMode mode = playbackQueue.getLoopMode();

        if (mode == PlaybackQueue.LoopMode.SINGLE) {
            loopModeButton.setText("\uD83D\uDD02");
            loopModeButton.setTooltip(new Tooltip("Loop current track"));
        } else if (mode == PlaybackQueue.LoopMode.PLAYLIST) {
            loopModeButton.setText("\uD83D\uDD01");
            loopModeButton.setTooltip(new Tooltip("Loop current queue"));
        } else {
            loopModeButton.setText("\uD83D\uDD01");
            loopModeButton.setTooltip(new Tooltip("Stops when queue ends"));
        }

        loopModeButton.getStyleClass().removeAll(
                "loop-button-none", "loop-button-single", "loop-button-playlist"
        );

        switch (mode) {
            case SINGLE -> loopModeButton.getStyleClass().add("loop-button-single");
            case PLAYLIST -> loopModeButton.getStyleClass().add("loop-button-playlist");
            default -> loopModeButton.getStyleClass().add("loop-button-none");
        }
    }

    private void updateQueueInfo() {
        if (queueInfoLabel != null) {
            int count = playbackQueue.getQueue().size();
            long totalDuration = playbackQueue.getQueue().stream()
                    .mapToLong(m -> m.duration)
                    .sum();
            queueInfoLabel.setText(count + " tracks • "
                    + MainController.formatTime(totalDuration));
        }
    }

    /**
     * Sets up the table columns for title and rating.
     */
    private void setupTableColumns() {
        // Title column - displays title and artist
        queueTitleColumn.setCellValueFactory(data -> {
            Music music = data.getValue();
            String display = music.title + " - " + (music.artist != null ? music.artist : "Unknown");
            return new SimpleStringProperty(display);
        });

        // Rating column with interactive stars
        queueRatingColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRatingAsStars()));
        queueRatingColumn.setCellFactory(col -> new TableCell<>() {
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
        queueTable.setSortPolicy(table -> false);
        queueTitleColumn.setSortable(false);
        queueRatingColumn.setSortable(false);
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
                queueTable.refresh();
                // Notify listener to sync other views (main table)
                if (eventListener != null) {
                    eventListener.onRatingChanged();
                }
            });
            stars.getChildren().add(star);
        }

        return stars;
    }
}
