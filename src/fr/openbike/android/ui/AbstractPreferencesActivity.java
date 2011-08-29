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
import android.content.Context;
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
import fr.openbike.android.IActivityHelper;
import fr.openbike.android.R;
import fr.openbike.android.service.ILocationService;
import fr.openbike.android.service.ILocationServiceListener;
import fr.openbike.android.service.LocationService;
import fr.openbike.android.utils.ActivityHelper;
import fr.openbike.android.utils.DetachableResultReceiver;

abstract public class AbstractPreferencesActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener, OnClickListener,
		ILocationServiceListener, DetachableResultReceiver.Receiver,
		IActivityHelper {

	// protected BikeFilter mActualFilter;
	// protected BikeFilter mModifiedFilter;
	protected Preference mNetworkPreference;
	protected Preference mReportBugPreference;
	protected Dialog mConfirmDialog;
	private ServiceConnection mConnection = null;
	private ILocationService mBoundService = null;
	private boolean mIsBound = false;
	private Location mLastLocation = null;
	protected ActivityHelper mActivityHelper = null;
	protected DetachableResultReceiver mReceiver = null;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_screen);
		mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				mBoundService = ((LocationService.LocationServiceBinder) service)
						.getService();
				mBoundService.addListener(AbstractPreferencesActivity.this);
			}

			public void onServiceDisconnected(ComponentName className) {
				mBoundService = null;
			}
		};
		mReceiver = DetachableResultReceiver.getInstance(new Handler());
		mActivityHelper = new ActivityHelper(this);
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
		SharedPreferences preferences = getPreferenceScreen()
				.getSharedPreferences();
		if (preferences.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, true)) {
			doBindService();
		}
	}

	@Override
	protected void onPause() {
		mReceiver.clearReceiver();
		super.onPause();
	}

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
			// Log.i("OpenBike", "Location changed");
			if (sharedPreferences.getBoolean(
					AbstractPreferencesActivity.LOCATION_PREFERENCE, true)) {
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
			mBoundService.removeListener(this);
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
	
	/* (non-Javadoc)
	 * @see fr.openbike.service.ILocationServiceListener#onLocationProvidersChanged(int)
	 */
	@Override
	public void onLocationProvidersChanged(int id) {
		showDialog(id);
	}
	
	@Override 
	public void onStationsUpdated() {
		//NOthing to do
	}

}
