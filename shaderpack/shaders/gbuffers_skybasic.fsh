#version 120

#include "/lib/lighting.glsl"

uniform mat4 gbufferProjectionInverse;
uniform float viewWidth;
uniform float viewHeight;

varying vec4 glcolor;
varying float isStar;

void main() {
    if (isStar > 0.5) {
        gl_FragData[0] = vec4(1.0, 0.97, 0.92, glcolor.a * 0.12 * (1.0 - ecoT()));
    } else {
        vec2 uv = gl_FragCoord.xy / vec2(viewWidth, viewHeight);
        vec4 p = gbufferProjectionInverse * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
        vec3 dir = normalize(p.xyz / p.w);
        gl_FragData[0] = vec4(toGamma(skyGradient(dir)), 1.0);
    }
}
