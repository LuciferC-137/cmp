package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.Optional;

/**
 * Dialog for editing music track metadata.
 * Provides two tabs: Info (title, artist, album, etc.) and Lyrics.
 * Changes are saved directly to the audio file using JAudioTagger.
 */
public class MetadataEditorDialog {

    private final Music music;
    private final File audioFile;
    
    // Info tab fields
    private TextField titleField;
    private TextField artistField;
    private TextField albumField;
    private TextField albumArtistField;
    private TextField yearField;
    private TextField trackNumberField;
    private TextField genreField;
    private TextField composerField;
    
    // Lyrics tab
    private TextArea lyricsArea;

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
        dialog.setHeaderText(music.title + " - " + music.artist);
        dialog.setResizable(true);
        dialog.setHeight(600); // Valeur plus raisonnable
        dialog.setWidth(500);

        // Centrer la fenêtre lors de l'affichage
        dialog.setOnShown(e -> {
            dialog.getDialogPane().getScene().getWindow().centerOnScreen();
        });

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
        tabPane.getTabs().add(createInfoTab());
        tabPane.getTabs().add(createLyricsTab());
        
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
        grid.add(artistField, 1, row++);
        
        // Album
        grid.add(new Label("Album:"), 0, row);
        albumField = new TextField();
        grid.add(albumField, 1, row++);
        
        // Album Artist
        grid.add(new Label("Album Artist:"), 0, row);
        albumArtistField = new TextField();
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
        
        // Genre
        grid.add(new Label("Genre:"), 0, row);
        genreField = new TextField();
        grid.add(genreField, 1, row++);
        
        // Composer
        grid.add(new Label("Composer:"), 0, row);
        composerField = new TextField();
        grid.add(composerField, 1, row++);
        
        // File path (read-only)
        grid.add(new Label("File:"), 0, row);
        TextField filePathField = new TextField(music.filePath);
        filePathField.setEditable(false);
        filePathField.setStyle("-fx-opacity: 0.7;");
        grid.add(filePathField, 1, row++);
        
        // Duration (read-only)
        grid.add(new Label("Duration:"), 0, row);
        TextField durationField = new TextField(music.getFormattedDuration());
        durationField.setEditable(false);
        durationField.setStyle("-fx-opacity: 0.7;");
        durationField.setPrefWidth(100);
        grid.add(durationField, 1, row);
        
        // Ajout du ScrollPane autour du GridPane
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
        
        // Ajout du ScrollPane autour du VBox
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        lyricsTab.setContent(scrollPane);
        return lyricsTab;
    }

    /**
     * Loads the current metadata from the audio file into the form fields.
     */
    private void loadMetadata() {
        try {
            AudioFile audioFile = AudioFileIO.read(this.audioFile);
            Tag tag = audioFile.getTag();
            
            if (tag != null) {
                titleField.setText(getTagValue(tag, FieldKey.TITLE));
                artistField.setText(getTagValue(tag, FieldKey.ARTIST));
                albumField.setText(getTagValue(tag, FieldKey.ALBUM));
                albumArtistField.setText(getTagValue(tag, FieldKey.ALBUM_ARTIST));
                yearField.setText(getTagValue(tag, FieldKey.YEAR));
                trackNumberField.setText(getTagValue(tag, FieldKey.TRACK));
                genreField.setText(getTagValue(tag, FieldKey.GENRE));
                composerField.setText(getTagValue(tag, FieldKey.COMPOSER));
                lyricsArea.setText(getTagValue(tag, FieldKey.LYRICS));
            } else {
                // No tag, use default values from Music object
                titleField.setText(music.title);
                artistField.setText(music.artist != null ? music.artist : "");
                albumField.setText(music.album != null ? music.album : "");
            }
        } catch (Exception e) {
            // Could not read file, use values from Music object
            titleField.setText(music.title);
            artistField.setText(music.artist != null ? music.artist : "");
            albumField.setText(music.album != null ? music.album : "");
            
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
            AudioFile audioFile = AudioFileIO.read(this.audioFile);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            
            // Update tag fields
            setTagValue(tag, FieldKey.TITLE, titleField.getText());
            setTagValue(tag, FieldKey.ARTIST, artistField.getText());
            setTagValue(tag, FieldKey.ALBUM, albumField.getText());
            setTagValue(tag, FieldKey.ALBUM_ARTIST, albumArtistField.getText());
            setTagValue(tag, FieldKey.YEAR, yearField.getText());
            setTagValue(tag, FieldKey.TRACK, trackNumberField.getText());
            setTagValue(tag, FieldKey.GENRE, genreField.getText());
            setTagValue(tag, FieldKey.COMPOSER, composerField.getText());
            setTagValue(tag, FieldKey.LYRICS, lyricsArea.getText());
            
            // Save the file
            audioFile.commit();
            
            // Update the Music object to reflect the changes in the UI
            music.title = titleField.getText().trim();
            music.artist = artistField.getText().trim();
            music.album = albumField.getText().trim();
            
            // Also update in the database if the music has an ID
            if (music.getId() != null) {
                MusicLibrary.getInstance().updateMusicMetadata(music);
            }
            
            return true;
            
        } catch (Exception e) {
            showError("Could not save metadata: " + e.getMessage());
            System.err.println("Error saving metadata: " + e.getMessage());
            return false;
        }
    }

    private String getTagValue(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return value != null ? value : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void setTagValue(Tag tag, FieldKey key, String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                tag.setField(key, value.trim());
            } else {
                tag.deleteField(key);
            }
        } catch (Exception e) {
            // Some fields might not be supported for certain formats
            System.err.println("Could not set field " + key + ": " + e.getMessage());
        }
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
