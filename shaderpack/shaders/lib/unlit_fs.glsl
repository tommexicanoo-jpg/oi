// Fragment shader sin iluminacion
#ifndef UNLIT_BOOST
#define UNLIT_BOOST 1.0
#endif

uniform sampler2D gtexture;

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
    vec4 c = texture2D(gtexture, texcoord) * glcolor;
    if (c.a < 0.01) discard;
    c.rgb *= UNLIT_BOOST;
    gl_FragData[0] = c;
}
