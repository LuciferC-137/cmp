package com.luciferc137.cmp.audio;

import com.luciferc137.cmp.library.Music;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import java.io.File;

public class VlcAudioPlayer implements AudioPlayer, VolumeControl {
    private int volume = 50;
    private AudioPlayerComponent audioPlayerComponent;
    private MediaPlayer mediaPlayer;

    public VlcAudioPlayer() {
        try {
            audioPlayerComponent = new AudioPlayerComponent();
            mediaPlayer = audioPlayerComponent.mediaPlayer();

            mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter());
        } catch (Exception e) {
            System.err.println("Error during VLC initialization: " + e.getMessage());
        }
    }

    public void play(Music music) {
        stop();

        try {
            File file = new File(music.filePath);
            if (!file.exists()) {
                System.err.println("File Not Found: " + music.filePath);
                return;
            }

            System.out.println("Loading media: " + file.getAbsolutePath());
            mediaPlayer.media().play(file.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error during media reading: " + e.getMessage());
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPlaying()) {
            mediaPlayer.controls().setPause(true);
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().setPause(false);
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.status().isPlaying();
    }

    public boolean isPaused() {
        return mediaPlayer != null && !mediaPlayer.status().isPlaying() && mediaPlayer.status().length() > 0;
    }

    public void release() {
        if (audioPlayerComponent != null) {
            audioPlayerComponent.release();
        }
    }

    @Override
    public void setVolume(int volume) {
        if (volume < DEFAULT_MIN_VOLUME || volume > DEFAULT_MAX_VOLUME) {
            return;
        }
        mediaPlayer.audio().setVolume(volume);
        this.volume = volume;
    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public long getPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.status().time();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.status().length();
        }
        return 0;
    }

    @Override
    public void seek(long positionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.controls().setTime(positionMs);
        }
    }

}

