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
 * Calculate consense scoring over the calculated image. Last pass in algorithm.
 *
 * @author Alberto Franco
 * @file   consense.glsl
 */
#version 120
uniform sampler2D   texture;    ///< The texture to process

const float texWidth  = 1.0 / 640.0;	///< one over texture width
const float texHeight = 1.0 / 480.0;	///< one over texture height
const int windowSize = 5;				///< Window semi-size

const float threshold = 30.0;			///< Thresholding value
/// value to divide
const float div = 1.0 / ((windowSize * 2 + 1) * (windowSize * 2 + 1));

/**
 * Convert HSV to RGB color space
 * @param h Hue value
 * @param s Saturation value
 * @param v Value
 */
vec4 hsvToRgb(float h, float s, float v) {
	float hue = h * 6.0;

	float c = v * s;
	float x = c  * (1 - abs(mod(hue, 2.0) - 1));

	if (hue < 1.0) {
		return vec4(c, x, 0.0, 1.0);
	} else if (hue < 2.0) {
		return vec4(x, c, 0.0, 1.0);
	} else if (hue < 3.0) {
		return vec4(0.0, c, x, 1.0);
	} else if (hue < 4.0) {
		return vec4(0.0, x, c, 1.0);
	} else if (hue < 5.0) {
		return vec4(x, 0.0, c, 1.0);
	} else {
		return vec4(c, 0.0, x, 1.0);
	}
}

/**
 * Calculate consensus over the given texture.
 * @param coords Texture coordinates.
 */
vec4 consensus(vec2 coords) {
	float resp = 0.0;
	vec4 color;
	for (int i = -windowSize; i <= windowSize; i++) {
		for (int j = -windowSize; j <= windowSize; j++) {
			color = texture2D(texture, coords + vec2(i * texWidth, j * texHeight));
			if (color == vec4(1.0, 1.0, 0.0, 1.0)) {
				resp += 1.0;
			}
			if (color == vec4(1.0, 0.0, 0.0, 1.0)) {
				resp += 0.125;
			}
			if (color == vec4(0.0, 1.0, 0.0, 1.0)) {
				resp += 0.125;
			}
		}
	}

	if (resp < threshold) {
		resp = 0.0;
	}
	return vec4(resp * div, resp * div, resp * div, 1.0);// hsvToRgb(resp * div, 0.8, 0.8);
}

/// Shader entry point
void main() {
	gl_FragColor = consensus(gl_TexCoord[0].st);
}