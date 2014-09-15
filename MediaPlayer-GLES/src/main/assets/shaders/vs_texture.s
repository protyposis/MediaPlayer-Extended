precision highp float;

uniform mat4 u_MVPMatrix;
uniform mat4 u_STMatrix;
attribute vec4 a_Position;
attribute vec4 a_TextureCoord;
varying vec2 v_TextureCoord;

void main() {
    gl_Position = u_MVPMatrix * a_Position;
    v_TextureCoord = (u_STMatrix * a_TextureCoord).xy;
}