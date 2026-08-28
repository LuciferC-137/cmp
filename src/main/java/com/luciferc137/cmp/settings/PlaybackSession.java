package com.luciferc137.cmp.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the playback session state that should be restored on app restart.
 * This includes the current playlist, current track, shuffle order, and playback position.
 */
public class PlaybackSession {

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
     * Loop mode: "NONE", "PLAYLIST", or "SINGLE"
     */
    private String loopMode = "PLAYLIST";

    /**
     * IDs of tracks in the queue (to restore the Local playlist)
     */
    private List<Long> queueTrackIds = new ArrayList<>();

    /**
     * List of playlist IDs that were open as tabs
     */
    private List<Long> openPlaylistIds = new ArrayList<>();

    /**
     * ID of the playlist currently displayed (not necessarily playing)
     */
    private long displayedPlaylistId = -1;

    public PlaybackSession() {
        // Default constructor for deserialization
    }

    // Getters and Setters


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


    @Override
    public String toString() {
        return "PlaybackSession{" +
                ", currentTrackId=" + currentTrackId +
                ", currentTrackIndex=" + currentTrackIndex +
                ", playbackPosition=" + playbackPosition +
                ", loopMode='" + loopMode + '\'' +
                ", queueSize=" + queueTrackIds.size() +
                '}';
    }
}

