/*
 * Copyright 2014 Mario Guggenberger <mg@protyposis.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
