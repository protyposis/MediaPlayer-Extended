// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform vec2 img_size;

void main (void) {
    vec2 uv = gl_FragCoord.xy / img_size;
    vec4 c = texture2D(img, uv);

    vec2 d = 1.0 / img_size;

    vec4 u = (
             -1.0 * texture2D(img, uv + vec2(-d.x, -d.y)) +
             -2.0 * texture2D(img, uv + vec2(-d.x,  0.0)) +
             -1.0 * texture2D(img, uv + vec2(-d.x,  d.y)) +
             +1.0 * texture2D(img, uv + vec2( d.x, -d.y)) +
             +2.0 * texture2D(img, uv + vec2( d.x,  0.0)) +
             +1.0 * texture2D(img, uv + vec2( d.x,  d.y))
             ) / 4.0;

    vec4 v = (
             -1.0 * texture2D(img, uv + vec2(-d.x, -d.y)) +
             -2.0 * texture2D(img, uv + vec2( 0.0, -d.y)) +
             -1.0 * texture2D(img, uv + vec2( d.x, -d.y)) +
             +1.0 * texture2D(img, uv + vec2(-d.x,  d.y)) +
             +2.0 * texture2D(img, uv + vec2( 0.0,  d.y)) +
             +1.0 * texture2D(img, uv + vec2( d.x,  d.y))
             ) / 4.0;

    gl_FragColor = vec4(vec3(dot(u.xyz, u.xyz),
                             dot(v.xyz, v.xyz),
                             dot(u.xyz, v.xyz)), 1.0);
}

