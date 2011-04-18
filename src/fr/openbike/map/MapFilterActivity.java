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
package fr.openbike.map;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import fr.openbike.R;
import fr.openbike.filter.FilterPreferencesActivity;

public class MapFilterActivity extends FilterPreferencesActivity {
	
	protected CheckBoxPreference mDistanceFilterCb;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.map_preferences);
		addPreferencesFromResource(R.xml.location_preferences);
		addPreferencesFromResource(R.xml.filter_preferences);
	    addPreferencesFromResource(R.xml.other_preferences);
	}

	@Override
	protected void onResume() {
		mDistanceFilterCb = (CheckBoxPreference) getPreferenceScreen()
				.findPreference(FilterPreferencesActivity.ENABLE_DISTANCE_FILTER);
		getPreferenceScreen().findPreference(
				FilterPreferencesActivity.DISTANCE_FILTER).setSummary(
				getString(R.string.distance_filter_summary)
						+ " "
						+ getPreferenceScreen().getSharedPreferences().getInt(
								FilterPreferencesActivity.DISTANCE_FILTER, 1000)
						+ "m");
		super.onResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(FilterPreferencesActivity.LOCATION_PREFERENCE)) {
			if (!sharedPreferences.getBoolean(FilterPreferencesActivity.LOCATION_PREFERENCE,
					false)) {
				mDistanceFilterCb.setChecked(false);
			}
		}
		super.onSharedPreferenceChanged(sharedPreferences, key);
	}

	/* (non-Javadoc)
	 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		
	}
}
