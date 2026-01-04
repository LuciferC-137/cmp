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

    // Listeners for library changes
    private Runnable onLibraryChangedListener;

    private MusicLibrary() {
        this.libraryService = LibraryService.getInstance();
        this.musicList = FXCollections.observableArrayList();
        this.availableTags = FXCollections.observableArrayList();
        this.totalCount = new ReadOnlyIntegerWrapper(0);
        this.advancedFilter = new SimpleObjectProperty<>(new AdvancedFilter());
        this.musicTagCache = new HashMap<>();
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
     */
    private void loadAllMusic() {
        List<MusicEntity> entities = libraryService.getAllMusics();

        List<Music> musics = entities.stream()
                .map(entity -> {
                    Music music = Music.fromEntity(entity);
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
     */
    public void applyFilterAndSort() {
        AdvancedFilter filter = advancedFilter.get();

        // Get all music from database
        List<MusicEntity> allEntities = libraryService.getAllMusics();

        // Convert to Music objects with tags
        List<Music> allMusic = allEntities.stream()
                .map(entity -> {
                    Music music = Music.fromEntity(entity);
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
     * Cycles the filter state for a rating.
     */
    public TagFilterState cycleRatingFilter(int rating) {
        TagFilterState newState = advancedFilter.get().cycleRatingFilter(rating);
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
     */
    public void updateRating(Music music, int rating) {
        if (music.getId() == null) return;

        libraryService.updateMusicRating(music.getId(), rating);
        music.setRating(rating);

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
        availableTags.remove(tag);

        // Remove from cache
        musicTagCache.values().forEach(tagIds -> tagIds.remove(tag.getId()));

        // Refresh
        refresh();
    }

    // ==================== Sync ====================

    /**
     * Synchronizes the library with a folder.
     */
    public CompletableFuture<SyncResult> syncFolder(String folderPath, SyncProgressListener listener) {
        return libraryService.syncFolderAsync(folderPath, new SyncProgressListener() {
            @Override
            public void onSyncStarted(int totalFiles) {
                listener.onSyncStarted(totalFiles);
            }

            @Override
            public void onFileProcessed(int currentFile, int totalFiles, String fileName) {
                listener.onFileProcessed(currentFile, totalFiles, fileName);
            }

            @Override
            public void onFileAdded(String path) {
                listener.onFileAdded(path);
            }

            @Override
            public void onFileUpdated(String path) {
                listener.onFileUpdated(path);
            }

            @Override
            public void onFileRemoved(String path) {
                listener.onFileRemoved(path);
            }

            @Override
            public void onError(String path, String error) {
                listener.onError(path, error);
            }

            @Override
            public void onSyncCompleted(SyncResult result) {
                listener.onSyncCompleted(result);
                Platform.runLater(() -> refresh());
            }
        });
    }

    // ==================== Helpers ====================

    /**
     * Returns a comparator for the given column and sort state.
     */
    private Comparator<Music> getComparator(SortableColumn column, ColumnSortState state) {
        if (column == null || state == ColumnSortState.NONE) {
            return Comparator.comparing(m -> 0); // No-op comparator
        }

        Comparator<Music> comparator = switch (column) {
            case TITLE -> Comparator.comparing(m -> m.title != null ? m.title.toLowerCase() : "");
            case ARTIST -> Comparator.comparing(m -> m.artist != null ? m.artist.toLowerCase() : "");
            case ALBUM -> Comparator.comparing(m -> m.album != null ? m.album.toLowerCase() : "");
            case DURATION -> Comparator.comparingLong(m -> m.duration);
        };

        if (state == ColumnSortState.DESCENDING) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    /**
     * Returns the list of all artists.
     */
    public List<String> getAllArtists() {
        return libraryService.getAllArtists();
    }

    /**
     * Returns the list of all albums.
     */
    public List<String> getAllAlbums() {
        return libraryService.getAllAlbums();
    }

    /**
     * Notifies the listener that the library has changed.
     */
    private void notifyLibraryChanged() {
        if (onLibraryChangedListener != null) {
            onLibraryChangedListener.run();
        }
    }
}

