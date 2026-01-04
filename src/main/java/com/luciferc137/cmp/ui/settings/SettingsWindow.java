package com.luciferc137.cmp.ui.settings;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Utility class for opening the settings window.
 */
public class SettingsWindow {

    private static Stage settingsStage = null;
    private static SettingsController currentController = null;
    private static Runnable onPlaylistsChangedCallback = null;

    /**
     * Sets the callback to be called when playlists change.
     */
    public static void setOnPlaylistsChangedCallback(Runnable callback) {
        onPlaylistsChangedCallback = callback;
    }

    /**
     * Opens the settings window.
     * If already open, brings it to front.
     *
     * @param owner The parent window
     */
    public static void show(Window owner) {
        show(owner, null);
    }

    /**
     * Opens the settings window on a specific category.
     * If already open, brings it to front and switches to the specified category.
     *
     * @param owner The parent window
     * @param initialCategory The category to show initially (e.g., "Library", "Playlists")
     */
    public static void show(Window owner, String initialCategory) {
        // If window is already open, bring it to front and switch category if needed
        if (settingsStage != null && settingsStage.isShowing()) {
            settingsStage.toFront();
            settingsStage.requestFocus();
            if (initialCategory != null && currentController != null) {
                currentController.selectCategory(initialCategory);
            }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    SettingsWindow.class.getResource("/ui/settings/settings.fxml")
            );

            Stage stage = new Stage();
            stage.setTitle("Settings");
            Scene scene = new Scene(loader.load());

            // Get the controller and set the callback
            SettingsController controller = loader.getController();
            currentController = controller;
            if (controller != null) {
                if (onPlaylistsChangedCallback != null) {
                    controller.setOnPlaylistsChangedCallback(onPlaylistsChangedCallback);
                }
                if (initialCategory != null) {
                    controller.selectCategory(initialCategory);
                }
            }

            // Apply dark theme stylesheet
            scene.getStylesheets().add(SettingsWindow.class.getResource("/ui/styles/dark-theme.css").toExternalForm());

            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setResizable(true);
            stage.setMinWidth(700);
            stage.setMinHeight(650);

            // Clean up reference when window is closed
            stage.setOnHidden(e -> {
                settingsStage = null;
                currentController = null;
            });

            settingsStage = stage;
            stage.show();

        } catch (IOException e) {
            System.err.println("Error opening settings: " + e.getMessage());
        }
    }

    /**
     * Closes the settings window if it's open.
     */
    public static void close() {
        if (settingsStage != null && settingsStage.isShowing()) {
            settingsStage.close();
            settingsStage = null;
        }
    }
}
