package com.luciferc137.cmp.ui.lyrics;

import javafx.animation.AnimationTimer;
import javafx.scene.control.ScrollPane;

import java.util.function.LongSupplier;

public class ScrollSynchronizer {
    // Scroll sync state
    private boolean scrollSyncEnabled = false;
    private LongSupplier positionSupplier;
    private LongSupplier durationSupplier;
    private AnimationTimer scrollSyncTimer;
    private final ScrollPane lyricsScrollPane;

    // Smooth scrolling interpolation
    private double currentScrollValue = 0;
    private static final double SCROLL_SMOOTHING = 0.08; // Lower = smoother but slower response

    public ScrollSynchronizer(ScrollPane lyricsScrollPane) {
        this.lyricsScrollPane = lyricsScrollPane;
    }

    public boolean isEnabled() {
        return scrollSyncEnabled;
    }

    public void disable() {
        scrollSyncEnabled = false;
        stopScrollSync();
    }

    public void enable() {
        scrollSyncEnabled = true;
        startScrollSync();
    }

    public void toggle() {
        if (scrollSyncEnabled) {
            disable();
        } else {
            enable();
        }
    }

    /**
     * Sets the playback position and duration suppliers for scroll synchronization.
     *
     * @param positionSupplier Supplier for current playback position in milliseconds
     * @param durationSupplier Supplier for total track duration in milliseconds
     */
    public void setPlaybackSuppliers(LongSupplier positionSupplier, LongSupplier durationSupplier) {
        this.positionSupplier = positionSupplier;
        this.durationSupplier = durationSupplier;
    }

    private void startScrollSync() {
        if (scrollSyncTimer != null) {
            scrollSyncTimer.stop();
        }

        // Initialize current scroll value to current position
        if (lyricsScrollPane != null) {
            currentScrollValue = lyricsScrollPane.getVvalue();
        }

        scrollSyncTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateScrollPosition();
            }
        };
        scrollSyncTimer.start();
    }

    private void stopScrollSync() {
        if (scrollSyncTimer != null) {
            scrollSyncTimer.stop();
            scrollSyncTimer = null;
        }
    }

    /**
     * Updates the scroll position based on playback progress.
     * The lyrics scroll so that the current position is always in the middle of the viewport.
     */
    private void updateScrollPosition() {
        if (lyricsScrollPane == null || positionSupplier == null || durationSupplier == null) {
            return;
        }

        long position = positionSupplier.getAsLong();
        long duration = durationSupplier.getAsLong();

        if (duration <= 0) {
            return;
        }

        double progress = (double) position / duration;
        progress = Math.clamp(progress, 0, 1);

        // Get viewport height ratio (how much of the content is visible)
        double viewportHeight = lyricsScrollPane.getViewportBounds().getHeight();
        double contentHeight = lyricsScrollPane.getContent().getBoundsInLocal().getHeight();

        if (contentHeight <= viewportHeight) {
            return;
        }

        double currentContentPos = progress * contentHeight;
        double viewportTopPos = currentContentPos - (viewportHeight / 2);

        double maxScroll = contentHeight - viewportHeight;
        viewportTopPos = Math.clamp(viewportTopPos, 0, maxScroll);

        double targetScrollValue = viewportTopPos / maxScroll;

        currentScrollValue = currentScrollValue + (targetScrollValue - currentScrollValue) * SCROLL_SMOOTHING;

        lyricsScrollPane.setVvalue(currentScrollValue);
    }
}
