package com.luciferc137.cmp.audio;

import com.luciferc137.cmp.library.Music;

public interface AudioPlayer {

    void play(Music music);

    void pause();

    void resume();

    void stop();

    boolean isPlaying();

    /**
     * Obtient la position actuelle de lecture en millisecondes.
     */
    long getPosition();

    /**
     * Obtient la durée totale du média en millisecondes.
     */
    long getDuration();

    /**
     * Déplace la lecture à une position spécifique.
     *
     * @param positionMs position en millisecondes
     */
    void seek(long positionMs);
}
