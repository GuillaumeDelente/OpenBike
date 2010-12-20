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
package fr.vcuboid;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class MyLocationProvider implements LocationListener {

	private boolean mIsGpsUsed = false;
	private boolean mIsNetworkUsed = false;
	private boolean mAskForGps = true;
	private boolean mIsInPause = true;
	private VcuboidManager mVcuboidManager = null;
	private LocationManager mLocationManager = null;
	private Runnable mRunOnFirstFix = null;
	private Location mLastFix = null;

	public MyLocationProvider(Context context, VcuboidManager vcuboidManager) {
		// this.context = context;
		Log.e("Vcuboid", "MyLocationProvider");
		mLocationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		mVcuboidManager = vcuboidManager;
		enableMyLocation();
	}

	public boolean isAskForGps() {
		Log.e("Vcuboid", "MyLocationProvider ask For GPS : " + mAskForGps);
		return mAskForGps;
	}
	
	public void setAskForGps(boolean ask) {
		Log.e("Vcuboid", "Set ask For GPS : " + ask);
		mAskForGps = ask;
	}
	
	public boolean isLocationAvailable() {
		return mLastFix != null;
	}
	
	public boolean areProvidersAvailable() {
		return mIsGpsUsed || mIsNetworkUsed;
	}

	public synchronized void enableMyLocation() {
		if (!mIsInPause)
			return;
		Log.e("Vcuboid", "MyLocationProvider : enable location");
		mIsInPause = false;
		List<String> providers = mLocationManager.getProviders(false);
		if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			Log.e("Vcuboid", "Updates for Network provider");
				mLocationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 5000, 20, this);
		}
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			Log.e("Vcuboid", "Updater for GPS provider");
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 30000, 20, this);
		}
	}

	public synchronized void disableMyLocation() {
		if (mIsInPause)
			return;
		mIsInPause = true;
		mLocationManager.removeUpdates(this);
		mIsGpsUsed = mIsNetworkUsed = false;
		Log.e("Vcuboid", "Location provider On Pause");
	}

	public Location getMyLocation() {
		return mLastFix;
	}
	
	@Override
	public synchronized void onLocationChanged(Location location) {
		if (mRunOnFirstFix != null) {
			mRunOnFirstFix.run();
			mRunOnFirstFix = null;
		}
		if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			Log.e("Vcuboid", "GPS Fix");
			if (mLastFix == null || !mIsNetworkUsed ||
					(location.getAccuracy() < mLastFix.getAccuracy())) {
				Log.e("Vcuboid", "is first or the most accurate");
				mLastFix = location;
				mVcuboidManager.onLocationChanged(
						location);
			}
		} else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
			Log.e("Vcuboid", "Network Fix");
			if (mLastFix == null || !mIsGpsUsed ||
					(location.getAccuracy() < mLastFix.getAccuracy())) {
				Log.e("Vcuboid", "is first or the most accurate, accuracy : " + location.getAccuracy());
				mLastFix = location;
				mVcuboidManager.onLocationChanged(
						location);
			}
		}
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		Log.e("Vcuboid", "onProviderDisabled " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			if (mAskForGps) {
				mVcuboidManager.showAskForGps();
				mAskForGps = false;
			}
			if (mIsNetworkUsed) {
				mIsGpsUsed = false;
			} else {
				setNoProvidersAvailable();
			}
		} else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
			if (mIsGpsUsed) {
				mIsNetworkUsed = false;
			} else {
				setNoProvidersAvailable();
			}
		}
	}
	
	private void setNoProvidersAvailable() {
		mIsNetworkUsed = mIsGpsUsed = false;
		mLastFix = null;
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.e("Vcuboid", "onProviderEnabled : " + provider);
		setAskForGps(true);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			mIsGpsUsed = true;
			mAskForGps = false;
		} else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
			mIsNetworkUsed = true;
		}
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	public synchronized boolean runOnFirstFix(Runnable runnable) {
		if (mLastFix == null) {
			mRunOnFirstFix = runnable;
			return false;
		} else {
			runnable.run();
			return true;
		}
	}
}
