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
 * Calcualte the response for the OR detection
 *
 * @author Alberto Franco
 * @file   qr_sdetection.glsl
 */
#version 120
uniform sampler2D   texture;    ///< The texture to process

const float paralThres = 0.01;         ///< Uniform for thresholding parallel
const float perspThres = 1.0;
const float texWidth  = 1.0 / 640.0;  ///< Uniform for texture width
const float texHeight = 1.0 / 480.0;  ///< Uniform for texture height

const int windowSize = 40;

/**
 * Search for lines in the given direction
 * @param coords The position to start for searching
 * @param searchDirection Searching direction
 * @return If three edges are found.
 */
float search(vec2 coords, vec2 searchDirection) {
    vec4 color;
    int i = 1, countLeft = 0, countRight = 0;
	bool done = false;
    float directionRight[3];
	float directionLeft[3];
    // Search up to window size or three edeges found
	for (i = 1; i < windowSize; i++) {
		color = texture2D(texture, coords + searchDirection * i);
        if (color.x > 0.5) {
            // Edge found
            directionLeft[countLeft] = (color.z - 0.5) / (color.y - 0.5);
            countLeft += 1;
			if (countLeft == 3) break;
        }
	}

	for (i = 1; i < windowSize; i++) {
		color = texture2D(texture, coords - searchDirection * i);
        if (color.x > 0.5) {
            // Edge found
            directionRight[countRight] = (color.z -0.5) / (color.y - 0.5);
            countRight += 1;
			if (countRight == 3) break;
        }
	}

    // Check if search has been successful
    if ((countLeft == 3) && (countRight == 3)) {
		return 1.0;
    }
    // if not return 0
    return 0.0;
}

/**
 * Calculate pixel-wise response of centering a QR code
 * @param coords Pixel coords to search
 * @return color output.
 */
vec4 qrDetection(vec2 coords) {
    // { edge, grad_x, grad_y, binary image }
    vec4 color = texture2D(texture, coords);
	// Exclude areas that are edge and has with binary image
    if (color.x > 0.5 ) {
        return vec4(1.0, 1.0, 1.0, 1.0);
    } else {
        float x = search(coords, vec2(texWidth, 0.0));
		float y = search(coords, vec2(0.0, texHeight));

        return vec4(x, y, 0.0, 1.0);
    }
}

/// Main shader function
void main() {
    // Texture holds { edge, xGradient, yGradient, directiton }
    gl_FragColor = qrDetection(gl_TexCoord[0].st);
}