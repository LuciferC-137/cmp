package com.luciferc137.cmp.settings;

/**
 * Class representing the application settings.
 * This class is serialized to JSON by SettingsManager.
 */
public class Settings {

    /**
     * Last used volume (0-100)
     */
    private int lastVolume = 50;

    /**
     * Music folder path
     */
    private String musicFolderPath = "";

    public Settings() {
        // Default constructor for deserialization
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

