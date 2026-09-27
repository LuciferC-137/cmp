package com.luciferc137.cmp.shader;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.Objects;


public final class ShaderBackgroundEngine {

    private static final String PLACEHOLDER_IMAGE_PATH = "/assets/placeholder_bg.png";

    private final ReadOnlyObjectWrapper<Image> outputImage =
            new ReadOnlyObjectWrapper<>(this, "outputImage");
    private final SimpleBooleanProperty running =
            new SimpleBooleanProperty(this, "running", false);

    private int viewportWidth = 0;
    private int viewportHeight = 0;

    /**
     * Start the engine. Must be called from the JavaFX Application Thread.
     * Target : initialize the offscreen GL context, compile the shader and start
     * the rendering loop (dedicated GL thread + synchronization with the FX Application Thread for frame publication).
     */
    public void start() {
        if (running.get()) {
            return;
        }
        loadPlaceholderImage();
        running.set(true);
        // TODO (LWJGL) : créer le contexte GL offscreen (OffscreenGLContext),
        //                compiler/linker le shader (ShaderProgram),
        //                démarrer la boucle de rendu (ShaderRenderer + PixelTransfer).
    }

    /**
     * Stop the engine and release any GL resources that may have been allocated.
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);
        // TODO (LWJGL) : arrêter la boucle de rendu, détruire le FBO/contexte GL.
    }

    /**
     * Inform the engine of the current display size of the background (in JavaFX scene pixels),
     * so that it can correctly size the rendering target.
     */
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        this.viewportWidth = width;
        this.viewportHeight = height;
        // TODO (LWJGL) : redimensionner le framebuffer/texture GL cible.
    }

    /**
     * Image currently produced by the engine. To bind to the background {@code ImageView}:
     * {@code backgroundView.imageProperty().bind(engine.outputImageProperty());}
     */
    public ReadOnlyObjectProperty<Image> outputImageProperty() {
        return outputImage.getReadOnlyProperty();
    }

    public Image getOutputImage() {
        return outputImage.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void loadPlaceholderImage() {
        try (InputStream stream = getClass().getResourceAsStream(PLACEHOLDER_IMAGE_PATH)) {
            Objects.requireNonNull(stream,
                    "Placeholder image not found on classpath : " + PLACEHOLDER_IMAGE_PATH);
            outputImage.set(new Image(stream));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not load placeholder image (" + PLACEHOLDER_IMAGE_PATH + ")", e);
        }
    }
}