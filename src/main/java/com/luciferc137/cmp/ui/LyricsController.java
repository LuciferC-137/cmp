package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.library.Music;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;

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
    @FXML private Button editButton;
    @FXML private ScrollPane lyricsScrollPane;

    private Music currentMusic;
    private Runnable onMetadataChanged;

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
        
        // Enable edit button
        editButton.setDisable(false);
    }

    private void clearDisplay() {
        titleLabel.setText("No track playing");
        artistLabel.setText("");
        albumLabel.setText("");
        lyricsLabel.setText("No lyrics available");
        statusLabel.setText("");
        coverArtView.setImage(CoverArtLoader.getDefaultCover(80));
        editButton.setDisable(true);
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
                lyricsLabel.setText("No lyrics available for this track.\n\nClick \"Edit\" to add lyrics.");
                statusLabel.setText("Tip: You can add lyrics in the Lyrics tab of the metadata editor.");
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
}

