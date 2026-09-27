package com.luciferc137.cmp.shader.gl;

/** Thrown when a shader fails to compile or a program fails to link. */
public class ShaderCompilationException extends RuntimeException {
    public ShaderCompilationException(String message) {
        super(message);
    }
}