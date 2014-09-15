#extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 v_TextureCoord;
uniform samplerExternalOES s_Texture;

void main() {
  gl_FragColor = texture2D(s_Texture, v_TextureCoord);
}
