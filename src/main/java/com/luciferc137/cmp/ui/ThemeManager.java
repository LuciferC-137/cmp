package com.luciferc137.cmp.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;

/**
 * Utility class for applying themes to JavaFX components.
 */
public class ThemeManager {
    
    private static final String DARK_THEME_PATH = "/ui/styles/dark-theme.css";
    
    /**
     * Gets the dark theme stylesheet URL.
     */
    public static String getDarkThemeUrl() {
        return ThemeManager.class.getResource(DARK_THEME_PATH).toExternalForm();
    }
    
    /**
     * Applies the dark theme to a Dialog.
     */
    public static void applyDarkTheme(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(getDarkThemeUrl());
    }
    
    /**
     * Applies the dark theme to an Alert.
     */
    public static void applyDarkTheme(Alert alert) {
        alert.getDialogPane().getStylesheets().add(getDarkThemeUrl());
    }
}

