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

import android.content.SharedPreferences;
import android.os.Bundle;
import fr.openbike.R;
import fr.openbike.filter.FilterPreferencesActivity;

public class StationMapFilterActivity extends FilterPreferencesActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.map_preferences);
		addPreferencesFromResource(R.xml.location_preferences);
		addPreferencesFromResource(R.xml.other_preferences);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(getString(R.string.use_location))) {
			if (!sharedPreferences.getBoolean(getString(R.string.use_location),
					false)) {
				sharedPreferences.edit().putBoolean(
						getString(R.string.enable_distance_filter), false).commit();
			}
		}
		super.onSharedPreferenceChanged(sharedPreferences, key);
	}
}
