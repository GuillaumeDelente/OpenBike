package fr.vcuboid;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class MyLocationProvider  implements LocationListener {
	
	private boolean myLocationEnabled;
	private LocationManager locationManager;
	private Runnable runOnFirstFix = null;
	private Location lastFix = null;

	public MyLocationProvider(Context context) {
		// this.context = context;
		Log.e("Vcuboid", "MyLocationOverlay");
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
	}

	public boolean isMyLocationEnabled() {
		return myLocationEnabled;
	}

	public synchronized boolean enableMyLocation() {
		Log.e("Vcuboid", "MyLocationOverlay : enable location");
		List<String> providers = locationManager.getProviders(true);
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			Log.e("Vcuboid", "GPS provider");
			myLocationEnabled = true;
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0L, 0L, this);
		} else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			Log.e("Vcuboid", "Network provider");
			myLocationEnabled = true;
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 10000, 0, this);
		} else {
			myLocationEnabled = false;
		}
		return myLocationEnabled;
	}

	public synchronized void disableMyLocation() {
		if (myLocationEnabled)
			locationManager.removeUpdates(this);
		myLocationEnabled = false;
		lastFix = null;
	}

	public Location getMyLocation() {
		return lastFix;
	}

	public synchronized void onLocationChanged(Location location) {
		lastFix = location;
		if (runOnFirstFix != null) {
			runOnFirstFix.run();
			runOnFirstFix = null;
		}
		VcuboidManager.getVcuboidManagerInstance().onLocationChanged(location);
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public synchronized boolean runOnFirstFix(Runnable runnable) {
		if (lastFix == null) {
			runOnFirstFix = runnable;
			return false;
		} else {
			runnable.run();
			return true;
		}
	}

	public void onAccuracyChanged(int sensor, int accuracy) {
	}

	protected boolean dispatchTap() {
		return false;
	}

}
