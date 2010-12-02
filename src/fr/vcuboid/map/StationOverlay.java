package fr.vcuboid.map;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import fr.vcuboid.BalloonOverlayView;
import fr.vcuboid.R;
import fr.vcuboid.utils.Utils;

public class StationOverlay extends Overlay {

	static private Bitmap mMarker = null;
	static private int mMarkerHeight = 0;
	static private int mMarkerWidth = 0;
	static private MapView mMapView;
	static private BalloonOverlayView balloonView;
	static MapController mMc;
	static private View mClickRegion;
	private GeoPoint mPoint = null;
	private int mBikes = 0;
	private int mSlots = 0;
	private int mDistance = 0;
	private int mId;
	public boolean isCurrent = false;

	public int getDistance() {
		return mDistance;
	}

	public void setdistance(int distance) {
		this.mDistance = distance;
	}

	public int getId() {
		return mId;
	}

	public StationOverlay(int id, int latitude, int longitude, int bikes,
			int slots, int distance) {
		mId = id;
		mPoint = new GeoPoint(latitude, longitude);
		mBikes = bikes;
		mSlots = slots;
		mDistance = distance;
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
			int latMin = latCenter - (latSpan / 2);
			int longMax = longCenter + (longSpan / 2);
			int longMin = longCenter - (longSpan / 2);
			Projection projection = mapView.getProjection();
			longMin += projection.fromPixels(mMarkerWidth, 0).getLongitudeE6();
			int stationLon = mPoint.getLongitudeE6();
			int stationLat = mPoint.getLatitudeE6();
			if (stationLat < latMin || stationLat > latMax
			|| stationLon < longMin || stationLon> longMax) { 
				return;
			}
			Point out = new Point();
			projection.toPixels(mPoint, out);
			Paint p1 = new Paint();
			p1.setAntiAlias(true);
			p1.setTextSize(15);
			p1.setTextAlign(Align.RIGHT);
			p1.setColor(Color.WHITE);
			p1.setTypeface(Typeface.DEFAULT_BOLD);
			out.y -= mMarkerHeight;
			canvas.drawBitmap(mMarker, out.x, out.y, null);
			canvas.drawText(String.valueOf(mBikes), out.x + 20, out.y + 20, p1);
			canvas.drawText(String.valueOf(mSlots), out.x + 20, out.y + 35, p1);
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
		projection.toPixels(mPoint, marker);
		if (touched.x >= marker.x && touched.x <= marker.x + mMarkerWidth
				&& touched.y <= marker.y
				&& touched.y >= marker.y - mMarkerHeight) {
			boolean isRecycled;
			if (balloonView == null) {
				balloonView = new BalloonOverlayView(mapView.getContext(),
						mMarkerWidth, mMarkerHeight);
				mClickRegion = (View) balloonView
						.findViewById(R.id.balloon_inner_layout);
				isRecycled = false;
			} else {
				isRecycled = true;
			}
			balloonView.setVisibility(View.GONE);
			List<Overlay> mapOverlays = mapView.getOverlays();
			Log.e("Vcuboid2", "Removing ballons");
			// Debuging only -------------------------------------
			if (mapOverlays.get(mapOverlays.size() - 1) instanceof MyCustomLocationOverlay) {
				Log.e("Vcuboid2", "MyPosition OK");
			}
			if (!(mapOverlays.get(mapOverlays.size() - 2) instanceof StationOverlay)) {
				Log.e("Vcuboid2", "First Station OK, baloon : "
						+ ((StationOverlay) (mapOverlays
								.get(mapOverlays.size() - 2))).isCurrent);

			}
			// --------------------------------------------------
			hideOtherBalloons(mapOverlays);
			isCurrent = true;
			Utils.sortStations(mapOverlays);
			Collections.reverse(mapOverlays);
			Log.e("Vcuboid2", "After adding ballons");
			if (mapOverlays.get(mapOverlays.size() - 1) instanceof MyCustomLocationOverlay) {
				Log.e("Vcuboid2", "MyPosition OK");
			}
			if (!(mapOverlays.get(mapOverlays.size() - 2) instanceof StationOverlay)) {
				Log.e("Vcuboid2", "First Station OK");
			}
			balloonView.setData(null, mDistance);
			MapView.LayoutParams params = new MapView.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					mPoint, MapView.LayoutParams.BOTTOM_CENTER);
			params.mode = MapView.LayoutParams.MODE_MAP;
			setBalloonTouchListener(mId);
			balloonView.setVisibility(View.VISIBLE);

			if (isRecycled) {
				balloonView.setLayoutParams(params);
			} else {
				mapView.addView(balloonView, params);
			}

			mMc.animateTo(mPoint);

			return true;
		}
		return false;
	}

	public void setBalloonBottomOffset(int pixels) {
		// viewOffset = pixels;
	}

	protected boolean onBalloonTap(int index) {
		return false;
	}

	private void hideBalloon() {
		isCurrent = false;
		if (balloonView != null) {
			balloonView.setVisibility(View.GONE);
		}
	}

	private void hideOtherBalloons(List<Overlay> overlays) {
		int baloonPosition = overlays.size() - 2;
		Overlay overlay = overlays.get(baloonPosition);
		if (overlay instanceof StationOverlay) {
			((StationOverlay) overlay).hideBalloon();
		}
	}

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

	public static void setMarker(Bitmap marker) {
		mMarker = marker;
		mMarkerHeight = marker.getHeight();
		mMarkerWidth = marker.getWidth();
	}

	public static void setMapView(MapView mapview) {
		mMapView = mapview;
		mMc = mMapView.getController();
	}
}
