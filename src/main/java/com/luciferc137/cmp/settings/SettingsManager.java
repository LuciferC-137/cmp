package com.luciferc137.cmp.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gestionnaire des paramètres de l'application.
 * Gère la lecture et l'écriture des paramètres dans un fichier JSON.
 */
public class SettingsManager {

    private static final String APP_FOLDER_NAME = ".cmp";
    private static final String SETTINGS_FILE_NAME = "settings.json";

    private static SettingsManager instance;

    private final Path settingsFilePath;
    private final Gson gson;
    private Settings settings;

    /**
     * Constructeur privé pour le pattern Singleton.
     */
    private SettingsManager() {
        this.settingsFilePath = getSettingsFilePath();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.settings = load();
    }

    /**
     * Retourne l'instance unique du SettingsManager.
     *
     * @return L'instance du SettingsManager
     */
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    /**
     * Détermine le chemin du fichier de paramètres.
     * Le fichier est stocké dans ~/.cmp/settings.json
     *
     * @return Le chemin vers le fichier de paramètres
     */
    private Path getSettingsFilePath() {
        String userHome = System.getProperty("user.home");
        Path appFolder = Paths.get(userHome, APP_FOLDER_NAME);
        return appFolder.resolve(SETTINGS_FILE_NAME);
    }

    /**
     * Charge les paramètres depuis le fichier JSON.
     * Si le fichier n'existe pas ou est invalide, retourne les paramètres par défaut.
     *
     * @return Les paramètres chargés ou les valeurs par défaut
     */
    private Settings load() {
        if (!Files.exists(settingsFilePath)) {
            return new Settings();
        }

        try {
            String json = Files.readString(settingsFilePath);
            Settings loaded = gson.fromJson(json, Settings.class);
            return loaded != null ? loaded : new Settings();
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des paramètres: " + e.getMessage());
            return new Settings();
        }
    }

    /**
     * Sauvegarde les paramètres dans le fichier JSON.
     * Crée le dossier parent si nécessaire.
     */
    public void save() {
        try {
            // Créer le dossier parent si nécessaire
            Path parentDir = settingsFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Écrire le fichier JSON
            String json = gson.toJson(settings);
            Files.writeString(settingsFilePath, json);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde des paramètres: " + e.getMessage());
        }
    }

    /**
     * Retourne les paramètres actuels.
     *
     * @return Les paramètres
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Obtient le dernier volume utilisé.
     *
     * @return Le volume (0-100)
     */
    public int getLastVolume() {
        return settings.getLastVolume();
    }

    /**
     * Définit et sauvegarde le dernier volume utilisé.
     *
     * @param volume Le volume (0-100)
     */
    public void setLastVolume(int volume) {
        settings.setLastVolume(volume);
        save();
    }

    /**
     * Obtient le chemin du dossier de musique.
     *
     * @return Le chemin du dossier
     */
    public String getMusicFolderPath() {
        return settings.getMusicFolderPath();
    }

    /**
     * Définit et sauvegarde le chemin du dossier de musique.
     *
     * @param path Le chemin du dossier
     */
    public void setMusicFolderPath(String path) {
        settings.setMusicFolderPath(path);
        save();
    }

    /**
     * Recharge les paramètres depuis le fichier.
     * Utile si le fichier a été modifié externellement.
     */
    public void reload() {
        this.settings = load();
    }
}

