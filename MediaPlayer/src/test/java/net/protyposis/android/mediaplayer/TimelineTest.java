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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Mario on 15.03.2018.
 */
public class TimelineTest {

    private Cue cue0 = new Cue(0, null);
    private Cue cue1 = new Cue(1, null);
    private Cue cue2 = new Cue(2, null);

    @Test
    public void setPlaybackPositionOnEmptyList() {
        Timeline t = new Timeline();
        t.setPlaybackPosition(1000);
    }

    @Test
    public void setPlaybackPositionToStartOnEmptyList() {
        Timeline t = new Timeline();
        t.setPlaybackPosition(0);
    }

    @Test
    public void movePlaybackPositionOnEmptyList() {
        Timeline t = new Timeline();
        t.movePlaybackPosition(1000, new OnCueListener());
    }

    @Test
    public void addCue() {
        Timeline t = new Timeline();
        t.addCue(cue0);
        assertEquals(1, t.count());
    }

    @Test
    public void removeCue() {
        Timeline t = new Timeline();
        t.addCue(cue0);
        t.removeCue(cue0);
        assertEquals(0, t.count());
    }

    @Test
    public void addCuesAndMovePlaybackPositionToEnd() {
        Timeline t = new Timeline();
        t.addCue(cue0);
        t.addCue(cue1);
        t.addCue(cue2);

        OnCueListener onCueListener = new OnCueListener();

        t.movePlaybackPosition(2, onCueListener);

        assertEquals(3, onCueListener.getCount());
    }

    @Test
    public void addCuesAndMovePlaybackPositionOverEnd() {
        Timeline t = new Timeline();
        t.addCue(cue0);
        t.addCue(cue1);
        t.addCue(cue2);

        OnCueListener onCueListener = new OnCueListener();

        t.movePlaybackPosition(3, onCueListener);

        assertEquals(3, onCueListener.getCount());
    }

    @Test
    public void addCuesAndMovePlaybackPositionToMiddle() {
        Timeline t = new Timeline();
        t.addCue(cue0);
        t.addCue(cue1);
        t.addCue(cue2);

        OnCueListener onCueListener = new OnCueListener();

        t.movePlaybackPosition(1, onCueListener);

        assertEquals(2, onCueListener.getCount());
    }

    @Test
    public void removeNextCue() {
        Timeline t = new Timeline();
        t.addCue(cue0);
        t.addCue(cue1);
        t.addCue(cue2);

        OnCueListener onCueListener = new OnCueListener();

        t.movePlaybackPosition(1, onCueListener);

        assertEquals(2, onCueListener.getCount());

        t.removeCue(cue2);

        onCueListener = new OnCueListener();

        t.movePlaybackPosition(3, onCueListener);

        assertEquals(0, onCueListener.getCount());
    }

    @Test
    public void removePreviousCue() {
        Timeline t = new Timeline();
        t.addCue(cue0);
        t.addCue(cue1);
        t.addCue(cue2);

        OnCueListener onCueListener = new OnCueListener();

        t.movePlaybackPosition(1, onCueListener);

        assertEquals(2, onCueListener.getCount());

        t.removeCue(cue1);

        onCueListener = new OnCueListener();

        t.movePlaybackPosition(3, onCueListener);

        assertEquals(1, onCueListener.getCount());
        assertEquals(cue2, onCueListener.getCues().get(0));
    }

    class OnCueListener implements Timeline.OnCueListener {

        private List<Cue> cues = new ArrayList<>();

        public OnCueListener() {
            System.out.println("new OnCueListener");
        }

        @Override
        public void onCue(Cue cue) {
            System.out.println(cue.toString());
            cues.add(cue);
        }

        public int getCount() {
            return cues.size();
        }

        public List<Cue> getCues() {
            return cues;
        }
    }
}