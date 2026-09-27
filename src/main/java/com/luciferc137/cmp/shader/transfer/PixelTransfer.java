package com.luciferc137.cmp.shader.transfer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

/**
 * Bridges a GL framebuffer to a JavaFX {@link WritableImage}.
 * <p>
 * {@link #capture()} must run on the GL render thread, with the framebuffer
 * to read already bound. {@link #writeTo} must run on the FX Application
 * Thread. Splitting the two is intentional: reading pixels is the
 * GL-thread-bound step, writing to the image is the FX-thread-bound step.
 * <p>
 * Pixels are read as {@code GL_BGRA} (matching
 * {@link PixelFormat#getByteBgraInstance()} exactly, avoiding a per-pixel
 * channel swap) and row-flipped, since GL's row 0 is the bottom of the
 * image while JavaFX's row 0 is the top.
 */
public final class PixelTransfer {

    private ByteBuffer readBuffer;
    private ByteBuffer flippedBuffer;
    private int width;
    private int height;

    public PixelTransfer(int width, int height) {
        allocate(width, height);
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) {
            return;
        }
        free();
        allocate(newWidth, newHeight);
    }

    /** Reads the currently bound framebuffer's color attachment. GL thread only. */
    public void capture() {
        readBuffer.clear();
        glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, readBuffer);
        flipRows();
    }

    /** Pushes the last {@link #capture()}'d frame into {@code image}. FX thread only. */
    public void writeTo(WritableImage image) {
        image.getPixelWriter().setPixels(0, 0, width, height,
                PixelFormat.getByteBgraInstance(), flippedBuffer, width * 4);
    }

    public void delete() {
        free();
    }

    private void flipRows() {
        int rowBytes = width * 4;
        for (int row = 0; row < height; row++) {
            int srcOffset = row * rowBytes;
            int dstOffset = (height - 1 - row) * rowBytes;
            flippedBuffer.put(dstOffset, readBuffer, srcOffset, rowBytes);
        }
    }

    private void allocate(int width, int height) {
        this.width = width;
        this.height = height;
        int size = width * height * 4;
        // Persistent native buffers (not garbage-collected): must be freed explicitly, see delete().
        readBuffer = MemoryUtil.memAlloc(size);
        flippedBuffer = MemoryUtil.memAlloc(size);
    }

    private void free() {
        MemoryUtil.memFree(readBuffer);
        MemoryUtil.memFree(flippedBuffer);
    }
}