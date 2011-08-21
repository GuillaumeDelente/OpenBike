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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import fr.openbike.R;
import fr.openbike.service.ILocationService;
import fr.openbike.service.ILocationServiceListener;
import fr.openbike.service.LocationService;

abstract public class AbstractPreferencesActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener, OnClickListener,
		ILocationServiceListener {

	// protected BikeFilter mActualFilter;
	// protected BikeFilter mModifiedFilter;
	protected Preference mNetworkPreference;
	protected Preference mReportBugPreference;
	protected Dialog mConfirmDialog;
	private ServiceConnection mConnection = null;
	private ILocationService mBoundService = null;
	private boolean mIsBound = false;
	private Location mLastLocation = null;

	public static final String NETWORK_PREFERENCE = "network";
	public static final String REPORT_BUG_PREFERENCE = "report_bug";
	public static final String FAVORITE_FILTER = "favorite_filter";
	public static final String BIKES_FILTER = "bikes_filter";
	public static final String SLOTS_FILTER = "slots_filter";
	public static final String ENABLE_DISTANCE_FILTER = "enable_distance_filter";
	public static final String DISTANCE_FILTER = "distance_filter";
	public static final String LOCATION_PREFERENCE = "use_location";
	public static final String CENTER_PREFERENCE = "center_on_location";
	public static final String UPDATE_SERVER_URL = "server_url";
	public static final String NETWORK_LATITUDE = "network_latitude";
	public static final String NETWORK_LONGITUDE = "network_longitude";
	public static final String NETWORK_NAME = "network_name";
	public static final String NETWORK_CITY = "network_city";
	public static final String SPECIAL_STATION = "special_station";

	public static final int NO_NETWORK = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				mBoundService = ((LocationService.LocationServiceBinder) service)
						.getService();
				mBoundService.addListener(AbstractPreferencesActivity.this);
			}

			public void onServiceDisconnected(ComponentName className) {
				mBoundService = null;
				Toast.makeText(AbstractPreferencesActivity.this, "Disconnected",
						Toast.LENGTH_SHORT).show();
			}
		};
	}

	@Override
	protected void onResume() {
		super.onResume();
		// mActualFilter = BikeFilter.getInstance(this);
		SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
		if (preferences.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, false)) {
			doBindService();
		}
	}

	/*
	 * @Override public void onPause() {
	 * getPreferenceScreen().getSharedPreferences()
	 * .unregisterOnSharedPreferenceChangeListener(this); if
	 * (mModifiedFilter.equals(mActualFilter)) { setResult(RESULT_CANCELED); //
	 * Log.e("OpenBike", "Exiting Preferences : Filter not changed"); } else {
	 * // Log.e("OpenBike", "Exiting Preferences : Filter Changed");
	 * setResult(RESULT_OK); mModifiedFilter.setNeedDbQuery(mActualFilter);
	 * mOpenBikeManager.setVcubFilter(mModifiedFilter); // TODO: //
	 * mOpenBikeManager.executeCreateVisibleStationsTask(false); //
	 * Log.e("OpenBike", "Only Favorites ? "); // +
	 * mModifiedFilter.isShowOnlyFavorites()); } super.onPause(); }
	 */

	@Override
	protected void onStop() {
		doUnbindService();
		super.onStop();
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case LocationService.ENABLE_GPS:
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
		case LocationService.NO_LOCATION_PROVIDER:
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(AbstractPreferencesActivity.LOCATION_PREFERENCE)) {
			// Log.i("OpenBike", "Location changed");
			if (sharedPreferences.getBoolean(
					AbstractPreferencesActivity.LOCATION_PREFERENCE, false)) {
				doBindService();
			} else {
				doUnbindService();
			}
		}
	}

	void doBindService() {
		bindService(new Intent(this, LocationService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public void onLocationChanged(Location location, boolean unused) {
		if (location == mLastLocation) {
			return;
		}
		mLastLocation = location;
	}
}
