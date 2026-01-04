package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.database.LibraryService;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.settings.PlaybackSession;
import com.luciferc137.cmp.settings.SettingsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles session persistence including:
 * - Saving the current playback state
 * - Restoring session on application startup
 * - Managing shuffle/loop state persistence
 */
public class SessionHandler {

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

    /**
     * Saves the current playback session.
     */
    public void saveSession(Long displayedPlaylistId, long currentPosition) {
        try {
            PlaybackSession session = settingsManager.getSession();

            // Save shuffle and loop states
            session.setShuffleEnabled(playbackQueue.isShuffleEnabled());
            session.setLoopMode(playbackQueue.getLoopMode().name());

            // Save current playlist info
            session.setPlayingPlaylistId(playbackQueue.getCurrentPlaylistId());
            session.setPlayingPlaylistName(playbackQueue.getCurrentPlaylistName());

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

            // Save shuffle order
            session.setShuffleOrder(playbackQueue.getShuffleOrder());
            session.setShufflePosition(playbackQueue.getShufflePosition());

            // Save queue track IDs (current playback queue)
            session.setQueueTrackIds(playbackQueue.getQueueTrackIds());

            // Save Local playlist content separately (preserved even when playing other playlists)
            session.setLocalPlaylistTrackIds(playbackQueue.getLocalPlaylistTrackIds());

            // Save displayed playlist
            session.setDisplayedPlaylistId(displayedPlaylistId != null ? displayedPlaylistId : -1);

            settingsManager.saveSession();
            System.out.println("Session saved: shuffle=" + session.isShuffleEnabled() +
                    ", loop=" + session.getLoopMode() +
                    ", queue=" + session.getQueueTrackIds().size() +
                    ", local=" + session.getLocalPlaylistTrackIds().size());
        } catch (Exception e) {
            System.err.println("Error saving session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restores the playback session from saved state.
     */
    public void restoreSession() {
        PlaybackSession session = settingsManager.getSession();
        isRestoringSession = true;

        try {
            System.out.println("Restoring session from file...");
            System.out.println("  Session shuffle: " + session.isShuffleEnabled());
            System.out.println("  Session loop: " + session.getLoopMode());
            System.out.println("  Session tracks: " + session.getQueueTrackIds().size());

            // Restore loop mode
            try {
                PlaybackQueue.LoopMode loopMode = PlaybackQueue.LoopMode.valueOf(session.getLoopMode());
                playbackQueue.setLoopMode(loopMode);
                if (restoreListener != null) {
                    restoreListener.onLoopModeRestored(loopMode);
                }
                System.out.println("Restored loop mode: " + loopMode);
            } catch (IllegalArgumentException e) {
                playbackQueue.setLoopMode(PlaybackQueue.LoopMode.PLAYLIST);
            }

            // Restore shuffle state
            playbackQueue.setShuffleEnabled(session.isShuffleEnabled());
            if (restoreListener != null) {
                restoreListener.onShuffleStateRestored(session.isShuffleEnabled());
            }
            System.out.println("Restored shuffle: " + session.isShuffleEnabled());

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
                    playbackQueue.restoreQueue(tracks,
                            session.getPlayingPlaylistName(),
                            session.getPlayingPlaylistId());

                    // Restore shuffle order if shuffle is enabled
                    if (session.isShuffleEnabled() && !session.getShuffleOrder().isEmpty()) {
                        playbackQueue.restoreShuffleState(
                                session.getShuffleOrder(),
                                session.getShufflePosition());
                    }

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

                    System.out.println("Restored queue with " + tracks.size() + " tracks, current index: " + trackIndex + ", position: " + session.getPlaybackPosition() + "ms");
                }
            }

            // Restore Local playlist content (separate from playback queue)
            List<Long> localTrackIds = session.getLocalPlaylistTrackIds();
            if (!localTrackIds.isEmpty()) {
                List<Music> localTracks = new ArrayList<>();
                for (Long trackId : localTrackIds) {
                    libraryService.getMusicById(trackId).ifPresent(entity -> {
                        Music music = Music.fromEntity(entity);
                        List<String> tagNames = libraryService.getMusicTagNames(trackId);
                        music.setTags(tagNames);
                        localTracks.add(music);
                    });
                }
                if (!localTracks.isEmpty()) {
                    playbackQueue.restoreLocalPlaylistContent(localTracks);
                    System.out.println("Restored Local playlist with " + localTracks.size() + " tracks");
                }
            }

            // Restore displayed playlist tab
            long displayedId = session.getDisplayedPlaylistId();
            Long displayedPlaylistId = displayedId == -1 ? null : displayedId;
            if (restoreListener != null) {
                restoreListener.onDisplayedPlaylistRestored(displayedPlaylistId);
            }

        } catch (Exception e) {
            System.err.println("Error restoring session: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isRestoringSession = false;
            if (restoreListener != null) {
                restoreListener.onSessionRestoreComplete();
            }
        }
    }
}

