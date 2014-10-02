precision mediump float;

uniform sampler2D s_Texture;
uniform vec2 u_TextureSize;
uniform float contrast;
uniform float brightness;

vec3 adjustContrast(vec3 rgb, float factor) {
    vec3 gray = vec3(0.5, 0.5, 0.5);
    return mix(gray, rgb, factor);
}

vec3 adjustBrightness(vec3 rgb, float factor) {
    vec3 black = vec3(0.0, 0.0, 0.0);
    return mix(black, rgb, factor);
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_TextureSize;
    vec3 rgb = texture2D(s_Texture, uv).rgb;

    rgb = adjustContrast(rgb, contrast);
    rgb = adjustBrightness(rgb, brightness);

    gl_FragColor = vec4(rgb, 1.0);
}