package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.audio.VlcAudioPlayer;
import com.luciferc137.cmp.audio.WaveformExtractor;
import com.luciferc137.cmp.library.Music;
import com.luciferc137.cmp.library.MusicLibrary;
import com.luciferc137.cmp.library.PlaybackQueue;
import com.luciferc137.cmp.settings.SettingsManager;
import com.luciferc137.cmp.ui.WaveformProgressBar;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all playback-related functionality including:
 * - Play/Pause/Stop/Next/Previous controls
 * - Progress tracking and display
 * - Waveform loading and navigation
 * - Track end detection
 * - Volume control
 */
public class PlaybackHandler {

    private final VlcAudioPlayer audioPlayer;
    private final WaveformExtractor waveformExtractor;
    private final PlaybackQueue playbackQueue;
    private final MusicLibrary musicLibrary;
    private final SettingsManager settingsManager;

    // UI Components (injected)
    private WaveformProgressBar waveformProgressBar;
    private Label currentTitleLabel;
    private Label currentArtistLabel;
    private Label elapsedTimeLabel;
    private Label totalTimeLabel;
    private Slider volumeSlider;

    private AnimationTimer progressTimer;
    private Music currentMusic;

    // Restored position from session (used for resuming at saved position)
    private long restoredPosition = 0;
    private boolean hasRestoredPosition = false;

    // Listener for playback events
    private PlaybackEventListener eventListener;

    /**
     * Listener interface for playback events.
     */
    public interface PlaybackEventListener {
        void onTrackChanged(Music music);
        void onSessionNeedsSave();
    }

    public PlaybackHandler(VlcAudioPlayer audioPlayer, WaveformExtractor waveformExtractor) {
        this.audioPlayer = audioPlayer;
        this.waveformExtractor = waveformExtractor;
        this.playbackQueue = PlaybackQueue.getInstance();
        this.musicLibrary = MusicLibrary.getInstance();
        this.settingsManager = SettingsManager.getInstance();
    }

    /**
     * Binds UI components to this handler.
     */
    public void bindUIComponents(
            WaveformProgressBar waveformProgressBar,
            Label currentTitleLabel,
            Label currentArtistLabel,
            Label elapsedTimeLabel,
            Label totalTimeLabel,
            Slider volumeSlider
    ) {
        this.waveformProgressBar = waveformProgressBar;
        this.currentTitleLabel = currentTitleLabel;
        this.currentArtistLabel = currentArtistLabel;
        this.elapsedTimeLabel = elapsedTimeLabel;
        this.totalTimeLabel = totalTimeLabel;
        this.volumeSlider = volumeSlider;
    }

    public void setEventListener(PlaybackEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Initializes the playback handler.
     */
    public void initialize() {
        initVolumeControl();
        initProgressTimer();
        setupTrackEndDetection();
        setupWaveformClickHandler();
    }

    // ==================== Volume Control ====================

    private void initVolumeControl() {
        if (volumeSlider != null) {
            int savedVolume = settingsManager.getLastVolume();
            volumeSlider.setValue(savedVolume);
            audioPlayer.setVolume(savedVolume);

            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                    audioPlayer.setVolume(newVal.intValue()));

            volumeSlider.setOnMouseReleased(event ->
                    settingsManager.setLastVolume((int) volumeSlider.getValue()));
        }
    }

    // ==================== Progress Tracking ====================

    private void initProgressTimer() {
        progressTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 33_000_000) { // ~30fps
                    updateProgress();
                    lastUpdate = now;
                }
            }
        };
        progressTimer.start();
    }

    private void updateProgress() {
        long duration = audioPlayer.getDuration();
        long position = audioPlayer.getPosition();

        // Update progress bar
        if (waveformProgressBar != null && audioPlayer.isPlaying()) {
            if (duration > 0) {
                double progress = (double) position / duration;
                waveformProgressBar.setProgress(progress);
            }
        }

        // Update time labels
        if (elapsedTimeLabel != null) {
            elapsedTimeLabel.setText(formatTime(position));
        }
        if (totalTimeLabel != null && duration > 0) {
            totalTimeLabel.setText(formatTime(duration));
        }
    }

    // ==================== Track End Detection ====================

    private void setupTrackEndDetection() {
        AnimationTimer endDetector = new AnimationTimer() {
            private boolean wasPlaying = false;
            private int stuckCount = 0;

            @Override
            public void handle(long now) {
                if (currentMusic == null) {
                    wasPlaying = false;
                    return;
                }

                long duration = audioPlayer.getDuration();
                long position = audioPlayer.getPosition();

                if (duration > 0 && position > 0) {
                    // Track ended: position >= duration - 500ms
                    if (position >= duration - 500) {
                        onTrackEnded();
                        return;
                    }

                    // Detect stuck at end
                    if (wasPlaying && !audioPlayer.isPlaying() && position > duration - 2000) {
                        stuckCount++;
                        if (stuckCount > 5) {
                            onTrackEnded();
                            stuckCount = 0;
                            return;
                        }
                    } else {
                        stuckCount = 0;
                    }
                }

                wasPlaying = audioPlayer.isPlaying();
            }
        };
        endDetector.start();
    }

    private void onTrackEnded() {
        Platform.runLater(() -> {
            Music next = playbackQueue.nextAuto();
            if (next != null) {
                playTrack(next);
            } else {
                audioPlayer.stop();
            }
        });
    }

    // ==================== Waveform ====================

    private void setupWaveformClickHandler() {
        if (waveformProgressBar != null) {
            waveformProgressBar.setOnMouseClicked(this::onWaveformClicked);
        }
    }

    private void onWaveformClicked(MouseEvent event) {
        if (currentMusic == null) return;

        double clickX = event.getX();
        double width = waveformProgressBar.getWidth();
        double progress = clickX / width;

        long duration = audioPlayer.getDuration();
        if (duration > 0) {
            long seekPosition = (long) (duration * progress);
            audioPlayer.seek(seekPosition);
            waveformProgressBar.setProgress(progress);
        }
    }

    private void loadWaveform(Music music) {
        if (waveformProgressBar == null) return;

        waveformProgressBar.clear();
        int numSamples = WaveformExtractor.DEFAULT_NUM_BINS;

        waveformExtractor.extractAsync(music.filePath, numSamples)
                .thenAccept(data -> Platform.runLater(() ->
                        waveformProgressBar.setWaveformData(data)));
    }

    // ==================== Playback Controls ====================

    /**
     * Plays the selected music from the table.
     *
     * @param selectedMusic The music selected in the table, or null to resume
     * @param tableContent Current content of the music table
     */
    public void playFromTable(Music selectedMusic, List<Music> tableContent) {
        if (selectedMusic == null) {
            if (currentMusic != null) {
                audioPlayer.resume();
            }
            return;
        }

        playbackQueue.setLocalQueue(new ArrayList<>(tableContent), selectedMusic);
        playTrack(selectedMusic);
        notifySessionNeedsSave();
    }

    /**
     * Plays a specific track.
     */
    public void playTrack(Music music) {
        if (music == null) return;

        if (currentMusic == null || !currentMusic.equals(music)) {
            currentMusic = music;
            loadWaveform(music);
            updateCurrentSongLabels(music);
        }

        audioPlayer.play(music);
        if (waveformProgressBar != null) {
            waveformProgressBar.setProgress(0);
        }

        notifyTrackChanged(music);
        notifySessionNeedsSave();
    }

    /**
     * Resumes playback or plays the current track from the queue.
     * Used when the Play button is pressed with no table selection.
     * This starts playback from the beginning (ignoring restored position).
     */
    public void resumeOrPlayCurrent() {
        if (audioPlayer.isPlaying()) {
            // Already playing, do nothing
            return;
        }

        // Try to resume if paused (player already has the track loaded)
        if (currentMusic != null && audioPlayer.isPaused()) {
            audioPlayer.resume();
            return;
        }

        // If no current music, try to play from queue (from beginning)
        Music queueCurrent = playbackQueue.getCurrentTrack();
        if (queueCurrent != null) {
            // Clear restored position since we're starting fresh
            hasRestoredPosition = false;
            restoredPosition = 0;
            playTrack(queueCurrent);
        }
    }

    /**
     * Resumes playback at the restored position (from saved session).
     * Used when the Pause button is pressed at startup to continue where left off.
     */
    public void resumeAtSavedPosition() {
        if (audioPlayer.isPlaying()) {
            // Already playing, just pause
            audioPlayer.pause();
            return;
        }

        // If paused, resume
        if (currentMusic != null && audioPlayer.isPaused()) {
            audioPlayer.resume();
            return;
        }

        // If we have a restored position, start playing and seek to it
        Music queueCurrent = playbackQueue.getCurrentTrack();
        if (queueCurrent != null) {
            if (hasRestoredPosition && restoredPosition > 0) {
                // Play the track first
                playTrack(queueCurrent);

                // Schedule seeking after a short delay to allow the player to initialize
                javafx.application.Platform.runLater(() -> {
                    // Use a small delay to ensure the player is ready
                    new Thread(() -> {
                        try {
                            Thread.sleep(500); // Wait for player to initialize
                            javafx.application.Platform.runLater(() -> {
                                audioPlayer.seek(restoredPosition);
                                if (waveformProgressBar != null) {
                                    long duration = audioPlayer.getDuration();
                                    if (duration > 0) {
                                        waveformProgressBar.setProgress((double) restoredPosition / duration);
                                    }
                                }
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                });

                // Clear the restored position after using it
                hasRestoredPosition = false;
            } else {
                // No saved position, just play from beginning
                playTrack(queueCurrent);
            }
        }
    }

    /**
     * Sets the restored position from a saved session.
     * Call this during session restoration.
     * Also updates the visual progress bar and loads the waveform.
     */
    public void setRestoredPosition(long position) {
        this.restoredPosition = position;
        this.hasRestoredPosition = position > 0;

        // Load waveform and display progress for the restored track
        Music restoredTrack = playbackQueue.getCurrentTrack();
        if (restoredTrack != null && position > 0) {
            // Load the waveform
            loadWaveform(restoredTrack);

            // Update the progress bar once waveform is loaded
            // We need to calculate progress based on track duration from metadata
            long duration = restoredTrack.duration;
            if (duration > 0) {
                double progress = (double) position / duration;
                Platform.runLater(() -> {
                    if (waveformProgressBar != null) {
                        waveformProgressBar.setProgress(progress);
                    }
                    // Also update elapsed time label
                    if (elapsedTimeLabel != null) {
                        elapsedTimeLabel.setText(formatTime(position));
                    }
                    if (totalTimeLabel != null) {
                        totalTimeLabel.setText(formatTime(duration));
                    }
                });
            }
        }
    }

    /**
     * Gets the restored position.
     */
    public long getRestoredPosition() {
        return restoredPosition;
    }

    /**
     * Checks if there's a restored position available.
     */
    public boolean hasRestoredPosition() {
        return hasRestoredPosition;
    }

    public void pause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
        } else {
            audioPlayer.resume();
        }
    }

    public void stop() {
        audioPlayer.stop();
    }

    public void previous() {
        Music prev = playbackQueue.previous();
        if (prev != null) {
            playTrack(prev);
        }
    }

    public void next() {
        Music next = playbackQueue.next();
        if (next != null) {
            playTrack(next);
        }
    }

    public void toggleShuffle() {
        playbackQueue.toggleShuffle();
    }

    public void cycleLoopMode() {
        playbackQueue.cycleLoopMode();
    }

    // ==================== UI Updates ====================

    private void updateCurrentSongLabels(Music music) {
        if (currentTitleLabel != null) {
            currentTitleLabel.setText(music.title != null ? music.title : "Unknown Title");
        }
        if (currentArtistLabel != null) {
            String artistAlbum = "";
            if (music.artist != null && !music.artist.isEmpty()) {
                artistAlbum = music.artist;
            }
            if (music.album != null && !music.album.isEmpty()) {
                if (!artistAlbum.isEmpty()) {
                    artistAlbum += " — ";
                }
                artistAlbum += music.album;
            }
            currentArtistLabel.setText(artistAlbum);
        }
        if (totalTimeLabel != null) {
            totalTimeLabel.setText(music.getFormattedDuration());
        }
        if (elapsedTimeLabel != null) {
            elapsedTimeLabel.setText("0:00");
        }
    }

    /**
     * Updates labels for a music (used for session restore).
     */
    public void displayTrackInfo(Music music) {
        if (music != null) {
            currentMusic = music;
            updateCurrentSongLabels(music);
            loadWaveform(music);
        }
    }

    // ==================== Utilities ====================

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    // ==================== Event Notifications ====================

    private void notifyTrackChanged(Music music) {
        if (eventListener != null) {
            eventListener.onTrackChanged(music);
        }
    }

    private void notifySessionNeedsSave() {
        if (eventListener != null) {
            eventListener.onSessionNeedsSave();
        }
    }

    // ==================== Getters ====================

    public Music getCurrentMusic() {
        return currentMusic;
    }

    public VlcAudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public long getCurrentPosition() {
        return audioPlayer.getPosition();
    }
}

