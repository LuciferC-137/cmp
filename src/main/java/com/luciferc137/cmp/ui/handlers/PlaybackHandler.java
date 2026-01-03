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
        int numSamples = 200;

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

