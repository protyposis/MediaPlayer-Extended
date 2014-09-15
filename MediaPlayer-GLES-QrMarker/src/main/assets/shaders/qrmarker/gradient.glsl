/**
 * Copyright (c) 2011, Alberto Franco
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of Alberto Franco nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ALBERTO FRANCO BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * --------------------------------------------------------------------------------
 * Calculate the gradient of the given texture with sobel filter.
 *
 * @author Alberto Franco
 * @file   gradient.glsl
 */
#version 120
uniform sampler2D   texture;    ///< The texture to process

const float texWidth  = 1.0 / 640.0;  ///< Uniform for texture width
const float texHeight = 1.0 / 480.0;  ///< Uniform for texture height

/**
 * Calculate the gradient at the given coords.
 * @param coords Texture coordinates to calculate the gradient at.
 */
vec4 gradient(vec2 coords) {
    // X gradient with sobel filtering
    float xGradient =
            texture2D(texture, vec2(coords.x + texWidth, coords.y + texHeight)).x +
        2 * texture2D(texture, vec2(coords.x + texWidth, coords.y            )).x +
            texture2D(texture, vec2(coords.x + texWidth, coords.y - texHeight)).x -
        (   texture2D(texture, vec2(coords.x - texWidth, coords.y - texHeight)).x +
        2 * texture2D(texture, vec2(coords.x - texWidth, coords.y            )).x +
            texture2D(texture, vec2(coords.x - texWidth, coords.y + texHeight)).x );

    // Y gradient with sobel filtering
    float yGradient =
            texture2D(texture, vec2(coords.x + texWidth, coords.y + texHeight)).x +
        2 * texture2D(texture, vec2(coords.x           , coords.y + texHeight)).x +
            texture2D(texture, vec2(coords.x - texWidth, coords.y + texHeight)).x -
        (   texture2D(texture, vec2(coords.x - texWidth, coords.y - texHeight)).x +
        2 * texture2D(texture, vec2(coords.x           , coords.y - texHeight)).x +
            texture2D(texture, vec2(coords.x + texWidth, coords.y - texHeight)).x );

	float amplitude = sqrt(xGradient * xGradient + yGradient * yGradient);

	// Pack amplitude
	const vec2 bitSh = vec2(256.0, 1.0);
	const vec2 bitMk = vec2(0.0, 1 / 256.0);
	vec2 pack = fract(amplitude * bitSh);
	pack -= pack.xx * bitMk;

	// Pack gradient direction
    xGradient = (xGradient + 4.0) / 8.0;
	yGradient = (yGradient + 4.0) / 8.0;

	return vec4(xGradient, yGradient, pack.x, pack.y);
}

/// Main function for shader
void main() {
    gl_FragColor = gradient(gl_TexCoord[0].st);
}