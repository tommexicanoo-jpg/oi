#version 120

#include "/lib/lighting.glsl"

uniform sampler2D gtexture;

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
    vec4 tex = texture2D(gtexture, texcoord) * glcolor;
    if (tex.a < 0.02) discard;

    vec3 info = sunInfo();
    vec3 col = mix(vec3(0.20, 0.24, 0.32), vec3(0.78, 0.84, 0.95), smoothstep(-0.2, 0.15, info.z));
    gl_FragData[0] = vec4(col, tex.a * 0.55);
}
