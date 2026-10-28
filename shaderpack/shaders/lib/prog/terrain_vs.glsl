#include "/lib/common.glsl"
#include "/lib/frame_vs.glsl"

attribute vec4 mc_Entity;
attribute vec2 mc_midTexCoord;

uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normalV;
varying vec3 viewPos;
varying vec3 shadowPos;
varying float emis;

bool isId(float id, float v) { return abs(id - v) < 0.5; }

// Plantas: se mueven con el viento (peso 0 en la raiz, 1 en la punta)
vec3 plantWind(vec3 wp, float w) {
    float t = frameTimeCounter * 1.6;
    float g = sin(t + wp.x * 0.9 + wp.z * 0.6) * 0.5 + sin(t * 1.7 + wp.z * 1.3 - wp.x * 0.4) * 0.5;
    float gust = 0.6 + 0.4 * sin(t * 0.35 + wp.x * 0.12 + wp.z * 0.09);
    float amp = (0.05 + rainStrength * 0.04) * WAVE_STRENGTH * w;
    float g2 = cos(t * 1.3 + wp.z * 1.1 + wp.x * 0.5);
    return vec3(g * gust * amp, 0.0, g2 * gust * amp * 0.6);
}

// Hojas: balanceo suave
vec3 leafWind(vec3 wp) {
    float t = frameTimeCounter * 1.2;
    float amp = (0.025 + rainStrength * 0.02) * WAVE_STRENGTH;
    return vec3(sin(t + wp.x * 1.3 + wp.y * 0.9),
                sin(t * 0.8 + wp.z * 1.1 + wp.y * 1.1) * 0.5,
                cos(t * 0.9 + wp.z * 1.4 + wp.x * 0.7)) * amp;
}

void main() {
    texcoord = gl_MultiTexCoord0.xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;

    vec3 nGeom = normalize(gl_NormalMatrix * gl_Normal);
    vec3 nShade = nGeom;

    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    vec3 feet = (gbufferModelViewInverse * vp).xyz;

    float id = mc_Entity.x;
    emis = isId(id, 10010.0) ? 1.0 : 0.0;

    float top = (texcoord.t < mc_midTexCoord.t) ? 1.0 : 0.0;
    float w = 0.0;
    bool plant = false;
    bool leaf = false;

    if (isId(id, 10001.0)) { plant = true; w = top; }
    else if (isId(id, 10004.0)) { plant = true; w = top * 0.5; }
    else if (isId(id, 10005.0)) { plant = true; w = 0.5 + 0.5 * top; }
    else if (isId(id, 10002.0)) leaf = true;

    vec3 upV = gbufferModelView[1].xyz;
    if (plant) {
        nShade = upV;                      // las plantas se iluminan por igual
    } else if (leaf) {
        nShade = normalize(mix(nGeom, upV, 0.55));
    }

#ifdef WAVING
    if (plant || leaf) {
        vec3 world = feet + cameraPosition;
        if (plant) { if (w > 0.0) feet += plantWind(world, w); }
        else feet += leafWind(world);
    }
#endif

    vec3 vpos = (gbufferModelView * vec4(feet, 1.0)).xyz;
    viewPos = vpos;
    normalV = nShade;
    gl_Position = gl_ProjectionMatrix * vec4(vpos, 1.0);

    computeFrame(vpos);

    // posicion en el mapa de sombras (con desplazamiento por normal)
    vec3 worldN = mat3(gbufferModelViewInverse) * nGeom;
    vec4 sp = shadowProjection * (shadowModelView * vec4(feet + worldN * 0.12, 1.0));
    shadowPos = sp.xyz;
}
