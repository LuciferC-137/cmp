package com.luciferc137.cmp.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the playback session state that should be restored on app restart.
 * This includes the current playlist, current track, shuffle order, and playback position.
 */
public class PlaybackSession {

    /**
     * ID of the playlist that was playing (-1 for Local, or actual playlist ID)
     */
    private long playingPlaylistId = -1;

    /**
     * Name of the playlist that was playing
     */
    private String playingPlaylistName = "Local";

    /**
     * ID of the track that was playing
     */
    private long currentTrackId = -1;

    /**
     * Index of the current track in the queue
     */
    private int currentTrackIndex = -1;

    /**
     * Playback position in milliseconds where the track was interrupted
     */
    private long playbackPosition = 0;

    /**
     * Whether shuffle mode was enabled
     */
    private boolean shuffleEnabled = false;

    /**
     * The shuffle order (list of track indices)
     */
    private List<Integer> shuffleOrder = new ArrayList<>();

    /**
     * Current position in the shuffle order
     */
    private int shufflePosition = 0;

    /**
     * Loop mode: "NONE", "PLAYLIST", or "SINGLE"
     */
    private String loopMode = "PLAYLIST";

    /**
     * IDs of tracks in the queue (to restore the Local playlist)
     */
    private List<Long> queueTrackIds = new ArrayList<>();

    /**
     * IDs of tracks in the Local playlist (preserved independently of what is currently playing)
     */
    private List<Long> localPlaylistTrackIds = new ArrayList<>();

    /**
     * List of playlist IDs that were open as tabs
     */
    private List<Long> openPlaylistIds = new ArrayList<>();

    /**
     * ID of the playlist currently displayed (not necessarily playing)
     */
    private long displayedPlaylistId = -1;

    /**
     * Order in which playlists were played (most recent first).
     * Used to display playlist tabs in playback order.
     */
    private List<Long> playlistPlayOrder = new ArrayList<>();

    public PlaybackSession() {
        // Default constructor for deserialization
    }

    // Getters and Setters

    public long getPlayingPlaylistId() {
        return playingPlaylistId;
    }

    public void setPlayingPlaylistId(long playingPlaylistId) {
        this.playingPlaylistId = playingPlaylistId;
    }

    public String getPlayingPlaylistName() {
        return playingPlaylistName;
    }

    public void setPlayingPlaylistName(String playingPlaylistName) {
        this.playingPlaylistName = playingPlaylistName;
    }

    public long getCurrentTrackId() {
        return currentTrackId;
    }

    public void setCurrentTrackId(long currentTrackId) {
        this.currentTrackId = currentTrackId;
    }

    public int getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    public void setCurrentTrackIndex(int currentTrackIndex) {
        this.currentTrackIndex = currentTrackIndex;
    }

    public long getPlaybackPosition() {
        return playbackPosition;
    }

    public void setPlaybackPosition(long playbackPosition) {
        this.playbackPosition = playbackPosition;
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public void setShuffleEnabled(boolean shuffleEnabled) {
        this.shuffleEnabled = shuffleEnabled;
    }

    public List<Integer> getShuffleOrder() {
        return shuffleOrder;
    }

    public void setShuffleOrder(List<Integer> shuffleOrder) {
        this.shuffleOrder = shuffleOrder != null ? shuffleOrder : new ArrayList<>();
    }

    public int getShufflePosition() {
        return shufflePosition;
    }

    public void setShufflePosition(int shufflePosition) {
        this.shufflePosition = shufflePosition;
    }

    public String getLoopMode() {
        return loopMode;
    }

    public void setLoopMode(String loopMode) {
        this.loopMode = loopMode != null ? loopMode : "PLAYLIST";
    }

    public List<Long> getQueueTrackIds() {
        return queueTrackIds;
    }

    public void setQueueTrackIds(List<Long> queueTrackIds) {
        this.queueTrackIds = queueTrackIds != null ? queueTrackIds : new ArrayList<>();
    }

    public List<Long> getLocalPlaylistTrackIds() {
        return localPlaylistTrackIds;
    }

    public void setLocalPlaylistTrackIds(List<Long> localPlaylistTrackIds) {
        this.localPlaylistTrackIds = localPlaylistTrackIds != null ? localPlaylistTrackIds : new ArrayList<>();
    }

    public List<Long> getOpenPlaylistIds() {
        return openPlaylistIds;
    }

    public void setOpenPlaylistIds(List<Long> openPlaylistIds) {
        this.openPlaylistIds = openPlaylistIds != null ? openPlaylistIds : new ArrayList<>();
    }

    public long getDisplayedPlaylistId() {
        return displayedPlaylistId;
    }

    public void setDisplayedPlaylistId(long displayedPlaylistId) {
        this.displayedPlaylistId = displayedPlaylistId;
    }

    public List<Long> getPlaylistPlayOrder() {
        return playlistPlayOrder;
    }

    public void setPlaylistPlayOrder(List<Long> playlistPlayOrder) {
        this.playlistPlayOrder = playlistPlayOrder != null ? playlistPlayOrder : new ArrayList<>();
    }

    /**
     * Checks if there's a valid session to restore.
     */
    public boolean hasValidSession() {
        // A session is valid if we have queue content OR if shuffle/loop settings have been changed
        return currentTrackId > 0 || !queueTrackIds.isEmpty() ||
               shuffleEnabled || !loopMode.equals("PLAYLIST");
    }

    @Override
    public String toString() {
        return "PlaybackSession{" +
                "playingPlaylistId=" + playingPlaylistId +
                ", currentTrackId=" + currentTrackId +
                ", currentTrackIndex=" + currentTrackIndex +
                ", playbackPosition=" + playbackPosition +
                ", shuffleEnabled=" + shuffleEnabled +
                ", loopMode='" + loopMode + '\'' +
                ", queueSize=" + queueTrackIds.size() +
                '}';
    }
}

