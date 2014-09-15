// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform sampler2D tfm;
uniform int pass;
uniform float sigma_d;
uniform float sigma_r;
uniform vec2 img_size;

void main (void) {
    float twoSigmaD2 = 2.0 * sigma_d * sigma_d;
    float twoSigmaR2 = 2.0 * sigma_r * sigma_r;
    vec2 uv = gl_FragCoord.xy / img_size;

    vec2 t = texture2D(tfm, uv).xy;
    vec2 dir = (pass == 0)? vec2(t.y, -t.x) : t;
    vec2 dabs = abs(dir);
    float ds = 1.0 / ((dabs.x > dabs.y)? dabs.x : dabs.y);
    dir /= img_size;

    vec3 center = texture2D(img, uv).rgb;
    vec3 sum = center;
    float norm = 1.0;
    float halfWidth = 2.0 * sigma_d;
    for (float d = ds; d <= halfWidth; d += ds) {
        vec3 c0 = texture2D(img, uv + d * dir).rgb;
        vec3 c1 = texture2D(img, uv - d * dir).rgb;
        float e0 = length(c0 - center);
        float e1 = length(c1 - center);

        float kerneld = exp( - d *d / twoSigmaD2 );
        float kernele0 = exp( - e0 *e0 / twoSigmaR2 );
        float kernele1 = exp( - e1 *e1 / twoSigmaR2 );
        norm += kerneld * kernele0;
        norm += kerneld * kernele1;

        sum += kerneld * kernele0 * c0;
        sum += kerneld * kernele1 * c1;
    }
    sum /= norm;
    //if (norm > 1.2)
    //  sum /= norm;
    //else {
    //  sum = (texture2D(img, uv + ds * dir).rgb +
    //         texture2D(img, uv - ds * dir).rgb) / 2.0;
    //}
    gl_FragColor = vec4(sum, 1.0);
}