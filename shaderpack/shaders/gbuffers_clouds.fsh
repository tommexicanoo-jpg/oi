#version 120

#include "/lib/lighting.glsl"

uniform sampler2D gtexture;

varying vec2 texcoord;
varying vec4 glcolor;
varying vec3 normalV;
varying vec3 viewPos;

void main() {
    vec4 tex = texture2D(gtexture, texcoord) * glcolor;
    if (tex.a < 0.05) discard;

    vec3 info = sunInfo();
    vec3 n = safeNormal(normalV);
    float upDot = dot(n, normalize(upPosition));
    float faceShade = 0.72 + 0.28 * upDot;

    // las nubes siguen iluminadas un rato despues de que el sol se oculte
    float lightK = smoothstep(-0.22, 0.15, info.z);
    vec3 dayCol = mix(vec3(1.0), vec3(1.0, 0.62, 0.45), info.y * 0.8);
    vec3 nightCol = vec3(0.09, 0.11, 0.19);
    vec3 col = mix(nightCol, dayCol, lightK) * faceShade;

    // tormenta: nubes grises
    float lum = dot(col, vec3(0.333));
    col = mix(col, vec3(lum) * 0.75, rainStrength * 0.6);

    col = applyFog(col, viewPos);
    gl_FragData[0] = vec4(col, tex.a * 0.9);
}
