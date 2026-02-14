package com.luciferc137.cmp.library;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    private final BooleanProperty shuffleEnabled;
    private final ObjectProperty<LoopMode> loopMode;
    private final StringProperty currentPlaylistName;
    private final LongProperty currentPlaylistId;

    // Separate storage for Local playlist content (preserved when playing other playlists)
    private final ObservableList<Music> localPlaylistContent;

    // Shuffle order - precomputed to avoid repeating songs
    private List<Integer> shuffleOrder;
    private int shufflePosition;
    private final Random random = new Random();

    // History of recently played playlists (most recent first)
    // -1 represents "Local", positive numbers are playlist IDs
    private final List<Long> playlistPlayOrder = new ArrayList<>();

    private PlaybackQueue() {
        this.queue = FXCollections.observableArrayList();
        this.localPlaylistContent = FXCollections.observableArrayList();
        this.currentTrack = new SimpleObjectProperty<>(null);
        this.currentIndex = new SimpleIntegerProperty(-1);
        this.shuffleEnabled = new SimpleBooleanProperty(false);
        this.loopMode = new SimpleObjectProperty<>(LoopMode.PLAYLIST);
        this.currentPlaylistName = new SimpleStringProperty("Local");
        this.currentPlaylistId = new SimpleLongProperty(-1);
        this.shuffleOrder = new ArrayList<>();
        this.shufflePosition = 0;
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
     * If shuffle is enabled, returns tracks in shuffle order.
     * Otherwise, returns tracks in their natural queue order.
     *
     * @return List of tracks in playback order
     */
    public List<Music> getTracksInPlaybackOrder() {
        if (queue.isEmpty()) {
            return new ArrayList<>();
        }

        if (isShuffleEnabled() && !shuffleOrder.isEmpty()) {
            // Return tracks in shuffle order
            List<Music> orderedTracks = new ArrayList<>();
            for (int index : shuffleOrder) {
                if (index >= 0 && index < queue.size()) {
                    orderedTracks.add(queue.get(index));
                }
            }
            return orderedTracks;
        } else {
            // Return tracks in natural order
            return new ArrayList<>(queue);
        }
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

    public BooleanProperty shuffleEnabledProperty() {
        return shuffleEnabled;
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled.get();
    }

    public void setShuffleEnabled(boolean enabled) {
        shuffleEnabled.set(enabled);
        if (enabled) {
            generateShuffleOrder();
        }
    }

    public void toggleShuffle() {
        setShuffleEnabled(!isShuffleEnabled());
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
    public LoopMode cycleLoopMode() {
        LoopMode current = getLoopMode();
        LoopMode next = switch (current) {
            case NONE -> LoopMode.PLAYLIST;
            case PLAYLIST -> LoopMode.SINGLE;
            case SINGLE -> LoopMode.NONE;
        };
        setLoopMode(next);
        return next;
    }

    public StringProperty currentPlaylistNameProperty() {
        return currentPlaylistName;
    }

    public String getCurrentPlaylistName() {
        return currentPlaylistName.get();
    }

    public LongProperty currentPlaylistIdProperty() {
        return currentPlaylistId;
    }

    public long getCurrentPlaylistId() {
        return currentPlaylistId.get();
    }

    // ==================== Queue Management ====================

    /**
     * Sets the queue with new tracks, typically from filtered table results.
     * This is used for the "Local" playlist.
     */
    public void setLocalQueue(List<Music> tracks, Music startTrack) {
        queue.setAll(tracks);
        localPlaylistContent.setAll(tracks); // Save Local content separately
        currentPlaylistName.set("Local");
        currentPlaylistId.set(-1);
        
        // Update playlist play order history
        updatePlaylistPlayOrder(-1L);

        if (startTrack != null) {
            int index = queue.indexOf(startTrack);
            if (index >= 0) {
                setCurrentIndex(index);
            } else if (!queue.isEmpty()) {
                setCurrentIndex(0);
            }
        } else if (!queue.isEmpty()) {
            setCurrentIndex(0);
        }

        if (isShuffleEnabled()) {
            generateShuffleOrder();
        }
    }

    /**
     * Gets the Local playlist content (preserved even when playing other playlists).
     */
    public ObservableList<Music> getLocalPlaylistContent() {
        return localPlaylistContent;
    }

    /**
     * Sets the Local playlist content without changing the current playback queue.
     */
    public void setLocalPlaylistContent(List<Music> tracks) {
        localPlaylistContent.setAll(tracks);
    }

    /**
     * Loads a saved playlist into the queue.
     */
    public void loadPlaylist(long playlistId, String playlistName, List<Music> tracks) {
        queue.setAll(tracks);
        currentPlaylistName.set(playlistName);
        currentPlaylistId.set(playlistId);
        
        // Update playlist play order history
        updatePlaylistPlayOrder(playlistId);

        if (!queue.isEmpty()) {
            setCurrentIndex(0);
        } else {
            setCurrentIndex(-1);
        }

        if (isShuffleEnabled()) {
            generateShuffleOrder();
        }
    }

    // ==================== Playlist Play Order ====================

    /**
     * Updates the playlist play order when a playlist starts playing.
     * Moves the playlist to the front of the order (most recently played).
     *
     * @param playlistId The playlist ID (-1 for Local)
     */
    private void updatePlaylistPlayOrder(long playlistId) {
        // Remove if already exists
        playlistPlayOrder.remove(Long.valueOf(playlistId));
        // Add to the front (most recent)
        playlistPlayOrder.add(0, playlistId);
    }

    /**
     * Gets the order in which playlists were played (most recent first).
     * This is used to display playlist tabs in playback order.
     *
     * @return List of playlist IDs in play order (-1 represents Local)
     */
    public List<Long> getPlaylistPlayOrder() {
        return new ArrayList<>(playlistPlayOrder);
    }

    /**
     * Sets the playlist play order (used for session restore).
     *
     * @param order The list of playlist IDs in play order
     */
    public void setPlaylistPlayOrder(List<Long> order) {
        playlistPlayOrder.clear();
        if (order != null) {
            playlistPlayOrder.addAll(order);
        }
    }

    /**
     * Gets the position of a playlist in the play order.
     * Lower number means more recently played.
     *
     * @param playlistId The playlist ID (-1 for Local)
     * @return The position (0 = most recent), or Integer.MAX_VALUE if never played
     */
    public int getPlaylistPlayOrderPosition(long playlistId) {
        int index = playlistPlayOrder.indexOf(playlistId);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    /**
     * Adds a track to the current queue.
     */
    public void addToQueue(Music track) {
        queue.add(track);
        if (isShuffleEnabled()) {
            // Add to shuffle order at a random position after current
            int insertPos = shufflePosition + 1 + random.nextInt(Math.max(1, shuffleOrder.size() - shufflePosition));
            shuffleOrder.add(Math.min(insertPos, shuffleOrder.size()), queue.size() - 1);
        }
    }

    /**
     * Removes a track from the queue.
     */
    public void removeFromQueue(int index) {
        if (index < 0 || index >= queue.size()) return;
        
        queue.remove(index);
        
        // Adjust current index if needed
        if (index < getCurrentIndex()) {
            currentIndex.set(getCurrentIndex() - 1);
        } else if (index == getCurrentIndex()) {
            // Current track was removed
            if (!queue.isEmpty()) {
                int newIndex = Math.min(index, queue.size() - 1);
                setCurrentIndex(newIndex);
            } else {
                setCurrentIndex(-1);
                currentTrack.set(null);
            }
        }

        if (isShuffleEnabled()) {
            generateShuffleOrder();
        }
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        queue.clear();
        currentIndex.set(-1);
        currentTrack.set(null);
        shuffleOrder.clear();
        shufflePosition = 0;
    }

    // ==================== Playback Control ====================

    /**
     * Sets the current track by index.
     */
    public void setCurrentIndex(int index) {
        if (index < 0 || index >= queue.size()) {
            currentIndex.set(-1);
            currentTrack.set(null);
            return;
        }
        
        currentIndex.set(index);
        currentTrack.set(queue.get(index));

        // Update shuffle position
        if (isShuffleEnabled() && !shuffleOrder.isEmpty()) {
            shufflePosition = shuffleOrder.indexOf(index);
            if (shufflePosition < 0) {
                shufflePosition = 0;
            }
        }
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
    public Music next() {
        if (queue.isEmpty()) return null;

        int nextIndex;
        if (isShuffleEnabled() && !shuffleOrder.isEmpty()) {
            shufflePosition++;
            if (shufflePosition >= shuffleOrder.size()) {
                shufflePosition = 0;
                generateShuffleOrder();
            }
            nextIndex = shuffleOrder.get(shufflePosition);
        } else {
            nextIndex = getCurrentIndex() + 1;
            if (nextIndex >= queue.size()) {
                nextIndex = 0; // Loop back to start
            }
        }

        setCurrentIndex(nextIndex);
        return getCurrentTrack();
    }

    /**
     * Auto-advances to the next track when a song ends.
     * Respects loop mode settings.
     * @return The next track to play, or null if playback should stop
     */
    public Music nextAuto() {
        if (queue.isEmpty()) return null;

        LoopMode mode = getLoopMode();

        // Single loop - return the same track
        if (mode == LoopMode.SINGLE) {
            return getCurrentTrack();
        }

        int nextIndex;
        if (isShuffleEnabled() && !shuffleOrder.isEmpty()) {
            shufflePosition++;
            if (shufflePosition >= shuffleOrder.size()) {
                // End of shuffle
                if (mode == LoopMode.PLAYLIST) {
                    shufflePosition = 0;
                    generateShuffleOrder();
                } else {
                    // NONE mode - stop at end
                    return null;
                }
            }
            nextIndex = shuffleOrder.get(shufflePosition);
        } else {
            nextIndex = getCurrentIndex() + 1;
            if (nextIndex >= queue.size()) {
                if (mode == LoopMode.PLAYLIST) {
                    nextIndex = 0; // Loop back to start
                } else {
                    // NONE mode - stop at end
                    return null;
                }
            }
        }

        setCurrentIndex(nextIndex);
        return getCurrentTrack();
    }

    /**
     * Moves to the previous track (user action - always moves).
     * @return The previous track, or null if at the beginning
     */
    public Music previous() {
        if (queue.isEmpty()) return null;

        int prevIndex;
        if (isShuffleEnabled() && !shuffleOrder.isEmpty()) {
            shufflePosition--;
            if (shufflePosition < 0) {
                shufflePosition = shuffleOrder.size() - 1;
            }
            prevIndex = shuffleOrder.get(shufflePosition);
        } else {
            prevIndex = getCurrentIndex() - 1;
            if (prevIndex < 0) {
                prevIndex = queue.size() - 1; // Loop to end
            }
        }

        setCurrentIndex(prevIndex);
        return getCurrentTrack();
    }

    /**
     * Checks if there's a next track available.
     */
    public boolean hasNext() {
        if (queue.isEmpty()) return false;
        if (isShuffleEnabled()) {
            return shufflePosition < shuffleOrder.size() - 1;
        }
        return getCurrentIndex() < queue.size() - 1;
    }

    /**
     * Checks if there's a previous track available.
     */
    public boolean hasPrevious() {
        if (queue.isEmpty()) return false;
        if (isShuffleEnabled()) {
            return shufflePosition > 0;
        }
        return getCurrentIndex() > 0;
    }

    // ==================== Shuffle ====================

    /**
     * Generates a new shuffle order starting from the current track.
     */
    private void generateShuffleOrder() {
        shuffleOrder.clear();
        if (queue.isEmpty()) return;

        // Create a list of all indices
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            indices.add(i);
        }

        // Remove current track from shuffle (it plays first)
        int currentIdx = getCurrentIndex();
        if (currentIdx >= 0) {
            indices.remove(Integer.valueOf(currentIdx));
            shuffleOrder.add(currentIdx);
        }

        // Shuffle remaining
        Collections.shuffle(indices, random);
        shuffleOrder.addAll(indices);

        shufflePosition = 0;
    }

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
     * Gets the current shuffle order for persistence.
     */
    public List<Integer> getShuffleOrder() {
        return new ArrayList<>(shuffleOrder);
    }

    /**
     * Gets the current shuffle position for persistence.
     */
    public int getShufflePosition() {
        return shufflePosition;
    }

    /**
     * Restores shuffle state from saved session.
     */
    public void restoreShuffleState(List<Integer> order, int position) {
        if (order != null && !order.isEmpty()) {
            this.shuffleOrder = new ArrayList<>(order);
            this.shufflePosition = Math.min(position, shuffleOrder.size() - 1);
        }
    }

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
     * Gets all track IDs in the Local playlist for persistence.
     */
    public List<Long> getLocalPlaylistTrackIds() {
        List<Long> ids = new ArrayList<>();
        for (Music music : localPlaylistContent) {
            if (music.getId() != null) {
                ids.add(music.getId());
            }
        }
        return ids;
    }

    /**
     * Restores the Local playlist content from a list of Music objects.
     */
    public void restoreLocalPlaylistContent(List<Music> tracks) {
        localPlaylistContent.setAll(tracks);
    }

    /**
     * Restores the queue from a list of Music objects.
     */
    public void restoreQueue(List<Music> tracks, String playlistName, long playlistId) {
        queue.setAll(tracks);
        currentPlaylistName.set(playlistName);
        currentPlaylistId.set(playlistId);
    }

    /**
     * Restores the current track index.
     */
    public void restoreCurrentIndex(int index) {
        if (index >= 0 && index < queue.size()) {
            setCurrentIndex(index);
        }
    }
}


