package com.luciferc137.cmp.library;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.database.model.MusicEntity;
import com.luciferc137.cmp.database.model.TagEntity;
import com.luciferc137.cmp.database.sync.SyncProgressListener;
import com.luciferc137.cmp.database.sync.SyncResult;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * MusicLibrary is the main interface between the database and the UI.
 * It provides an ObservableList of Music objects that can be filtered, searched, and sorted.
 * This class follows the singleton pattern to ensure a single source of truth for the library.
 */
public class MusicLibrary {

    private static MusicLibrary instance;

    private final LibraryService libraryService;
    private final ObservableList<Music> musicList;
    private final ObservableList<TagEntity> availableTags;
    private final ReadOnlyIntegerWrapper totalCount;
    private final ObjectProperty<AdvancedFilter> advancedFilter;

    // Cache for music tag associations
    private final Map<Long, Set<Long>> musicTagCache;

    // Central cache of Music objects by ID - single source of truth
    private final Map<Long, Music> musicCache;

    // Listeners for library changes
    private Runnable onLibraryChangedListener;

    // Listener for rating changes - will be called whenever a rating is updated
    private Runnable onRatingChangedListener;

    private MusicLibrary() {
        this.libraryService = LibraryService.getInstance();
        this.musicList = FXCollections.observableArrayList();
        this.availableTags = FXCollections.observableArrayList();
        this.totalCount = new ReadOnlyIntegerWrapper(0);
        this.advancedFilter = new SimpleObjectProperty<>(new AdvancedFilter());
        this.musicTagCache = new HashMap<>();
        this.musicCache = new HashMap<>();
    }

    /**
     * Returns the singleton instance of MusicLibrary.
     */
    public static synchronized MusicLibrary getInstance() {
        if (instance == null) {
            instance = new MusicLibrary();
        }
        return instance;
    }

    /**
     * Returns the observable list of music.
     */
    public ObservableList<Music> getMusicList() {
        return musicList;
    }

    /**
     * Returns the list of available tags.
     */
    public ObservableList<TagEntity> getAvailableTags() {
        return availableTags;
    }

    /**
     * Returns the total count of music in the library.
     */
    public ReadOnlyIntegerProperty totalCountProperty() {
        return totalCount.getReadOnlyProperty();
    }

    /**
     * Returns the advanced filter property.
     */
    public ObjectProperty<AdvancedFilter> advancedFilterProperty() {
        return advancedFilter;
    }

    /**
     * Gets the current advanced filter.
     */
    public AdvancedFilter getAdvancedFilter() {
        return advancedFilter.get();
    }

    /**
     * Sets a listener to be called when the library content changes.
     */
    public void setOnLibraryChanged(Runnable listener) {
        this.onLibraryChangedListener = listener;
    }

    /**
     * Sets a listener to be called when any rating is changed.
     * This allows UI components to refresh when ratings are updated from any source.
     */
    public void setOnRatingChanged(Runnable listener) {
        this.onRatingChangedListener = listener;
    }

    /**
     * Gets a Music object by ID from the central cache.
     * Returns null if not found in cache.
     * @param musicId The ID of the music to get
     * @return The cached Music object, or null if not found
     */
    public Music getMusicById(Long musicId) {
        if (musicId == null) return null;
        return musicCache.get(musicId);
    }

    /**
     * Gets Music objects by IDs from the central cache.
     * Only returns objects that are found in the cache.
     * @param musicIds The IDs of the music to get
     * @return List of cached Music objects
     */
    public List<Music> getMusicsByIds(List<Long> musicIds) {
        if (musicIds == null) return new ArrayList<>();
        return musicIds.stream()
                .map(this::getMusicById)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== Refresh & Load ====================

    /**
     * Refreshes the music list from the database using the current filter.
     */
    public void refresh() {
        loadAllMusic();
        loadAvailableTags();
        applyFilterAndSort();
    }

    /**
     * Refreshes the music list asynchronously.
     */
    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.runAsync(this::refresh);
    }

    /**
     * Loads all music from database into memory.
     * Uses the central cache to ensure single instances per music ID.
     */
    private void loadAllMusic() {
        List<MusicEntity> entities = libraryService.getAllMusics();

        List<Music> musics = entities.stream()
                .map(entity -> {
                    // Get or create Music object from cache
                    Music music = musicCache.get(entity.getId());
                    if (music == null) {
                        music = Music.fromEntity(entity);
                        musicCache.put(entity.getId(), music);
                    } else {
                        // Update existing object with latest data from DB
                        music.title = entity.getTitle();
                        music.artist = entity.getArtist();
                        music.album = entity.getAlbum();
                        music.filePath = entity.getPath();
                        music.duration = entity.getDuration();
                        music.setRating(entity.getRating());
                    }

                    // Load tags for this music
                    List<String> tagNames = libraryService.getMusicTagNames(entity.getId());
                    music.setTags(tagNames);

                    // Cache tag IDs
                    Set<Long> tagIds = libraryService.getMusicTagIds(entity.getId());
                    musicTagCache.put(entity.getId(), tagIds);

                    return music;
                })
                .collect(Collectors.toList());

        totalCount.set(musics.size());

        // Store all music and apply filter
        if (Platform.isFxApplicationThread()) {
            musicList.setAll(musics);
        } else {
            Platform.runLater(() -> musicList.setAll(musics));
        }
    }

    /**
     * Loads available tags from database.
     */
    private void loadAvailableTags() {
        List<TagEntity> tags = libraryService.getAllTags();
        if (Platform.isFxApplicationThread()) {
            availableTags.setAll(tags);
        } else {
            Platform.runLater(() -> availableTags.setAll(tags));
        }
    }

    // ==================== Filtering ====================

    /**
     * Applies the current filter and sort to the music list.
     * Uses the central cache to ensure consistent Music instances.
     */
    public void applyFilterAndSort() {
        AdvancedFilter filter = advancedFilter.get();

        // Get all music from database
        List<MusicEntity> allEntities = libraryService.getAllMusics();

        // Convert to Music objects using the cache
        List<Music> allMusic = allEntities.stream()
                .map(entity -> {
                    // Get or create Music object from cache
                    Music music = musicCache.get(entity.getId());
                    if (music == null) {
                        music = Music.fromEntity(entity);
                        musicCache.put(entity.getId(), music);
                    } else {
                        // Update existing object with latest data from DB
                        music.title = entity.getTitle();
                        music.artist = entity.getArtist();
                        music.album = entity.getAlbum();
                        music.filePath = entity.getPath();
                        music.duration = entity.getDuration();
                        music.setRating(entity.getRating());
                    }
                    List<String> tagNames = libraryService.getMusicTagNames(entity.getId());
                    music.setTags(tagNames);
                    return music;
                })
                .toList();

        // Apply filters
        List<Music> filtered = allMusic.stream()
                .filter(music -> {
                    Set<Long> tagIds = musicTagCache.getOrDefault(music.getId(), Collections.emptySet());
                    return filter.matches(music, tagIds);
                })
                .collect(Collectors.toList());

        // Apply sorting
        if (filter.hasSorting()) {
            Comparator<Music> comparator = getComparator(filter.getSortColumn(), filter.getSortState());
            filtered.sort(comparator);
        }

        // Update list
        if (Platform.isFxApplicationThread()) {
            musicList.setAll(filtered);
            notifyLibraryChanged();
        } else {
            Platform.runLater(() -> {
                musicList.setAll(filtered);
                notifyLibraryChanged();
            });
        }
    }

    /**
     * Sets a search query.
     */
    public void search(String query) {
        advancedFilter.get().setSearchQuery(query);
        applyFilterAndSort();
    }

    /**
     * Clears the search query.
     */
    public void clearSearch() {
        advancedFilter.get().clearSearch();
        applyFilterAndSort();
    }

    /**
     * Cycles the sort state for a column.
     */
    public void cycleSort(SortableColumn column) {
        advancedFilter.get().cycleSort(column);
        applyFilterAndSort();
    }

    /**
     * Cycles the filter state for a tag.
     */
    public TagFilterState cycleTagFilter(Long tagId) {
        TagFilterState newState = advancedFilter.get().cycleTagFilter(tagId);
        applyFilterAndSort();
        return newState;
    }

    /**
     * Toggles the selection of a rating in the filter.
     * @return true if the rating is now selected, false otherwise
     */
    public boolean toggleRatingFilter(int rating) {
        boolean newState = advancedFilter.get().toggleRatingFilter(rating);
        applyFilterAndSort();
        return newState;
    }

    /**
     * Clears all filters and sorting.
     */
    public void clearAllFilters() {
        advancedFilter.get().clearAll();
        applyFilterAndSort();
    }

    // ==================== Rating & Tags ====================

    /**
     * Updates the metadata for a music track in the database.
     * The Music object should already have updated title, artist, and album.
     */
    public void updateMusicMetadata(Music music) {
        if (music.getId() == null) return;

        libraryService.updateMusicMetadata(
                music.getId(),
                music.title,
                music.artist,
                music.album
        );

        // Notify listeners
        if (onLibraryChangedListener != null) {
            onLibraryChangedListener.run();
        }
    }

    /**
     * Updates the rating for a music track.
     * Also updates the cached Music object to ensure all views stay synchronized.
     */
    public void updateRating(Music music, int rating) {
        if (music.getId() == null) return;

        libraryService.updateMusicRating(music.getId(), rating);
        music.setRating(rating);

        // Also update the cached instance if it's a different object
        Music cachedMusic = musicCache.get(music.getId());
        if (cachedMusic != null && cachedMusic != music) {
            cachedMusic.setRating(rating);
        }

        // Notify all listeners that a rating has changed (for UI sync)
        if (onRatingChangedListener != null) {
            onRatingChangedListener.run();
        }

        // Refresh to apply rating filters
        if (advancedFilter.get().hasActiveRatingFilters()) {
            applyFilterAndSort();
        }
    }

    /**
     * Adds a tag to a music track.
     */
    public void addTagToMusic(Music music, TagEntity tag) {
        if (music.getId() == null || tag.getId() == null) return;

        libraryService.addTagToMusic(music.getId(), tag.getId());
        music.addTag(tag.getName());

        // Update cache
        musicTagCache.computeIfAbsent(music.getId(), k -> new HashSet<>()).add(tag.getId());

        // Refresh if tag filters are active
        if (advancedFilter.get().hasActiveTagFilters()) {
            applyFilterAndSort();
        }
    }

    /**
     * Removes a tag from a music track.
     */
    public void removeTagFromMusic(Music music, TagEntity tag) {
        if (music.getId() == null || tag.getId() == null) return;

        libraryService.removeTagFromMusic(music.getId(), tag.getId());
        music.removeTag(tag.getName());

        // Update cache
        Set<Long> tagIds = musicTagCache.get(music.getId());
        if (tagIds != null) {
            tagIds.remove(tag.getId());
        }

        // Refresh if tag filters are active
        if (advancedFilter.get().hasActiveTagFilters()) {
            applyFilterAndSort();
        }
    }

    /**
     * Creates a new tag.
     */
    public Optional<TagEntity> createTag(String name, String color) {
        Optional<TagEntity> tag = libraryService.createTag(name, color);
        tag.ifPresent(availableTags::add);
        return tag;
    }

    /**
     * Deletes a tag.
     */
    public void deleteTag(TagEntity tag) {
        if (tag.getId() == null) return;

        libraryService.deleteTag(tag.getId());

        // Remove from available tags
        availableTags.remove(tag);

        // Remove from all music (in cache)
        for (Music music : musicCache.values()) {
            music.removeTag(tag.getName());
        }

        // Remove from music tag cache
        musicTagCache.values().forEach(tagIds -> tagIds.remove(tag.getId()));

        // Refresh music list if tag filters are active
        if (advancedFilter.get().hasActiveTagFilters()) {
            applyFilterAndSort();
        }
    }

    /**
     * Notifie les listeners que la bibliothèque a changé.
     */
    private void notifyLibraryChanged() {
        if (onLibraryChangedListener != null) {
            onLibraryChangedListener.run();
        }
    }

    /**
     * Retourne le comparateur pour le tri des musiques selon la colonne et l'état.
     */
    private Comparator<Music> getComparator(SortableColumn column, ColumnSortState state) {
        Comparator<Music> comparator;
        switch (column) {
            case TITLE -> comparator = Comparator.comparing(m -> m.title, Comparator.nullsLast(String::compareToIgnoreCase));
            case ARTIST -> comparator = Comparator.comparing(m -> m.artist, Comparator.nullsLast(String::compareToIgnoreCase));
            case ALBUM -> comparator = Comparator.comparing(m -> m.album, Comparator.nullsLast(String::compareToIgnoreCase));
            case DURATION -> comparator = Comparator.comparingLong(m -> m.duration);
            default -> comparator = Comparator.comparing(m -> m.title, Comparator.nullsLast(String::compareToIgnoreCase));
        }
        if (state == ColumnSortState.DESCENDING) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    /**
     * Synchronise le dossier musical avec la bibliothèque et notifie la progression.
     * Lance la synchronisation en tâche de fond et rafraîchit la bibliothèque à la fin.
     * @param folderPath Chemin du dossier à synchroniser
     * @param listener Listener de progression pour le suivi
     */
    public void syncFolder(String folderPath, com.luciferc137.cmp.database.sync.SyncProgressListener listener) {
        new Thread(() -> {
            // Délègue la synchronisation à LibraryService
            com.luciferc137.cmp.database.sync.SyncResult result = libraryService.syncFolder(folderPath, listener);
            // Rafraîchit la bibliothèque après la synchro
            Platform.runLater(this::refresh);
        }).start();
    }
}
