package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.audio.LyricsService;
import com.luciferc137.cmp.library.Music;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Controller for the lyrics window FXML.
 * Handles display of current track lyrics and metadata.
 */
public class LyricsController {

    @FXML private ImageView coverArtView;
    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label lyricsLabel;
    @FXML private Label statusLabel;
    @FXML private Label loadingLabel;
    @FXML private Button editButton;
    @FXML private Button fetchButton;
    @FXML private Button syncScrollButton;
    @FXML private ScrollPane lyricsScrollPane;
    @FXML private HBox loadingBox;

    private Music currentMusic;
    private Consumer<Music> onMetadataChanged;
    private boolean isFetching = false;

    // Scroll sync state
    private boolean scrollSyncEnabled = false;
    private LongSupplier positionSupplier;
    private LongSupplier durationSupplier;
    private AnimationTimer scrollSyncTimer;

    // Smooth scrolling interpolation
    private double currentScrollValue = 0;
    private static final double SCROLL_SMOOTHING = 0.08; // Lower = smoother but slower response

    @FXML
    public void initialize() {
        // Set default cover art
        if (coverArtView != null) {
            coverArtView.setImage(CoverArtLoader.getDefaultCover(80));
        }

        // Setup sync scroll button
        setupSyncScrollButton();
    }

    /**
     * Sets up the sync scroll button and scroll listeners.
     */
    private void setupSyncScrollButton() {
        if (syncScrollButton == null) return;

        // Initial style (disabled)
        updateSyncButtonStyle();

        // Toggle sync on button click
        syncScrollButton.setOnAction(e -> {
            scrollSyncEnabled = !scrollSyncEnabled;
            updateSyncButtonStyle();
            if (scrollSyncEnabled) {
                startScrollSync();
            } else {
                stopScrollSync();
            }
        });

        // Disable sync when user manually scrolls with mouse wheel
        if (lyricsScrollPane != null) {
            lyricsScrollPane.setOnScroll(event -> {
                if (scrollSyncEnabled) {
                    scrollSyncEnabled = false;
                    updateSyncButtonStyle();
                    stopScrollSync();
                }
            });

            // Disable sync when user interacts with the scrollbar
            lyricsScrollPane.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) {
                    lyricsScrollPane.lookupAll(".scroll-bar").forEach(node -> {
                        if (node instanceof ScrollBar scrollBar &&
                            scrollBar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                            scrollBar.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                                if (scrollSyncEnabled) {
                                    scrollSyncEnabled = false;
                                    updateSyncButtonStyle();
                                    stopScrollSync();
                                }
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
    private void updateSyncButtonStyle() {
        if (syncScrollButton == null) return;

        if (scrollSyncEnabled) {
            syncScrollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #1E90FF; -fx-text-fill: white;");
            syncScrollButton.setText("⇅");
        } else {
            syncScrollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #3C3C3C; -fx-text-fill: #808080;");
            syncScrollButton.setText("⇅");
        }
    }

    /**
     * Sets the playback position and duration suppliers for scroll synchronization.
     *
     * @param positionSupplier Supplier for current playback position in milliseconds
     * @param durationSupplier Supplier for total track duration in milliseconds
     */
    public void setPlaybackSuppliers(LongSupplier positionSupplier, LongSupplier durationSupplier) {
        this.positionSupplier = positionSupplier;
        this.durationSupplier = durationSupplier;
    }

    /**
     * Starts the scroll synchronization timer.
     */
    private void startScrollSync() {
        if (scrollSyncTimer != null) {
            scrollSyncTimer.stop();
        }

        // Initialize current scroll value to current position
        if (lyricsScrollPane != null) {
            currentScrollValue = lyricsScrollPane.getVvalue();
        }

        scrollSyncTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateScrollPosition();
            }
        };
        scrollSyncTimer.start();
    }

    /**
     * Stops the scroll synchronization timer.
     */
    private void stopScrollSync() {
        if (scrollSyncTimer != null) {
            scrollSyncTimer.stop();
            scrollSyncTimer = null;
        }
    }

    /**
     * Updates the scroll position based on playback progress.
     * The lyrics scroll so that the current position is always in the middle of the viewport.
     * Uses smooth interpolation for fluid scrolling.
     *
     * - At the start (0%), scroll is at top (vvalue = 0)
     * - At 50% of the song, scroll reaches middle position
     * - At 100% of the song, scroll reaches the bottom (vvalue = 1)
     *
     * The formula ensures that the "current lyrics" appear in the middle of the viewport
     * by offsetting the scroll position.
     */
    private void updateScrollPosition() {
        if (lyricsScrollPane == null || positionSupplier == null || durationSupplier == null) {
            return;
        }

        long position = positionSupplier.getAsLong();
        long duration = durationSupplier.getAsLong();

        if (duration <= 0) {
            return;
        }

        // Calculate progress (0.0 to 1.0)
        double progress = (double) position / duration;
        progress = Math.max(0, Math.min(1, progress));

        // Get viewport height ratio (how much of the content is visible)
        double viewportHeight = lyricsScrollPane.getViewportBounds().getHeight();
        double contentHeight = lyricsScrollPane.getContent().getBoundsInLocal().getHeight();

        if (contentHeight <= viewportHeight) {
            // Content fits in viewport, no need to scroll
            return;
        }

        // Calculate target scroll position
        // At any progress point, the "current lyrics position" in the content is:
        // currentContentPos = progress * contentHeight
        // We want this position to be at the center of the viewport:
        // viewportTopPos = currentContentPos - viewportHeight/2
        double currentContentPos = progress * contentHeight;
        double viewportTopPos = currentContentPos - (viewportHeight / 2);

        // Clamp to valid range [0, contentHeight - viewportHeight]
        double maxScroll = contentHeight - viewportHeight;
        viewportTopPos = Math.max(0, Math.min(maxScroll, viewportTopPos));

        // Convert to vvalue (0 to 1)
        double targetScrollValue = viewportTopPos / maxScroll;

        // Apply smooth interpolation (lerp)
        // Move currentScrollValue towards targetScrollValue by a fraction each frame
        currentScrollValue = currentScrollValue + (targetScrollValue - currentScrollValue) * SCROLL_SMOOTHING;

        // Apply the smoothed scroll value
        lyricsScrollPane.setVvalue(currentScrollValue);
    }

    /**
     * Cleans up resources when the controller is no longer needed.
     */
    public void cleanup() {
        stopScrollSync();
    }

    /**
     * Updates the display with the given music track.
     *
     * @param music The music track to display
     */
    public void setMusic(Music music) {
        this.currentMusic = music;
        
        if (music == null) {
            clearDisplay();
            return;
        }
        
        // Update track info
        titleLabel.setText(music.title != null ? music.title : "Unknown Title");
        artistLabel.setText(music.artist != null ? music.artist : "Unknown Artist");
        albumLabel.setText(music.album != null ? music.album : "");
        
        // Load cover art
        loadCoverArt(music);
        
        // Load lyrics
        loadLyrics(music);
        
        // Enable buttons
        editButton.setDisable(false);
        fetchButton.setDisable(false);
    }

    /**
     * Refreshes the display of the current music track.
     * Updates title, artist, album, and cover art without reloading lyrics.
     * Used after metadata has been edited.
     */
    public void refreshDisplay() {
        if (currentMusic == null) {
            return;
        }

        // Update track info labels
        titleLabel.setText(currentMusic.title != null ? currentMusic.title : "Unknown Title");
        artistLabel.setText(currentMusic.artist != null ? currentMusic.artist : "Unknown Artist");
        albumLabel.setText(currentMusic.album != null ? currentMusic.album : "");

        // Reload cover art (in case it was changed)
        loadCoverArt(currentMusic);
    }

    private void clearDisplay() {
        titleLabel.setText("No track playing");
        artistLabel.setText("");
        albumLabel.setText("");
        lyricsLabel.setText("No lyrics available");
        statusLabel.setText("");
        coverArtView.setImage(CoverArtLoader.getDefaultCover(80));
        editButton.setDisable(true);
        fetchButton.setDisable(true);
        hideLoading();
    }

    private void loadCoverArt(Music music) {
        if (music.filePath != null) {
            // Load cover art synchronously (CoverArtLoader handles fallback to default)
            javafx.scene.image.Image coverImage = CoverArtLoader.loadCoverArt(music.filePath, 80);
            coverArtView.setImage(coverImage);
        } else {
            coverArtView.setImage(CoverArtLoader.getDefaultCover(80));
        }
    }

    private void loadLyrics(Music music) {
        if (music.filePath == null) {
            lyricsLabel.setText("No lyrics available");
            statusLabel.setText("");
            return;
        }

        try {
            File audioFile = new File(music.filePath);
            AudioMetadata metadata = AudioMetadata.fromFile(audioFile);
            String lyrics = metadata.getLyrics();
            
            if (lyrics != null && !lyrics.trim().isEmpty()) {
                lyricsLabel.setText(lyrics);
                statusLabel.setText("");
                // Reset scroll to top
                lyricsScrollPane.setVvalue(0);
            } else {
                lyricsLabel.setText("No lyrics available for this track.\n\nClick the fetch button to search online, or \"Edit\" to add lyrics manually.");
                statusLabel.setText("Tip: Use the download button to fetch lyrics from the internet.");
            }
        } catch (IOException e) {
            lyricsLabel.setText("Unable to load lyrics.");
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Sets a callback to be invoked when metadata is changed.
     *
     * @param callback The callback to run with the edited music
     */
    public void setOnMetadataChanged(Consumer<Music> callback) {
        this.onMetadataChanged = callback;
    }

    @FXML
    private void onEditMetadata() {
        if (currentMusic == null) {
            return;
        }
        
        boolean saved = MetadataEditorDialog.show(currentMusic);
        if (saved) {
            // Refresh the track info display (title, artist, album, cover art)
            refreshDisplay();

            // Refresh the lyrics display
            loadLyrics(currentMusic);
            
            // Notify parent controller of changes
            if (onMetadataChanged != null) {
                onMetadataChanged.accept(currentMusic);
            }
        }
    }

    @FXML
    private void onFetchLyrics() {
        if (currentMusic == null || isFetching) {
            return;
        }

        String artist = currentMusic.artist;
        String title = currentMusic.title;

        if (artist == null || artist.trim().isEmpty()) {
            showErrorAlert("Cannot Fetch Lyrics", "Artist name is missing for this track.");
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            showErrorAlert("Cannot Fetch Lyrics", "Song title is missing for this track.");
            return;
        }

        // Show loading state
        showLoading("Searching lyrics for \"" + title + "\" by " + artist + "...");
        isFetching = true;
        fetchButton.setDisable(true);

        // Fetch lyrics asynchronously
        LyricsService.fetchLyricsAsync(artist, title)
                .thenAccept(result -> Platform.runLater(() -> {
                    hideLoading();
                    isFetching = false;
                    fetchButton.setDisable(false);

                    if (result.isSuccess()) {
                        showLyricsConfirmationDialog(result.getLyrics());
                    } else {
                        showErrorAlert("Lyrics Not Found", result.getErrorMessage());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        hideLoading();
                        isFetching = false;
                        fetchButton.setDisable(false);
                        showErrorAlert("Error", "An unexpected error occurred: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void showLoading(String message) {
        if (loadingBox != null && loadingLabel != null) {
            loadingLabel.setText(message);
            loadingBox.setVisible(true);
            loadingBox.setManaged(true);
        }
        statusLabel.setText("Please wait...");
    }

    private void hideLoading() {
        if (loadingBox != null) {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
        }
        statusLabel.setText("");
    }

    private void showLyricsConfirmationDialog(String fetchedLyrics) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Lyrics Found");
        dialog.setHeaderText("Review the fetched lyrics before saving");
        dialog.setResizable(true);

        // Apply dark theme
        ThemeManager.applyDarkTheme(dialog);

        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label infoLabel = new Label("Lyrics for \"" + currentMusic.title + "\" by " + currentMusic.artist + ":");
        infoLabel.setStyle("-fx-font-weight: bold;");

        TextArea lyricsArea = new TextArea(fetchedLyrics);
        lyricsArea.setWrapText(true);
        lyricsArea.setEditable(true);
        lyricsArea.setPrefRowCount(20);
        lyricsArea.setPrefColumnCount(50);
        VBox.setVgrow(lyricsArea, Priority.ALWAYS);

        Label hintLabel = new Label("You can edit the lyrics before saving.");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        content.getChildren().addAll(infoLabel, lyricsArea, hintLabel);

        dialog.getDialogPane().setContent(content);

        // Add buttons
        ButtonType saveButton = new ButtonType("Save to File", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        // Set size and center on screen
        dialog.setOnShown(e -> {
            var window = dialog.getDialogPane().getScene().getWindow();
            if (window instanceof javafx.stage.Stage stage) {
                stage.setMinWidth(600);
                stage.setMinHeight(550);
                stage.setWidth(600);
                stage.setHeight(550);
                stage.centerOnScreen();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButton) {
            String editedLyrics = lyricsArea.getText();
            saveLyricsToFile(editedLyrics);
        }
    }

    private void saveLyricsToFile(String lyrics) {
        if (currentMusic == null || currentMusic.filePath == null) {
            showErrorAlert("Error", "Cannot save lyrics: no file path available.");
            return;
        }

        try {
            File audioFile = new File(currentMusic.filePath);
            AudioMetadata metadata = AudioMetadata.fromFile(audioFile);
            metadata.setLyrics(lyrics);
            metadata.saveToFile(audioFile);

            // Refresh the display
            loadLyrics(currentMusic);

            // Notify parent controller
            if (onMetadataChanged != null) {
                onMetadataChanged.accept(currentMusic);
            }

            statusLabel.setText("Lyrics saved successfully!");

        } catch (IOException e) {
            showErrorAlert("Error Saving Lyrics", "Could not save lyrics to file: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.applyDarkTheme(alert);
        alert.showAndWait();
    }
}

