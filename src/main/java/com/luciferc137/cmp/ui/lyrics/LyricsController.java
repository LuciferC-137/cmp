package com.luciferc137.cmp.ui.lyrics;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.fetch.LyricsService;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.ui.utils.AlbumColorExtractor;
import com.luciferc137.cmp.ui.utils.CoverArtLoader;
import com.luciferc137.cmp.ui.dialog.MetadataEditorDialog;
import com.luciferc137.cmp.ui.utils.ThemeManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Controller for the lyrics window FXML.
 * Handles display of current track lyrics and metadata.
 */
public class LyricsController {
    private static final Color APP_BASE_BACKGROUND = Color.web("#282828");

    private static final double BASE_FONT_SIZE = 16;
    private static final double MIN_ZOOM_PERCENT = 50;
    private static final double MAX_ZOOM_PERCENT = 300;
    private static final double ZOOM_STEP_PERCENT = 10;
    private double lyricsZoomPercent = 100;

    private static final int COLOR_NUMBER = 3; // Number of dominant colors to extract for gradient
    private static final double GRADIENT_TRANSITION_MS = 200; // Duration of gradient transition in milliseconds
    private double gradientInterp = 1.0; // Interpolation factor for gradient transition
    private long lastGradientUpdateTime = 0; // Last time the gradient was updated
    private AnimationTimer gradientTimer;

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
    @FXML private HBox headerBox;

    @FXML private ScrollSynchronizer scrollSynchronizer;

    private Music currentMusic;
    private Consumer<Music> onMetadataChanged;
    private boolean isFetching = false;
    private List<Color> albumColors;
    private List<Color> prevAlbumColors;


    @FXML
    public void initialize() {
        scrollSynchronizer = new ScrollSynchronizer(lyricsScrollPane);

        // Set default cover art
        if (coverArtView != null) {
            coverArtView.setImage(CoverArtLoader.getDefaultCover(80));
        }

        // Set headerBox listener to update gradient when resized
        if (headerBox != null) {
            headerBox.widthProperty().addListener((
                    obs, oldVal, newVal)
                    -> refreshBackgroundGradient());
            headerBox.heightProperty().addListener((
                    obs, oldVal, newVal)
                    -> refreshBackgroundGradient());
        }

        setupSyncScrollButton();
        setupLyricsFontZoom();
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
            scrollSynchronizer.toggle();
            updateSyncButtonStyle();
        });

        // Disable sync when user manually scrolls with mouse wheel
        if (lyricsScrollPane != null) {
            lyricsScrollPane.setOnScroll(event -> {
                scrollSynchronizer.disable();
                updateSyncButtonStyle();
            });

            // Disable sync when user interacts with the scrollbar
            lyricsScrollPane.skinProperty().addListener((obs, oldSkin, newSkin) -> {
                if (newSkin != null) {
                    lyricsScrollPane.lookupAll(".scroll-bar").forEach(node -> {
                        if (node instanceof ScrollBar scrollBar &&
                            scrollBar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                            scrollBar.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                                scrollSynchronizer.disable();
                                updateSyncButtonStyle();
                            });
                        }
                    });
                }
            });
        }
    }

    private void setupLyricsFontZoom() {
        if (lyricsScrollPane == null || lyricsLabel == null) return;

        applyLyricsFontSize();

        lyricsScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double relevantDelta = event.getDeltaY() != 0
                        ? event.getDeltaY()
                        : event.getDeltaX();

                if (relevantDelta != 0) {
                    double direction = relevantDelta > 0 ? 1 : -1;
                    lyricsZoomPercent = Math.clamp(
                            lyricsZoomPercent + direction * ZOOM_STEP_PERCENT,
                            MIN_ZOOM_PERCENT,
                            MAX_ZOOM_PERCENT
                    );
                    applyLyricsFontSize();
                }
                event.consume();
            }
        });
    }

    private void applyLyricsFontSize() {
        double fontSize = BASE_FONT_SIZE * (lyricsZoomPercent / 100.0);
        lyricsLabel.setStyle("-fx-font-size: " + fontSize + "px;");
    }

    /**
     * Updates the sync button style based on current state.
     */
    private void updateSyncButtonStyle() {
        if (syncScrollButton == null) return;

        if (scrollSynchronizer.isEnabled()) {
            syncScrollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #1E90FF; -fx-text-fill: white;");
            syncScrollButton.setText("⇅");
        } else {
            syncScrollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #3C3C3C; -fx-text-fill: #808080;");
            syncScrollButton.setText("⇅");
        }
    }

    /**
     * Cleans up resources when the controller is no longer needed.
     */
    public void cleanup() {
        scrollSynchronizer.disable();
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
        updateGradientColor();
        
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
        updateGradientColor();
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
        if (music.absPath() != null) {
            // Load cover art synchronously (CoverArtLoader handles fallback to default)
            javafx.scene.image.Image coverImage = CoverArtLoader.loadCoverArt(music.absPath(), 80);
            coverArtView.setImage(coverImage);
        } else {
            coverArtView.setImage(CoverArtLoader.getDefaultCover(80));
        }
    }

    /**
     * Refreshes the background gradient based on the current album colors and interpolation factor.
     * This method is called during the gradient transition animation.
     */
    public void refreshBackgroundGradient() {
        List<Color> colors = new ArrayList<>();
        if (gradientInterp >= 1.0) {
            colors = albumColors != null ? albumColors : List.of(APP_BASE_BACKGROUND);
        } else {
            for (int i = 0; i < COLOR_NUMBER; i++) {
                if (albumColors != null && prevAlbumColors != null) {
                    colors.add(prevAlbumColors.get(i).interpolate(albumColors.get(i),
                            gradientInterp));
                }
            }
            long now = System.currentTimeMillis();
            gradientInterp += (double) (now - lastGradientUpdateTime) / GRADIENT_TRANSITION_MS;
            lastGradientUpdateTime = now;
            if (gradientInterp > 1.0) {
                gradientInterp = 1.0;
            }
        }

        Background bg = AlbumColorExtractor.buildAngledBackground(
                !colors.isEmpty() ? colors : List.of(APP_BASE_BACKGROUND),
                45, headerBox.getWidth(), headerBox.getHeight(),
                APP_BASE_BACKGROUND, 0.5);
        headerBox.setBackground(bg);
    }

    /**
     * Updates the album colors and starts the gradient transition.
     * This should be called whenever the cover art changes.
     */
    public void updateGradientColor() {
        Platform.runLater(
                () -> {
                    this.prevAlbumColors = albumColors;
                    this.albumColors = AlbumColorExtractor
                            .extractDominantColors(coverArtView.getImage(), COLOR_NUMBER);
                    this.gradientInterp = 0.0;
                    this.lastGradientUpdateTime = System.currentTimeMillis();
                    startGradientRefreshLoop();
                }
        );
    }

    /**
     * Starts the gradient refresh loop using an AnimationTimer.
     * This will continuously update the background gradient until the transition is complete.
     */
    private void startGradientRefreshLoop() {
        if (gradientTimer != null) {
            gradientTimer.stop();
        }

        gradientTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gradientInterp < 1.0) {
                    refreshBackgroundGradient();
                } else {
                    stop();
                }
            }
        };

        gradientTimer.start();
    }

    private void loadLyrics(Music music) {
        if (music.absPath() == null) {
            lyricsLabel.setText("No lyrics available");
            statusLabel.setText("");
            return;
        }

        try {
            File audioFile = new File(music.absPath());
            AudioMetadata metadata = AudioMetadata.fromFile(audioFile);
            String lyrics = removeLrcTimestamps(metadata.getLyrics());
            
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

    public void setPlaybackSuppliers(LongSupplier positionSupplier, LongSupplier durationSupplier) {
        this.scrollSynchronizer.setPlaybackSuppliers(positionSupplier, durationSupplier);
    }

    /**
     * Removes LRC timestamps from synced lyrics.
     * Timestamps look like [00:15.50]
     */
    private static String removeLrcTimestamps(String synchedLyrics) {
        if (synchedLyrics == null) return null;
        // Remove patterns like [00:00.00] or [0:00.00]
        return synchedLyrics.replaceAll("\\[\\d{1,2}:\\d{2}\\.\\d{2}]\\s*", "");
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

        showLoading("Searching lyrics for \"" + title + "\" by " + artist + "...");
        isFetching = true;
        fetchButton.setDisable(true);

        LyricsService.fetchLyricsAsync(artist, title)
                .thenAccept(result -> Platform.runLater(() -> {
                    hideLoading();
                    isFetching = false;
                    fetchButton.setDisable(false);

                    if (result.isSuccess()) {
                        LyricsConfirmationDialog.show(title, artist, result.getLyrics())
                                .ifPresent(this::saveLyricsToFile);
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

    private void saveLyricsToFile(String lyrics) {
        if (currentMusic == null || currentMusic.absPath() == null) {
            showErrorAlert("Error", "Cannot save lyrics: no file path available.");
            return;
        }

        try {
            File audioFile = new File(currentMusic.absPath());
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

