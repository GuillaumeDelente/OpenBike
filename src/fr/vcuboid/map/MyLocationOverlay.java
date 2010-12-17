package fr.vcuboid.map;

import java.util.List;

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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.VcuboidManager;

public class MyLocationOverlay extends Overlay implements LocationListener {
	private final MapView mapView;
	private boolean myLocationEnabled;
	private LocationManager locationManager;
	private Runnable runOnFirstFix = null;
	private Location lastFix = null;
	private Paint paint = new Paint();

	public MyLocationOverlay(Context context, MapView mapView) {
		// this.context = context;
		Log.e("Vcuboid", "MyLocationOverlay");
		this.mapView = mapView;
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

	@Override
	public synchronized boolean draw(Canvas canvas, MapView mapView,
			boolean shadow, long when) {
		if (!shadow) {
			if (isMyLocationEnabled() && lastFix != null) {
				drawMyLocation(canvas, mapView, lastFix, getMyLocation(), when);
			}
		}
		return false;
	}

	protected void drawMyLocation(Canvas canvas, MapView mapView,
			Location lastFix, GeoPoint myLocation, long when) {
		Projection p = mapView.getProjection();
		float accuracy = p.metersToEquatorPixels(lastFix.getAccuracy());
		Point loc = p.toPixels(myLocation, null);
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		if (accuracy > 10.0f) {
			paint.setAlpha(50);
			canvas.drawCircle(loc.x, loc.y, accuracy, paint);
		}
		paint.setAlpha(255);
		canvas.drawCircle(loc.x, loc.y, 10, paint);

	}

	public Location getLastFix() {
		return lastFix;
	}

	public GeoPoint getMyLocation() {
		if (lastFix == null)
			return new GeoPoint(0, 0);
		return new GeoPoint((int) (lastFix.getLatitude() * 1E6), (int) (lastFix
				.getLongitude() * 1E6));
	}

	public synchronized void onLocationChanged(Location location) {
		lastFix = location;
		if (runOnFirstFix != null) {
			runOnFirstFix.run();
			runOnFirstFix = null;
		}
		mapView.invalidate();
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