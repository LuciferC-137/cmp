package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.audio.LyricsService;
import com.luciferc137.cmp.library.Music;
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
    @FXML private ScrollPane lyricsScrollPane;
    @FXML private HBox loadingBox;

    private Music currentMusic;
    private Runnable onMetadataChanged;
    private boolean isFetching = false;

    @FXML
    public void initialize() {
        // Set default cover art
        if (coverArtView != null) {
            coverArtView.setImage(CoverArtLoader.getDefaultCover(80));
        }
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
     * @param callback The callback to run
     */
    public void setOnMetadataChanged(Runnable callback) {
        this.onMetadataChanged = callback;
    }

    @FXML
    private void onEditMetadata() {
        if (currentMusic == null) {
            return;
        }
        
        boolean saved = MetadataEditorDialog.show(currentMusic);
        if (saved) {
            // Refresh the lyrics display
            loadLyrics(currentMusic);
            
            // Notify parent controller of changes
            if (onMetadataChanged != null) {
                onMetadataChanged.run();
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
                onMetadataChanged.run();
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

