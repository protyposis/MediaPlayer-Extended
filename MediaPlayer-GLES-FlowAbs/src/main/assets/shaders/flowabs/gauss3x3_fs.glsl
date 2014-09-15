// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform vec2 img_size;

#define SRC(__x, __y) \
    texture2D(img, uv + vec2(__x, __y) / img_size).rgb

void main (void) {
    vec2 uv = gl_FragCoord.xy / img_size;

    vec3 c = ( 1.0 * SRC(-1.0, -1.0) +
               4.0 * SRC( 0.0, -1.0) +
               1.0 * SRC( 1.0, -1.0) +
               4.0 * SRC(-1.0,  0.0) +
              16.0 * SRC( 0.0,  0.0) +
               4.0 * SRC( 1.0,  0.0) +
               1.0 * SRC(-1.0,  1.0) +
               4.0 * SRC( 0.0,  1.0) +
               1.0 * SRC( 1.0,  1.0)
             ) / 36.0;

    gl_FragColor = vec4(c, 1.0);
}

