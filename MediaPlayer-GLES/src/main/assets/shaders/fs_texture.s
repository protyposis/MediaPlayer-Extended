precision highp float;

varying vec2 v_TextureCoord;
uniform sampler2D s_Texture;

void main() {
  gl_FragColor = texture2D(s_Texture, v_TextureCoord);
}
