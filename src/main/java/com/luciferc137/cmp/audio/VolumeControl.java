package com.luciferc137.cmp.audio;

public interface VolumeControl {
    static final int DEFAULT_MAX_VOLUME = 100;
    static final int DEFAULT_MIN_VOLUME = 0;

    void setVolume(int volume);

    int getVolume();

}
