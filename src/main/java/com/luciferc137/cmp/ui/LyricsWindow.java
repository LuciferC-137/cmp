package com.luciferc137.cmp.ui;

import com.luciferc137.cmp.library.Music;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;

/**
 * Manages the lyrics display window.
 * The window is non-modal, allowing the user to continue using the main application.
 */
public class LyricsWindow {

    private static Stage lyricsStage;
    private static LyricsController controller;

    /**
     * Shows the lyrics window. If already open, brings it to front and updates content.
     *
     * @param ownerWindow The owner window (for positioning)
     * @param currentMusic The currently playing music track
     * @param onMetadataChanged Callback when metadata is changed via the edit button
     */
    public static void show(Window ownerWindow, Music currentMusic, Runnable onMetadataChanged) {

        if (lyricsStage != null && lyricsStage.isShowing()) {
            // Window already open, just update content and bring to front
            if (controller != null) {
                controller.setMusic(currentMusic);
            }
            lyricsStage.toFront();
            lyricsStage.requestFocus();
            lyricsStage.centerOnScreen();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    LyricsWindow.class.getResource("/ui/lyrics.fxml")
            );
            
            lyricsStage = new Stage();
            Scene scene = new Scene(loader.load());
            
            // Apply dark theme
            scene.getStylesheets().add(ThemeManager.getDarkThemeUrl());
            
            controller = loader.getController();
            controller.setMusic(currentMusic);
            controller.setOnMetadataChanged(onMetadataChanged);
            
            lyricsStage.setTitle("Lyrics");
            lyricsStage.setScene(scene);
            lyricsStage.setMinWidth(700);
            lyricsStage.setMinHeight(650);
            
            // Try to load the lyrics icon
            try {
                Image icon = new Image(Objects.requireNonNull(
                        LyricsWindow.class.getResourceAsStream("/icons/lyrics.png")));
                lyricsStage.getIcons().add(icon);
            } catch (Exception e) {
                // Icon not available, continue without it
            }

            
            // Clear references when window is closed
            lyricsStage.setOnHidden(event -> {
                lyricsStage = null;
                controller = null;
            });
            
            // Center on screen after the window is shown and sized
            lyricsStage.setOnShown(event -> lyricsStage.centerOnScreen());

            lyricsStage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading lyrics window: " + e.getMessage());
        }
    }

    /**
     * Updates the lyrics window with a new track (if window is open).
     *
     * @param music The music track to display
     */
    public static void updateCurrentTrack(Music music) {
        if (lyricsStage != null && lyricsStage.isShowing() && controller != null) {
            controller.setMusic(music);
        }
    }

    /**
     * Checks if the lyrics window is currently open.
     *
     * @return true if the window is open
     */
    public static boolean isShowing() {
        return lyricsStage != null && lyricsStage.isShowing();
    }

    /**
     * Closes the lyrics window if it's open.
     */
    public static void close() {
        if (lyricsStage != null) {
            lyricsStage.close();
        }
    }
}

