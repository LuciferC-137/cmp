package com.luciferc137.cmp.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Custom component displaying a progress bar with
 * a waveform histogram in the background.
 */
public class WaveformProgressBar extends Pane {

    private final Canvas waveformCanvas;
    private final Canvas progressCanvas;

    private float[] waveformData;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    private Color waveformColor = Color.rgb(100, 100, 100, 0.5);
    private Color waveformPlayedColor = Color.rgb(30, 144, 255, 0.8);
    private Color progressLineColor = Color.rgb(30, 144, 255);
    private Color backgroundColor = Color.rgb(30, 30, 30);

    public WaveformProgressBar() {
        waveformCanvas = new Canvas();
        progressCanvas = new Canvas();

        getChildren().addAll(waveformCanvas, progressCanvas);

        // Bind canvas size to container size
        waveformCanvas.widthProperty().bind(widthProperty());
        waveformCanvas.heightProperty().bind(heightProperty());
        progressCanvas.widthProperty().bind(widthProperty());
        progressCanvas.heightProperty().bind(heightProperty());

        // Redraw when size changes
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        // Redraw when progress changes
        progress.addListener((obs, oldVal, newVal) -> drawProgress());

        // Default style
        setStyle("-fx-background-color: #282828;");
        setMinHeight(50);
        setPrefHeight(60);
    }

    /**
     * Sets the waveform data to display.
     *
     * @param data array of normalized values (0.0 to 1.0)
     */
    public void setWaveformData(float[] data) {
        this.waveformData = data;
        redraw();
    }

    /**
     * Gets the progress property (0.0 to 1.0).
     */
    public DoubleProperty progressProperty() {
        return progress;
    }

    /**
     * Sets the current progress.
     *
     * @param value value between 0.0 and 1.0
     */
    public void setProgress(double value) {
        progress.set(Math.max(0, Math.min(1, value)));
    }

    /**
     * Gets the current progress.
     */
    public double getProgress() {
        return progress.get();
    }

    /**
     * Clears the waveform data.
     */
    public void clear() {
        this.waveformData = null;
        this.progress.set(0);
        redraw();
    }

    /**
     * Completely redraws the component.
     */
    private void redraw() {
        drawWaveform();
        drawProgress();
    }

    /**
     * Draws the waveform histogram.
     */
    private void drawWaveform() {
        double width = getWidth();
        double height = getHeight();

        if (width <= 0 || height <= 0) return;

        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        // Background
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);

        if (waveformData == null || waveformData.length == 0) {
            return;
        }

        double barWidth = width / waveformData.length;
        double centerY = height / 2;
        double maxBarHeight = height * 0.8; // 80% of height

        for (int i = 0; i < waveformData.length; i++) {
            double barHeight = waveformData[i] * maxBarHeight;
            double x = i * barWidth;

            // Draw symmetric bar (mirrored top/bottom)
            gc.setFill(waveformColor);
            gc.fillRect(x, centerY - barHeight / 2, Math.max(1, barWidth - 1), barHeight);
        }
    }

    /**
     * Draws the progress overlay.
     */
    private void drawProgress() {
        double width = getWidth();
        double height = getHeight();

        if (width <= 0 || height <= 0) return;

        GraphicsContext gc = progressCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double progressX = width * progress.get();

        if (waveformData != null && waveformData.length > 0) {
            double barWidth = width / waveformData.length;
            double centerY = height / 2;
            double maxBarHeight = height * 0.8;

            // Redraw "played" bars with progress color
            int barsPlayed = (int) (waveformData.length * progress.get());

            for (int i = 0; i < barsPlayed && i < waveformData.length; i++) {
                double barHeight = waveformData[i] * maxBarHeight;
                double x = i * barWidth;

                gc.setFill(waveformPlayedColor);
                gc.fillRect(x, centerY - barHeight / 2, Math.max(1, barWidth - 1), barHeight);
            }
        }

        // Progress line
        if (progress.get() > 0) {
            gc.setStroke(progressLineColor);
            gc.setLineWidth(2);
            gc.strokeLine(progressX, 0, progressX, height);
        }
    }

    public void setWaveformColor(Color color) {
        this.waveformColor = color;
        redraw();
    }

    public void setWaveformPlayedColor(Color color) {
        this.waveformPlayedColor = color;
        redraw();
    }

    public void setProgressLineColor(Color color) {
        this.progressLineColor = color;
        redraw();
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        redraw();
    }
}

