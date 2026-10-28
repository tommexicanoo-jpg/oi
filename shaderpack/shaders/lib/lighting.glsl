#ifndef LIGHTING_GLSL
#define LIGHTING_GLSL

// Solo para shaders de fragmentos.
//
// Los programas que iluminan geometria (terreno, agua, entidades) definen
// FRAME_VARYINGS antes de incluir este archivo y reciben los datos del dia ya
// calculados por el vertex shader (lib/frame_vs.glsl). Los demas (cielo,
// nubes, clima) calculan lo que necesitan por pixel.

#include "/lib/sky.glsl"

uniform sampler2D shadowtex1;
uniform float far;
uniform float frameTimeCounter;
uniform int isEyeInWater;
uniform int heldBlockLightValue;

#ifdef FRAME_VARYINGS
uniform mat4 gbufferModelView;

varying vec4 fInfo;
varying vec3 fLight;
varying vec3 fDirCol;
varying vec3 fAmb;
varying vec4 fFogT;
#else
uniform ivec2 eyeBrightnessSmooth;
#endif

vec3 safeNormal(vec3 n) {
    float l = dot(n, n);
    return (l > 0.05) ? n * inversesqrt(l) : nrm(upPosition);
}

// ---------------------------------------------------------------
//  Sombras (4 muestras con ruido intercalado)
// ---------------------------------------------------------------
float getShadow(vec3 sp, float ndl) {
#ifdef SHADOWS
    float fade = 1.0 - smoothstep(0.82, 0.98, length(sp.xy));
    if (fade <= 0.001) return 1.0;

    vec3 p = distortShadow(sp) * 0.5 + 0.5;

    float texel = 1.0 / float(shadowMapResolution);
    float radius = texel * 1.5 * SHADOW_SOFTNESS;
    float bias = 0.00035 + 0.0006 * (1.0 - clamp(ndl, 0.0, 1.0));

    float a = ign(gl_FragCoord.xy) * 6.2831853;
    vec2 r = vec2(cos(a), sin(a));
    mat2 rot = mat2(r.x, r.y, -r.y, r.x);

    float s = 0.0;
    s += step(p.z - bias, texture2D(shadowtex1, p.xy + rot * vec2( 0.354,  0.000) * radius).r);
    s += step(p.z - bias, texture2D(shadowtex1, p.xy + rot * vec2(-0.451,  0.413) * radius).r);
    s += step(p.z - bias, texture2D(shadowtex1, p.xy + rot * vec2( 0.069, -0.787) * radius).r);
    s += step(p.z - bias, texture2D(shadowtex1, p.xy + rot * vec2( 0.569,  0.742) * radius).r);
    s *= 0.25;

    return mix(1.0, s, fade);
#else
    return 1.0;
#endif
}

#ifdef FRAME_VARYINGS
// ---------------------------------------------------------------
//  Iluminacion principal. Devuelve color en espacio gamma (0..1)
// ---------------------------------------------------------------
vec3 shade(vec3 albedoG, vec2 lmRaw, vec3 nV, vec3 sp, float ao, float emis, float dist) {
    vec3 albedo = toLinear(albedoG);
    vec2 lm = clamp((lmRaw - 0.03125) / 0.9375, 0.0, 1.0);

    float nosky = fInfo.w;     // 1 en Nether / End
    float day = fInfo.x;
    vec3 up = nrm(gbufferModelView[1].xyz);

    // --- luz directa (sol, luna, o luz cenital en el End) ---
    float ndl = dot(nV, fLight);
    float dirAmount = max(ndl, 0.0);
    float skyMask = mix(smoothstep(0.55, 0.92, lm.y), 1.0, nosky);
    vec3 direct = vec3(0.0);
    if (dirAmount > 0.0 && skyMask > 0.0 && (fDirCol.r + fDirCol.g + fDirCol.b) > 0.001) {
        float shadow = 1.0;
        if (nosky < 0.5) shadow = getShadow(sp, ndl);
        direct = fDirCol * (dirAmount * shadow * skyMask * (1.0 - rainStrength * 0.7));
    }

    // --- ambiente ---
    float hemi = 0.65 + 0.35 * dot(nV, up);
    float skyAccess = mix(lm.y * lm.y, 1.0, nosky);
    vec3 ambient = fAmb * (hemi * skyAccess);

    // --- luz de antorchas (y luz en la mano) ---
    float torchLevel = lm.x;
#ifdef DYNAMIC_LIGHT
    if (heldBlockLightValue > 0) {
        float hv = float(heldBlockLightValue);
        float hl = clamp(1.0 - dist / (hv * 0.9), 0.0, 1.0);
        torchLevel = max(torchLevel, (hv / 15.0) * hl);
    }
#endif
    float torchFade = 1.0 - 0.75 * day * lm.y;
#ifdef ECO_TORCH_FLICKER
    float flick = 0.86 + 0.09 * sin(frameTimeCounter * 11.0 + dist * 0.3) + 0.05 * sin(frameTimeCounter * 27.0);
#else
    float flick = 1.0;
#endif
    vec3 torch = vec3(1.00, 0.46, 0.15) * (2.3 * flick * torchLevel * torchLevel * torchLevel * torchFade);

    vec3 light = direct + (ambient + torch) * ao;
    light = max(light, vec3(0.010, 0.012, 0.018));

    vec3 col = albedo * (light + emis * 2.2);
    return toGamma(tonemap(col * 0.9));
}
#endif

// ---------------------------------------------------------------
//  Niebla (se funde con el color del cielo)
// ---------------------------------------------------------------
vec3 applyFog(vec3 colG, vec3 vpos) {
    float d = length(vpos);
    float f;
    vec3 target;

    if (isEyeInWater == 1) {
        f = 1.0 - exp(-d * 0.085);
        target = fogColor;
    } else if (isEyeInWater == 2) {
        f = 1.0 - exp(-d * 0.7);
        target = fogColor;
    } else {
#ifdef FRAME_VARYINGS
        float nosky = fInfo.w;
        float eyeSky = fFogT.a;
        target = fFogT.rgb;
#else
        float nosky = 0.0;
        float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
        target = ecoFogGamma();
#endif
        float edge = smoothstep(far * mix(0.55, 0.30, nosky), far * 0.98, d);
        // El Eco: niebla densa que se espesa cada dia (1 - e^(-d*k))
        float dens = mix(0.040, 0.150, ecoT()) * ECO_FOG * FOG_DENSITY;
        float haze = 1.0 - exp(-d * dens);
        haze = mix(haze * 0.75, haze, max(eyeSky, 0.5));
        haze = max(haze, nosky * 0.6);
        f = 1.0 - (1.0 - edge) * (1.0 - clamp(haze, 0.0, 1.0));
    }
    return mix(colG, target, clamp(f, 0.0, 1.0));
}

#endif
