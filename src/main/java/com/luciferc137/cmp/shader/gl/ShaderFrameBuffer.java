package com.luciferc137.cmp.shader.gl;

import static org.lwjgl.opengl.GL33.*;

/**
 * A framebuffer object (FBO) with a single RGBA8 color texture attachment —
 * the render target used instead of the (invisible) default framebuffer.
 * <p>
 * No depth/stencil attachment: a flat fullscreen shader needs neither.
 * <p>
 * Must be created, used and deleted on the GL render thread, after
 * {@link OffscreenGLContext#create()}.
 */
public final class ShaderFrameBuffer {

    private int framebufferId;
    private int colorTextureId;
    private int width;
    private int height;

    public ShaderFrameBuffer(int width, int height) {
        allocate(width, height);
    }

    /** Binds this FBO as the current render target and sets the GL viewport. */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glViewport(0, 0, width, height);
    }

    /** Restores the default framebuffer (unused for display, but keeps GL state clean). */
    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Recreates the color texture at the new size. Cheap relative to
     * recreating the FBO itself, so the framebuffer object is reused.
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) {
            return;
        }
        glDeleteTextures(colorTextureId);
        glDeleteFramebuffers(framebufferId);
        allocate(newWidth, newHeight);
    }

    public int getColorTextureId() {
        return colorTextureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void delete() {
        glDeleteTextures(colorTextureId);
        glDeleteFramebuffers(framebufferId);
    }

    private void allocate(int width, int height) {
        this.width = width;
        this.height = height;

        colorTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        framebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTextureId, 0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete, GL status: 0x" + Integer.toHexString(status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}