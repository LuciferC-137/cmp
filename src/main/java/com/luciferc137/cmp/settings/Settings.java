package com.luciferc137.cmp.settings;

import com.luciferc137.cmp.shader.ShaderBackgroundEngine;

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

    /**
     * Background render mode
     */
    private ShaderBackgroundEngine.RenderMode renderMode = ShaderBackgroundEngine.RenderMode.SHADER;

    public Settings() {
        // Default constructor for deserialization
    }

    public int getLastVolume() {
        return lastVolume;
    }

    public void setLastVolume(int lastVolume) {
        this.lastVolume = Math.clamp(lastVolume, 0, 100);
    }

    public String getMusicFolderPath() {
        return musicFolderPath;
    }

    public void setMusicFolderPath(String musicFolderPath) {
        this.musicFolderPath = musicFolderPath != null ? musicFolderPath : "";
    }

    public ShaderBackgroundEngine.RenderMode getRenderMode() {
        return renderMode;
    }

    public void setRenderMode(ShaderBackgroundEngine.RenderMode renderMode) {
        this.renderMode = renderMode;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "lastVolume=" + lastVolume +
                ", musicFolderPath='" + musicFolderPath + '\'' +
                ", renderMode=" + renderMode.toString() +
                '}';
    }
}

