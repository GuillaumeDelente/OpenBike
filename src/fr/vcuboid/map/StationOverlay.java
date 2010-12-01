package fr.vcuboid.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class StationOverlay extends Overlay {

	static private Bitmap mMarker = null;
	static private int mShiftY = 0;
	static private int mShiftX = 0;
	private GeoPoint mPoint = null;
	private int mLatitude = 0;
	private int mLongitude = 0;
	private int mBikes = 0;
	private int mSlots = 0;
	private int mPos = 0;

	public StationOverlay(int latitude, int longitude, int bikes, int slots, int pos) {
		mPoint = new GeoPoint(latitude, longitude);
		mBikes = bikes;
		mSlots = slots;
		mLatitude = latitude;
		mLongitude = longitude;
		mPos = pos;
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		GeoPoint mapCenter = mapView.getMapCenter();
		int latCenter = mapCenter.getLatitudeE6();
		int longCenter = mapCenter.getLongitudeE6();
		int latSpan = mapView.getLatitudeSpan();
		int longSpan = mapView.getLongitudeSpan();
		int latMax = latCenter + (latSpan / 2);
		int latMin = latCenter - (latSpan / 2);
		int longMax = longCenter + (longSpan / 2);
		int longMin = longCenter - (longSpan / 2);
		if (mLatitude < latMin || mLatitude > latMax || mLongitude < longMin
				|| mLongitude > longMax) {
			return;
		}
		
		Projection projection = mapView.getProjection();
		Point out = new Point();
		Paint p1 = new Paint();
		p1.setAntiAlias(true);
		p1.setTextSize(15);
		p1.setTextAlign(Align.RIGHT);
		p1.setColor(Color.WHITE);
		p1.setTypeface(Typeface.DEFAULT_BOLD);
		projection.toPixels(mPoint, out);
		out.y -= mShiftY;
		canvas.drawBitmap(mMarker, out.x, out.y, null);
		canvas.drawText(String.valueOf(mBikes), out.x + 20, out.y + 20, p1);
		canvas.drawText(String.valueOf(mSlots), out.x + 20, out.y + 35, p1);
		p1.setTextAlign(Align.LEFT);
		p1.setTextSize(12);
		canvas.drawText("Vélos", out.x + 25, out.y + 20, p1);
		canvas.drawText("Places", out.x + 25, out.y + 35, p1);
		super.draw(canvas, mapView, shadow);
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		Point touched = new Point();
		Point marker = new Point();
		Projection projection = mapView.getProjection();
		projection.toPixels(p, touched);
		projection.toPixels(mPoint, marker);
		if (touched.x >= marker.x && 
				touched.x <= marker.x + mShiftX &&
				touched.y <= marker.y && 
				touched.y >= marker.y - mShiftY) {
			Log.e("Vcuboid", "Velos : " + mBikes);
			return true;
		}
		return false;
	}

	public static void setMarker(Bitmap marker) {
		mMarker = marker;
		mShiftY = marker.getHeight();
		mShiftX = marker.getWidth();
	}
}
