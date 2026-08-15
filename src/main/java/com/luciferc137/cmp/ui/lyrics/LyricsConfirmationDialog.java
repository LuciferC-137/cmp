package com.luciferc137.cmp.ui.lyrics;

import com.luciferc137.cmp.ui.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Dialog that lets the user review and edit lyrics fetched from an
 * external source before saving them to a file.
 */
public final class LyricsConfirmationDialog {

    private static final double DIALOG_WIDTH = 600;
    private static final double DIALOG_HEIGHT = 550;

    private LyricsConfirmationDialog() {
        // Utility class, not meant to be instantiated
    }

    /**
     * Shows the confirmation dialog and blocks until the user closes it.
     *
     * @param trackTitle    title of the track the lyrics belong to
     * @param trackArtist   artist of the track the lyrics belong to
     * @param fetchedLyrics the lyrics text to review, pre-filled in the editor
     * @return the (possibly edited) lyrics if the user chose to save,
     *         or an empty Optional if the dialog was canceled
     */
    public static Optional<String> show(String trackTitle, String trackArtist, String fetchedLyrics) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Lyrics Found");
        dialog.setHeaderText("Review the fetched lyrics before saving");
        dialog.setResizable(true);

        ThemeManager.applyDarkTheme(dialog);

        TextArea lyricsArea = new TextArea(fetchedLyrics);
        lyricsArea.setWrapText(true);
        lyricsArea.setEditable(true);
        lyricsArea.setPrefRowCount(20);
        lyricsArea.setPrefColumnCount(50);
        VBox.setVgrow(lyricsArea, Priority.ALWAYS);

        dialog.getDialogPane().setContent(buildContent(trackTitle, trackArtist, lyricsArea));

        ButtonType saveButton = new ButtonType("Save to File", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        dialog.setOnShown(e -> centerAndResize(dialog));

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButton) {
            return Optional.of(lyricsArea.getText());
        }
        return Optional.empty();
    }

    private static VBox buildContent(String trackTitle, String trackArtist, TextArea lyricsArea) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label infoLabel = new Label("Lyrics for \"" + trackTitle + "\" by " + trackArtist + ":");
        infoLabel.setStyle("-fx-font-weight: bold;");

        Label hintLabel = new Label("You can edit the lyrics before saving.");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        content.getChildren().addAll(infoLabel, lyricsArea, hintLabel);
        return content;
    }

    private static void centerAndResize(Dialog<ButtonType> dialog) {
        var window = dialog.getDialogPane().getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.setMinWidth(DIALOG_WIDTH);
            stage.setMinHeight(DIALOG_HEIGHT);
            stage.setWidth(DIALOG_WIDTH);
            stage.setHeight(DIALOG_HEIGHT);
            stage.centerOnScreen();
        }
    }
}