package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.library.Music;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.Optional;

/**
 * Dialog for editing music track metadata.
 * Provides three tabs: Info (title, artist, album, etc.), Lyrics, and Technical.
 * Changes are saved directly to the audio file using the AudioMetadata class.
 * This class uses FXML for the UI layout (metadata-editor.fxml) and
 * MetadataEditorController for handling the form logic.
 */
public class MetadataEditorDialog {

    private final Music music;

    public MetadataEditorDialog(Music music) {
        this.music = music;
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
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/metadata-editor.fxml"));
            DialogPane dialogPane = loader.load();

            // Get controller and initialize with music
            MetadataEditorController controller = loader.getController();
            controller.setMusic(music);

            // Create dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Metadata");
            dialog.setHeaderText(controller.getHeaderText());
            dialog.setResizable(true);
            dialog.setDialogPane(dialogPane);

            // Add buttons
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Apply dark theme
            ThemeManager.applyDarkTheme(dialog);

            // Set size and center when shown
            dialog.setOnShown(e -> {
                var window = dialog.getDialogPane().getScene().getWindow();
                if (window instanceof javafx.stage.Stage stage) {
                    stage.setMinWidth(720);
                    stage.setMinHeight(700);
                    stage.setWidth(720);
                    stage.setHeight(700);
                    stage.centerOnScreen();
                }
            });

            // Show dialog and handle result
            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == saveButtonType) {
                return controller.saveMetadata();
            }

            return false;

        } catch (IOException e) {
            System.err.println("Error loading metadata editor FXML: " + e.getMessage());
            return false;
        }
    }
}
