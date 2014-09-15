// by Jan Eric Kyprianidis <www.kyprianidis.com>
uniform sampler2D img;
uniform vec2 img_size;

void main (void) {
    vec2 uv = gl_FragCoord.xy / img_size;
    vec3 g = texture2D(img, uv).xyz;

    float lambda1 = 0.5 * (g.y + g.x +
              sqrt(g.y*g.y - 2.0*g.x*g.y + g.x*g.x + 4.0*g.z*g.z));
    vec2 v = vec2(g.x - lambda1, g.z);

    gl_FragColor = (length(v) > 0.0)?
        vec4(normalize(v), sqrt(lambda1), 1.0) :
        vec4(0.0, 1.0, 0.0, 1.0);

    //v = normalize(v);
    //gl_FragColor = vec4(v.x, v.y, 0.0, 1.0);
    //gl_FragColor = vec4(100.0*vec3(length(v)), 1.0);
}

