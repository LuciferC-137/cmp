package com.luciferc137.cmp.ui.lyrics;

import com.luciferc137.cmp.ui.utils.AlbumColorExtractor;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class LyricsColorHandler {
    private static final Color APP_BASE_BACKGROUND = Color.web("#282828");

    private static final int COLOR_NUMBER = 3; // Number of dominant colors to extract for gradient
    private static final double GRADIENT_TRANSITION_MS = 200; // Duration of gradient transition in milliseconds
    private double gradientInterp = 1.0; // Interpolation factor for gradient transition
    private long lastGradientUpdateTime = 0; // Last time the gradient was updated
    private AnimationTimer gradientTimer;

    private List<Color> albumColors;
    private List<Color> prevAlbumColors;

    private final LyricsController lyricsController;

    public LyricsColorHandler(LyricsController lyricsController) {
        this.lyricsController = lyricsController;
    }

    /**
     * Refreshes the background gradient based on the current album colors and interpolation factor.
     * This method is called during the gradient transition animation.
     */
    public void refreshBackgroundGradient() {
        List<Color> colors = new ArrayList<>();
        if (gradientInterp >= 1.0) {
            colors = albumColors != null ? albumColors : List.of(APP_BASE_BACKGROUND);
        } else {
            for (int i = 0; i < COLOR_NUMBER; i++) {
                if (albumColors != null && prevAlbumColors != null) {
                    colors.add(prevAlbumColors.get(i).interpolate(albumColors.get(i),
                            gradientInterp));
                }
            }
            long now = System.currentTimeMillis();
            gradientInterp += (double) (now - lastGradientUpdateTime) / GRADIENT_TRANSITION_MS;
            lastGradientUpdateTime = now;
            if (gradientInterp > 1.0) {
                gradientInterp = 1.0;
            }
        }

        Background bg = AlbumColorExtractor.buildAngledBackground(
                !colors.isEmpty() ? colors : List.of(APP_BASE_BACKGROUND),
                45, lyricsController.headerBox.getWidth(),
                lyricsController.headerBox.getHeight(),
                APP_BASE_BACKGROUND, 0.5);
        lyricsController.headerBox.setBackground(bg);
    }

    /**
     * Updates the album colors and starts the gradient transition.
     * This should be called whenever the cover art changes.
     */
    public void updateGradientColor() {
        Platform.runLater(
                () -> {
                    this.prevAlbumColors = albumColors;
                    this.albumColors = AlbumColorExtractor
                            .extractDominantColors(lyricsController.coverArtView.getImage(), COLOR_NUMBER);
                    this.gradientInterp = 0.0;
                    this.lastGradientUpdateTime = System.currentTimeMillis();
                    startGradientRefreshLoop();
                }
        );
    }

    /**
     * Starts the gradient refresh loop using an AnimationTimer.
     * This will continuously update the background gradient until the transition is complete.
     */
    private void startGradientRefreshLoop() {
        if (gradientTimer != null) {
            gradientTimer.stop();
        }

        gradientTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gradientInterp < 1.0) {
                    refreshBackgroundGradient();
                } else {
                    stop();
                }
            }
        };

        gradientTimer.start();
    }
}
