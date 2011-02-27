/*
 * Copyright (C) 2011 Guillaume Delente
 *
 * This file is part of OpenBike.
 *
 * OpenBike is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * OpenBike is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenBike.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.openbike.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import fr.openbike.R;

public class SeekBarPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {
	private SeekBar mBar;
	private TextView mDistanceTextView;
	private static SharedPreferences mSharedPreferences;
	private static String DISTANCE_FILTER;
	private static final int DEFAULT_DISTANCE = 1000;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		DISTANCE_FILTER = context.getString(R.string.distance_filter);
		setDialogLayoutResource(R.layout.distance_preference_layout);
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		mDistanceTextView.setText(" " + progress + "m.");
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	@Override
    protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
        mDistanceTextView = (TextView) view.findViewById(R.id.distance_preference_meters);
        int distance = getValue();
        mDistanceTextView.setText(" " + distance + "m.");
        mBar = (SeekBar) view.findViewById(R.id.distance_seekBar);
        mBar.setMax(2000);
        mBar.setOnSeekBarChangeListener(this);
        mBar.setProgress(distance);
        return view;
    }

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			setValue(mBar.getProgress());
		}
	}

	private void setValue(int value) {
		Editor ed = mSharedPreferences.edit();
		ed.putInt(DISTANCE_FILTER, value);
		ed.commit();
	}

	private int getValue() {
		return mSharedPreferences.getInt(DISTANCE_FILTER, DEFAULT_DISTANCE);
	}
}