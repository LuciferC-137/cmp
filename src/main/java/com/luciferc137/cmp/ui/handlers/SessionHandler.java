package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.MainApp;
import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.settings.PlaybackSession;
import com.luciferc137.cmp.settings.SettingsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Handles session persistence including:
 * - Saving the current playback state
 * - Restoring session on application startup
 * - Managing shuffle/loop state persistence
 */
public class SessionHandler implements Handler {

    private final SettingsManager settingsManager;
    private final PlaybackQueue playbackQueue;
    private final LibraryService libraryService;

    private boolean isRestoringSession = false;

    // Listener for session restore events
    private SessionRestoreListener restoreListener;

    /**
     * Listener interface for session restore events.
     */
    public interface SessionRestoreListener {
        void onShuffleStateRestored(boolean enabled);
        void onLoopModeRestored(PlaybackQueue.LoopMode mode);
        void onCurrentTrackRestored(Music music);
        void onPlaybackPositionRestored(long position);
        void onDisplayedPlaylistRestored(Long playlistId);
        void onSessionRestoreComplete();
    }

    public SessionHandler() {
        this.settingsManager = SettingsManager.getInstance();
        this.playbackQueue = PlaybackQueue.getInstance();
        this.libraryService = LibraryService.getInstance();
    }

    public void setRestoreListener(SessionRestoreListener listener) {
        this.restoreListener = listener;
    }

    public boolean isRestoringSession() {
        return isRestoringSession;
    }

    @Override
    public void initialize() {
        restoreSession();
    }

    /**
     * Saves the current playback session.
     */
    public void saveSession(Long displayedPlaylistId, long currentPosition) {
        try {
            PlaybackSession session = settingsManager.getSession();

            // Save shuffle and loop states
            session.setLoopMode(playbackQueue.getLoopMode().name());

            // Save current track info
            Music current = playbackQueue.getCurrentTrack();
            if (current != null && current.getId() != null) {
                session.setCurrentTrackId(current.getId());
                session.setCurrentTrackIndex(playbackQueue.getCurrentIndex());
                session.setPlaybackPosition(currentPosition);
            } else {
                session.setCurrentTrackId(-1);
                session.setCurrentTrackIndex(-1);
                session.setPlaybackPosition(0);
            }

            // Save queue track IDs (current playback queue)
            session.setQueueTrackIds(playbackQueue.getQueueTrackIds());

            // Save displayed playlist
            session.setDisplayedPlaylistId(displayedPlaylistId != null ? displayedPlaylistId : -1);

            settingsManager.saveSession();
            MainApp.logger.log(Level.INFO, "Session saved: " + session);
        } catch (Exception e) {
            MainApp.logger.log(Level.SEVERE, "Error saving session", e);
        }
    }

    /**
     * Restores the playback session from saved state.
     */
    public void restoreSession() {
        PlaybackSession session = settingsManager.getSession();
        isRestoringSession = true;

        try {
            MainApp.logger.log(Level.INFO, "Restoring session: " + session);

            // Restore loop mode
            try {
                PlaybackQueue.LoopMode loopMode = PlaybackQueue.LoopMode.valueOf(session.getLoopMode());
                playbackQueue.setLoopMode(loopMode);
                if (restoreListener != null) {
                    restoreListener.onLoopModeRestored(loopMode);
                }
            } catch (IllegalArgumentException e) {
                playbackQueue.setLoopMode(PlaybackQueue.LoopMode.PLAYLIST);
            }

            // Restore queue from track IDs
            List<Long> trackIds = session.getQueueTrackIds();
            if (!trackIds.isEmpty()) {
                List<Music> tracks = new ArrayList<>();
                for (Long trackId : trackIds) {
                    libraryService.getMusicById(trackId).ifPresent(entity -> {
                        Music music = Music.fromEntity(entity);
                        List<String> tagNames = libraryService.getMusicTagNames(trackId);
                        music.setTags(tagNames);
                        tracks.add(music);
                    });
                }

                if (!tracks.isEmpty()) {
                    // Notify that playback order is now fully restored
                    playbackQueue.notifyPlaybackOrderChanged();

                    // Restore current track
                    int trackIndex = session.getCurrentTrackIndex();
                    if (trackIndex >= 0 && trackIndex < tracks.size()) {
                        playbackQueue.restoreCurrentIndex(trackIndex);

                        // Notify listener
                        Music current = playbackQueue.getCurrentTrack();
                        if (current != null && restoreListener != null) {
                            restoreListener.onCurrentTrackRestored(current);
                        }

                        // Restore playback position
                        long savedPosition = session.getPlaybackPosition();
                        if (savedPosition > 0 && restoreListener != null) {
                            restoreListener.onPlaybackPositionRestored(savedPosition);
                        }
                    }
                }
            }

            // Restore displayed playlist tab
            long displayedId = session.getDisplayedPlaylistId();
            Long displayedPlaylistId = displayedId == -1 ? null : displayedId;
            if (restoreListener != null) {
                restoreListener.onDisplayedPlaylistRestored(displayedPlaylistId);
            }

        } catch (Exception e) {
            MainApp.logger.log(Level.SEVERE, "Error restoring session", e);
        } finally {
            isRestoringSession = false;
            if (restoreListener != null) {
                restoreListener.onSessionRestoreComplete();
            }
        }
    }
}

