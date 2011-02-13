/*
 * Copyright (C) 2010 Guillaume Delente
 *
 * This file is part of .
 *
 * Vcuboid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * Vcuboid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vcuboid.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.vcuboid.filter;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.util.Log;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;

abstract public class FilterPreferencesActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener {

	protected VcubFilter mActualFilter;
	protected VcubFilter mModifiedFilter;
	protected CheckBoxPreference mDistanceFilterCb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mDistanceFilterCb = (CheckBoxPreference) getPreferenceScreen()
				.findPreference(getString(R.string.enable_distance_filter));
		getPreferenceScreen().findPreference(
				getString(R.string.distance_filter)).setSummary(
				getString(R.string.distance_filter_summary)
						+ " "
						+ getPreferenceScreen().getSharedPreferences().getInt(
								getString(R.string.distance_filter), 1000)
						+ "m");
		mActualFilter = VcuboidManager.getVcuboidManagerInstance()
				.getVcubFilter();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		try {
			mModifiedFilter = mActualFilter.clone();
		} catch (CloneNotSupportedException e) {
			// Cannot happend
		}
	}

	@Override
	public void onPause() {
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		if (mModifiedFilter.equals(mActualFilter)) {
			setResult(RESULT_CANCELED);
			Log.e("Vcuboid", "Exiting Preferences : Filter not changed");
		} else {
			Log.e("Vcuboid", "Exiting Preferences : Filter Changed");
			setResult(RESULT_OK);
			mModifiedFilter.setNeedDbQuery(mActualFilter);
			VcuboidManager.getVcuboidManagerInstance().setVcubFilter(
					mModifiedFilter);
			VcuboidManager.getVcuboidManagerInstance()
					.createVisibleStationList();
			Log.e("Vcuboid", "Only Favorites ? "
					+ mModifiedFilter.isShowOnlyFavorites());
		}
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(getString(R.string.favorite_filter))) {
			Log.i("Vcuboid", "Favorites changed");
			mModifiedFilter.setShowOnlyFavorites(sharedPreferences.getBoolean(
					getString(R.string.favorite_filter), false));
		} else if (key.equals(getString(R.string.bikes_filter))) {
			Log.i("Vcuboid", "Bikes changed");
			mModifiedFilter.setShowOnlyWithBikes(sharedPreferences.getBoolean(
					getString(R.string.bikes_filter), false));
		} else if (key.equals(getString(R.string.slots_filter))) {
			Log.i("Vcuboid", "Slots changed");
			mModifiedFilter.setShowOnlyWithSlots(sharedPreferences.getBoolean(
					getString(R.string.slots_filter), false));
		} else if (key.equals(getString(R.string.enable_distance_filter))) {
			Log.i("Vcuboid", "Enable / disable filter changed");
			mModifiedFilter
					.setDistanceFilter(sharedPreferences.getBoolean(
							getString(R.string.enable_distance_filter), true) ? sharedPreferences
							.getInt(getString(R.string.distance_filter), 1000)
							: 0);
		} else if (key.equals(getString(R.string.distance_filter))) {
			Log.i("Vcuboid", "Distance filter changed");
			mModifiedFilter.setDistanceFilter(sharedPreferences.getInt(
					getString(R.string.distance_filter), 1000));
			getPreferenceScreen().findPreference(
					getString(R.string.distance_filter)).setSummary(
					getString(R.string.distance_filter_summary)
							+ " "
							+ sharedPreferences.getInt(
									getString(R.string.distance_filter), 1000)
							+ "m");
		} else if (key.equals(getString(R.string.use_location))) {
			Log.i("Vcuboid", "Location changed");
			VcuboidManager vcubManager = VcuboidManager
					.getVcuboidManagerInstance();
			if (sharedPreferences.getBoolean(getString(R.string.use_location),
					true)) {
				Log.i("Vcuboid", "use Location");
				vcubManager.useLocation();
			} else {
				Log.i("Vcuboid", "dont Use Location");
				mDistanceFilterCb.setChecked(false);
				vcubManager.dontUseLocation();
			}
		}
	}
}
