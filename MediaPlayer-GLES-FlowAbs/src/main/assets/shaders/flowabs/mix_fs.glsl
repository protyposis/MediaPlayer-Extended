// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform sampler2D edges;
uniform vec2 img_size;
uniform vec3 edge_color;

void main (void) {
    vec2 uv = gl_FragCoord.xy / img_size;
    vec2 d = 1.0 / img_size;
    vec3 c = texture2D(img, uv).xyz;
    float e = texture2D(edges, uv).x;
    gl_FragColor = vec4(mix(edge_color, c, e), 1.0);
}
