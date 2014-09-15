/*
 * Copyright (c) 2014 Mario Guggenberger <mario.guggenberger@aau.at>
 *
 * This file is part of ITEC MediaPlayer.
 *
 * ITEC MediaPlayer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ITEC MediaPlayer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ITEC MediaPlayer.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.aau.itec.android.mediaplayer.gles;

import android.opengl.GLES20;

/**
 * Created by maguggen on 04.07.2014.
 */
public class Framebuffer {

    private int mFramebuffer;
    private Texture2D mTargetTexture;

    public Framebuffer(int width, int height) {
        int[] framebuffer = new int[1];
        GLES20.glGenFramebuffers(1, framebuffer, 0);
        mFramebuffer = framebuffer[0];

        /* Every framebuffer has its own texture attached, because switching between framebuffers
         * is faster and recommended against switching texture attachements of a single framebuffer.
         * http://stackoverflow.com/a/6435997
         * http://stackoverflow.com/a/6767452 (comments!)
         */
        mTargetTexture = Texture2D.generateFloatTexture(width, height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mTargetTexture.getHandle(), 0);

        checkFramebufferStatus();
    }

    public void bind(boolean clear) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);

        if(clear) {
            // for performance on Android, clear after every bind: http://stackoverflow.com/a/11052366
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        }
    }

    public void bind() {
        bind(true);
    }

    public Texture2D getTexture() {
        return mTargetTexture;
    }

    private void checkFramebufferStatus() {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if(status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("glCheckFramebufferStatus error " + String.format("0x%X", status));
        }
    }
}
