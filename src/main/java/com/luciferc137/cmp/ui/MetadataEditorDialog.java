package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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

    // Cover art
    private ImageView coverArtView;
    private static final int COVER_ART_SIZE = 100;

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
        dialog.setWidth(670);
        dialog.setHeight(600);

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
        tabPane.setPrefWidth(620);
        tabPane.setPrefHeight(500);
        
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
        
        // Main container: cover art on left, fields on right
        HBox mainContainer = new HBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setAlignment(Pos.TOP_LEFT);

        // Cover art container
        VBox coverContainer = new VBox(10);
        coverContainer.setAlignment(Pos.TOP_CENTER);

        coverArtView = new ImageView();
        coverArtView.setFitWidth(COVER_ART_SIZE);
        coverArtView.setFitHeight(COVER_ART_SIZE);
        coverArtView.setPreserveRatio(true);
        coverArtView.setSmooth(true);

        // Set default cover initially
        coverArtView.setImage(CoverArtLoader.getDefaultCover(COVER_ART_SIZE));

        // Style the cover art container - make it clickable
        VBox imageWrapper = new VBox(coverArtView);
        imageWrapper.setAlignment(Pos.CENTER);
        imageWrapper.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-color: #2a2a2a; -fx-background-radius: 4;");
        imageWrapper.setPadding(new Insets(5));
        imageWrapper.setMinSize(COVER_ART_SIZE + 10, COVER_ART_SIZE + 10);
        imageWrapper.setMaxSize(COVER_ART_SIZE + 10, COVER_ART_SIZE + 10);
        imageWrapper.setCursor(Cursor.HAND);

        // Click handler to change cover art
        imageWrapper.setOnMouseClicked(event -> onChangeCoverArt());

        // Hover effect
        imageWrapper.setOnMouseEntered(e ->
            imageWrapper.setStyle("-fx-border-color: #1E90FF; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-color: #2a2a2a; -fx-background-radius: 4;"));
        imageWrapper.setOnMouseExited(e ->
            imageWrapper.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-color: #2a2a2a; -fx-background-radius: 4;"));

        Label coverLabel = new Label("Click to change");
        coverLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        coverContainer.getChildren().addAll(imageWrapper, coverLabel);

        // Fields grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        int row = 0;
        
        // Title
        grid.add(new Label("Title:"), 0, row);
        titleField = new TextField();
        titleField.setPrefWidth(300);
        grid.add(titleField, 1, row++);
        
        // Artist
        grid.add(new Label("Artist:"), 0, row);
        artistField = new TextField();
        artistField.setPrefWidth(300);
        grid.add(artistField, 1, row++);
        
        // Album
        grid.add(new Label("Album:"), 0, row);
        albumField = new TextField();
        albumField.setPrefWidth(300);
        grid.add(albumField, 1, row++);
        
        // Album Artist
        grid.add(new Label("Album Artist:"), 0, row);
        albumArtistField = new TextField();
        albumArtistField.setPrefWidth(300);
        grid.add(albumArtistField, 1, row++);
        
        // Year
        grid.add(new Label("Year:"), 0, row);
        yearField = new TextField();
        yearField.setPrefWidth(100);
        grid.add(yearField, 1, row++);
        
        // Track Number
        grid.add(new Label("Track Number:"), 0, row);
        trackNumberField = new TextField();
        trackNumberField.setPrefWidth(100);
        grid.add(trackNumberField, 1, row++);
        
        // Disc Number
        grid.add(new Label("Disc Number:"), 0, row);
        discNumberField = new TextField();
        discNumberField.setPrefWidth(100);
        grid.add(discNumberField, 1, row++);

        // Genre
        grid.add(new Label("Genre:"), 0, row);
        genreField = new TextField();
        genreField.setPrefWidth(200);
        grid.add(genreField, 1, row++);
        
        // Composer
        grid.add(new Label("Composer:"), 0, row);
        composerField = new TextField();
        composerField.setPrefWidth(300);
        grid.add(composerField, 1, row++);
        
        HBox.setHgrow(grid, Priority.ALWAYS);
        mainContainer.getChildren().addAll(coverContainer, grid);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(mainContainer);
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

            // Load cover art using CoverArtLoader
            coverArtView.setImage(CoverArtLoader.loadCoverArt(metadata, COVER_ART_SIZE));

        } catch (IOException e) {
            // Could not read file, use values from Music object
            titleField.setText(music.title);
            artistField.setText(nvl(music.artist));
            albumField.setText(nvl(music.album));

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
