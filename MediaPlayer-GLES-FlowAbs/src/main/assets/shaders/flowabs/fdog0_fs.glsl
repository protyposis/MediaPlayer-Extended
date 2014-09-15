// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform sampler2D tfm;
uniform float sigma_e;
uniform float sigma_r;
uniform float tau;
uniform vec2 img_size;

void main() {
    float twoSigmaESquared = 2.0 * sigma_e * sigma_e;
    float twoSigmaRSquared = 2.0 * sigma_r * sigma_r;
    vec2 uv = gl_FragCoord.xy / img_size;

    vec2 t = texture2D(tfm, uv).xy;
    vec2 n = vec2(t.y, -t.x);
    vec2 nabs = abs(n);
    float ds = 1.0 / ((nabs.x > nabs.y)? nabs.x : nabs.y);
    n /= img_size;

    vec2 sum = texture2D( img, uv ).xx;
    vec2 norm = vec2(1.0, 1.0);

    float halfWidth = 2.0 * sigma_r;
    for( float d = ds; d <= halfWidth; d += ds ) {
        vec2 kernel = vec2( exp( -d * d / twoSigmaESquared ),
                            exp( -d * d / twoSigmaRSquared ));
        norm += 2.0 * kernel;

        vec2 L0 = texture2D( img, uv - d*n ).xx;
        vec2 L1 = texture2D( img, uv + d*n ).xx;

        sum += kernel * ( L0 + L1 );
    }
    sum /= norm;

    float diff = 100.0 * (sum.x - tau * sum.y);
    gl_FragColor = vec4(vec3(diff), 1.0);
}
