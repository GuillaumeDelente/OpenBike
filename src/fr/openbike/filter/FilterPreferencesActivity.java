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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import fr.openbike.MyLocationProvider;
import fr.openbike.OpenBikeManager;
import fr.openbike.R;
import fr.openbike.list.OpenBikeListActivity;

abstract public class FilterPreferencesActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener, OnClickListener {

	protected BikeFilter mActualFilter;
	protected BikeFilter mModifiedFilter;
	protected Preference mNetworkPreference;
	protected Preference mReportBugPreference;
	protected Dialog mConfirmDialog;
	private OpenBikeManager mOpenBikeManager;

	public static final String NETWORK_PREFERENCE = "network";
	public static final String REPORT_BUG_PREFERENCE = "report_bug";
	public static final String FAVORITE_FILTER = "favorite_filter";
	public static final String BIKES_FILTER = "bikes_filter";
	public static final String SLOTS_FILTER = "slots_filter";
	public static final String ENABLE_DISTANCE_FILTER = "enable_distance_filter";
	public static final String DISTANCE_FILTER = "distance_filter";
	public static final String LOCATION_PREFERENCE = "use_location";
	public static final String CENTER_PREFERENCE = "center_on_location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mOpenBikeManager = OpenBikeManager.getOpenBikeManagerInstance(this);
		mActualFilter = mOpenBikeManager.getOpenBikeFilter();
		mNetworkPreference = getPreferenceScreen().findPreference(
				FilterPreferencesActivity.NETWORK_PREFERENCE);
		mReportBugPreference = getPreferenceScreen().findPreference(
				FilterPreferencesActivity.REPORT_BUG_PREFERENCE);
		mNetworkPreference.setSummary(OpenBikeManager.NETWORK_NAME + " : "
				+ OpenBikeManager.NETWORK_CITY);
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
			mOpenBikeManager.setVcubFilter(mModifiedFilter);
			//TODO:
			//mOpenBikeManager.executeCreateVisibleStationsTask(false);
			// Log.e("OpenBike", "Only Favorites ? "
			// + mModifiedFilter.isShowOnlyFavorites());
		}
		super.onPause();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case MyLocationProvider.ENABLE_GPS:
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					getString(R.string.gps_disabled)).setMessage(
					getString(R.string.should_enable_gps) + "\n"
							+ getString(R.string.show_location_parameters))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent gpsOptionsIntent = new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivity(gpsOptionsIntent);
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case MyLocationProvider.NO_LOCATION_PROVIDER:
			// Log.i("OpenBike", "onPrepareDialog : NO_LOCATION_PROVIDER");
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					getString(R.string.location_disabled)).setMessage(
					getString(R.string.should_enable_location) + "\n"
							+ getString(R.string.show_location_parameters))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent gpsOptionsIntent = new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivity(gpsOptionsIntent);
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		}
		return super.onCreateDialog(id);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mNetworkPreference) {
			startActivity(new Intent(this, OpenBikeListActivity.class)
					.setAction(OpenBikeListActivity.ACTION_CHOOSE_NETWORK)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		} else if (preference == mReportBugPreference) {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"contact@openbike.fr"});
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Bug OpenBike");
			startActivity(Intent.createChooser(emailIntent, "Signaler un bug"));
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(FilterPreferencesActivity.FAVORITE_FILTER)) {
			// Log.i("OpenBike", "Favorites changed");
			mModifiedFilter.setShowOnlyFavorites(sharedPreferences.getBoolean(
					FilterPreferencesActivity.FAVORITE_FILTER, false));
		} else if (key.equals(FilterPreferencesActivity.BIKES_FILTER)) {
			// Log.i("OpenBike", "Bikes changed");
			mModifiedFilter.setShowOnlyWithBikes(sharedPreferences.getBoolean(
					FilterPreferencesActivity.BIKES_FILTER, false));
		} else if (key.equals(FilterPreferencesActivity.SLOTS_FILTER)) {
			// Log.i("OpenBike", "Slots changed");
			mModifiedFilter.setShowOnlyWithSlots(sharedPreferences.getBoolean(
					FilterPreferencesActivity.SLOTS_FILTER, false));
		} else if (key.equals(FilterPreferencesActivity.ENABLE_DISTANCE_FILTER)) {
			// Log.i("OpenBike", "Enable / disable filter changed");
			mModifiedFilter
					.setDistanceFilter(sharedPreferences.getBoolean(
							FilterPreferencesActivity.ENABLE_DISTANCE_FILTER,
							false) ? sharedPreferences.getInt(
							FilterPreferencesActivity.DISTANCE_FILTER, 1000)
							: 0);
		} else if (key.equals(FilterPreferencesActivity.DISTANCE_FILTER)) {
			// Log.i("OpenBike", "Distance filter changed");
			mModifiedFilter.setDistanceFilter(sharedPreferences.getInt(
					FilterPreferencesActivity.DISTANCE_FILTER, 1000));
			getPreferenceScreen().findPreference(
					FilterPreferencesActivity.DISTANCE_FILTER).setSummary(
					getString(R.string.distance_filter_summary)
							+ " "
							+ sharedPreferences.getInt(
									FilterPreferencesActivity.DISTANCE_FILTER,
									1000) + "m");
		} else if (key.equals(FilterPreferencesActivity.LOCATION_PREFERENCE)) {
			// Log.i("OpenBike", "Location changed");
			if (sharedPreferences.getBoolean(
					FilterPreferencesActivity.LOCATION_PREFERENCE, false)) {
				// Log.i("OpenBike", "use Location");
				mOpenBikeManager.useLocation();
			} else {
				// Log.i("OpenBike", "dont Use Location");
				mOpenBikeManager.dontUseLocation();
			}
		}
	}
}
