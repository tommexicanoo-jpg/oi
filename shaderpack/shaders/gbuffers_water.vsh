#version 120

#include "/lib/common.glsl"
#include "/lib/frame_vs.glsl"

attribute vec4 mc_Entity;

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
varying vec3 worldPos;
varying float isWater;

void main() {
    texcoord = gl_MultiTexCoord0.xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;

    vec3 nGeom = normalize(gl_NormalMatrix * gl_Normal);
    normalV = nGeom;

    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    vec3 feet = (gbufferModelViewInverse * vp).xyz;
    vec3 world = feet + cameraPosition;
    worldPos = world;

    isWater = (abs(mc_Entity.x - 10003.0) < 0.5) ? 1.0 : 0.0;

#ifdef WATER_WAVES
    if (isWater > 0.5) {
        float t = frameTimeCounter;
        float hgt = sin(world.x * 1.3 + t * 1.1) * 0.5
                  + sin(world.z * 1.7 - t * 0.9 + world.x * 0.4) * 0.35
                  + sin((world.x - world.z) * 3.2 + t * 1.9) * 0.15;
        feet.y += hgt * 0.03;
    }
#endif

    vec3 vpos = (gbufferModelView * vec4(feet, 1.0)).xyz;
    viewPos = vpos;
    gl_Position = gl_ProjectionMatrix * vec4(vpos, 1.0);

    computeFrame(vpos);

    vec3 worldN = mat3(gbufferModelViewInverse) * nGeom;
    vec4 sp = shadowProjection * (shadowModelView * vec4(feet + worldN * 0.12, 1.0));
    shadowPos = sp.xyz;
}
