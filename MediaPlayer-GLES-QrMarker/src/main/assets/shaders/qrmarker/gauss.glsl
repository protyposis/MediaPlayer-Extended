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
 * First step of the algorithm: gauss blurring
 * @author  Alberto Franco
 * @file    gauss.glsl
 */
#version 120
uniform sampler2D   texture;    ///< The texture to process
uniform float width;
uniform float height;

const float texWidth  = 1.0 / 640.0;  ///< one over texture width
const float texHeight = 1.0 / 480.0;  ///< one over texture height

/// A const to avoid lots of division
const float oneOverLot      = 1.0 / 159.0;
const float oneOverThree    = 1.0 / 3.0;
const int   windowSize      = 2;

/// Convert the color to gray scale
float toGrayScale(vec4 color) {
    return (color.x + color.y + color.z) * oneOverThree;
}

/// Calculate the gaussian blur at given coords
float gaussBlur(vec2 coords) {
    float gaussFilter[25];

    gaussFilter[0] = 2.0; gaussFilter[1] = 4.0;
    gaussFilter[2] = 5.0, gaussFilter[3] = 4.0;
    gaussFilter[4] = 2.0;

    gaussFilter[5] = 4.0; gaussFilter[6] = 9.0;
    gaussFilter[7] = 12.0, gaussFilter[8] = 9.0;
    gaussFilter[9] = 4.0;

    gaussFilter[10] = 5.0; gaussFilter[11] = 12.0;
    gaussFilter[12] = 15.0, gaussFilter[13] = 12.0;
    gaussFilter[14] = 5.0;

    gaussFilter[15] = 4.0; gaussFilter[16] = 9.0;
    gaussFilter[17] = 12.0, gaussFilter[18] = 9.0;
    gaussFilter[19] = 4.0;

    gaussFilter[20] = 2.0; gaussFilter[21] = 4.0;
    gaussFilter[22] = 5.0, gaussFilter[23] = 4.0;
    gaussFilter[24] = 2.0;

    int   offset = 0, x, y;
    float color = 0.0;
    vec4  auxColor;
    /// Loop through the window and calculate the convolution
    for (y = -windowSize; y <= windowSize; y++) {
        for (x = -windowSize; x <= windowSize; x++) {
            auxColor = texture2D(texture, coords + vec2(x * texWidth, y * texHeight));
            color += toGrayScale(auxColor) * gaussFilter[offset] * oneOverLot;
            offset += 1;
        }
    }
    return color;
}

/// Main function for shader
void main() {
    float mid = gaussBlur(gl_TexCoord[0].st);
    gl_FragColor = vec4(mid, mid, mid, 1.0);
}