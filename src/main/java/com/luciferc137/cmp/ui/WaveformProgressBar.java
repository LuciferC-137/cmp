package com.luciferc137.cmp.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Composant personnalisé affichant une barre de progression avec
 * un histogramme de waveform en arrière-plan.
 */
public class WaveformProgressBar extends Pane {

    private final Canvas waveformCanvas;
    private final Canvas progressCanvas;

    private float[] waveformData;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    private Color waveformColor = Color.rgb(100, 100, 100, 0.5);
    private Color waveformPlayedColor = Color.rgb(30, 144, 255, 0.8);
    private Color progressLineColor = Color.rgb(30, 144, 255);
    private Color backgroundColor = Color.rgb(40, 40, 40);

    public WaveformProgressBar() {
        waveformCanvas = new Canvas();
        progressCanvas = new Canvas();

        getChildren().addAll(waveformCanvas, progressCanvas);

        // Lier la taille des canvas à la taille du conteneur
        waveformCanvas.widthProperty().bind(widthProperty());
        waveformCanvas.heightProperty().bind(heightProperty());
        progressCanvas.widthProperty().bind(widthProperty());
        progressCanvas.heightProperty().bind(heightProperty());

        // Redessiner quand la taille change
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        // Redessiner quand la progression change
        progress.addListener((obs, oldVal, newVal) -> drawProgress());

        // Style par défaut
        setStyle("-fx-background-color: #282828;");
        setMinHeight(50);
        setPrefHeight(60);
    }

    /**
     * Définit les données de waveform à afficher.
     *
     * @param data tableau de valeurs normalisées (0.0 à 1.0)
     */
    public void setWaveformData(float[] data) {
        this.waveformData = data;
        redraw();
    }

    /**
     * Obtient la propriété de progression (0.0 à 1.0).
     */
    public DoubleProperty progressProperty() {
        return progress;
    }

    /**
     * Définit la progression actuelle.
     *
     * @param value valeur entre 0.0 et 1.0
     */
    public void setProgress(double value) {
        progress.set(Math.max(0, Math.min(1, value)));
    }

    /**
     * Obtient la progression actuelle.
     */
    public double getProgress() {
        return progress.get();
    }

    /**
     * Efface les données de waveform.
     */
    public void clear() {
        this.waveformData = null;
        this.progress.set(0);
        redraw();
    }

    /**
     * Redessine complètement le composant.
     */
    private void redraw() {
        drawWaveform();
        drawProgress();
    }

    /**
     * Dessine l'histogramme de waveform.
     */
    private void drawWaveform() {
        double width = getWidth();
        double height = getHeight();

        if (width <= 0 || height <= 0) return;

        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        // Fond
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);

        if (waveformData == null || waveformData.length == 0) {
            return;
        }

        double barWidth = width / waveformData.length;
        double centerY = height / 2;
        double maxBarHeight = height * 0.8; // 80% de la hauteur

        for (int i = 0; i < waveformData.length; i++) {
            double barHeight = waveformData[i] * maxBarHeight;
            double x = i * barWidth;

            // Dessiner la barre symétrique (miroir haut/bas)
            gc.setFill(waveformColor);
            gc.fillRect(x, centerY - barHeight / 2, Math.max(1, barWidth - 1), barHeight);
        }
    }

    /**
     * Dessine l'overlay de progression.
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

            // Redessiner les barres "jouées" avec la couleur de progression
            int barsPlayed = (int) (waveformData.length * progress.get());

            for (int i = 0; i < barsPlayed && i < waveformData.length; i++) {
                double barHeight = waveformData[i] * maxBarHeight;
                double x = i * barWidth;

                gc.setFill(waveformPlayedColor);
                gc.fillRect(x, centerY - barHeight / 2, Math.max(1, barWidth - 1), barHeight);
            }
        }

        // Ligne de progression
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

