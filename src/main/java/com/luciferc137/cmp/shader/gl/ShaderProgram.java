package com.luciferc137.cmp.shader.gl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.lwjgl.opengl.GL33.*;

/**
 * A linked GL program (vertex + fragment), with uniform location caching.
 * Must be created, used and deleted on the GL render thread, after
 * {@link OffscreenGLContext#create()}.
 * Fragment shaders are expected to expose {@code uniform vec2 iResolution}
 * and {@code uniform float iTime}, matching the Shadertoy-style convention
 * already used for the WebView prototype.
 */
public final class ShaderProgram {

    /**
     * Shared pass-through vertex shader: the background is always a flat
     * fullscreen quad, so no per-shader vertex logic is ever needed.
     */
    public static final String PASSTHROUGH_VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
            """;

    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    private ShaderProgram(int programId) {
        this.programId = programId;
    }

    /**
     * Reads a classpath resource (e.g. {@code /shaders/background.frag}) as
     * UTF-8 text.
     */
    public static String loadSource(String classpathResource) {
        try (InputStream stream = ShaderProgram.class.getResourceAsStream(classpathResource)) {
            Objects.requireNonNull(stream, "Shader resource not found: " + classpathResource);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            stream.transferTo(buffer);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read shader resource: " + classpathResource, e);
        }
    }

    /** Convenience for {@code compile(loadSource(classpathResource))}. */
    public static ShaderProgram compileFromResource(String classpathResource) {
        return compile(loadSource(classpathResource));
    }

    /**
     * Compiles {@code fragmentSource} against {@link #PASSTHROUGH_VERTEX_SOURCE}
     * and links the result.
     *
     * @throws ShaderCompilationException on compile or link failure
     */
    public static ShaderProgram compile(String fragmentSource) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, PASSTHROUGH_VERTEX_SOURCE);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            throw new ShaderCompilationException("Program link failed: " + log);
        }

        // Shader objects can be freed once linked into the program.
        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return new ShaderProgram(program);
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            String kind = type == GL_VERTEX_SHADER ? "Vertex" : "Fragment";
            throw new ShaderCompilationException(kind + " shader compilation failed: " + log);
        }
        return shader;
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setUniform1f(String name, float value) {
        glUniform1f(location(name), value);
    }

    public void setUniform2f(String name, float x, float y) {
        glUniform2f(location(name), x, y);
    }

    /**
     * Locations are cached after first lookup. Returns -1 for a uniform
     * absent from the shader (e.g. optimized out for being unused) —
     * silently ignored rather than failing, since that's a valid shader.
     */
    private int location(String name) {
        return uniformLocations.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    /** Releases the GL program. The instance is unusable afterward. */
    public void delete() {
        glDeleteProgram(programId);
        uniformLocations.clear();
    }
}