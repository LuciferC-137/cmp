package com.luciferc137.cmp.shader.gl;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Geometry for a single oversized triangle covering the full clip space —
 * cheaper than a two-triangle quad, no shared diagonal edge.
 * <p>
 * Must be created, drawn and deleted on the GL render thread.
 */
public final class FullscreenQuad {

    private static final float[] VERTICES = {
            -1f, -1f,
            3f, -1f,
            -1f,  3f
    };

    private final int vaoId;
    private final int vboId;

    public FullscreenQuad() {
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        try (MemoryStack stack = stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(VERTICES.length);
            buffer.put(VERTICES).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        }

        // Location 0 matches `layout(location = 0) in vec2 aPos;` in
        // ShaderProgram.PASSTHROUGH_VERTEX_SOURCE.
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    public void draw() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
    }

    public void delete() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}