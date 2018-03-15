/*
 * Copyright 2018 Mario Guggenberger <mg@protyposis.net>
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

package net.protyposis.android.mediaplayer;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by Mario on 15.03.2018.
 */
class Timeline {

    private LinkedList<Cue> mList;
    private ListIterator<Cue> mListIterator;
    private int mListPosition;

    public Timeline() {
        reset();
    }

    public void addCue(Cue cue) {
        ListIterator<Cue> iterator = mList.listIterator();

        while(iterator.hasNext()) {
            Cue c = iterator.next();

            if (c.getTime() > cue.getTime()) {
                break;
            }
        }

        iterator.add(cue);

        int cueIndex = iterator.previousIndex();

        if (cueIndex < mListPosition) {
            mListPosition++;
        }

        mListIterator = mList.listIterator(mListPosition);
    }

    public boolean removeCue(Cue cue) {
        int cueIndex = mList.indexOf(cue);

        if (cueIndex == -1) {
            return false;
        }

        mList.remove(cueIndex);

        if (cueIndex < mListPosition) {
            mListPosition--;
        }

        mListIterator = mList.listIterator(mListPosition);

        return true;
    }

    public void setPlaybackPosition(int position) {
        ListIterator<Cue> iterator = mList.listIterator();

        while(iterator.hasNext()) {
            Cue c = iterator.next();

            if (c.getTime() > position) {
                break;
            }
        }

        if(iterator.hasPrevious()) {
            iterator.previous();
        }

        mListIterator = iterator;
        mListPosition = iterator.nextIndex();
    }

    public void movePlaybackPosition(int position, OnCueListener listener) {
        if (mListIterator == null) {
            mListIterator = mList.listIterator();
        }

        while (mListIterator.hasNext()) {
            Cue cue = mListIterator.next();
            mListPosition++;

            if (cue.getTime() <= position) {
                listener.onCue(cue);
            } else {
                mListIterator.previous();
                mListPosition--;
                break;
            }
        }
    }

    public int count() {
        return mList.size();
    }

    public void reset() {
        mList = new LinkedList<>();
        mListIterator = null;
        mListPosition = 0;
    }

    public interface OnCueListener {
        void onCue(Cue cue);
    }
}
