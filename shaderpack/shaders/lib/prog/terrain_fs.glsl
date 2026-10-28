#define FRAME_VARYINGS
#include "/lib/lighting.glsl"

uniform sampler2D gtexture;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normalV;
varying vec3 viewPos;
varying vec3 shadowPos;
varying float emis;

void main() {
    vec4 tex = texture2D(gtexture, texcoord);
    if (tex.a < 0.1) discard;

    vec3 albedo = tex.rgb * glcolor.rgb;

    // los bloques "emisivos" brillan mas en las partes claras de su textura
    float em = emis * smoothstep(0.35, 0.8, max(albedo.r, max(albedo.g, albedo.b)));

    vec3 n = safeNormal(normalV);
    float dist = length(viewPos);

    vec3 col = shade(albedo, lmcoord, n, shadowPos, glcolor.a, em, dist);
    col = applyFog(col, viewPos);

    gl_FragData[0] = vec4(col, 1.0);
}
