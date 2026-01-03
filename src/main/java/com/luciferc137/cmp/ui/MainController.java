package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.VlcAudioPlayer;
import com.luciferc137.cmp.audio.WaveformExtractor;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.library.*;
import com.luciferc137.cmp.model.Music;
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

import java.util.HashMap;
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

    private final VlcAudioPlayer audioPlayer = new VlcAudioPlayer();
    private final WaveformExtractor waveformExtractor = new WaveformExtractor();
    private final SettingsManager settingsManager = SettingsManager.getInstance();
    private final MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private AnimationTimer progressTimer;
    private Music currentMusic;

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

        // Bind the TableView to the MusicLibrary's observable list
        musicTable.setItems(musicLibrary.getMusicList());

        // Load music from the database
        musicLibrary.refresh();

        // Double-click to play
        musicTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                onPlay();
            }
        });

        // Context menu for tag management
        musicTable.setRowFactory(tv -> {
            TableRow<Music> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    showMusicContextMenu(row.getItem(), event.getScreenX(), event.getScreenY());
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
     * Shows context menu for a music item to manage tags.
     */
    private void showMusicContextMenu(Music music, double screenX, double screenY) {
        ContextMenu contextMenu = new ContextMenu();

        // Add tags submenu
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
        createTag.setOnAction(e -> {
            showCreateTagDialog();
        });
        addTagMenu.getItems().add(createTag);

        contextMenu.getItems().add(addTagMenu);

        // Rating submenu
        Menu ratingMenu = new Menu("Set Rating");
        for (int i = 0; i <= 5; i++) {
            final int rating = i;
            String label = i == 0 ? "No rating" : "★".repeat(i) + "☆".repeat(5 - i);
            CheckMenuItem ratingItem = new CheckMenuItem(label);
            ratingItem.setSelected(music.getRating() == i);
            ratingItem.setOnAction(e -> {
                musicLibrary.updateRating(music, rating);
                musicTable.refresh();
            });
            ratingMenu.getItems().add(ratingItem);
        }
        contextMenu.getItems().add(ratingMenu);

        // Play option
        contextMenu.getItems().add(new SeparatorMenuItem());
        MenuItem playItem = new MenuItem("Play");
        playItem.setOnAction(e -> {
            musicTable.getSelectionModel().select(music);
            onPlay();
        });
        contextMenu.getItems().add(playItem);

        contextMenu.show(musicTable, screenX, screenY);
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
     * Updates the progress bar based on playback position.
     */
    private void updateProgress() {
        if (waveformProgressBar != null && audioPlayer.isPlaying()) {
            long duration = audioPlayer.getDuration();
            if (duration > 0) {
                double progress = (double) audioPlayer.getPosition() / duration;
                waveformProgressBar.setProgress(progress);
            }
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
            return;
        }

        if (currentMusic == null || !currentMusic.equals(selected)) {
            currentMusic = selected;
            loadWaveform(selected);
        }

        audioPlayer.play(selected);
        if (waveformProgressBar != null) {
            waveformProgressBar.setProgress(0);
        }
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
    private void onSettings() {
        SettingsWindow.show(musicTable.getScene().getWindow());
    }
}
