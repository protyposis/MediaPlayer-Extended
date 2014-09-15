precision mediump float;

uniform sampler2D s_Texture;
uniform vec2 u_TextureSize;
uniform vec4 color;

void main() {
  gl_FragColor = texture2D(s_Texture, gl_FragCoord.xy / u_TextureSize) * color;
}