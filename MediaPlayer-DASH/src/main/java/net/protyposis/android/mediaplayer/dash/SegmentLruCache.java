/*
 * Copyright (c) 2014 Mario Guggenberger <mg@protyposis.net>
 *
 * This file is part of MediaPlayer-Extended.
 *
 * MediaPlayer-Extended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MediaPlayer-Extended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MediaPlayer-Extended.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.protyposis.android.mediaplayer.dash;

import android.util.LruCache;

/**
 * Created by maguggen on 28.08.2014.
 */
class SegmentLruCache extends LruCache<Integer, CachedSegment> {

    public SegmentLruCache(int maxBytes) {
        super(maxBytes);
    }

    @Override
    protected void entryRemoved(boolean evicted, Integer key, CachedSegment oldValue, CachedSegment newValue) {
        if(newValue != null && newValue == oldValue) {
            // When a value replaces itself, do nothing
            return;
        }

        // Delete the file upon cache removal, no matter if through a put or eviction
        oldValue.file.delete();
    }

    @Override
    protected int sizeOf(Integer key, CachedSegment value) {
        // Return the size of the file
        // NOTE an alternative would be to operate on time units and return the length of the segment
        return (int)value.file.length();
    }
}
