package com.luciferc137.cmp.settings;

/**
 * Classe représentant les paramètres de l'application.
 * Cette classe est sérialisée en JSON par SettingsManager.
 */
public class Settings {

    /**
     * Dernier volume utilisé (0-100)
     */
    private int lastVolume = 50;

    /**
     * Chemin du dossier de musique
     */
    private String musicFolderPath = "";

    public Settings() {
        // Constructeur par défaut pour la désérialisation
    }

    public int getLastVolume() {
        return lastVolume;
    }

    public void setLastVolume(int lastVolume) {
        this.lastVolume = Math.max(0, Math.min(100, lastVolume));
    }

    public String getMusicFolderPath() {
        return musicFolderPath;
    }

    public void setMusicFolderPath(String musicFolderPath) {
        this.musicFolderPath = musicFolderPath != null ? musicFolderPath : "";
    }

    @Override
    public String toString() {
        return "Settings{" +
                "lastVolume=" + lastVolume +
                ", musicFolderPath='" + musicFolderPath + '\'' +
                '}';
    }
}

