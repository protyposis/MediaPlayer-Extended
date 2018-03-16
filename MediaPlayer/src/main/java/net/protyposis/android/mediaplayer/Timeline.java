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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by Mario on 15.03.2018.
 */
class Timeline {

    /**
     * A linked list that stores the sequence of cues in the timeline ascending by time.
     */
    private LinkedList<Cue> mList;
    /**
     * The iterator to traverse the timeline sequentially.
     */
    private ListIterator<Cue> mListIterator;
    /**
     * Tracks the current position in the list so we can easily create a new list iterator
     * when necessary.
     */
    private int mListPosition;
    /**
     * A hashtable to keep track of cues in the timeline that can be used to check for existing
     * cues in O(1). This is solely used to determine the return value of {@link #removeCue(Cue)}
     * and to keep track of the {@link #count()}.
     */
    private HashSet<Cue> mCues;
    /**
     * A list of cues to be added by the next {@link #setPlaybackPosition(int)} or
     * {@link #movePlaybackPosition(int, OnCueListener)}.
     *
     * We do not insert the cues directly into the timeline for performance reasons:
     *  - to avoid the need to execute the playback position methods in a synchronized block
     *  - because batch insertions can be done with a single iteration of the timeline
     */
    private ArrayList<Cue> mCuesToAdd;
    /**
     * A list of cues to be removed by the next {@link #setPlaybackPosition(int)} or
     * {@link #movePlaybackPosition(int, OnCueListener)}.
     *
     * Same performance reasons as {@link #mCuesToAdd}.
     */
    private ArrayList<Cue> mCuesToRemove;
    /**
     * Keeps track of the number of additions and removals so we can determine when the cues
     * have been added/removed and we need to do a {@link #updateCueList()}.
     */
    private int mModCount;
    /**
     * Keeps track of the number of modifications after the last {@link #updateCueList()}. Is used
     * together with {@link #mModCount} to determine if the cue list needs to be updated.
     */
    private int mLastUpdateModCount;

    /**
     * Sorts cues in ascending time order.
     */
    private Comparator<Cue> mCueTimeSortComparator = new Comparator<Cue>() {
        @Override
        public int compare(Cue lhs, Cue rhs) {
            if (lhs.getTime() == rhs.getTime()) {
                return 0;
            }
            else if (lhs.getTime() < rhs.getTime()) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    public Timeline() {
        reset();
    }

    public synchronized void addCue(Cue cue) {
        mCuesToAdd.add(cue);
        mCues.add(cue);
        mModCount++;
    }

    public synchronized boolean removeCue(Cue cue) {
        if (mCues.contains(cue)) {
            mCues.remove(cue);
            mCuesToRemove.add(cue);
            mModCount++;
            return true;
        }

        return false;
    }

    /**
     * Sets the playback position to a new position without announcing cues, e.g. when seeking.
     * @param position the new playback position
     */
    public void setPlaybackPosition(int position) {
        if (mModCount != mLastUpdateModCount) {
            // Update the timeline list if cues have been added or removed
            // We check the mod count here to avoid an unnecessary function call
            updateCueList();
        }

        // Create a new iterator, ...
        ListIterator<Cue> iterator = mList.listIterator();

        // move to the desired position, ...
        while(iterator.hasNext()) {
            Cue c = iterator.next();

            if (c.getTime() > position) {
                break;
            }
        }
        if(iterator.hasPrevious()) {
            iterator.previous();
        }

        // And replace the previous iterator
        mListIterator = iterator;
        mListPosition = iterator.nextIndex();
    }

    /**
     * Moves the playback position from the current to the requested position, announcing all
     * passed cues that are positioned in between.
     * @param position the new playback position
     * @param listener a listener that receives all cues between the previous and new position
     */
    public void movePlaybackPosition(int position, OnCueListener listener) {
        if (mModCount != mLastUpdateModCount) {
            // Update the timeline list if cues have been added or removed
            // We check the mod count here to avoid an unnecessary function call
            updateCueList();
        }

        // Move through the timeline and announce cues
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

    /**
     * Gets the number of cues.
     * @return the number of cues
     */
    public int count() {
        return mCues.size();
    }

    /**
     * Resets the timeline to its initial empty state.
     */
    public synchronized void reset() {
        mList = new LinkedList<>();
        mListIterator = mList.listIterator();
        mListPosition = 0;
        mCues = new HashSet<>();
        mCuesToAdd = new ArrayList<>();
        mCuesToRemove = new ArrayList<>();
        mModCount = 0;
        mLastUpdateModCount = 0;
    }

    private synchronized void updateCueList() {
        if (!mCuesToAdd.isEmpty()) {
            Collections.sort(mCuesToAdd, mCueTimeSortComparator);

            int cuesToAddIndex = 0;
            ListIterator<Cue> iterator = mList.listIterator();

            // Add cues into list
            while(cuesToAddIndex < mCuesToAdd.size() && iterator.hasNext()) {
                Cue cue = iterator.next();
                if (cue.getTime() < mCuesToAdd.get(cuesToAddIndex).getTime()) {
                    iterator.add(mCuesToAdd.get(cuesToAddIndex));
                    cuesToAddIndex++;

                    int cueIndex = iterator.previousIndex();
                    if (cueIndex < mListPosition) {
                        mListPosition++;
                    }
                }
            }

            // Append remaining cues to end of list
            while(cuesToAddIndex < mCuesToAdd.size()) {
                iterator.add(mCuesToAdd.get(cuesToAddIndex));
                cuesToAddIndex++;
            }

            mCuesToAdd.clear();
        }

        if (!mCuesToRemove.isEmpty()) {
            HashSet<Cue> cuesToRemove = new HashSet<>(mCuesToRemove);

            int cuesToRemoveIndex = 0;
            ListIterator<Cue> iterator = mList.listIterator();

            while(cuesToRemoveIndex < mCuesToRemove.size() && iterator.hasNext()) {
                Cue cue = iterator.next();
                if (cuesToRemove.contains(cue)) {
                    iterator.remove();
                    cuesToRemoveIndex++;

                    int cueIndex = iterator.nextIndex();
                    if (cueIndex < mListPosition) {
                        mListPosition--;
                    }
                }
            }

            mCuesToRemove.clear();
        }

        mLastUpdateModCount = mModCount;

        // We possibly modified the cue list so we need to create a new iterator instance
        mListIterator = mList.listIterator(mListPosition);
    }

    public interface OnCueListener {
        void onCue(Cue cue);
    }
}
