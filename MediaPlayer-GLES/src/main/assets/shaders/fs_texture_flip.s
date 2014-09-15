precision highp float;

varying vec2 v_TextureCoord;
uniform sampler2D s_Texture;
uniform int mode;

void main() {
  vec2 inv = v_TextureCoord;
  if(mode == 1) {
    inv = vec2(1.0 - v_TextureCoord.x, v_TextureCoord.y);
  } else if(mode == 2) {
    inv = vec2(v_TextureCoord.x, 1.0 - v_TextureCoord.y);
  } else if(mode == 3) {
    inv = vec2(1.0 - v_TextureCoord.x, 1.0 - v_TextureCoord.y);
  }
  gl_FragColor = texture2D(s_Texture, inv);
}
