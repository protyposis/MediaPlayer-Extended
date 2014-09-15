// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform vec2 img_size;
uniform float sigma;

void main (void) {
    vec2 uv = gl_FragCoord.xy / img_size;
    float twoSigma2 = 2.0 * sigma * sigma;
    int halfWidth = int(ceil( 2.0 * sigma ));

    vec3 sum = vec3(0.0);
    if (halfWidth > 0) {
        float norm = 0.0;

        for ( int i = -halfWidth; i <= halfWidth; ++i ) {
            for ( int j = -halfWidth; j <= halfWidth; ++j ) {
                float d = length(vec2(i,j));
                float kernel = exp( -d *d / twoSigma2 );

                vec3 c = texture2D(img, uv + vec2(i,j) / img_size).rgb;

                norm += kernel;
                sum += kernel * c;
            }
        }
        sum /= norm;
    } else {
        sum =  texture2D(img, uv).rgb;
    }
    gl_FragColor = vec4(sum, 1.0);
}