#version 330 core

out vec4 fragColor;

uniform vec2 iResolution;
uniform float iTime;

void main() {
    vec2 uv = gl_FragCoord.xy / iResolution.xy;
    fragColor = vec4(0.15, 0.15, 0.15, 1.0);
}