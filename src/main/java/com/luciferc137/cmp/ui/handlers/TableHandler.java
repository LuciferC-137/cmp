package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.library.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

/**
 * Handles the music table configuration and sorting including:
 * - Column setup with cell factories
 * - Sortable column headers with click handlers
 * - Rating stars display and interaction
 * - Column header updates for sort indicators
 */
public class TableHandler {

    private final MusicLibrary musicLibrary;

    // UI Components
    private TableView<Music> musicTable;
    private TableColumn<Music, String> titleColumn;
    private TableColumn<Music, String> artistColumn;
    private TableColumn<Music, String> albumColumn;
    private TableColumn<Music, String> durationColumn;
    private TableColumn<Music, String> tagsColumn;
    private TableColumn<Music, String> ratingColumn;

    // Event listener
    private TableEventListener eventListener;

    /**
     * Listener interface for table events.
     */
    public interface TableEventListener {
        void onShowTagFilterPopup();
        void onShowRatingFilterPopup();
    }

    public TableHandler() {
        this.musicLibrary = MusicLibrary.getInstance();
    }

    /**
     * Binds UI components to this handler.
     */
    public void bindUIComponents(
            TableView<Music> musicTable,
            TableColumn<Music, String> titleColumn,
            TableColumn<Music, String> artistColumn,
            TableColumn<Music, String> albumColumn,
            TableColumn<Music, String> durationColumn,
            TableColumn<Music, String> tagsColumn,
            TableColumn<Music, String> ratingColumn
    ) {
        this.musicTable = musicTable;
        this.titleColumn = titleColumn;
        this.artistColumn = artistColumn;
        this.albumColumn = albumColumn;
        this.durationColumn = durationColumn;
        this.tagsColumn = tagsColumn;
        this.ratingColumn = ratingColumn;
    }

    public void setEventListener(TableEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Initializes the table columns.
     */
    public void initialize() {
        setupTableColumns();
    }

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

        // Tags column
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
        tagsColumn.setGraphic(createFilterableHeader("Tags", this::onTagFilterClick));
        tagsColumn.setText("");
        tagsColumn.setSortable(false);

        // Rating column
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
        ratingColumn.setGraphic(createFilterableHeader("Rating", this::onRatingFilterClick));
        ratingColumn.setText("");
        ratingColumn.setSortable(false);

        // Disable default sorting behavior
        musicTable.setSortPolicy(table -> false);
    }

    private void setupSortableColumn(TableColumn<Music, String> column, SortableColumn sortCol) {
        column.setSortable(false);

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
    public void updateColumnHeaders() {
        AdvancedFilter filter = musicLibrary.getAdvancedFilter();
        SortableColumn activeCol = filter.getSortColumn();
        ColumnSortState activeState = filter.getSortState();

        updateSortableColumnHeader(titleColumn, SortableColumn.TITLE, activeCol, activeState);
        updateSortableColumnHeader(artistColumn, SortableColumn.ARTIST, activeCol, activeState);
        updateSortableColumnHeader(albumColumn, SortableColumn.ALBUM, activeCol, activeState);
        updateSortableColumnHeader(durationColumn, SortableColumn.DURATION, activeCol, activeState);
    }

    private void updateSortableColumnHeader(
            TableColumn<Music, String> column,
            SortableColumn sortCol,
            SortableColumn activeCol,
            ColumnSortState activeState
    ) {
        String text = sortCol.getDisplayName();
        if (sortCol == activeCol && activeState != ColumnSortState.NONE) {
            text += activeState.getSymbol();
        }
        ((Label) column.getGraphic()).setText(text);
    }

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

    private void onTagFilterClick() {
        if (eventListener != null) {
            eventListener.onShowTagFilterPopup();
        }
    }

    private void onRatingFilterClick() {
        if (eventListener != null) {
            eventListener.onShowRatingFilterPopup();
        }
    }

    /**
     * Refreshes the table view.
     */
    public void refresh() {
        if (musicTable != null) {
            musicTable.refresh();
        }
    }
}

