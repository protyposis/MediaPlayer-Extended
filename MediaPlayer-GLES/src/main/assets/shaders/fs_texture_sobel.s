precision highp float;

varying vec2 v_TextureCoord;
uniform sampler2D s_Texture;
uniform vec2 u_TextureSize;
uniform float thresholdL;
uniform float thresholdH;
uniform vec3 color;

#define AVG(__rgb) \
    (__rgb.r + __rgb.g + __rgb.b)/3.0

void main() {
    //vec2 uv = gl_FragCoord.xy / u_TextureSize;
    vec2 uv = v_TextureCoord;
    vec2 d = 1.0 / u_TextureSize;

    vec4 h = (
         -1.0 * texture2D(s_Texture, uv + vec2(-d.x, -d.y)) +
         -2.0 * texture2D(s_Texture, uv + vec2(-d.x,  0.0)) +
         -1.0 * texture2D(s_Texture, uv + vec2(-d.x,  d.y)) +
         +1.0 * texture2D(s_Texture, uv + vec2( d.x, -d.y)) +
         +2.0 * texture2D(s_Texture, uv + vec2( d.x,  0.0)) +
         +1.0 * texture2D(s_Texture, uv + vec2( d.x,  d.y))
    );

    vec4 v = (
         -1.0 * texture2D(s_Texture, uv + vec2(-d.x, -d.y)) +
         -2.0 * texture2D(s_Texture, uv + vec2( 0.0, -d.y)) +
         -1.0 * texture2D(s_Texture, uv + vec2( d.x, -d.y)) +
         +1.0 * texture2D(s_Texture, uv + vec2(-d.x,  d.y)) +
         +2.0 * texture2D(s_Texture, uv + vec2( 0.0,  d.y)) +
         +1.0 * texture2D(s_Texture, uv + vec2( d.x,  d.y))
    );

    float sobel = length(vec2(AVG(h), AVG(v)));
    sobel = smoothstep(thresholdL, thresholdH, sobel);
    vec3 c = vec3(sobel, sobel, sobel) * color;

    gl_FragColor = vec4(c, 1.0);
}