#ifndef COMMON_GLSL
#define COMMON_GLSL

#include "/lib/settings.glsl"

// Gamma 2.0 (mucho mas barato que pow(x, 2.2) y casi identico a la vista)
vec3 toLinear(vec3 c) { return c * c; }
vec3 toGamma(vec3 c) { return sqrt(c); }

// Ruido de gradiente intercalado (barato, bueno para suavizar sombras)
float ign(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

// ACES (aproximacion de Narkowicz)
vec3 tonemap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

// Distorsion del shadow map: mas resolucion cerca del jugador.
// IMPORTANTE: se usa igual en shadow.vsh y al muestrear.
vec3 distortShadow(vec3 p) {
    float f = length(p.xy) * 0.85 + 0.15;
    return vec3(p.xy / f, p.z * 0.35);
}

// normalize() que nunca produce NaN (un NaN deja la pantalla negra)
vec3 nrm(vec3 v) {
    float l = dot(v, v);
    return (l > 1e-8) ? v * inversesqrt(l) : vec3(0.0, 1.0, 0.0);
}

#endif
