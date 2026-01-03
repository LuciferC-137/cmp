package com.luciferc137.cmp.database;

import com.luciferc137.cmp.database.dao.MusicDao;
import com.luciferc137.cmp.database.dao.PlaylistDao;
import com.luciferc137.cmp.database.dao.TagDao;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.PlaylistEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.database.sync.LibrarySyncService;
import com.luciferc137.cmp.database.sync.SyncProgressListener;
import com.luciferc137.cmp.database.sync.SyncResult;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service principal pour la gestion de la bibliothèque musicale.
 * Fournit une interface simplifiée pour accéder aux différentes fonctionnalités.
 */
public class LibraryService {

    private static LibraryService instance;

    private final MusicDao musicDao;
    private final PlaylistDao playlistDao;
    private final TagDao tagDao;
    private final LibrarySyncService syncService;

    private LibraryService() {
        // Initialiser la base de données
        DatabaseManager.getInstance();

        this.musicDao = new MusicDao();
        this.playlistDao = new PlaylistDao();
        this.tagDao = new TagDao();
        this.syncService = new LibrarySyncService();
    }

    /**
     * Retourne l'instance unique du LibraryService.
     *
     * @return L'instance du LibraryService
     */
    public static synchronized LibraryService getInstance() {
        if (instance == null) {
            instance = new LibraryService();
        }
        return instance;
    }

    // ==================== Synchronisation ====================

    /**
     * Synchronise un dossier avec la bibliothèque (synchrone).
     *
     * @param folderPath Le chemin du dossier
     * @return Le résultat de la synchronisation
     */
    public SyncResult syncFolder(String folderPath) {
        return syncService.syncFolder(folderPath);
    }

    /**
     * Synchronise un dossier avec la bibliothèque (synchrone avec listener).
     *
     * @param folderPath Le chemin du dossier
     * @param listener Le listener de progression
     * @return Le résultat de la synchronisation
     */
    public SyncResult syncFolder(String folderPath, SyncProgressListener listener) {
        return syncService.syncFolder(folderPath, listener);
    }

    /**
     * Synchronise un dossier avec la bibliothèque (asynchrone).
     *
     * @param folderPath Le chemin du dossier
     * @return Un CompletableFuture avec le résultat
     */
    public CompletableFuture<SyncResult> syncFolderAsync(String folderPath) {
        return syncService.syncFolderAsync(folderPath);
    }

    /**
     * Synchronise un dossier avec la bibliothèque (asynchrone avec listener).
     *
     * @param folderPath Le chemin du dossier
     * @param listener Le listener de progression
     * @return Un CompletableFuture avec le résultat
     */
    public CompletableFuture<SyncResult> syncFolderAsync(String folderPath, SyncProgressListener listener) {
        return syncService.syncFolderAsync(folderPath, listener);
    }

    /**
     * Vérifie si une synchronisation est en cours.
     *
     * @return true si une sync est en cours
     */
    public boolean isSyncing() {
        return syncService.isSyncing();
    }

    /**
     * Annule la synchronisation en cours.
     */
    public void cancelSync() {
        syncService.cancelSync();
    }

    // ==================== Musiques ====================

    /**
     * Retourne toutes les musiques de la bibliothèque.
     *
     * @return Liste de toutes les musiques
     */
    public List<MusicEntity> getAllMusics() {
        try {
            return musicDao.findAll();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des musiques: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Recherche une musique par son ID.
     *
     * @param id L'ID de la musique
     * @return La musique si trouvée
     */
    public Optional<MusicEntity> getMusicById(long id) {
        try {
            return musicDao.findById(id);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche de musique: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Recherche des musiques par texte (titre, artiste, album).
     *
     * @param query Le texte à rechercher
     * @return Liste des musiques correspondantes
     */
    public List<MusicEntity> searchMusics(String query) {
        try {
            return musicDao.search(query);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retourne les musiques d'un artiste.
     *
     * @param artist Le nom de l'artiste
     * @return Liste des musiques de l'artiste
     */
    public List<MusicEntity> getMusicsByArtist(String artist) {
        try {
            return musicDao.findByArtist(artist);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par artiste: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retourne les musiques d'un album.
     *
     * @param album Le nom de l'album
     * @return Liste des musiques de l'album
     */
    public List<MusicEntity> getMusicsByAlbum(String album) {
        try {
            return musicDao.findByAlbum(album);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par album: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retourne la liste de tous les artistes.
     *
     * @return Liste des artistes
     */
    public List<String> getAllArtists() {
        try {
            return musicDao.findAllArtists();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des artistes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retourne la liste de tous les albums.
     *
     * @return Liste des albums
     */
    public List<String> getAllAlbums() {
        try {
            return musicDao.findAllAlbums();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des albums: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Compte le nombre total de musiques.
     *
     * @return Le nombre de musiques
     */
    public int getMusicCount() {
        try {
            return musicDao.count();
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage: " + e.getMessage());
            return 0;
        }
    }

    // ==================== Playlists ====================

    /**
     * Crée une nouvelle playlist.
     *
     * @param name Le nom de la playlist
     * @return La playlist créée
     */
    public Optional<PlaylistEntity> createPlaylist(String name) {
        try {
            PlaylistEntity playlist = new PlaylistEntity(name);
            playlistDao.insert(playlist);
            return Optional.of(playlist);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la playlist: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retourne toutes les playlists.
     *
     * @return Liste des playlists
     */
    public List<PlaylistEntity> getAllPlaylists() {
        try {
            return playlistDao.findAll();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des playlists: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Supprime une playlist.
     *
     * @param playlistId L'ID de la playlist
     */
    public void deletePlaylist(long playlistId) {
        try {
            playlistDao.delete(playlistId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la playlist: " + e.getMessage());
        }
    }

    /**
     * Ajoute une musique à une playlist.
     *
     * @param playlistId L'ID de la playlist
     * @param musicId L'ID de la musique
     */
    public void addMusicToPlaylist(long playlistId, long musicId) {
        try {
            playlistDao.addMusic(playlistId, musicId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout à la playlist: " + e.getMessage());
        }
    }

    /**
     * Supprime une musique d'une playlist.
     *
     * @param playlistId L'ID de la playlist
     * @param musicId L'ID de la musique
     */
    public void removeMusicFromPlaylist(long playlistId, long musicId) {
        try {
            playlistDao.removeMusic(playlistId, musicId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la playlist: " + e.getMessage());
        }
    }

    /**
     * Retourne les musiques d'une playlist.
     *
     * @param playlistId L'ID de la playlist
     * @return Liste des musiques de la playlist
     */
    public List<MusicEntity> getPlaylistMusics(long playlistId) {
        try {
            return playlistDao.getMusicsByPlaylist(playlistId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des musiques: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== Tags ====================

    /**
     * Crée un nouveau tag.
     *
     * @param name Le nom du tag
     * @return Le tag créé
     */
    public Optional<TagEntity> createTag(String name) {
        return createTag(name, "#808080");
    }

    /**
     * Crée un nouveau tag avec une couleur.
     *
     * @param name Le nom du tag
     * @param color La couleur du tag (hex)
     * @return Le tag créé
     */
    public Optional<TagEntity> createTag(String name, String color) {
        try {
            TagEntity tag = new TagEntity(name, color);
            tagDao.insert(tag);
            return Optional.of(tag);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création du tag: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retourne tous les tags.
     *
     * @return Liste des tags
     */
    public List<TagEntity> getAllTags() {
        try {
            return tagDao.findAll();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des tags: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Supprime un tag.
     *
     * @param tagId L'ID du tag
     */
    public void deleteTag(long tagId) {
        try {
            tagDao.delete(tagId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du tag: " + e.getMessage());
        }
    }

    /**
     * Ajoute un tag à une musique.
     *
     * @param musicId L'ID de la musique
     * @param tagId L'ID du tag
     */
    public void addTagToMusic(long musicId, long tagId) {
        try {
            tagDao.addTagToMusic(musicId, tagId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du tag: " + e.getMessage());
        }
    }

    /**
     * Supprime un tag d'une musique.
     *
     * @param musicId L'ID de la musique
     * @param tagId L'ID du tag
     */
    public void removeTagFromMusic(long musicId, long tagId) {
        try {
            tagDao.removeTagFromMusic(musicId, tagId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du tag: " + e.getMessage());
        }
    }

    /**
     * Retourne les tags d'une musique.
     *
     * @param musicId L'ID de la musique
     * @return Liste des tags de la musique
     */
    public List<TagEntity> getMusicTags(long musicId) {
        try {
            return tagDao.getTagsByMusic(musicId);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des tags: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns music tracks with a specific tag.
     *
     * @param tagId The tag ID
     * @return List of music with this tag
     */
    public List<MusicEntity> getMusicsByTag(long tagId) {
        try {
            return tagDao.getMusicsByTag(tagId);
        } catch (SQLException e) {
            System.err.println("Error searching by tag: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Updates the rating for a music track.
     *
     * @param musicId The music ID
     * @param rating The new rating (0-5)
     */
    public void updateMusicRating(long musicId, int rating) {
        try {
            musicDao.updateRating(musicId, rating);
        } catch (SQLException e) {
            System.err.println("Error updating rating: " + e.getMessage());
        }
    }

    /**
     * Gets all tag IDs for a music track.
     *
     * @param musicId The music ID
     * @return Set of tag IDs
     */
    public java.util.Set<Long> getMusicTagIds(long musicId) {
        try {
            return tagDao.getTagsByMusic(musicId).stream()
                    .map(TagEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (SQLException e) {
            System.err.println("Error getting music tags: " + e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Gets tag names for a music track.
     *
     * @param musicId The music ID
     * @return List of tag names
     */
    public List<String> getMusicTagNames(long musicId) {
        try {
            return tagDao.getTagsByMusic(musicId).stream()
                    .map(TagEntity::getName)
                    .collect(java.util.stream.Collectors.toList());
        } catch (SQLException e) {
            System.err.println("Error getting music tag names: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== Utilities ====================

    /**
     * Closes service resources.
     */
    public void shutdown() {
        syncService.shutdown();
        DatabaseManager.getInstance().close();
    }
}

