#version 120

#include "/lib/settings.glsl"

uniform vec3 fogColor;
uniform float frameTimeCounter;

uniform sampler2D colortex0;
uniform float viewWidth;
uniform float viewHeight;

varying vec2 texcoord;

void main() {
    vec3 c = texture2D(colortex0, texcoord).rgb;

#ifdef SHARPEN
    // enfoque barato (4 muestras extra)
    vec2 px = 1.0 / vec2(viewWidth, viewHeight);
    vec3 blur = texture2D(colortex0, texcoord + vec2(px.x, 0.0)).rgb
              + texture2D(colortex0, texcoord - vec2(px.x, 0.0)).rgb
              + texture2D(colortex0, texcoord + vec2(0.0, px.y)).rgb
              + texture2D(colortex0, texcoord - vec2(0.0, px.y)).rgb;
    c += (c - blur * 0.25) * 0.35;
#endif

    // ---- El Eco ----
    float dd = 0.0;
#ifdef ECO_AUTO_DAY
    float dv = (fogColor.b - 0.03) * 250.0;
    dd = (dv > 0.5 && dv < 5.5) ? floor(dv + 0.5) : float(ECO_DAY);
#else
    dd = float(ECO_DAY);
#endif
    float et = (dd - 1.0) * 0.25;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, SATURATION * (1.0 - 0.35 * et));
    c *= mix(vec3(0.92, 1.00, 1.02), vec3(1.12, 0.90, 0.90), et);   // frio -> rojizo
    float g = fract(sin(dot(texcoord * 913.0 + fract(frameTimeCounter), vec2(12.9898, 78.233))) * 43758.5453);
    c += (g - 0.5) * ECO_GRAIN * (0.6 + et);
    c = (c - 0.5) * CONTRAST + 0.5;

#ifdef VIGNETTE
    vec2 q = texcoord - 0.5;
    c *= 1.0 - dot(q, q) * (0.85 + 0.6 * et);
#endif

    gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
