precision mediump float;

uniform sampler2D s_Texture;
uniform vec2 u_TextureSize;
uniform vec4 color;
varying vec2 v_TextureCoord;

void main() {
  gl_FragColor = texture2D(s_Texture, v_TextureCoord) * color;
}