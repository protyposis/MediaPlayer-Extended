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

package at.aau.itec.android.mediaplayerdemo;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import at.aau.itec.android.mediaplayer.GLTextureView;
import at.aau.itec.android.mediaplayer.effects.FloatParameter;
import at.aau.itec.android.mediaplayer.effects.IntegerParameter;
import at.aau.itec.android.mediaplayer.effects.Parameter;

/**
 * Created by Mario on 06.09.2014.
 */
public class ParameterListAdapter extends BaseAdapter {

    private Activity mActivity;
    private GLTextureView mTextureView;
    public List<Parameter> mParameters;

    public ParameterListAdapter(Activity activity, GLTextureView textureView, List<Parameter> parameters) {
        mActivity = activity;
        mTextureView = textureView;
        mParameters = new ArrayList<Parameter>(parameters);
    }

    @Override
    public int getCount() {
        return mParameters.size();
    }

    @Override
    public Parameter getItem(int position) {
        return mParameters.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Parameter parameter = getItem(position);
        View view = convertView;

        if(convertView == null) {
            view = mActivity.getLayoutInflater().inflate(R.layout.list_item_parameter_seekbar, parent, false);
        }

        ((TextView) view.findViewById(R.id.name)).setText(parameter.getName());
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        final TextView valueView = (TextView) view.findViewById(R.id.value);
        final Button resetButton = (Button) view.findViewById(R.id.reset);

        if (parameter.getType() == Parameter.Type.INTEGER) {
            final IntegerParameter p = (IntegerParameter) parameter;
            int interval = p.getMax() - p.getMin();
            seekBar.setMax(interval);
            seekBar.setProgress(p.getValue() - p.getMin());
            SeekBar.OnSeekBarChangeListener changeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    final int value = progress + p.getMin();
                    mTextureView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            p.setValue(value);
                        }
                    });
                    valueView.setText(String.format("%d", value));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            };
            seekBar.setOnSeekBarChangeListener(changeListener);
            changeListener.onProgressChanged(seekBar, seekBar.getProgress(), false); // init value label
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekBar.setProgress(p.getDefault() - p.getMin());
                }
            });
        } else if (parameter.getType() == Parameter.Type.FLOAT) {
            final int precision = 100; // 2 digits after comma
            final FloatParameter p = (FloatParameter) parameter;
            float interval = p.getMax() - p.getMin();
            seekBar.setMax((int) (interval * precision));
            seekBar.setProgress((int) ((p.getValue() - p.getMin()) * precision));
            SeekBar.OnSeekBarChangeListener changeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                    final float value = (progress / (float) precision) + p.getMin();
                    mTextureView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            p.setValue(value);
                        }
                    });
                    valueView.setText(String.format("%.2f", value));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            };
            seekBar.setOnSeekBarChangeListener(changeListener);
            changeListener.onProgressChanged(seekBar, seekBar.getProgress(), false); // init value label
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekBar.setProgress((int) ((p.getDefault() - p.getMin()) * precision));
                }
            });
        }

        return view;
    }

    public void clear() {
        mParameters.clear();
        notifyDataSetChanged();
    }
}