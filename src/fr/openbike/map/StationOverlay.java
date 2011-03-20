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
package fr.openbike.map;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import fr.openbike.R;
import fr.openbike.object.MinimalStation;
import fr.openbike.utils.Utils;

public class StationOverlay extends Overlay {

	static private Bitmap mMarker = null;
	static private int mMarkerHeight = 0;
	static private int mMarkerWidth = 0;
	static private MapView mMapView;
	static private List<Overlay> mMapOverlays;
	static private BalloonOverlayView mBalloonView;
	static private String BIKES;
	static private String BIKE;	
	static private String SLOTS;
	static private String SLOT;
	static MapController mMc;
	static private Paint paint = new Paint();
	static private Point point1 = new Point();
	static private Point point2 = new Point();
	
	static private int latCenter;
	static private int longCenter;
	static private int latSpan;
	static private int longSpan;
	static private int latMax;
	static private int longMax;
	static private int longMin;
	static private int latMin;
	static private int stationLon;
	static private int stationLat;
	
	private MinimalStation mStation;
	private boolean mIsCurrent = false;
	
	public MinimalStation getStation() {
		return mStation;
	}

	public StationOverlay(MinimalStation station) {
		mStation = station;
	}
	
	public StationOverlay(MinimalStation station, boolean isCurrent) {
		mStation = station;
		mIsCurrent = isCurrent;
	}

	public boolean isCurrent() {
		return mIsCurrent;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (!shadow) {
			GeoPoint mapCenter = mapView.getMapCenter();
			latCenter = mapCenter.getLatitudeE6();
			longCenter = mapCenter.getLongitudeE6();
			latSpan = mapView.getLatitudeSpan();
			longSpan = mapView.getLongitudeSpan();
			latMax = latCenter + (latSpan / 2);
			longMax = longCenter + (longSpan / 2);
			Projection projection = mapView.getProjection();
			longMin = projection.fromPixels(-mMarkerWidth, 0)
					.getLongitudeE6();
			latMin = projection.fromPixels(0,
					mMapView.getHeight() + mMarkerHeight).getLatitudeE6();
			stationLon = mStation.getGeoPoint().getLongitudeE6();
			stationLat = mStation.getGeoPoint().getLatitudeE6();
			if (stationLat < latMin || stationLat > latMax
					|| stationLon < longMin || stationLon > longMax) {
				return;
			}
			projection.toPixels(mStation.getGeoPoint(), point1);
			paint.setAntiAlias(true);
			paint.setTextSize(15);
			paint.setTextAlign(Align.RIGHT);
			paint.setColor(Color.WHITE);
			paint.setTypeface(Typeface.DEFAULT_BOLD);
			point1.y -= mMarkerHeight;
			canvas.drawBitmap(mMarker, point1.x, point1.y, null);
			canvas.drawText(String.valueOf(mStation.getBikes()), point1.x + 20,
					point1.y + 20, paint);
			canvas.drawText(String.valueOf(mStation.getSlots()), point1.x + 20,
					point1.y + 35, paint);
			paint.setTextAlign(Align.LEFT);
			paint.setTextSize(12);
			canvas.drawText(mStation.getBikes() > 1 ? BIKES : BIKE, point1.x + 25, point1.y + 20, paint);
			canvas.drawText(mStation.getBikes() > 1 ? SLOTS : SLOT, point1.x + 25, point1.y + 35, paint);
		}
		super.draw(canvas, mapView, shadow);
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		Projection projection = mapView.getProjection();
		projection.toPixels(p, point1);
		projection.toPixels(mStation.getGeoPoint(), point2);
		if (point1.x >= point2.x && point1.x <= point2.x + mMarkerWidth
				&& point1.y <= point2.y
				&& point1.y >= point2.y - mMarkerHeight) {
			boolean isRecycled;
			if (mBalloonView == null) {
				mBalloonView = new BalloonOverlayView(mapView.getContext(),
						mMarkerHeight, mMarkerWidth);
				isRecycled = false;
			} else {
				isRecycled = true;
			}
			hideOtherBalloons();
			mIsCurrent = true;
			if (mStation.getDistance() != -1)
				Utils.sortStationsByDistance(mMapOverlays);
			else
				Utils.sortStationsByName(mMapOverlays);
			Collections.reverse(mMapOverlays);
			mBalloonView.setData(mStation);
			MapView.LayoutParams params = new MapView.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					mStation.getGeoPoint(), MapView.LayoutParams.BOTTOM_CENTER);
			params.mode = MapView.LayoutParams.MODE_MAP;
			// setBalloonTouchListener(mId);
			mBalloonView.setVisibility(View.VISIBLE);
			if (isRecycled) {
				mBalloonView.setLayoutParams(params);
			} else {
				mapView.addView(mBalloonView, params);
			}
			mMc.animateTo(mStation.getGeoPoint());
			return true;
		} else {
			if (this == mMapOverlays.get(0))
				hideOtherBalloons();
			return false;
		}
	}

	public void setBalloonBottomOffset(int pixels) {
		// viewOffset = pixels;
	}

	protected boolean onBalloonTap(int index) {
		return false;
	}

	public void hideBalloon() {
		//Log.i("OpenBike", "hideBalloon " + mStation.getId());
		if (mIsCurrent) {
			mIsCurrent = false;
			if (mBalloonView != null) {				
				mBalloonView.disableListeners();
				mBalloonView.setVisibility(View.GONE);
			}
		} else {
		}
	}
	
	public static void hideBalloonWithNoStation() {
		//Log.i("OpenBike", "Hide balloon without Station");
		if (mBalloonView != null) {				
			mBalloonView.disableListeners();
			mBalloonView.setVisibility(View.GONE);
		}
	}

	public void refreshBalloon() {
		//Log.i("OpenBike", "Refreshing Balloon");
		if (mIsCurrent) {
			if (mBalloonView != null) {
				mBalloonView.disableListeners();
				mBalloonView.refreshData(mStation);
				mBalloonView.invalidate();
			}
		}
	}

	private void hideOtherBalloons() {
		int size = mMapOverlays.size();
		int baloonPosition = size
				- (mMapOverlays.get(size - 1) instanceof MyLocationOverlay ? 2 : 1);
		Overlay overlay = mMapOverlays.get(baloonPosition);
		if (overlay instanceof StationOverlay) {
			((StationOverlay) overlay).hideBalloon();
		} else {
		}
	}
	
	public BalloonOverlayView getBallonView() {
		return mBalloonView;
	}
	
	static public void setBalloonView(BalloonOverlayView balloon) {
		mBalloonView = balloon;
	}
	
	public void setCurrent() {
		mIsCurrent = true;
	}

	/*
	 * private void setBalloonTouchListener(final int thisIndex) {
	 * 
	 * try {
	 * 
	 * @SuppressWarnings("unused") Method m =
	 * this.getClass().getDeclaredMethod("onBalloonTap", int.class);
	 * 
	 * mClickRegion.setOnTouchListener(new OnTouchListener() { public boolean
	 * onTouch(View v, MotionEvent event) {
	 * 
	 * View l = ((View) v.getParent()) .findViewById(R.id.balloon_main_layout);
	 * Drawable d = l.getBackground();
	 * 
	 * if (event.getAction() == MotionEvent.ACTION_DOWN) { int[] states = {
	 * android.R.attr.state_pressed }; if (d.setState(states)) {
	 * d.invalidateSelf(); } return true; } else if (event.getAction() ==
	 * MotionEvent.ACTION_UP) { int newStates[] = {}; if (d.setState(newStates))
	 * { d.invalidateSelf(); } // call overridden method
	 * onBalloonTap(thisIndex); return true; } else { return false; }
	 * 
	 * } });
	 * 
	 * } catch (SecurityException e) { //Log.e("BalloonItemizedOverlay",
	 * "setBalloonTouchListener reflection SecurityException"); return; } catch
	 * (NoSuchMethodException e) { // method not overridden - do nothing return;
	 * } }
	 */

	public static void init(Bitmap marker, MapView mapview, Context context) {
		mMarker = marker;
		mMarkerHeight = marker.getHeight();
		mMarkerWidth = marker.getWidth();
		mMapView = mapview;
		mMc = mapview.getController();
		mMapOverlays = mapview.getOverlays();
		BIKES = context.getString(R.string.bikes_);
		BIKE = context.getString(R.string.bike_);
		SLOT = context.getString(R.string.slot_);
		SLOTS = context.getString(R.string.slots_);
	}
}