// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform sampler2D tfm;
uniform float sigma_m;
uniform float phi;
uniform vec2 img_size;

struct lic_t {
    vec2 p;
    vec2 t;
    float w;
    float dw;
};

void step(inout lic_t s) {
    vec2 t = texture2D(tfm, s.p).xy;
    if (dot(t, s.t) < 0.0) t = -t;
    s.t = t;

    s.dw = (abs(t.x) > abs(t.y))?
        abs((fract(s.p.x) - 0.5 - sign(t.x)) / t.x) :
        abs((fract(s.p.y) - 0.5 - sign(t.y)) / t.y);

    s.p += t * s.dw / img_size;
    s.w += s.dw;
}

void main (void) {
    float twoSigmaMSquared = 2.0 * sigma_m * sigma_m;
    float halfWidth = 2.0 * sigma_m;
    vec2 uv = gl_FragCoord.xy / img_size;

    float H = texture2D( img, uv ).x;
    float w = 1.0;

    lic_t a, b;
    a.p = b.p = uv;
    a.t = texture2D( tfm, uv ).xy / img_size;
    b.t = -a.t;
    a.w = b.w = 0.0;

    while (a.w < halfWidth) {
        step(a);
        float k = a.dw * exp(-a.w * a.w / twoSigmaMSquared);
        H += k * texture2D(img, a.p).x;
        w += k;
    }
    while (b.w < halfWidth) {
        step(b);
        float k = b.dw * exp(-b.w * b.w / twoSigmaMSquared);
        H += k * texture2D(img, b.p).x;
        w += k;
    }
    H /= w;

    float edge = ( H > 0.0 )? 1.0 : 2.0 * smoothstep(-2.0, 2.0, phi * H );
    gl_FragColor = vec4(vec3(edge), 1.0);
}
