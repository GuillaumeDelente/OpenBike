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

import java.util.ArrayList;
import java.util.List;

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
import android.provider.BaseColumns;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import fr.openbike.database.OpenBikeDBAdapter;

class StationsOverlay extends BalloonItemizedOverlay<OverlayItem> {

	private List<OverlayItem> items = new ArrayList<OverlayItem>();
	private MarkerDrawable marker = null;
	private PinDrawable pin = null;
	private static Paint mTextPaint = new Paint();
	private static Paint mPinPaint = new Paint();
	private static boolean mDrawText;

	int IMAGE_HEIGHT;

	public StationsOverlay(Resources resources, BitmapDrawable drawable,
			Cursor stationsCursor, MapView mv) {
		super(drawable, mv);
		pin = new PinDrawable();
		this.marker = new MarkerDrawable(resources, drawable.getBitmap());
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(15);
		mTextPaint.setTextAlign(Align.RIGHT);
		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		IMAGE_HEIGHT = marker.getIntrinsicHeight();
		while (stationsCursor.moveToNext()) {
			items.add(new StationOverlay(new GeoPoint(stationsCursor
					.getInt(stationsCursor
							.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE)),
					stationsCursor.getInt(stationsCursor
							.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE))),
					String.valueOf(stationsCursor.getInt(stationsCursor
							.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES))),
					String.valueOf(stationsCursor.getInt(stationsCursor
							.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS))),
					stationsCursor.getInt(stationsCursor
							.getColumnIndex(BaseColumns._ID))));
		}
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return (items.get(i));
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
			boundCenterBottom(marker);
		}
	}

	@Override
	public int size() {
		return (items.size());
	}

	public class StationOverlay extends OverlayItem {

		private int mId;

		public StationOverlay(GeoPoint point, String bikes, String slots, int id) {
			super(point, bikes, slots);
			mId = id;
		}

		@Override
		public Drawable getMarker(int stateBitset) {
			if (mDrawText) {
				marker.bike = this.mTitle;
				marker.slots = this.mSnippet;
				return marker;
			} else {
				return pin;
			}
		}

		public int getId() {
			return mId;
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
			canvas.drawText(String.valueOf(bike), -6, -31, mTextPaint);
			canvas.drawText(String.valueOf(slots), -6, -16, mTextPaint);
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