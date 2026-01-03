package com.luciferc137.cmp.audio;

import com.luciferc137.cmp.library.Music;

public interface AudioPlayer {

    void play(Music music);

    void pause();

    void resume();

    void stop();

    boolean isPlaying();

    /**
     * Gets the current playback position in milliseconds.
     */
    long getPosition();

    /**
     * Gets the total media duration in milliseconds.
     */
    long getDuration();

    /**
     * Seeks to a specific position.
     *
     * @param positionMs position in milliseconds
     */
    void seek(long positionMs);
}
