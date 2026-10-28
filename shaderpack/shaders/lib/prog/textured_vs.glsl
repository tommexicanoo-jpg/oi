#include "/lib/common.glsl"
#include "/lib/frame_vs.glsl"

uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normalV;
varying vec3 viewPos;
varying vec3 shadowPos;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;

    normalV = gl_NormalMatrix * gl_Normal;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    viewPos = vp.xyz;
    gl_Position = gl_ProjectionMatrix * vp;

    computeFrame(vp.xyz);

    vec3 feet = (gbufferModelViewInverse * vp).xyz;
    vec3 worldN = mat3(gbufferModelViewInverse) * normalV;
    vec4 sp = shadowProjection * (shadowModelView * vec4(feet + worldN * 0.12, 1.0));
    shadowPos = sp.xyz;
}
