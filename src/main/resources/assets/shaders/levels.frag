#version 330 core

uniform vec2 iResolution;
uniform float iTime;

out vec4 fragColor;

float random(vec2 st) {
    return fract(sin(dot(st.xy,
            vec2(12.9898,78.233)))*
    43758.5453123);
}

vec3 bg(vec2 st) {
    return vec3(0.16);
}

float bar(vec2 st, float height, float margin) {
    return step(st.y, height)
    *( 1.- step(st.x - floor(st.x), margin));
}

#define N 40.
#define blue vec3(21. / 255., 112. / 255., 192. / 255.)

void main() {
    vec2 st = gl_FragCoord.xy/iResolution.xy;

    vec3 color = bg(st);

    st.x *= N;

    vec2 barseed = vec2(floor(st.x));

    float height = 0.2 + random(barseed) * 0.2;

    height += sin(iTime + 10. * random(barseed + 10.)) * 0.1;

    color = mix(color, blue,
            bar(st, height, 0.1));

    fragColor = vec4(color, 1.0);
}
