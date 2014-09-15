#define KERNEL_SIZE 9
precision highp float;

varying vec2 v_TextureCoord;
uniform sampler2D s_Texture;
uniform float u_Kernel[KERNEL_SIZE];
uniform vec2 u_TexOffset[KERNEL_SIZE];

void main() {
    int i = 0;
    vec4 sum = vec4(0.0);
    for (i = 0; i < KERNEL_SIZE; i++) {
        vec4 c = texture2D(s_Texture, v_TextureCoord + u_TexOffset[i]);
        sum += c * u_Kernel[i];
    }
    gl_FragColor = sum;
}