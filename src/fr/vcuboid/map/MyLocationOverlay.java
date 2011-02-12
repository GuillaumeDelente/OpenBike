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
package fr.vcuboid.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import fr.vcuboid.R;

public class MyLocationOverlay extends Overlay {
	private Bitmap mMarker;
	private float mShiftX = 0;
	private float mShiftY = 0;
	private GeoPoint mGeoPoint = null;
	private float mAccuracy = 0;
	private Paint paint = new Paint();

	public MyLocationOverlay(Context context, MapView mapView) {
		Log.e("Vcuboid", "MyLocationOverlay");
		mMarker = BitmapFactory.decodeResource(context.getResources(), 
				R.drawable.ic_maps_indicator_current_position);
		mShiftX = mMarker.getWidth() / 2;
		mShiftY = mMarker.getHeight() / 2;
		paint.setAntiAlias(true);
	}

	public boolean isMyLocationDrawn() {
		return mGeoPoint != null;
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
			if (accuracy > 10.0f) {
				paint.setColor(Color.BLUE);
				paint.setAlpha(50);
				canvas.drawCircle(loc.x, loc.y, accuracy, paint);
			}
			paint.setColor(Color.TRANSPARENT);
			paint.setAlpha(255);
			canvas.drawBitmap(mMarker, loc.x - mShiftX, loc.y - mShiftY, paint);
		}
		return false;
	}
}