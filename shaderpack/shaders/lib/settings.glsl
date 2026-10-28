#ifndef SETTINGS_GLSL
#define SETTINGS_GLSL

// ---------------------------------------------------------------
//  Luma Lite - opciones (se editan desde el menu de shaders)
// ---------------------------------------------------------------

// Sombras
#define SHADOWS
const int shadowMapResolution = 1024; //[512 768 1024 1536 2048]
const float shadowDistance = 64.0; //[32.0 48.0 64.0 80.0 96.0 128.0]
const float sunPathRotation = -25.0;
#define SHADOW_SOFTNESS 1.0 //[0.5 1.0 1.5 2.0]

// Mundo
#define WAVING
#define WAVE_STRENGTH 1.0 //[0.5 0.75 1.0 1.5 2.0]
#define WATER_WAVES
#define DYNAMIC_LIGHT

// Imagen
#define SHARPEN
#define SATURATION 0.80 //[0.5 0.65 0.8 0.9 1.0 1.12]
#define CONTRAST 1.10 //[0.9 1.0 1.06 1.1 1.2]
#define VIGNETTE
#define FOG_DENSITY 1.0 //[0.0 0.5 1.0 1.5 2.0]

// ----- El Eco -----
#define ECO_AUTO_DAY                // lee el dia del mod (si falla usa ECO_DAY)
#define ECO_DAY 1 //[1 2 3 4 5]
#define ECO_FOG 1.0 //[0.5 0.75 1.0 1.5 2.0]
#define ECO_GRAIN 0.035 //[0.0 0.02 0.035 0.06 0.1]
#define ECO_TORCH_FLICKER

#endif
