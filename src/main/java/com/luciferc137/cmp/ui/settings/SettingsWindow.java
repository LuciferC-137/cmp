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

    /**
     * Opens the settings window.
     * If already open, brings it to front.
     *
     * @param owner The parent window
     */
    public static void show(Window owner) {
        // If window is already open, bring it to front
        if (settingsStage != null && settingsStage.isShowing()) {
            settingsStage.toFront();
            settingsStage.requestFocus();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    SettingsWindow.class.getResource("/ui/settings/settings.fxml")
            );

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setResizable(true);
            stage.setMinWidth(500);
            stage.setMinHeight(350);

            // Clean up reference when window is closed
            stage.setOnHidden(e -> settingsStage = null);

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
