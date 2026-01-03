package com.luciferc137.cmp.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Settings manager for the application.
 * Handles reading and writing settings and session data to JSON files.
 */
public class SettingsManager {

    private static final String APP_FOLDER_NAME = ".cmp";
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String SESSION_FILE_NAME = "session.json";

    private static SettingsManager instance;

    private final Path settingsFilePath;
    private final Path sessionFilePath;
    private final Gson gson;
    private Settings settings;
    private PlaybackSession session;

    /**
     * Private constructor for Singleton pattern.
     */
    private SettingsManager() {
        this.settingsFilePath = getAppFolder().resolve(SETTINGS_FILE_NAME);
        this.sessionFilePath = getAppFolder().resolve(SESSION_FILE_NAME);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.settings = loadSettings();
        this.session = loadSession();
    }

    /**
     * Returns the unique instance of SettingsManager.
     */
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    /**
     * Returns the app folder path (~/.cmp/).
     */
    private Path getAppFolder() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, APP_FOLDER_NAME);
    }

    /**
     * Loads settings from the JSON file.
     */
    private Settings loadSettings() {
        if (!Files.exists(settingsFilePath)) {
            return new Settings();
        }

        try {
            String json = Files.readString(settingsFilePath);
            Settings loaded = gson.fromJson(json, Settings.class);
            return loaded != null ? loaded : new Settings();
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
            return new Settings();
        }
    }

    /**
     * Loads playback session from the JSON file.
     */
    private PlaybackSession loadSession() {
        if (!Files.exists(sessionFilePath)) {
            return new PlaybackSession();
        }

        try {
            String json = Files.readString(sessionFilePath);
            PlaybackSession loaded = gson.fromJson(json, PlaybackSession.class);
            return loaded != null ? loaded : new PlaybackSession();
        } catch (IOException e) {
            System.err.println("Error loading session: " + e.getMessage());
            return new PlaybackSession();
        }
    }

    /**
     * Saves settings to the JSON file.
     */
    public void save() {
        try {
            Path parentDir = settingsFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            String json = gson.toJson(settings);
            Files.writeString(settingsFilePath, json);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    /**
     * Saves playback session to the JSON file.
     */
    public synchronized void saveSession() {
        try {
            Path parentDir = sessionFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            String json = gson.toJson(session);
            Files.writeString(sessionFilePath, json,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("Error saving session: " + e.getMessage());
        }
    }

    /**
     * Returns the current settings.
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Returns the current playback session.
     */
    public PlaybackSession getSession() {
        return session;
    }

    /**
     * Gets the last used volume.
     */
    public int getLastVolume() {
        return settings.getLastVolume();
    }

    /**
     * Sets and saves the last used volume.
     */
    public void setLastVolume(int volume) {
        settings.setLastVolume(volume);
        save();
    }

    /**
     * Gets the music folder path.
     */
    public String getMusicFolderPath() {
        return settings.getMusicFolderPath();
    }

    /**
     * Sets and saves the music folder path.
     */
    public void setMusicFolderPath(String path) {
        settings.setMusicFolderPath(path);
        save();
    }

    /**
     * Reloads settings from file.
     */
    public void reload() {
        this.settings = loadSettings();
        this.session = loadSession();
    }
}
