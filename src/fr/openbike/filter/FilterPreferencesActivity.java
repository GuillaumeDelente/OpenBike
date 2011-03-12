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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import fr.openbike.OpenBikeManager;
import fr.openbike.R;

abstract public class FilterPreferencesActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener, OnClickListener {

	protected BikeFilter mActualFilter;
	protected BikeFilter mModifiedFilter;
	protected Preference mResetButton;
	protected Dialog mConfirmDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mActualFilter = OpenBikeManager.getVcuboidManagerInstance()
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
			// Log.e("OpenBike", "Exiting Preferences : Filter not changed");
		} else {
			// Log.e("OpenBike", "Exiting Preferences : Filter Changed");
			setResult(RESULT_OK);
			mModifiedFilter.setNeedDbQuery(mActualFilter);
			OpenBikeManager.getVcuboidManagerInstance().setVcubFilter(
					mModifiedFilter);
			OpenBikeManager.getVcuboidManagerInstance()
					.executeCreateVisibleStationsTask(false);
			// Log.e("OpenBike", "Only Favorites ? "
			// + mModifiedFilter.isShowOnlyFavorites());
		}
		super.onPause();
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mResetButton) {
			showResetDialog();
		}
		return false;
	}

	private void showResetDialog() {
		CharSequence msg = getResources()
				.getText(R.string.reset_dialog_message);
		mConfirmDialog = new AlertDialog.Builder(this).setMessage(msg)
				.setTitle(R.string.reset_dialog_title).setIcon(
						android.R.drawable.ic_dialog_alert).setPositiveButton(
						android.R.string.yes, this).setNegativeButton(
						android.R.string.cancel, this).show();
	}

	public void onClick(DialogInterface dialog, int button) {

		if (button == DialogInterface.BUTTON_POSITIVE) {
			OpenBikeManager.getVcuboidManagerInstance().resetDb();
		} else {
			// Unknown - should not happen
			return;
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(getString(R.string.favorite_filter))) {
			// Log.i("OpenBike", "Favorites changed");
			mModifiedFilter.setShowOnlyFavorites(sharedPreferences.getBoolean(
					getString(R.string.favorite_filter), false));
		} else if (key.equals(getString(R.string.bikes_filter))) {
			// Log.i("OpenBike", "Bikes changed");
			mModifiedFilter.setShowOnlyWithBikes(sharedPreferences.getBoolean(
					getString(R.string.bikes_filter), false));
		} else if (key.equals(getString(R.string.slots_filter))) {
			// Log.i("OpenBike", "Slots changed");
			mModifiedFilter.setShowOnlyWithSlots(sharedPreferences.getBoolean(
					getString(R.string.slots_filter), false));
		} else if (key.equals(getString(R.string.enable_distance_filter))) {
			// Log.i("OpenBike", "Enable / disable filter changed");
			mModifiedFilter
					.setDistanceFilter(sharedPreferences.getBoolean(
							getString(R.string.enable_distance_filter), true) ? sharedPreferences
							.getInt(getString(R.string.distance_filter), 1000)
							: 0);
		} else if (key.equals(getString(R.string.distance_filter))) {
			// Log.i("OpenBike", "Distance filter changed");
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
			// Log.i("OpenBike", "Location changed");
			OpenBikeManager vcubManager = OpenBikeManager
					.getVcuboidManagerInstance();
			if (sharedPreferences.getBoolean(getString(R.string.use_location),
					true)) {
				// Log.i("OpenBike", "use Location");
				vcubManager.useLocation();
			} else {
				// Log.i("OpenBike", "dont Use Location");
				vcubManager.dontUseLocation();
			}
		}
	}
}
