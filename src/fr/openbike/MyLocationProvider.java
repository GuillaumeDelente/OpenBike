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
package fr.openbike;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class MyLocationProvider implements LocationListener {

	public static final int ENABLE_GPS = -4;
	public static final int NO_LOCATION_PROVIDER = -5;
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
	private OpenBikeManager mOpenBikeManager = null;
	private LocationManager mLocationManager = null;
	private Location mLastFix = null;

	public MyLocationProvider(Context context, OpenBikeManager openBikeManager) {
		// this.context = context;
		//Log.e("OpenBike", "MyLocationProvider");
		mLocationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		mOpenBikeManager = openBikeManager;
		enableMyLocation();
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
		//Log.i("OpenBike", "MyLocationProvider : enable location");
		mIsInPause = false;
		List<String> providers = mLocationManager.getProviders(false);
		if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			//Log.i("OpenBike", "Updates for Network provider");
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, MINIMUM_ELAPSED_NETWORK, MINIMUM_DISTANCE_NETWORK, this);
		}
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			//Log.i("OpenBike", "Updater for GPS provider");
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, MINIMUM_ELAPSED_GPS, MINIMUM_DISTANCE_GPS, this);
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
		//Log.e("OpenBike", "Location provider On Pause");
	}

	public Location getMyLocation() {
		return mLastFix;
	}

	@Override
	public synchronized void onLocationChanged(Location location) {
		// Because we stop updates as often as possible, when
		// we switch from map to list, a new location is triggered
		// so check if it's not the same
		if (mLastFix != null && mLastFix.distanceTo(location) < 
				(mIsGpsUsed ? MINIMUM_DISTANCE_GPS : MINIMUM_DISTANCE_NETWORK))
			return;
		if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			//Log.i("OpenBike", "GPS Fix");
			mLastFix = location;
			mOpenBikeManager.onLocationChanged(location);
			mIsGpsUsed = true;
		} else if (location.getProvider().equals(
				LocationManager.NETWORK_PROVIDER) && !mIsGpsUsed) {
			//Log.i("OpenBike", "Network Fix");
			if (mLastFix == null || !mIsGpsUsed) {
				//Log.i("OpenBike", "is first or the only one");
				mLastFix = location;
				mOpenBikeManager.onLocationChanged(location);
			}
		}
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		//Log.i("OpenBike", "onProviderDisabled " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			mIsGpsAvailable = false;
			mIsGpsUsed = false;
		} else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
			mIsNetworkAvailable = false;
		}
		if (!mIsNetworkAvailable && !mIsGpsAvailable && (mLastFix != null || mAskForLocation)) {
			mLastFix = null;
			mOpenBikeManager.onLocationChanged(null);
			if (mAskForLocation) {
				mOpenBikeManager.showNoLocationProvider();
				mAskForLocation = false;
			}
		} else if (mAskForGps && !mIsGpsAvailable && mIsNetworkAvailable) {
			mOpenBikeManager.showAskForGps();
			mAskForGps = false;
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		//Log.i("OpenBike", "onProviderEnabled : " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			mIsGpsAvailable = true;
		}
		if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
			mIsNetworkAvailable = true;
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
