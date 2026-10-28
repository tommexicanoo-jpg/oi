#version 120

varying vec4 glcolor;
varying float isStar;

void main() {
    glcolor = gl_Color;
    // las estrellas llegan en blanco puro; el domo del cielo no
    isStar = float(gl_Color.r == gl_Color.g && gl_Color.g == gl_Color.b && gl_Color.r > 0.0);
    gl_Position = ftransform();
}
