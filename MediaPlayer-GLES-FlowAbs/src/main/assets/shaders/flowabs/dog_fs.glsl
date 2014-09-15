// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform float sigma_e;
uniform float sigma_r;
uniform float tau;
uniform float phi;
uniform vec2 img_size;

void main() {
    vec2 uv = gl_FragCoord.xy / img_size;
    float twoSigmaESquared = 2.0 * sigma_e * sigma_e;
    float twoSigmaRSquared = 2.0 * sigma_r * sigma_r;
    int halfWidth = int(ceil( 2.0 * sigma_r ));

    vec2 sum = vec2(0.0);
    vec2 norm = vec2(0.0);

    for ( int i = -halfWidth; i <= halfWidth; ++i ) {
        for ( int j = -halfWidth; j <= halfWidth; ++j ) {
            float d = length(vec2(i,j));
            vec2 kernel = vec2( exp( -d * d / twoSigmaESquared ),
                                exp( -d * d / twoSigmaRSquared ));

            vec2 L = texture2D(img, uv + vec2(i,j) / img_size).xx;

            norm += 2.0 * kernel;
            sum += kernel * L;
        }
    }
    sum /= norm;

    float H = 100.0 * (sum.x - tau * sum.y);
    float edge = ( H > 0.0 )? 1.0 : 2.0 * smoothstep(-2.0, 2.0, phi * H );
    gl_FragColor = vec4(vec3(edge), 1.0);
}
