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

import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import fr.vcuboid.object.Station;
import fr.vcuboid.utils.Utils;

public class StationOverlay extends Overlay {

	static private Bitmap mMarker = null;
	static private int mMarkerHeight = 0;
	static private int mMarkerWidth = 0;
	static private MapView mMapView;
	static private List<Overlay> mMapOverlays;
	static public BalloonOverlayView balloonView;
	static MapController mMc;
	private Station mStation;
	
	public Station getStation() {
		return mStation;
	}

	public boolean isCurrent = false;
	
	public StationOverlay(Station station) {
		mStation = station;
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
			int longMin = projection.fromPixels(-mMarkerWidth, 0).getLongitudeE6();
			int latMin = projection.fromPixels(0, mMapView.getHeight() + mMarkerHeight).getLatitudeE6();
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
			canvas.drawText(String.valueOf(mStation.getBikes()), out.x + 20, out.y + 20, p1);
			canvas.drawText(String.valueOf(mStation.getSlots()), out.x + 20, out.y + 35, p1);
			p1.setTextAlign(Align.LEFT);
			p1.setTextSize(12);
			canvas.drawText("Vélos", out.x + 25, out.y + 20, p1);
			canvas.drawText("Places", out.x + 25, out.y + 35, p1);
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
			boolean isRecycled;
			if (balloonView == null) {
				balloonView = new BalloonOverlayView(mapView.getContext(),
						mMarkerWidth, mMarkerHeight);
				isRecycled = false;
			} else {
				isRecycled = true;
			}
			hideOtherBalloons(mMapOverlays);
			isCurrent = true;
			if (mStation.getDistance() !=-1)
				Utils.sortStationsByDistance(mMapOverlays);
			else
				Utils.sortStationsByName(mMapOverlays);
			Collections.reverse(mMapOverlays);
			balloonView.setData(null, mStation.getDistance());
			MapView.LayoutParams params = new MapView.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					mStation.getGeoPoint(), MapView.LayoutParams.BOTTOM_CENTER);
			params.mode = MapView.LayoutParams.MODE_MAP;
			//setBalloonTouchListener(mId);
			balloonView.setVisibility(View.VISIBLE);
			if (isRecycled) {
				balloonView.setLayoutParams(params);
			} else {
				mapView.addView(balloonView, params);
			}
			mMc.animateTo(mStation.getGeoPoint());
			return true;
		} else {
			if (this == mMapOverlays.get(0))
				hideOtherBalloons(mMapOverlays);
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
		isCurrent = false;
		if (balloonView != null) {
			balloonView.setVisibility(View.GONE);
		}
	}

	private void hideOtherBalloons(List<Overlay> overlays) {
		int baloonPosition = overlays.size() - (mStation.getDistance() !=-1 ? 2 : 1);
		Overlay overlay = overlays.get(baloonPosition);
		if (overlay instanceof StationOverlay) {
			((StationOverlay) overlay).hideBalloon();
		} else {
			Log.e("Balloon", "hideOtherBalloons, before last not a StationOverlay");
		}
	}
/*
	private void setBalloonTouchListener(final int thisIndex) {

		try {
			@SuppressWarnings("unused")
			Method m = this.getClass().getDeclaredMethod("onBalloonTap",
					int.class);

			mClickRegion.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {

					View l = ((View) v.getParent())
							.findViewById(R.id.balloon_main_layout);
					Drawable d = l.getBackground();

					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						int[] states = { android.R.attr.state_pressed };
						if (d.setState(states)) {
							d.invalidateSelf();
						}
						return true;
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						int newStates[] = {};
						if (d.setState(newStates)) {
							d.invalidateSelf();
						}
						// call overridden method
						onBalloonTap(thisIndex);
						return true;
					} else {
						return false;
					}

				}
			});

		} catch (SecurityException e) {
			Log.e("BalloonItemizedOverlay",
					"setBalloonTouchListener reflection SecurityException");
			return;
		} catch (NoSuchMethodException e) {
			// method not overridden - do nothing
			return;
		}
	}
*/
	public static void setMarker(Bitmap marker) {
		mMarker = marker;
		mMarkerHeight = marker.getHeight();
		mMarkerWidth = marker.getWidth();
	}

	public static void setMapView(MapView mapview) {
		mMapView = mapview;
		mMc = mapview.getController();
		mMapOverlays = mapview.getOverlays();
	}
}
