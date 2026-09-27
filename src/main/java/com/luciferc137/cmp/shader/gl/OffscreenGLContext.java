package com.luciferc137.cmp.shader.gl;

import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * OpenGL context "offscreen" : no window is ever displayed on the screen,
 * but we have a real GL context usable for rendering into an FBO (see {@link ShaderFrameBuffer}).
 */
public final class OffscreenGLContext {

    private long windowHandle = NULL_HANDLE;
    private boolean created = false;

    private static final long NULL_HANDLE = 0L;

    /**
     * Initialize GLFW, create the hidden window and make its GL context current on the calling thread.
     * Should be called only once, from the dedicated rendering thread.
    */
    public void create() {
        if (created) {
            return;
        }

        if (!glfwInit()) {
            throw new IllegalStateException("Could not initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        windowHandle = glfwCreateWindow(1, 1, "offscreen-shader-context", NULL_HANDLE, NULL_HANDLE);
        if (windowHandle == NULL_HANDLE) {
            glfwTerminate();
            throw new IllegalStateException("Could not create the offscreen GLFW window");
        }

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();

        glfwSwapInterval(0);

        created = true;
    }

    /**
     * Render current GL context on the calling thread.
     * Necessary if rendering must resume after switching to another context
     * (normally unnecessary as long as only one GL thread is used, but
     * keeps the door open for later).
    */
    public void makeCurrent() {
        requireCreated();
        glfwMakeContextCurrent(windowHandle);
    }

    /**
     * Destroy the hidden window and terminate GLFW. To be called from the same
     * thread as {@link #create()}, once rendering is definitively stopped.
     */
    public void destroy() {
        if (!created) {
            return;
        }
        glfwDestroyWindow(windowHandle);
        windowHandle = NULL_HANDLE;
        glfwTerminate();
        created = false;
    }

    public boolean isCreated() {
        return created;
    }

    private void requireCreated() {
        if (!created) {
            throw new IllegalStateException("OffscreenGLContext.create() has not been called yet");
        }
    }
}