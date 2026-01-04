package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Dialog for editing music track metadata.
 * Provides two tabs: Info (title, artist, album, etc.) and Lyrics.
 * Changes are saved directly to the audio file using the AudioMetadata class.
 */
public class MetadataEditorDialog {

    private final Music music;
    private final File audioFile;
    private AudioMetadata metadata;

    // Info tab fields
    private TextField titleField;
    private TextField artistField;
    private TextField albumField;
    private TextField albumArtistField;
    private TextField yearField;
    private TextField trackNumberField;
    private TextField discNumberField;
    private TextField genreField;
    private TextField composerField;
    
    // Lyrics tab
    private TextArea lyricsArea;

    // Technical info (read-only)
    private TextField bitrateField;
    private TextField sampleRateField;
    private TextField formatField;

    public MetadataEditorDialog(Music music) {
        this.music = music;
        this.audioFile = new File(music.filePath);
    }

    /**
     * Shows the metadata editor dialog for the given music track.
     * 
     * @param music The music track to edit
     * @return true if changes were saved, false otherwise
     */
    public static boolean show(Music music) {
        MetadataEditorDialog editor = new MetadataEditorDialog(music);
        return editor.showDialog();
    }

    private boolean showDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Metadata");
        dialog.setHeaderText(music.title + " - " + (music.artist != null ? music.artist : "Unknown Artist"));
        dialog.setResizable(true);
        dialog.setWidth(600);
        dialog.setHeight(500);

        // Center window on screen when shown
        dialog.setOnShown(e -> dialog.getDialogPane().getScene().getWindow().centerOnScreen());

        // Apply dark theme
        ThemeManager.applyDarkTheme(dialog);
        
        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create tab pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefWidth(500);
        tabPane.setPrefHeight(400);
        
        // Add tabs
        tabPane.getTabs().add(createInfoTab());      // Editable metadata
        tabPane.getTabs().add(createLyricsTab());    // Editable lyrics
        tabPane.getTabs().add(createTechnicalTab()); // Read-only technical info

        dialog.getDialogPane().setContent(tabPane);
        
        // Load current metadata
        loadMetadata();
        
        // Show dialog and handle result
        Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == saveButtonType) {
            return saveMetadata();
        }
        
        return false;
    }

    private Tab createInfoTab() {
        Tab infoTab = new Tab("Info");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Title
        grid.add(new Label("Title:"), 0, row);
        titleField = new TextField();
        titleField.setPrefWidth(350);
        grid.add(titleField, 1, row++);
        
        // Artist
        grid.add(new Label("Artist:"), 0, row);
        artistField = new TextField();
        artistField.setPrefWidth(350);
        grid.add(artistField, 1, row++);
        
        // Album
        grid.add(new Label("Album:"), 0, row);
        albumField = new TextField();
        albumField.setPrefWidth(350);
        grid.add(albumField, 1, row++);
        
        // Album Artist
        grid.add(new Label("Album Artist:"), 0, row);
        albumArtistField = new TextField();
        albumArtistField.setPrefWidth(350);
        grid.add(albumArtistField, 1, row++);
        
        // Year
        grid.add(new Label("Year:"), 0, row);
        yearField = new TextField();
        yearField.setPrefWidth(350);
        grid.add(yearField, 1, row++);
        
        // Track Number
        grid.add(new Label("Track Number:"), 0, row);
        trackNumberField = new TextField();
        trackNumberField.setPrefWidth(350);
        grid.add(trackNumberField, 1, row++);
        
        // Disc Number
        grid.add(new Label("Disc Number:"), 0, row);
        discNumberField = new TextField();
        discNumberField.setPrefWidth(350);
        grid.add(discNumberField, 1, row++);

        // Genre
        grid.add(new Label("Genre:"), 0, row);
        genreField = new TextField();
        genreField.setPrefWidth(350);
        grid.add(genreField, 1, row++);
        
        // Composer
        grid.add(new Label("Composer:"), 0, row);
        composerField = new TextField();
        composerField.setPrefWidth(350);
        grid.add(composerField, 1, row++);
        
        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        infoTab.setContent(scrollPane);
        return infoTab;
    }

    private Tab createLyricsTab() {
        Tab lyricsTab = new Tab("Lyrics");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label label = new Label("Lyrics:");
        
        lyricsArea = new TextArea();
        lyricsArea.setWrapText(true);
        lyricsArea.setPrefRowCount(15);
        VBox.setVgrow(lyricsArea, Priority.ALWAYS);
        
        content.getChildren().addAll(label, lyricsArea);
        
        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        lyricsTab.setContent(scrollPane);
        return lyricsTab;
    }

    private Tab createTechnicalTab() {
        Tab techTab = new Tab("Technical");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        // File path
        grid.add(new Label("File:"), 0, row);
        TextField filePathField = new TextField(music.filePath);
        filePathField.setEditable(false);
        filePathField.setStyle("-fx-opacity: 0.7;");
        filePathField.setPrefWidth(350);
        grid.add(filePathField, 1, row++);

        // Duration
        grid.add(new Label("Duration:"), 0, row);
        TextField durationField = new TextField(music.getFormattedDuration());
        durationField.setEditable(false);
        durationField.setStyle("-fx-opacity: 0.7;");
        durationField.setPrefWidth(350);
        grid.add(durationField, 1, row++);

        // Format
        grid.add(new Label("Format:"), 0, row);
        formatField = new TextField();
        formatField.setEditable(false);
        formatField.setStyle("-fx-opacity: 0.7;");
        formatField.setPrefWidth(350);
        grid.add(formatField, 1, row++);

        // Bitrate
        grid.add(new Label("Bitrate:"), 0, row);
        bitrateField = new TextField();
        bitrateField.setEditable(false);
        bitrateField.setStyle("-fx-opacity: 0.7;");
        bitrateField.setPrefWidth(350);
        grid.add(bitrateField, 1, row++);

        // Sample Rate
        grid.add(new Label("Sample Rate:"), 0, row);
        sampleRateField = new TextField();
        sampleRateField.setEditable(false);
        sampleRateField.setStyle("-fx-opacity: 0.7;");
        sampleRateField.setPrefWidth(350);
        grid.add(sampleRateField, 1, row);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        techTab.setContent(scrollPane);
        return techTab;
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
            if (metadata.getFormat() != null) {
                formatField.setText(metadata.getFormat().getDescription() +
                    (metadata.getEncodingType() != null ? " (" + metadata.getEncodingType() + ")" : ""));
            }
            bitrateField.setText(metadata.getFormattedBitrate());
            sampleRateField.setText(metadata.getFormattedSampleRate());

        } catch (IOException e) {
            // Could not read file, use values from Music object
            titleField.setText(music.title);
            artistField.setText(nvl(music.artist));
            albumField.setText(nvl(music.album));

            showError("Could not read metadata from file: " + e.getMessage());
        }
    }

    /**
     * Saves the modified metadata to the audio file.
     * 
     * @return true if save was successful
     */
    private boolean saveMetadata() {
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
