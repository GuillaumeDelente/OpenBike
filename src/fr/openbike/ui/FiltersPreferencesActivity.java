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
package fr.openbike.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.util.Log;
import fr.openbike.R;

public class FiltersPreferencesActivity extends AbstractPreferencesActivity {

	protected CheckBoxPreference mDistanceFilterCb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mActivityHelper.setupActionBar(getString(R.string.btn_filters));
		addPreferencesFromResource(R.xml.filter_preferences);
		mDistanceFilterCb = (CheckBoxPreference) getPreferenceScreen()
				.findPreference(
						AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER);
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		Log.d("OpenBike", "onResume");
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		mDistanceFilterCb.setEnabled(prefs.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, false));
		mDistanceFilterCb
				.setSummary(mDistanceFilterCb.isEnabled() ? R.string.enable_distance_filter_summary
						: R.string.enable_location_before);
		getPreferenceScreen().findPreference(
				AbstractPreferencesActivity.DISTANCE_FILTER).setSummary(
				getString(R.string.distance_filter_summary)
						+ " "
						+ prefs.getInt(
								AbstractPreferencesActivity.DISTANCE_FILTER,
								1000) + "m");
		super.onResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		super.onSharedPreferenceChanged(sharedPreferences, key);
		Log.d("OpenBike", "Preferences changed");
		if (key.equals(AbstractPreferencesActivity.DISTANCE_FILTER)) {
			getPreferenceScreen()
					.findPreference(AbstractPreferencesActivity.DISTANCE_FILTER)
					.setSummary(
							getString(R.string.distance_filter_summary)
									+ " "
									+ sharedPreferences
											.getInt(
													AbstractPreferencesActivity.DISTANCE_FILTER,
													1000) + "m");
		}
	}
}
