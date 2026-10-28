#version 120

#define FRAME_VARYINGS
#include "/lib/lighting.glsl"

uniform sampler2D gtexture;
uniform mat4 gbufferModelViewInverse;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normalV;
varying vec3 viewPos;
varying vec3 shadowPos;
varying vec3 worldPos;
varying float isWater;

void main() {
    vec4 tex = texture2D(gtexture, texcoord);
    vec3 n = safeNormal(normalV);
    float dist = length(viewPos);
    vec3 vdir = viewPos / max(dist, 0.0001);

    if (isWater > 0.5) {
        // --- normales de olas (derivadas analiticas, muy baratas) ---
#ifdef WATER_WAVES
        vec3 wn = mat3(gbufferModelViewInverse) * n;
        float t = frameTimeCounter;
        vec2 p = worldPos.xz;
        float a1 = p.x * 1.3 + t * 1.1;
        float a2 = p.y * 1.7 - t * 0.9 + p.x * 0.4;
        float a3 = (p.x - p.y) * 3.2 + t * 1.9;
        vec2 g;
        g.x = 0.65 * cos(a1) + 0.14 * cos(a2) + 0.48 * cos(a3);
        g.y = 0.60 * cos(a2) - 0.48 * cos(a3);
        wn.xz -= g * 0.07 * max(wn.y, 0.0);
        n = normalize(mat3(gbufferModelView) * normalize(wn));
#endif

        float lum = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
        vec3 base = glcolor.rgb * (0.55 + lum * 0.9);
        vec3 lit = shade(base, lmcoord, n, shadowPos, 1.0, 0.0, dist);

        // --- reflejo del cielo (fresnel) ---
        vec3 R = reflect(vdir, n);
        float f1 = 1.0 - clamp(dot(-vdir, n), 0.0, 1.0);
        float f2 = f1 * f1;
        float fres = f2 * f2;
        float skyVis = smoothstep(0.4, 0.9, (lmcoord.y - 0.03125) / 0.9375);
        float rk = (0.06 + 0.8 * fres) * skyVis;
        if (isEyeInWater == 1) rk *= 0.3;
        if (rk > 0.01 && fInfo.w < 0.5) {
            lit = mix(lit, toGamma(skyGradient(R)), clamp(rk, 0.0, 1.0));
        }

        // --- brillo del sol / luna ---
        float sp = max(dot(R, fLight), 0.0);
        float sp2 = sp * sp;
        float sp4 = sp2 * sp2;
        float sp8 = sp4 * sp4;
        float sp16 = sp8 * sp8;
        float sp32 = sp16 * sp16;
        float sp64 = sp32 * sp32;
        float sp128 = sp64 * sp64;
        float spec = sp128 * sp32 * sp16 * sp4;   // sp^180
        lit += fDirCol * (spec * skyVis * (1.0 - rainStrength) * 0.7);

        float alpha = clamp(0.60 + fres * 0.30, 0.0, 0.95);
        gl_FragData[0] = vec4(applyFog(lit, viewPos), alpha);
    } else {
        // vidrio teñido, hielo, etc.
        vec4 c = tex * glcolor;
        vec3 lit = shade(c.rgb, lmcoord, n, shadowPos, 1.0, 0.0, dist);
        gl_FragData[0] = vec4(applyFog(lit, viewPos), c.a);
    }
}
