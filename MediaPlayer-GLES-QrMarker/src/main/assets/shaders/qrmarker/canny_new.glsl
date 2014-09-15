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
 * Perform canny edge detection on the current texture.
 * In our algorithm is the third pass.
 *
 * @author Alberto Franco
 * @file   canny_new.glsl
 */
#version 120

uniform sampler2D text; ///< Color texture

const float texWidth  = 1.0 / 640.0;	///< Web cam width size
const float texHeight = 1.0 / 480.0;	///< Web cam height size
const float threshold = 0.2;			///< Threshold value

const vec2 unshift = vec2(1.0 / 256.0, 1.0); ///< Value used to unpack 16 bit float data

const float atan0   = 0.414213;  ///< Support value for atan
const float atan45  = 2.414213;  ///< Support value for atan
const float atan90  = -2.414213; ///< Support value for atan
const float atan135 = -0.414213; ///< Support value for atan

/// Fast atan for canny usage.
vec2 atanForCanny(float x) {
    if (x < atan0 && x > atan135) {
        return vec2(1.0, 0.0);
    }
    if (x < atan90 && x > atan45) {
        return vec2(0.0, 1.0);
    }
    if (x > atan135 && x < atan90) {
        return vec2(-1.0, 1.0);
    }
    return vec2(1.0, 1.0);
}

/**
 * Function that performs canny edge detection.
 * @param coords Texture coordinates to analyize
 */
vec4 cannyEdge(vec2 coords) {
  vec4 color = texture2D(text, coords);
  color.z = dot(color.zw, unshift);

  // Thresholding
  if (color.z > threshold) {
    // Restore gradient directions.
    color.x -= 0.5;
    color.y -= 0.5;

    vec2 offset = atanForCanny(color.y / color.x);
    offset.x *= texWidth;
    offset.y *= texHeight;

    vec4 forward  = texture2D(text, coords + offset);
    vec4 backward = texture2D(text, coords - offset);
    // Uncompress mag data
    forward.z  = dot(forward.zw, unshift);
    backward.z = dot(backward.zw, unshift);

    // Check maximum.
    if (forward.z >= color.z ||
        backward.z >= color.z) {
      return vec4(0.0, 0.0, 0.0, 1.0);
    } else {
      color.x += 0.5; color.y += 0.5;
      return vec4(1.0, color.x, color.y, 1.0);
    }
  }
  return vec4(0.0, 0.0, 0.0, 1.0);
}

/// Shader entry point
void main() {
	gl_FragColor = cannyEdge(gl_TexCoord[0].st);
}