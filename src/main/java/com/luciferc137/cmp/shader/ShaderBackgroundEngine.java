package com.luciferc137.cmp.shader;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.Objects;

/**
 * Public facade of the shader background system — the only class the rest
 * of the app (controllers, FXML) should ever reference.
 */
public final class ShaderBackgroundEngine {

    private static final String PLACEHOLDER_IMAGE_PATH = "/assets/placeholder_bg.png";
    private static final String FRAGMENT_SHADER_RESOURCE = "/assets/shaders/square_bg.frag";
    private static final int DEFAULT_WIDTH = 16;
    private static final int DEFAULT_HEIGHT = 16;

    private final ReadOnlyObjectWrapper<Image> outputImage =
            new ReadOnlyObjectWrapper<>(this, "outputImage");
    private final SimpleBooleanProperty running =
            new SimpleBooleanProperty(this, "running", false);

    private int viewportWidth = DEFAULT_WIDTH;
    private int viewportHeight = DEFAULT_HEIGHT;
    private ShaderRenderer renderer;

    /**
     * Start the engine. Must be called from the JavaFX Application Thread.
     * Shows the static placeholder immediately, then spawns the dedicated
     * GL render thread, which takes over {@link #outputImageProperty()} as
     * soon as its first frame is ready. Call {@link #resize} beforehand (or
     * shortly after) with the real background size — it defaults to a small
     * placeholder size otherwise.
     */
    public void start() {
        if (running.get()) {
            return;
        }
        loadPlaceholderImage();
        renderer = new ShaderRenderer(FRAGMENT_SHADER_RESOURCE, viewportWidth, viewportHeight, outputImage::set);
        renderer.start();
        running.set(true);
    }

    /**
     * Stop the engine and release its GL resources. Blocks briefly (up to
     * ~1s) until the render thread has shut down cleanly.
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        renderer.stop();
        renderer = null;
        running.set(false);
    }

    /**
     * Inform the engine of the current display size of the background (in
     * JavaFX scene pixels), so it can correctly size the rendering target.
     */
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        this.viewportWidth  = Math.min(width, 4096);
        this.viewportHeight = Math.min(height, 4096);
        if (renderer != null) {
            renderer.requestResize(width, height);
        }
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