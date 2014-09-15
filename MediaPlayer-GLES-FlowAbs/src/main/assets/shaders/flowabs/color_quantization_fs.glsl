// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform vec2 img_size;
uniform int nbins;
uniform float phi_q;

void main (void) {
    vec2 uv = gl_FragCoord.xy / img_size;
    vec2 d = 1.0 / img_size;
    vec3 c = texture2D(img, uv).xyz;

    float qn = floor(c.x * float(nbins) + 0.5) / float(nbins);
    float qs = smoothstep(-2.0, 2.0, phi_q * (c.x - qn) * 100.0) - 0.5;
    float qc = qn + qs / float(nbins);

    gl_FragColor = vec4( vec3(qc, c.yz), 1.0 );
}
