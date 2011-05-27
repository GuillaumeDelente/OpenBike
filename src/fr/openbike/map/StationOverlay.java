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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import fr.openbike.object.MinimalStation;
import fr.openbike.utils.Utils;

public class StationOverlay extends Overlay {

	static private List<Overlay> mMapOverlays;
	static private BalloonOverlayView2 mBalloonView;
	static MapController mMc;
	static private Paint paint = new Paint();
	static private Point point1 = new Point();
	static private Point point2 = new Point();
	static private RectF rectf = new RectF();

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
			Projection projection = mapView.getProjection();
			projection.toPixels(mStation.getGeoPoint(), point1);
			paint.setStyle(Paint.Style.FILL);

			int bikes = mStation.getBikes();
			int slots = mStation.getSlots();

			if (bikes == 0) {
				paint.setColor(Color.BLACK);
			} else if (bikes < 3) {
				paint.setColor(Color.RED);
			} else {
				paint.setColor(Color.GREEN);
			}
			paint.setAlpha(150);

			bikes = 5 + bikes; // Half size of a Semi circle size
			rectf.set(point1.x - bikes, point1.y - bikes, point1.x + bikes,
					point1.y + bikes);
			canvas.drawArc(rectf, 90, 180, true, paint);
			paint.setStyle(Style.STROKE);
			paint.setColor(Color.WHITE);
			canvas.drawArc(rectf, 90, 180, true, paint);

			paint.setStyle(Paint.Style.FILL);

			if (slots == 0) {
				paint.setColor(Color.BLACK);
			} else if (slots < 4) {
				paint.setColor(Color.RED);
			} else {
				paint.setColor(Color.GREEN);
			}
			if (mStation.getSlots() == 0) {
				paint.setColor(Color.BLACK);
			}
			paint.setAlpha(150);

			slots = 5 + slots; // Half size of a Semi circle size
			rectf.set(point1.x - slots, point1.y - slots, point1.x + slots,
					point1.y + slots);
			canvas.drawArc(rectf, 270, 180, true, paint);
			paint.setStyle(Style.STROKE);
			paint.setColor(Color.WHITE);
			canvas.drawArc(rectf, 270, 180, true, paint);

		}
	}

	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		Projection projection = mapView.getProjection();
		projection.toPixels(p, point1);
		projection.toPixels(mStation.getGeoPoint(), point2);
		// point1 : touched
		// point2 : station
		// FIXME replace by true values
		if (point1.x >= point2.x - 30 / 2 && point1.x <= point2.x + 30 / 2
				&& point1.y <= point2.y && point1.y >= point2.y - 60) {
			boolean isRecycled;
			if (mBalloonView == null) {
				mBalloonView = new BalloonOverlayView2(mapView.getContext(), 0,
						0);
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
		// Log.i("OpenBike", "hideBalloon " + mStation.getId());
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
		// Log.i("OpenBike", "Hide balloon without Station");
		if (mBalloonView != null) {
			mBalloonView.disableListeners();
			mBalloonView.setVisibility(View.GONE);
		}
	}

	public void refreshBalloon() {
		// Log.i("OpenBike", "Refreshing Balloon");
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
				- (mMapOverlays.get(size - 1) instanceof MyLocationOverlay ? 2
						: 1);
		Overlay overlay = mMapOverlays.get(baloonPosition);
		if (overlay instanceof StationOverlay) {
			((StationOverlay) overlay).hideBalloon();
		} else {
		}
	}

	public BalloonOverlayView2 getBallonView() {
		return mBalloonView;
	}

	static public void setBalloonView(BalloonOverlayView2 balloon) {
		mBalloonView = balloon;
	}

	public void setCurrent() {
		mIsCurrent = true;
	}

	public static void init(Context context, MapView mapView) {
		mMc = mapView.getController();
		mMapOverlays = mapView.getOverlays();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1);
	}
}