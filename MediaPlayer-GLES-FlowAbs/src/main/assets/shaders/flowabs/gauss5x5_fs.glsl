// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform vec2 img_size;

void main (void) {
    float F[25];
    F[ 0] = 1.0; F[ 1] = 1.0; F[ 2] = 2.0; F[ 3] = 1.0; F[ 4] = 1.0;
    F[ 5] = 1.0; F[ 6] = 2.0; F[ 7] = 4.0; F[ 8] = 2.0; F[ 9] = 1.0;
    F[11] = 2.0; F[12] = 4.0; F[13] = 8.0; F[14] = 4.0; F[15] = 2.0;
    F[16] = 1.0; F[17] = 2.0; F[18] = 4.0; F[19] = 2.0; F[20] = 1.0;
    F[21] = 1.0; F[22] = 1.0; F[23] = 2.0; F[24] = 1.0; F[24] = 1.0;

    vec2 uv = gl_FragCoord.xy / img_size;

    vec3 c  = vec3(0.0);
    for (int j = 0; j < 5; ++j) {
        for (int i = 0; i < 5; ++i) {
            c += F[j * 5 + i] * texture2D(img, uv + vec2(i - 2, j - 2) / img_size).rgb;
        }
    }
    c /= 52.0;
    gl_FragColor = vec4(c, 1.0);
}
