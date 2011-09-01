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
package fr.openbike.android.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import fr.openbike.android.R;

public class LocationService extends Service implements LocationListener {

	private List<ILocationServiceListener> listeners = null;
	public static final int DISTANCE_UNAVAILABLE = -1;
	public static final int MINIMUM_DISTANCE_NETWORK = 50;
	public static final int MINIMUM_DISTANCE_GPS = 10;
	public static final int MINIMUM_ELAPSED_NETWORK = 5000;
	public static final int MINIMUM_ELAPSED_GPS = 5000;
	private boolean mIsGpsUsed = false;
	private boolean mIsNetworkAvailable = true;
	private boolean mIsGpsAvailable = true;
	private boolean mAskForGps = true;
	private boolean mAskForLocation = true;
	private boolean mIsInPause = true;
	private LocationManager mLocationManager = null;
	private Location mLastFix = null;
	private final IBinder mBinder = new LocationBinder();
	private Runnable mStopServiceRunnable = null;
	private Handler mDelayedHandler = null;

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocationBinder extends Binder {
		public LocationService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return LocationService.this;
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public void onCreate() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				enableMyLocation();
			}
		}).run();
		listeners = new ArrayList<ILocationServiceListener>();
		mStopServiceRunnable = new Runnable() {

			@Override
			public void run() {
				stopSelf();
			}
		};
		mDelayedHandler = new Handler();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// Ajout d'un listener
	public void addListener(ILocationServiceListener listener) {
		listeners.add(listener);
		listener.onLocationChanged(mLastFix, true);
		mDelayedHandler.removeCallbacks(mStopServiceRunnable);
		if (!mIsGpsAvailable && !mIsNetworkAvailable && mAskForLocation) {
			listener.onLocationProvidersChanged(R.id.no_location_provider);
			mAskForLocation = false;
		} else if (!mIsGpsAvailable && mIsNetworkAvailable && mAskForGps) {
			listener.onLocationProvidersChanged(R.id.enable_gps);
			mAskForGps = false;
		}
	}

	// Suppression d'un listener
	public void removeListener(ILocationServiceListener listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			mDelayedHandler.postDelayed(mStopServiceRunnable, 2000);
		}
	}

	// Notification des listeners
	private void fireLocationChanged(Location l) {
		for (ILocationServiceListener listener : listeners) {
			listener.onLocationChanged(l, false);
		}
	}

	private void fireShowNoLocationProvider() {
		int size = listeners.size();
		if (size != 0) {
			ILocationServiceListener listener = listeners.get(size - 1);
			listener.onLocationProvidersChanged(R.id.no_location_provider);
			mAskForLocation = false;
		}
	}

	private void fireShowAskForGps() {
		int size = listeners.size();
		if (size != 0) {
			ILocationServiceListener listener = listeners.get(size - 1);
			listener.onLocationProvidersChanged(R.id.enable_gps);
			mAskForGps = false;
		}
	}

	@Override
	public void onDestroy() {
		disableMyLocation();
		this.listeners.clear();
	}

	public boolean isGpsEnabled() {
		return mIsGpsAvailable;
	}

	public boolean isProviderEnabled() {
		return mIsGpsAvailable || mIsNetworkAvailable;
	}

	public boolean isLocationAvailable() {
		return mLastFix != null;
	}

	public synchronized void enableMyLocation() {
		if (!mIsInPause)
			return;
		mIsInPause = false;
		List<String> providers = mLocationManager.getProviders(false);
		if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			// Log.i("OpenBike", "Updates for Network provider");
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, MINIMUM_ELAPSED_NETWORK,
					MINIMUM_DISTANCE_NETWORK, this);
		}
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			// Log.i("OpenBike", "Updater for GPS provider");
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, MINIMUM_ELAPSED_GPS,
					MINIMUM_DISTANCE_GPS, this);
		}
	}

	public synchronized void disableMyLocation() {
		if (mIsInPause)
			return;
		mIsInPause = true;
		mLocationManager.removeUpdates(this);
		mIsGpsUsed = false;
		mIsNetworkAvailable = true;
		mIsGpsAvailable = true;
		// Log.e("OpenBike", "Location provider On Pause");
	}

	public Location getMyLocation() {
		return mLastFix;
	}

	@Override
	public synchronized void onLocationChanged(Location location) {
		// Because we stop updates as often as possible, when
		// we switch from map to list, a new location is triggered
		// so check if it's not the same
		if (mLastFix != null
				&& mLastFix.distanceTo(location) < (mIsGpsUsed ? MINIMUM_DISTANCE_GPS
						: MINIMUM_DISTANCE_NETWORK))
			return;
		if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			Log.i("OpenBike", "GPS Fix" + location);
			mLastFix = location;
			fireLocationChanged(location);
			mIsGpsUsed = true;
		} else if (location.getProvider().equals(
				LocationManager.NETWORK_PROVIDER)
				&& !mIsGpsUsed) {
			Log.i("OpenBike", "Network Fix " + location);
			if (mLastFix == null || !mIsGpsUsed) {
				mLastFix = location;
				fireLocationChanged(location);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// Log.i("OpenBike", "onProviderDisabled " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			mIsGpsAvailable = false;
			mIsGpsUsed = false;
		} else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
			mIsNetworkAvailable = false;
		}
		if (!mIsNetworkAvailable && !mIsGpsAvailable) {
			mLastFix = null;
			fireLocationChanged(null);
		}
		if (!mIsGpsAvailable && !mIsNetworkAvailable && mAskForLocation) {
			fireShowNoLocationProvider();
		} else if (!mIsGpsAvailable && mIsNetworkAvailable && mAskForGps) {
			fireShowAskForGps();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		// Log.i("OpenBike", "onProviderEnabled : " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			mIsGpsAvailable = true;
			mAskForGps = true;
			mAskForLocation = true;
		}
		if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
			mIsNetworkAvailable = true;
			mAskForLocation = true;
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}