package com.luciferc137.cmp.library;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;

/**
 * Manages the currently playing playlist with support for sequential and shuffle playback.
 */
public class PlaybackQueue {

    /**
     * Loop modes for playback.
     */
    public enum LoopMode {
        /** No looping - stop at end of playlist */
        NONE,
        /** Loop the entire playlist */
        PLAYLIST,
        /** Loop the current single track */
        SINGLE
    }

    private static PlaybackQueue instance;

    private final ObservableList<Music> queue;
    private final ObjectProperty<Music> currentTrack;
    private final IntegerProperty currentIndex;
    private final ObjectProperty<LoopMode> loopMode;

    // Version counter to notify listeners when playback order changes (shuffle regenerated, etc.)
    private final IntegerProperty playbackOrderVersion = new SimpleIntegerProperty(0);

    private PlaybackQueue() {
        this.queue = FXCollections.observableArrayList();
        this.currentTrack = new SimpleObjectProperty<>(null);
        this.currentIndex = new SimpleIntegerProperty(-1);
        this.loopMode = new SimpleObjectProperty<>(LoopMode.PLAYLIST);
    }

    public static synchronized PlaybackQueue getInstance() {
        if (instance == null) {
            instance = new PlaybackQueue();
        }
        return instance;
    }

    // ==================== Properties ====================

    public ObservableList<Music> getQueue() {
        return queue;
    }

    /**
     * Returns the tracks in playback order.
     *
     * @return List of tracks in playback order
     */
    public List<Music> getTracksInPlaybackOrder() {
        return new ArrayList<>(queue);
    }

    public ObjectProperty<Music> currentTrackProperty() {
        return currentTrack;
    }

    public Music getCurrentTrack() {
        return currentTrack.get();
    }

    public IntegerProperty currentIndexProperty() {
        return currentIndex;
    }

    public int getCurrentIndex() {
        return currentIndex.get();
    }

    /**
     * Property that changes whenever the playback order changes (shuffle regenerated, etc.).
     * Listeners can use this to refresh displays that depend on playback order.
     */
    public IntegerProperty playbackOrderVersionProperty() {
        return playbackOrderVersion;
    }

    public ObjectProperty<LoopMode> loopModeProperty() {
        return loopMode;
    }

    public LoopMode getLoopMode() {
        return loopMode.get();
    }

    public void setLoopMode(LoopMode mode) {
        loopMode.set(mode);
    }

    /**
     * Cycles through loop modes: NONE -> PLAYLIST -> SINGLE -> NONE
     */
    public void cycleLoopMode() {
        LoopMode current = getLoopMode();
        LoopMode next = switch (current) {
            case NONE -> LoopMode.PLAYLIST;
            case PLAYLIST -> LoopMode.SINGLE;
            case SINGLE -> LoopMode.NONE;
        };
        setLoopMode(next);
    }

    // =================== Listeners ====================

    public void addQueueListener(ChangeListener<ObservableList<Music>> listener) {
        queue.addListener((javafx.collections.ListChangeListener<? super Music>) change -> {
            listener.changed(null, null, queue);
        });
    }

    public void addCurrentTrackListener(ChangeListener<Music> listener) {
        currentTrack.addListener(listener);
    }

    // ==================== Queue Management ====================

    /**
     * Sets the entire queue to a new list of tracks. Overwrites any existing tracks.
     * Resets the current index to 0 if the list is not empty, or -1 if it is.
     */
    public void setQueue(List<Music> musics) {
        queue.setAll(musics);
        if (!musics.isEmpty()) {
            setCurrentIndex(0);
        } else {
            setCurrentIndex(-1);
        }
    }

    public void addToQueue(Music track) {
        queue.add(track);
    }

    public void addToQueue(List<Music> tracks) {
        queue.addAll(tracks);
    }

    public void removeFromQueue(int index) {
        if (index < 0 || index >= size()) return;
        
        queue.remove(index);
        
        // Adjust current index if needed
        if (index < getCurrentIndex()) {
            currentIndex.set(getCurrentIndex() - 1);
        } else if (index == getCurrentIndex()) {
            // Current track was removed
            if (!queue.isEmpty()) {
                int newIndex = Math.min(index, size() - 1);
                setCurrentIndex(newIndex);
            } else {
                setCurrentIndex(-1);
                currentTrack.set(null);
            }
        }
    }

    public void removeFromQueue(List<Integer> indices) {
        List<Integer> sortedIndices = new ArrayList<>(indices);
        sortedIndices.sort(Collections.reverseOrder());

        for (int index : sortedIndices) {
            removeFromQueue(index);
        }
    }

    public void moveBatch(List<Integer> indices, int toIndex) {
        int[] indicesArray = indices.stream().mapToInt(Integer::intValue).toArray();
        moveBatch(indicesArray, toIndex);
    }

    public void moveBatch(int[] indices, int toIndex) {
        if (indices.length == 0 || toIndex < 0 || toIndex > size()) return;

        List<Integer> sortedIndices = Arrays.stream(indices)
                .boxed()
                .filter(index -> index >= 0 && index < size())
                .distinct()
                .sorted()
                .toList();

        if (sortedIndices.isEmpty()) return;

        int oldCurrentIndex = getCurrentIndex();
        int currentMovedPosition = -1;

        for (int i = 0; i < sortedIndices.size(); i++) {
            if (sortedIndices.get(i) == oldCurrentIndex) {
                currentMovedPosition = i;
                break;
            }
        }

        List<Music> movingTracks = new ArrayList<>();

        for (int i = sortedIndices.size() - 1; i >= 0; i--) {
            int index = sortedIndices.get(i);
            movingTracks.addFirst(queue.remove(index));
        }

        int removedBeforeTarget = 0;

        for (int index : sortedIndices) {
            if (index < toIndex) {
                removedBeforeTarget++;
            }
        }

        int insertionIndex = toIndex - removedBeforeTarget;
        insertionIndex = Math.clamp(insertionIndex, 0, queue.size());

        queue.addAll(insertionIndex, movingTracks);

        if (currentMovedPosition >= 0) {
            setCurrentIndex(insertionIndex + currentMovedPosition);
        } else if (oldCurrentIndex >= 0) {
            int newCurrentIndex = oldCurrentIndex;
            for (int index : sortedIndices) {
                if (index < oldCurrentIndex) {
                    newCurrentIndex--;
                }
            }
            if (insertionIndex <= newCurrentIndex) {
                newCurrentIndex += movingTracks.size();
            }
            setCurrentIndex(newCurrentIndex);
        }
        notifyPlaybackOrderChanged();
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        queue.clear();
        currentIndex.set(-1);
        currentTrack.set(null);
    }

    // ==================== Playback Control ====================

    /**
     * Sets the current track by index.
     */
    public void setCurrentIndex(int index) {
        if (index < 0 || index >= size()) {
            currentIndex.set(-1);
            currentTrack.set(null);
            return;
        }
        
        currentIndex.set(index);
        currentTrack.set(queue.get(index));
    }

    /**
     * Plays a specific track in the queue.
     */
    public void playTrack(Music track) {
        int index = queue.indexOf(track);
        if (index >= 0) {
            setCurrentIndex(index);
        }
    }

    /**
     * Moves to the next track (user action - always moves).
     * @return The next track, or null if at the end
     */
    public int next() {
        if (queue.isEmpty()) return -1;
        
        int nextIndex = getCurrentIndex() + 1;
        if (nextIndex >= size()) {
            nextIndex = 0; // Loop back to start
        }

        setCurrentIndex(nextIndex);
        return getCurrentIndex();
    }

    /**
     * Auto-advances to the next track when a song ends.
     * Respects loop mode settings.
     * @return The next track to play, or null if playback should stop
     */
    public int nextAuto() {
        if (queue.isEmpty()) return -1;

        LoopMode mode = getLoopMode();

        // Single loop - return the same track
        if (mode == LoopMode.SINGLE) {
            return getCurrentIndex();
        }
        // No loop and at the end - stop playback
        if (mode == LoopMode.NONE && getCurrentIndex() >= size() - 1) {
            return -1;
        }

        return next();
    }

    /**
     * Moves to the previous track (user action - always moves).
     * @return The previous track, or null if at the beginning
     */
    public int previous() {
        if (queue.isEmpty()) return -1;
        
        int prevIndex = getCurrentIndex() - 1;
        if (prevIndex < 0) {
            prevIndex = size() - 1; // Loop to end
        }

        setCurrentIndex(prevIndex);
        return getCurrentIndex();
    }

    /**
     * Checks if there's a next track available.
     */
    public boolean hasNext() {
        if (queue.isEmpty()) return false;
        return getCurrentIndex() < size() - 1;
    }

    /**
     * Checks if there's a previous track available.
     */
    public boolean hasPrevious() {
        if (queue.isEmpty()) return false;
        return getCurrentIndex() > 0;
    }

    public int getIndexOf(Music track) {
        if (track == null) return -1;
        if (!queue.contains(track)) return -1;
        return queue.indexOf(track);
    }

    // ==================== Shuffle ====================

    /**
     * Shuffle the queue and place the current track at the top of the shuffled list.
     */
    public void shuffle() {
        if (queue.isEmpty()) return;

        Music current = getCurrentTrack();
        Collections.shuffle(queue);
        if (current != null) {
            putIndexAtTop(queue.indexOf(current));
        }
        setCurrentIndex(0);

        // Notify listeners that playback order has changed
        notifyPlaybackOrderChanged();
    }

    private void putIndexAtTop(int index) {
        if (index < 0 || index >= size()) return;

        Music track = queue.remove(index);
        queue.addFirst(track);
    }

    /**
     * Notifies listeners that the playback order has changed.
     * Call this after restoring queue and shuffle state to refresh displays.
     */
    public void notifyPlaybackOrderChanged() {
        playbackOrderVersion.set(playbackOrderVersion.get() + 1);
    }

    // =================== Sorting ====================

    /**
     * Sorts the queue based on a given comparator.
     */
    public void sortQueue(Comparator<Music> comparator) {
        FXCollections.sort(queue, comparator);
        notifyPlaybackOrderChanged();
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the size of the queue.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Checks if the queue is empty.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // ==================== Session Persistence ====================

    /**
     * Gets all track IDs in the queue for persistence.
     */
    public List<Long> getQueueTrackIds() {
        List<Long> ids = new ArrayList<>();
        for (Music music : queue) {
            if (music.getId() != null) {
                ids.add(music.getId());
            }
        }
        return ids;
    }
    
    /**
     * Restores the queue from a list of Music objects.
     */
    public void restoreQueue(List<Music> tracks) {
        setQueue(tracks);
    }

    /**
     * Restores the current track index.
     */
    public void restoreCurrentIndex(int index) {
        if (index >= 0 && index < size()) {
            setCurrentIndex(index);
        }
    }
}


