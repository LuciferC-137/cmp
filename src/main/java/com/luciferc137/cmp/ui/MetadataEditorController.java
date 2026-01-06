package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.audio.LyricsService;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;

/**
 * Controller for the metadata editor dialog.
 * Handles editing of music track metadata including title, artist, album, lyrics, etc.
 */
public class MetadataEditorController {

    // Info tab fields
    @FXML private ImageView coverArtView;
    @FXML private StackPane imageWrapper;
    @FXML private TextField titleField;
    @FXML private TextField artistField;
    @FXML private TextField albumField;
    @FXML private TextField albumArtistField;
    @FXML private TextField yearField;
    @FXML private TextField trackNumberField;
    @FXML private TextField discNumberField;
    @FXML private TextField genreField;
    @FXML private TextField composerField;

    // Lyrics tab
    @FXML private TextArea lyricsArea;
    @FXML private Button fetchLyricsButton;
    @FXML private ProgressIndicator lyricsProgressIndicator;
    @FXML private Label lyricsStatusLabel;

    // Technical tab fields
    @FXML private TextField filePathField;
    @FXML private TextField durationField;
    @FXML private TextField formatField;
    @FXML private TextField bitrateField;
    @FXML private TextField sampleRateField;

    private Music music;
    private File audioFile;
    private AudioMetadata metadata;

    private static final int COVER_ART_SIZE = 100;

    @FXML
    public void initialize() {
        // Setup cover art click handler
        if (imageWrapper != null) {
            imageWrapper.setOnMouseClicked(event -> onChangeCoverArt());

            // Hover effect
            imageWrapper.setOnMouseEntered(e ->
                imageWrapper.setStyle("-fx-border-color: #1E90FF; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-color: #2a2a2a; -fx-background-radius: 4;"));
            imageWrapper.setOnMouseExited(e ->
                imageWrapper.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-color: #2a2a2a; -fx-background-radius: 4;"));
        }

        // Set default cover art
        if (coverArtView != null) {
            coverArtView.setImage(CoverArtLoader.getDefaultCover(COVER_ART_SIZE));
        }
    }

    /**
     * Initializes the controller with the music track to edit.
     * 
     * @param music The music track to edit
     */
    public void setMusic(Music music) {
        this.music = music;
        this.audioFile = new File(music.filePath);
        loadMetadata();
    }

    /**
     * Returns the title and artist for the dialog header.
     */
    public String getHeaderText() {
        if (music == null) return "";
        return music.title + " - " + (music.artist != null ? music.artist : "Unknown Artist");
    }

    /**
     * Loads the current metadata from the audio file into the form fields.
     */
    private void loadMetadata() {
        try {
            metadata = AudioMetadata.fromFile(audioFile);

            // Populate form fields
            titleField.setText(nvl(metadata.getTitle()));
            artistField.setText(nvl(metadata.getArtist()));
            albumField.setText(nvl(metadata.getAlbum()));
            albumArtistField.setText(nvl(metadata.getAlbumArtist()));
            yearField.setText(nvl(metadata.getYear()));
            trackNumberField.setText(nvl(metadata.getTrackNumber()));
            discNumberField.setText(nvl(metadata.getDiscNumber()));
            genreField.setText(nvl(metadata.getGenre()));
            composerField.setText(nvl(metadata.getComposer()));
            lyricsArea.setText(nvl(metadata.getLyrics()));

            // Technical info
            filePathField.setText(music.filePath);
            durationField.setText(music.getFormattedDuration());

            if (metadata.getFormat() != null) {
                formatField.setText(metadata.getFormat().getDescription() +
                    (metadata.getEncodingType() != null ? " (" + metadata.getEncodingType() + ")" : ""));
            }
            bitrateField.setText(metadata.getFormattedBitrate());
            sampleRateField.setText(metadata.getFormattedSampleRate());

            // Load cover art using CoverArtLoader
            coverArtView.setImage(CoverArtLoader.loadCoverArt(metadata, COVER_ART_SIZE));

        } catch (IOException e) {
            // Could not read file, use values from Music object
            titleField.setText(music.title);
            artistField.setText(nvl(music.artist));
            albumField.setText(nvl(music.album));
            filePathField.setText(music.filePath);
            durationField.setText(music.getFormattedDuration());

            showError("Could not read metadata from file: " + e.getMessage());
        }
    }

    /**
     * Handles click on cover art to allow changing it.
     */
    private void onChangeCoverArt() {
        File imageFile = CoverArtLoader.showCoverArtChooser(coverArtView.getScene().getWindow());
        if (imageFile != null) {
            byte[] imageData = CoverArtLoader.loadImageAsBytes(imageFile);
            if (imageData != null) {
                // Update the metadata with new cover art
                if (metadata != null) {
                    metadata.setCoverArt(imageData);
                    metadata.setCoverArtMimeType(CoverArtLoader.getMimeType(imageFile));
                }

                // Update the displayed image
                Image newCover = CoverArtLoader.loadCoverArt(imageData, COVER_ART_SIZE);
                if (newCover != null) {
                    coverArtView.setImage(newCover);
                }
            }
        }
    }

    /**
     * Handles the fetch lyrics button click.
     */
    @FXML
    private void onFetchLyrics() {
        String artist = artistField.getText();
        String title = titleField.getText();

        if (artist == null || artist.trim().isEmpty()) {
            lyricsStatusLabel.setText("Artist name is required");
            lyricsStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff6b6b;");
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            lyricsStatusLabel.setText("Title is required");
            lyricsStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff6b6b;");
            return;
        }

        // Show loading state
        fetchLyricsButton.setDisable(true);
        lyricsProgressIndicator.setVisible(true);
        lyricsStatusLabel.setText("Searching...");
        lyricsStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E90FF;");

        // Fetch lyrics asynchronously
        LyricsService.fetchLyricsAsync(artist, title)
                .thenAccept(result -> Platform.runLater(() -> {
                    fetchLyricsButton.setDisable(false);
                    lyricsProgressIndicator.setVisible(false);

                    if (result.isSuccess()) {
                        lyricsArea.setText(result.getLyrics());
                        lyricsStatusLabel.setText("✓ Lyrics found!");
                        lyricsStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4CAF50;");
                    } else {
                        lyricsStatusLabel.setText(result.getErrorMessage());
                        lyricsStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff6b6b;");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        fetchLyricsButton.setDisable(false);
                        lyricsProgressIndicator.setVisible(false);
                        lyricsStatusLabel.setText("Error: " + ex.getMessage());
                        lyricsStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ff6b6b;");
                    });
                    return null;
                });
    }

    /**
     * Saves the modified metadata to the audio file.
     * Called when the Save button is clicked.
     * 
     * @return true if save was successful
     */
    public boolean saveMetadata() {
        try {
            if (metadata == null) {
                metadata = AudioMetadata.fromFile(audioFile);
            }

            // Update metadata object from form fields
            metadata.setTitle(titleField.getText().trim());
            metadata.setArtist(artistField.getText().trim());
            metadata.setAlbum(albumField.getText().trim());
            metadata.setAlbumArtist(albumArtistField.getText().trim());
            metadata.setYear(yearField.getText().trim());
            metadata.setTrackNumber(trackNumberField.getText().trim());
            metadata.setDiscNumber(discNumberField.getText().trim());
            metadata.setGenre(genreField.getText().trim());
            metadata.setComposer(composerField.getText().trim());
            metadata.setLyrics(lyricsArea.getText());

            // Save to file
            metadata.saveToFile(audioFile);

            // Update the Music object to reflect the changes in the UI
            music.title = metadata.getTitle();
            music.artist = metadata.getArtist();
            music.album = metadata.getAlbum();

            // Also update in the database if the music has an ID
            if (music.getId() != null) {
                MusicLibrary.getInstance().updateMusicMetadata(music);
            }

            return true;

        } catch (IOException e) {
            showError("Could not save metadata: " + e.getMessage());
            System.err.println("Error saving metadata: " + e.getMessage());
            return false;
        }
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.applyDarkTheme(alert);
        alert.showAndWait();
    }
}

