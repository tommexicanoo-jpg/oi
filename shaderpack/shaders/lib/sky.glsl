#ifndef SKY_GLSL
#define SKY_GLSL

// Cielo del Overworld. Se usa tanto en vertex como en fragment shaders.

#include "/lib/common.glsl"

uniform vec3 sunPosition;
uniform vec3 upPosition;
uniform vec3 fogColor;
uniform vec3 skyColor;
uniform float rainStrength;

// ---- EL ECO: dia eterno-noche. Sin sol, sin atardecer. ----
// x = dia (siempre 0), y = atardecer (0), z = altura (fija, bajo el horizonte)
vec3 sunInfo() {
    return vec3(0.0, 0.0, -0.6);
}

// Dia del mod (1..5). El mod codifica el dia en el canal azul del color de
// niebla: b = 0.03 + 0.004 * dia. Si no se detecta, se usa la opcion ECO_DAY.
float ecoDay() {
#ifdef ECO_AUTO_DAY
    float d = (fogColor.b - 0.03) * 250.0;
    if (d > 0.5 && d < 5.5) return floor(d + 0.5);
#endif
    return float(ECO_DAY);
}

// 0 en el dia 1, 1 en el dia 5
float ecoT() { return (ecoDay() - 1.0) * 0.25; }

// Color de niebla (espacio gamma): gris verdoso -> rojo sangre oscuro
vec3 ecoFogGamma() {
    return mix(vec3(0.050, 0.058, 0.064), vec3(0.120, 0.012, 0.012), ecoT());
}

vec3 skyGradient(vec3 dir) {
    vec3 up = nrm(upPosition);
    float h = dot(dir, up);
    float x = 1.0 - clamp(h, 0.0, 1.0);
    float t = 1.0 - x * x * sqrt(x);
    vec3 horizon = toLinear(ecoFogGamma());
    vec3 col = mix(horizon, horizon * 0.30, t);
    // un tenue resplandor palido de la luna tras la bruma
    float sd = max(dot(dir, nrm(sunPosition)), 0.0);
    float s8 = sd * sd; s8 *= s8; s8 *= s8;
    col += vec3(0.30, 0.34, 0.42) * s8 * s8 * 0.05 * (1.0 - ecoT());
    return col;
}

#endif
