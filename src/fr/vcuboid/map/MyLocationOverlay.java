package fr.vcuboid.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class MyLocationOverlay extends Overlay {
	private final MapView mapView;
	private Runnable runOnFirstFix = null;
	private GeoPoint mGeoPoint = null;
	private float mAccuracy = 0;
	private Paint paint = new Paint();

	public MyLocationOverlay(Context context, MapView mapView) {
		// this.context = context;
		Log.e("Vcuboid", "MyLocationOverlay");
		this.mapView = mapView;
	}

	public void setCurrentLocation(Location location) {
		if (location == null) {
			mGeoPoint = null;
			mAccuracy = 0;
		} else {
			mGeoPoint = new GeoPoint((int) (location.getLatitude() * 1E6),
					(int) (location.getLongitude() * 1E6));
			mAccuracy = location.getAccuracy();
		}
	}

	@Override
	public synchronized boolean draw(Canvas canvas, MapView mapView,
			boolean shadow, long when) {
		if (!shadow && mGeoPoint != null) {
			Projection p = mapView.getProjection();
			float accuracy = p.metersToEquatorPixels(mAccuracy);
			Point loc = p.toPixels(mGeoPoint, null);
			paint.setAntiAlias(true);
			paint.setColor(Color.BLUE);
			if (accuracy > 10.0f) {
				paint.setAlpha(50);
				canvas.drawCircle(loc.x, loc.y, accuracy, paint);
			}
			paint.setAlpha(255);
			canvas.drawCircle(loc.x, loc.y, 10, paint);
		}
		return false;
	}
}