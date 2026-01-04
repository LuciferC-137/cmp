package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.audio.AudioMetadata;
import com.luciferc137.cmp.library.Music;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Dialog for changing cover art for multiple music tracks at once.
 * Allows selecting an image file and applying it to all selected tracks.
 */
public class BatchCoverArtDialog {

    private static final int COVER_ART_SIZE = 120;

    private final List<Music> musicList;
    private final Runnable onComplete;

    private ImageView coverArtView;
    private byte[] selectedImageData;
    private String selectedMimeType;
    private Label statusLabel;

    public BatchCoverArtDialog(List<Music> musicList, Runnable onComplete) {
        this.musicList = musicList;
        this.onComplete = onComplete;
    }

    /**
     * Shows the batch cover art change dialog.
     *
     * @param musicList The list of music tracks to update
     * @param onComplete Callback when operation completes
     */
    public static void show(List<Music> musicList, Runnable onComplete) {
        if (musicList == null || musicList.isEmpty()) {
            return;
        }
        BatchCoverArtDialog dialog = new BatchCoverArtDialog(musicList, onComplete);
        dialog.showDialog();
    }

    private void showDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Cover Art");
        dialog.setHeaderText("Change cover art for " + musicList.size() + " track(s)");
        dialog.setResizable(true);

        // Center window on screen when shown
        dialog.setOnShown(e -> dialog.getDialogPane().getScene().getWindow().centerOnScreen());

        // Apply dark theme
        ThemeManager.applyDarkTheme(dialog);

        // Add buttons
        ButtonType applyButtonType = new ButtonType("Apply to All", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        // Main layout: horizontal split - cover art on left, track list on right
        HBox mainContent = new HBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setAlignment(Pos.TOP_LEFT);

        // Left side: Cover art preview
        VBox coverContainer = new VBox(10);
        coverContainer.setAlignment(Pos.TOP_CENTER);
        coverContainer.setMinWidth(COVER_ART_SIZE + 20);

        coverArtView = new ImageView();
        coverArtView.setFitWidth(COVER_ART_SIZE);
        coverArtView.setFitHeight(COVER_ART_SIZE);
        coverArtView.setPreserveRatio(true);
        coverArtView.setSmooth(true);
        coverArtView.setImage(CoverArtLoader.getDefaultCover(COVER_ART_SIZE));

        // Styled wrapper for cover art
        VBox imageWrapper = new VBox(coverArtView);
        imageWrapper.setAlignment(Pos.CENTER);
        imageWrapper.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 4; " +
                "-fx-background-color: #2a2a2a; -fx-background-radius: 4;");
        imageWrapper.setPadding(new Insets(5));
        imageWrapper.setMinSize(COVER_ART_SIZE + 10, COVER_ART_SIZE + 10);
        imageWrapper.setMaxSize(COVER_ART_SIZE + 10, COVER_ART_SIZE + 10);
        imageWrapper.setCursor(Cursor.HAND);

        // Click handler to select image
        imageWrapper.setOnMouseClicked(event -> selectCoverArt(dialog));

        // Hover effect
        imageWrapper.setOnMouseEntered(e ->
                imageWrapper.setStyle("-fx-border-color: #1E90FF; -fx-border-width: 2; -fx-border-radius: 4; " +
                        "-fx-background-color: #2a2a2a; -fx-background-radius: 4;"));
        imageWrapper.setOnMouseExited(e ->
                imageWrapper.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-border-radius: 4; " +
                        "-fx-background-color: #2a2a2a; -fx-background-radius: 4;"));

        Label clickLabel = new Label("Click to select");
        clickLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        // Status label
        statusLabel = new Label("No image selected");
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(COVER_ART_SIZE + 10);

        coverContainer.getChildren().addAll(imageWrapper, clickLabel, statusLabel);

        // Right side: Track list with ScrollPane
        VBox trackListContainer = new VBox(8);
        trackListContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(trackListContainer, Priority.ALWAYS);

        Label tracksLabel = new Label("Tracks to update (" + musicList.size() + "):");
        tracksLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        // ListView for all tracks with scroll
        ListView<String> trackListView = new ListView<>();
        trackListView.setItems(FXCollections.observableArrayList(
                musicList.stream()
                        .map(m -> m.title + (m.artist != null ? " - " + m.artist : ""))
                        .toList()
        ));
        trackListView.setPrefHeight(200);
        trackListView.setMinHeight(150);
        trackListView.setStyle("-fx-font-size: 11px;");
        VBox.setVgrow(trackListView, Priority.ALWAYS);

        trackListContainer.getChildren().addAll(tracksLabel, trackListView);

        mainContent.getChildren().addAll(coverContainer, trackListContainer);

        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(320);

        // Disable apply button until an image is selected
        dialog.getDialogPane().lookupButton(applyButtonType).setDisable(true);

        // Show dialog and handle result
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == applyButtonType && selectedImageData != null) {
            applyCoverArtToAll();
        }
    }

    private void selectCoverArt(Dialog<?> dialog) {
        File imageFile = CoverArtLoader.showCoverArtChooser(dialog.getDialogPane().getScene().getWindow());
        if (imageFile != null) {
            byte[] imageData = CoverArtLoader.loadImageAsBytes(imageFile);
            if (imageData != null) {
                selectedImageData = imageData;
                selectedMimeType = CoverArtLoader.getMimeType(imageFile);

                // Update preview
                Image newCover = CoverArtLoader.loadCoverArt(imageData, COVER_ART_SIZE);
                if (newCover != null) {
                    coverArtView.setImage(newCover);
                }

                statusLabel.setText(imageFile.getName());
                statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #4CAF50;");

                // Enable apply button
                ButtonType applyButtonType = dialog.getDialogPane().getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst().orElse(null);
                if (applyButtonType != null) {
                    dialog.getDialogPane().lookupButton(applyButtonType).setDisable(false);
                }
            }
        }
    }

    private void applyCoverArtToAll() {
        if (selectedImageData == null) return;

        // Apply synchronously (no popups)
        for (Music music : musicList) {
            try {
                File audioFile = new File(music.filePath);
                if (audioFile.exists()) {
                    AudioMetadata metadata = AudioMetadata.fromFile(audioFile);
                    metadata.setCoverArt(selectedImageData);
                    metadata.setCoverArtMimeType(selectedMimeType);
                    metadata.saveToFile(audioFile);
                }
            } catch (IOException e) {
                System.err.println("Error updating cover art for " + music.title + ": " + e.getMessage());
            }
        }

        // Call completion callback
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
