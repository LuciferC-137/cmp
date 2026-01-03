package com.luciferc137.cmp.ui.handlers;

import com.luciferc137.cmp.library.PlaybackQueue;
import javafx.scene.control.Button;

/**
 * Handles shuffle and loop button state and styling.
 */
public class ShuffleLoopHandler {

    private final PlaybackQueue playbackQueue;

    private Button shuffleButton;
    private Button loopButton;

    public ShuffleLoopHandler() {
        this.playbackQueue = PlaybackQueue.getInstance();
    }

    /**
     * Binds UI components.
     */
    public void bindUIComponents(Button shuffleButton, Button loopButton) {
        this.shuffleButton = shuffleButton;
        this.loopButton = loopButton;
    }

    /**
     * Updates the shuffle button style based on state.
     */
    public void updateShuffleButtonStyle() {
        if (shuffleButton != null) {
            boolean enabled = playbackQueue.isShuffleEnabled();
            shuffleButton.setText(enabled ? "Shuf ✓" : "Shuf");
            shuffleButton.setStyle(enabled ?
                    "-fx-font-size: 11px; -fx-background-color: #4CAF50; -fx-text-fill: white;" :
                    "-fx-font-size: 11px;");
        }
    }

    /**
     * Updates the loop button style based on mode.
     */
    public void updateLoopButtonStyle() {
        if (loopButton != null) {
            PlaybackQueue.LoopMode mode = playbackQueue.getLoopMode();
            String text = switch (mode) {
                case NONE -> "Loop";
                case PLAYLIST -> "All";
                case SINGLE -> "One";
            };
            loopButton.setText(text);
            loopButton.setStyle(mode != PlaybackQueue.LoopMode.NONE ?
                    "-fx-font-size: 11px; -fx-background-color: #2196F3; -fx-text-fill: white;" :
                    "-fx-font-size: 11px;");
        }
    }

    /**
     * Updates both button styles.
     */
    public void updateAllButtonStyles() {
        updateShuffleButtonStyle();
        updateLoopButtonStyle();
    }
}

