package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

public class QueuePanelHandler implements Handler {
    private final MusicLibrary musicLibrary;
    private final PlaybackQueue playbackQueue;

    // UI Components
    public TableView<Music> queueTable;
    public TableColumn<Music, String> queueTitleColumn;
    public TableColumn<Music, String> queueRatingColumn;

    private QueuePanelHandler.QueueEventListener eventListener;

    private boolean isSyncEnabled = false;

    public interface QueueEventListener {
        void onQueueItemSelected(Music music);
        void onQueueItemRemoved(int index);
        void onRatingChanged();
        void onContextMenuRequested(List<Music> music, double screenX, double screenY);
    }

    public QueuePanelHandler() {
        this.musicLibrary = MusicLibrary.getInstance();
        this.playbackQueue = PlaybackQueue.getInstance();
    }

    public void bindUIComponents(TableView<Music> queueTable,
                                 TableColumn<Music, String> queueTitleColumn,
                                 TableColumn<Music, String> queueRatingColumn) {
        this.queueTable = queueTable;
        this.queueTitleColumn = queueTitleColumn;
        this.queueRatingColumn = queueRatingColumn;
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
                Music selected = queueTable.getSelectionModel().getSelectedItem();
                if (selected != null && eventListener != null) {
                    eventListener.onQueueItemSelected(selected);
                }
            }
        });

        queueTable.setOnContextMenuRequested(event -> {
            if (eventListener != null) {
                List<Music> selected = queueTable.getSelectionModel().getSelectedItems();
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

        queueTable.setRowFactory(tableView -> {
            TableRow<Music> row = new TableRow<>();

            row.itemProperty().addListener((obs, oldMusic, newMusic) -> {
                updateCurrentTrackStyle(row);
            });

            return row;
        });

        playbackQueue.addQueueListener((musicObservable, oldMusic, newMusic) -> {
            queueTable.refresh();
            onTrackChange();
        });

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
        Music music = row.getItem();

        row.getStyleClass().remove("playlist-current-track");

        if (music != null && music.equals(playbackQueue.getCurrentTrack())) {
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
