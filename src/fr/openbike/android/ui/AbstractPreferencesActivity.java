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
package fr.openbike.android.ui;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import fr.openbike.android.IActivityHelper;
import fr.openbike.android.R;
import fr.openbike.android.service.ILocationServiceListener;
import fr.openbike.android.service.LocationService;
import fr.openbike.android.service.LocationService.LocationBinder;
import fr.openbike.android.utils.ActivityHelper;
import fr.openbike.android.utils.DetachableResultReceiver;

abstract public class AbstractPreferencesActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener, OnClickListener,
		ILocationServiceListener, DetachableResultReceiver.Receiver,
		IActivityHelper {

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
	public static final String LAST_UPDATE = "last_update";
	public static final String STATIONS_VERSION = "stations_version";

	public static final int NO_NETWORK = 0;

	protected Preference mNetworkPreference;
	protected Preference mReportBugPreference;
	protected Dialog mConfirmDialog;
	private Location mLastLocation = null;
	protected ActivityHelper mActivityHelper = null;
	protected DetachableResultReceiver mReceiver = null;
	private SharedPreferences mSharedPreferences = null;
	private boolean mBound = false;
	private LocationService mService = null;

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocationBinder binder = (LocationBinder) service;
			mService = binder.getService();
			mBound = true;
			mService.addListener(AbstractPreferencesActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_screen);
		mReceiver = DetachableResultReceiver.getInstance(new Handler());
		mActivityHelper = new ActivityHelper(this);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActivityHelper.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mReceiver.setReceiver(this);
	}

	@Override
	protected void onStart() {
		if (mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, true)) {
			Intent intent = new Intent(this, LocationService.class);
			bindService(intent, mConnection, 0);
		}
		super.onStart();
	}

	@Override
	protected void onPause() {
		mReceiver.clearReceiver();
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (mBound) {
			unbindService(mConnection);
			mService.removeListener(AbstractPreferencesActivity.this);
			mBound = false;
		}
		super.onStop();
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		default:
			Dialog dialog = getActivityHelper().onCreateDialog(id);
			if (dialog != null)
				return dialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		getActivityHelper().onPrepareDialog(id, dialog);
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(AbstractPreferencesActivity.LOCATION_PREFERENCE)) {
			if (sharedPreferences.getBoolean(
					AbstractPreferencesActivity.LOCATION_PREFERENCE, true)) {
				Intent intent = new Intent(this, LocationService.class);
				bindService(intent, mConnection, 0);
			} else if (mBound) {
				mService.removeListener(AbstractPreferencesActivity.this);
				mBound = false;
				unbindService(mConnection);
			}
		}
	}

	@Override
	public void onLocationChanged(Location location, boolean unused) {
		if (location == mLastLocation) {
			return;
		}
		mLastLocation = location;
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		// We are not interested in anything
	}

	/**
	 * Returns the {@link ActivityHelper} object associated with this activity.
	 */
	@Override
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}

	@Override
	public void onLocationProvidersChanged(int id) {
		if (!isFinishing()) {
			showDialog(id);
		}
	}

	@Override
	public void onStationsUpdated() {
		// NOthing to do
	}

}
