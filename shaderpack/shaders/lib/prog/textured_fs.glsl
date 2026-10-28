// Entidades, mano, particulas y todo lo que no tenga programa propio.

#define FRAME_VARYINGS
#include "/lib/lighting.glsl"

uniform sampler2D gtexture;
uniform vec4 entityColor;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normalV;
varying vec3 viewPos;
varying vec3 shadowPos;

void main() {
    vec4 tex = texture2D(gtexture, texcoord) * glcolor;
    if (tex.a < 0.1) discard;

    // destello rojo al recibir dano, etc.
    tex.rgb = mix(tex.rgb, entityColor.rgb, entityColor.a);

    vec3 n = safeNormal(normalV);
    float dist = length(viewPos);

    vec3 col = shade(tex.rgb, lmcoord, n, shadowPos, 1.0, 0.0, dist);
    col = applyFog(col, viewPos);

    gl_FragData[0] = vec4(col, tex.a);
}
