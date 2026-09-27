package com.luciferc137.cmp.shader;

import com.luciferc137.cmp.MainApp;
import com.luciferc137.cmp.shader.gl.FullscreenQuad;
import com.luciferc137.cmp.shader.gl.OffscreenGLContext;
import com.luciferc137.cmp.shader.gl.ShaderFrameBuffer;
import com.luciferc137.cmp.shader.gl.ShaderProgram;
import com.luciferc137.cmp.shader.transfer.PixelTransfer;
import javafx.application.Platform;
import javafx.scene.image.WritableImage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;

import static org.lwjgl.opengl.GL33.*;

/**
 * Owns the whole GL pipeline (context, program, FBO, quad, pixel transfer)
 * and drives the render loop on a single dedicated thread — every GL object
 * is created, used and destroyed there, never on the FX Application Thread.
 * <p>
 * A new frame is only pushed to JavaFX (via the {@code onFrame} callback,
 * always run on the FX Application Thread) when the target size changes;
 * otherwise the same {@link WritableImage} is mutated in place each frame,
 * which redraws automatically wherever it's displayed.
 * <p>
 * Package-private: {@link ShaderBackgroundEngine} is the only intended entry
 * point from the rest of the app.
 */
final class ShaderRenderer {

    private static final long TARGET_FRAME_NANOS = 1_000_000_000L / 60;

    private final String fragmentShaderResource;
    private final Consumer<WritableImage> onFrame;
    private final AtomicInteger pendingWidth = new AtomicInteger();
    private final AtomicInteger pendingHeight = new AtomicInteger();

    private volatile boolean running;
    private Thread thread;

    ShaderRenderer(String fragmentShaderResource, int initialWidth, int initialHeight,
                   Consumer<WritableImage> onFrame) {
        this.fragmentShaderResource = fragmentShaderResource;
        this.onFrame = onFrame;
        pendingWidth.set(Math.max(1, initialWidth));
        pendingHeight.set(Math.max(1, initialHeight));
    }

    void start() {
        if (thread != null) {
            return;
        }
        running = true;
        thread = new Thread(this::runLoop, "shader-render-thread");
        thread.setDaemon(true);
        thread.start();
    }

    /** Blocks (briefly) until the render thread has released its GL resources. */
    void stop() {
        running = false;
        if (thread == null) {
            return;
        }
        thread.interrupt();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
    }

    /** Thread-safe: may be called from any thread, picked up on the next frame. */
    void requestResize(int width, int height) {
        pendingWidth.set(Math.max(1, width));
        pendingHeight.set(Math.max(1, height));
    }

    private void runLoop() {
        OffscreenGLContext context = new OffscreenGLContext();
        ShaderProgram program = null;
        ShaderFrameBuffer frameBuffer = null;
        FullscreenQuad quad = null;
        PixelTransfer pixelTransfer = null;

        try {
            context.create();
            program = ShaderProgram.compileFromResource(fragmentShaderResource);
            quad = new FullscreenQuad();

            int width = pendingWidth.get();
            int height = pendingHeight.get();
            frameBuffer = new ShaderFrameBuffer(width, height);
            pixelTransfer = new PixelTransfer(width, height);
            WritableImage currentImage = publishNewImage(width, height);

            long startNanos = System.nanoTime();

            while (running) {
                long frameStart = System.nanoTime();

                int newWidth = pendingWidth.get();
                int newHeight = pendingHeight.get();
                if (newWidth != frameBuffer.getWidth() || newHeight != frameBuffer.getHeight()) {
                    frameBuffer.resize(newWidth, newHeight);
                    pixelTransfer.resize(newWidth, newHeight);
                    currentImage = publishNewImage(newWidth, newHeight);
                }

                float elapsedSeconds = (frameStart - startNanos) / 1_000_000_000f;

                frameBuffer.bind();
                glClearColor(0f, 0f, 0f, 0f);
                glClear(GL_COLOR_BUFFER_BIT);
                program.use();
                program.setUniform2f("iResolution", frameBuffer.getWidth(), frameBuffer.getHeight());
                program.setUniform1f("iTime", elapsedSeconds);
                quad.draw();
                pixelTransfer.capture();
                frameBuffer.unbind();

                WritableImage frameImage = currentImage;
                PixelTransfer transfer = pixelTransfer;
                CountDownLatch writeDone = new CountDownLatch(1);
                Platform.runLater(() -> {
                    try {
                        transfer.writeTo(frameImage);
                    } catch (Exception e) {
                        MainApp.logger.log(Level.WARNING, "Error while writing frame", e);
                    } finally {
                        writeDone.countDown();
                    }
                });
                try {
                    writeDone.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                    break;
                }

                pace(frameStart);
            }
        } catch (Exception e) {
            MainApp.logger.log(Level.WARNING, "Error in shader rendering loop", e);
        } finally {
            if (quad != null) quad.delete();
            if (frameBuffer != null) frameBuffer.delete();
            if (program != null) program.delete();
            if (pixelTransfer != null) pixelTransfer.delete();
            context.destroy();
        }
    }

    private WritableImage publishNewImage(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        Platform.runLater(() -> onFrame.accept(image));
        return image;
    }

    private void pace(long frameStartNanos) {
        long sleepNanos = TARGET_FRAME_NANOS - (System.nanoTime() - frameStartNanos);
        if (sleepNanos <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}