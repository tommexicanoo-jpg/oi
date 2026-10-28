#ifndef FRAME_VS_GLSL
#define FRAME_VS_GLSL

// Calcula UNA vez por vertice todo lo que no depende del pixel (hora del dia,
// color del sol, ambiente, color de niebla...) y lo manda como varying.
// Asi el fragment shader hace mucho menos trabajo.
//
// Dimension: el vertex shader de la carpeta world-1 define NETHER y el de
// world1 define END.

#include "/lib/sky.glsl"

uniform mat4 gbufferModelView;
uniform vec3 shadowLightPosition;
uniform ivec2 eyeBrightnessSmooth;

varying vec4 fInfo;    // dia, atardecer, altura del sol, sin-cielo (Nether/End)
varying vec3 fLight;   // direccion de la luz directa (vista)
varying vec3 fDirCol;  // color de la luz directa (lineal)
varying vec3 fAmb;     // color del ambiente (lineal)
varying vec4 fFogT;    // color de niebla (gamma) + luz de cielo del jugador

void computeFrame(vec3 vpos) {
#if defined(NETHER)
    // Nether: ambiente naranja/rojo, sin sol ni sombras
    fInfo = vec4(0.0, 0.0, 0.0, 1.0);
    fLight = nrm(gbufferModelView[1].xyz);
    fDirCol = vec3(0.0);
    fAmb = vec3(1.45, 0.58, 0.22);
    fFogT = vec4(min(fogColor * 1.5 + vec3(0.04, 0.012, 0.0), vec3(1.0)), 0.0);
#elif defined(END)
    // End: ambiente morado, un poco mas de luz desde arriba
    fInfo = vec4(0.0, 0.0, 0.0, 1.0);
    fLight = nrm(gbufferModelView[1].xyz);
    fDirCol = vec3(0.30, 0.17, 0.55);
    fAmb = vec3(0.50, 0.27, 0.95);
    fFogT = vec4(min(fogColor * 3.0 + vec3(0.05, 0.02, 0.09), vec3(1.0)), 0.0);
#else
    vec3 info = sunInfo();
    float day = info.x;
    float sunset = info.y;
    float h = info.z;

    vec3 L = nrm(shadowLightPosition);
    float isSun = step(0.0, dot(L, nrm(sunPosition)));

    vec3 sunCol = mix(vec3(1.00, 0.90, 0.74) * 2.5, vec3(1.00, 0.48, 0.20) * 2.1, sunset);
    vec3 moonCol = mix(vec3(0.42, 0.55, 0.85) * 0.11, vec3(0.55, 0.20, 0.20) * 0.07, ecoT());
    float sunPow = smoothstep(-0.02, 0.14, h);
    float moonPow = smoothstep(-0.02, 0.14, -h);

    vec3 skyAmb = mix(toLinear(skyColor), vec3(0.45), 0.4) * 0.95;
    skyAmb = mix(vec3(0.020, 0.026, 0.040), vec3(0.030, 0.012, 0.014), ecoT());
    skyAmb += vec3(0.10, 0.055, 0.04) * sunset;

    float eyeSky = float(eyeBrightnessSmooth.y) / 240.0;
    vec3 target = ecoFogGamma();

    fInfo = vec4(info, 0.0);
    fLight = L;
    fDirCol = (isSun > 0.5) ? sunCol * sunPow : moonCol * moonPow;
    fAmb = skyAmb;
    fFogT = vec4(target, eyeSky);
#endif
}

#endif
