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
package fr.openbike.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.provider.BaseColumns;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import fr.openbike.database.OpenBikeDBAdapter;

public class StationsOverlay extends BalloonItemizedOverlay<OverlayItem> {

	private List<StationOverlay> items = new ArrayList<StationOverlay>();
	private MarkerDrawable marker = null;
	private PinDrawable pin = null;
	private static Paint mTextPaint = new Paint();
	private static Paint mPinPaint = new Paint();
	private static boolean mDrawText;

	int IMAGE_HEIGHT;

	public StationsOverlay(Resources resources, BitmapDrawable drawable,
			MapView mv, Context context) {
		super(drawable, mv, context);
		this.marker = new MarkerDrawable(resources, drawable.getBitmap());
		boundCenterBottom(marker);
		pin = new PinDrawable();
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(15);
		mTextPaint.setTextAlign(Align.RIGHT);
		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		IMAGE_HEIGHT = marker.getIntrinsicHeight();
		populate();
	}

	public void setItems(ArrayList<StationOverlay> list) {
		items = list;
		setLastFocusedIndex(-1);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return (items.get(i));
	}

	public List<StationOverlay> getOverlayList() {
		return items;
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (!shadow) {
			if (mapView.getZoomLevel() >= 16) {
				mDrawText = true;
				setBalloonBottomOffset(IMAGE_HEIGHT);
			} else {
				mDrawText = false;
				setBalloonBottomOffset(0);
			}
			super.draw(canvas, mapView, shadow);
		}
	}

	@Override
	public int size() {
		return (items.size());
	}

	public ArrayList<StationOverlay> getOverlaysFromCursor(
			Cursor stationsCursor, Location location, int distanceFilter) {
		ArrayList<StationOverlay> overlays = new ArrayList<StationOverlay>(
				stationsCursor.getCount());
		if (!stationsCursor.moveToFirst())
			return overlays;
		int latitudeColumn = stationsCursor
				.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE);
		int longitudeColumn = stationsCursor
				.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE);
		int bikesColumn = stationsCursor
				.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES);
		int slotsColumn = stationsCursor
				.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS);
		int idColumn = stationsCursor.getColumnIndex(BaseColumns._ID);
		StationOverlay overlay;
		Location stationLocation = new Location("");
		float distance = 0;
		do {
			overlay = new StationOverlay(new GeoPoint(stationsCursor
					.getInt(latitudeColumn), stationsCursor
					.getInt(longitudeColumn)), String.valueOf(stationsCursor
					.getInt(bikesColumn)), String.valueOf(stationsCursor
					.getInt(slotsColumn)), stationsCursor.getInt(idColumn));
			if (distanceFilter != 0 && location != null) {
				stationLocation.setLatitude(((double) stationsCursor
						.getInt(latitudeColumn)) * 1E-6);
				stationLocation.setLongitude(((double) stationsCursor
						.getInt(longitudeColumn)) * 1E-6);
				distance = location.distanceTo(stationLocation);
				if (distance > distanceFilter) {
					continue;
				}
			}
			overlays.add(overlay);
		} while (stationsCursor.moveToNext());
		return overlays;
	}

	public class StationOverlay extends OverlayItem {

		private int mId;
		private String mBikes;
		private String mSlots;

		public StationOverlay(GeoPoint point, String bikes, String slots, int id) {
			super(point, null, null);
			mId = id;
			mBikes = String.valueOf(bikes);
			mSlots = String.valueOf(slots);
		}

		@Override
		public Drawable getMarker(int stateBitset) {
			if (mDrawText) {
				marker.bike = mBikes;
				marker.slots = mSlots;
				return marker;
			} else {
				return pin;
			}
		}

		public int getId() {
			return mId;
		}

		public void setId(int id) {
			mId = id;
		}

		public void setBikes(int bikes) {
			mBikes = String.valueOf(bikes);
		}

		public void setSlots(int slots) {
			mSlots = String.valueOf(slots);
		}
	}

	private class MarkerDrawable extends BitmapDrawable {

		// For fast access and lot of modifications
		public volatile String bike;
		public volatile String slots;

		public MarkerDrawable(Resources r, Bitmap b) {
			super(r, b);
			this.setAlpha(200);
		}

		@Override
		public void draw(Canvas canvas) {
			super.draw(canvas);
			canvas.drawText(bike, -6, -31, mTextPaint);
			canvas.drawText(slots, -6, -16, mTextPaint);
		}
	}

	private class PinDrawable extends BitmapDrawable {

		public PinDrawable() {
			super();
			mPinPaint.setStyle(Paint.Style.FILL);
			mPinPaint.setColor(Color.BLUE);
			mPinPaint.setAlpha(150);
			mPinPaint.setAntiAlias(true);
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawCircle(0, 0, 5, mPinPaint);
		}
	}
}