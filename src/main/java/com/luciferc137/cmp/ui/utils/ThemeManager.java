package com.luciferc137.cmp.ui.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;

import java.util.Objects;

/**
 * Utility class for applying themes to JavaFX components.
 */
public class ThemeManager {
    
    private static final String DARK_THEME_PATH = "/ui/styles/dark-theme.css";
    
    /**
     * Gets the dark theme stylesheet URL.
     */
    public static String getDarkThemeUrl() {
        return Objects.requireNonNull(ThemeManager.class.getResource(DARK_THEME_PATH))
                .toExternalForm();
    }

    /**
     * Applies the dark theme to a JavaFX Scene.
     */
    public static void applyDarkTheme(Scene scene) {
        scene.getStylesheets().add(getDarkThemeUrl());
        scene.getRoot().getStyleClass().add("secondary-window");
    }

    /**
     * Applies the dark theme to a JavaFX Parent.
     */
    public static void applyDarkTheme(Parent root) {
        root.getStylesheets().add(getDarkThemeUrl());
        root.getStyleClass().add("secondary-window");
    }
    
    /**
     * Applies the dark theme to a Dialog.
     */
    public static void applyDarkTheme(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(getDarkThemeUrl());
        dialog.getDialogPane().getStyleClass().add("secondary-window");
    }
    
    /**
     * Applies the dark theme to an Alert.
     */
    public static void applyDarkTheme(Alert alert) {
        alert.getDialogPane().getStylesheets().add(getDarkThemeUrl());
        alert.getDialogPane().getStyleClass().add("secondary-window");
    }
}

