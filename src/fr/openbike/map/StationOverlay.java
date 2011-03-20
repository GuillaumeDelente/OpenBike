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
			int latCenter = mapCenter.getLatitudeE6();
			int longCenter = mapCenter.getLongitudeE6();
			int latSpan = mapView.getLatitudeSpan();
			int longSpan = mapView.getLongitudeSpan();
			int latMax = latCenter + (latSpan / 2);
			int longMax = longCenter + (longSpan / 2);
			Projection projection = mapView.getProjection();
			int longMin = projection.fromPixels(-mMarkerWidth, 0)
					.getLongitudeE6();
			int latMin = projection.fromPixels(0,
					mMapView.getHeight() + mMarkerHeight).getLatitudeE6();
			int stationLon = mStation.getGeoPoint().getLongitudeE6();
			int stationLat = mStation.getGeoPoint().getLatitudeE6();
			if (stationLat < latMin || stationLat > latMax
					|| stationLon < longMin || stationLon > longMax) {
				return;
			}
			Point out = new Point();
			projection.toPixels(mStation.getGeoPoint(), out);
			Paint p1 = new Paint();
			p1.setAntiAlias(true);
			p1.setTextSize(15);
			p1.setTextAlign(Align.RIGHT);
			p1.setColor(Color.WHITE);
			p1.setTypeface(Typeface.DEFAULT_BOLD);
			out.y -= mMarkerHeight;
			canvas.drawBitmap(mMarker, out.x, out.y, null);
			canvas.drawText(String.valueOf(mStation.getBikes()), out.x + 20,
					out.y + 20, p1);
			canvas.drawText(String.valueOf(mStation.getSlots()), out.x + 20,
					out.y + 35, p1);
			p1.setTextAlign(Align.LEFT);
			p1.setTextSize(12);
			canvas.drawText(mStation.getBikes() > 1 ? BIKES : BIKE, out.x + 25, out.y + 20, p1);
			canvas.drawText(mStation.getBikes() > 1 ? SLOTS : SLOT, out.x + 25, out.y + 35, p1);
		}
		super.draw(canvas, mapView, shadow);
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		Point touched = new Point();
		Point marker = new Point();
		Projection projection = mapView.getProjection();
		projection.toPixels(p, touched);
		projection.toPixels(mStation.getGeoPoint(), marker);
		if (touched.x >= marker.x && touched.x <= marker.x + mMarkerWidth
				&& touched.y <= marker.y
				&& touched.y >= marker.y - mMarkerHeight) {
			//Log.d("OpenBike", "station");
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