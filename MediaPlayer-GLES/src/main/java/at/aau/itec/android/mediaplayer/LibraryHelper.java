/*
 * Copyright (c) 2014 Mario Guggenberger <mg@protyposis.net>
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

package at.aau.itec.android.mediaplayer;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by maguggen on 16.06.2014.
 */
public class LibraryHelper {

    private static Context sContext;

    static Context getContext() {
        return sContext;
    }

    static void setContext(Context context) {
        sContext = context;
    }

    public static String loadTextFromAsset(String file) {
        if(sContext == null) {
            throw new RuntimeException("context has not been set");
        }

        try {
            InputStream in = sContext.getAssets().open(file);

            InputStreamReader inReader = new InputStreamReader(in);
            BufferedReader inBReader = new BufferedReader(inReader);
            String line;
            StringBuilder text = new StringBuilder();

            while (( line = inBReader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }

            return text.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isBetween(float check, float lowerBound, float upperBound) {
        return check >= lowerBound && check <= upperBound;
    }

    public static float clamp(float check, float lowerBound, float upperBound) {
        if(check < lowerBound) {
            return lowerBound;
        } else if(check > upperBound) {
            return upperBound;
        }
        return check;
    }
}
