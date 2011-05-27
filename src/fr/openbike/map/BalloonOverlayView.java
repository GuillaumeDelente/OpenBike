/***
 * Copyright (c) 2010 readyState Software Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package fr.openbike.map;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import fr.openbike.MyLocationProvider;
import fr.openbike.R;
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.database.StationsProvider;
import fr.openbike.utils.Utils;

/**
 * A view representing a MapView marker information balloon.
 * <p>
 * This class has a number of Android resource dependencies:
 * <ul>
 * <li>drawable/balloon_overlay_bg_selector.xml</li>
 * <li>drawable/balloon_overlay_close.png</li>
 * <li>drawable/balloon_overlay_focused.9.png</li>
 * <li>drawable/balloon_overlay_unfocused.9.png</li>
 * <li>layout/balloon_map_overlay.xml</li>
 * </ul>
 * </p>
 * 
 * @author Jeff Gilfelt
 * 
 */
public class BalloonOverlayView<Item extends OverlayItem> extends FrameLayout {

	private LinearLayout mLinearLayout;
	private TextView mTextViewTitle;
	private TextView mBikesTextView;
	private TextView mSlotsTextView;
	private TextView mClosedTextView;
	private TextView mDistanceTextView;
	private CheckBox mFavoriteCheckBox;
	private Context mContext;
	private int mBottomOffset;

	/**
	 * Create a new BalloonOverlayView.
	 * 
	 * @param context
	 *            - The activity context.
	 * @param balloonBottomOffset
	 *            - The bottom padding (in pixels) to be applied when rendering
	 *            this view.
	 */
	public BalloonOverlayView(Context context, int offset) {

		super(context);
		this.mContext = context;
		mLinearLayout = new LinearLayout(context);
		mLinearLayout.setVisibility(VISIBLE);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.balloon_overlay, mLinearLayout);
		mTextViewTitle = (TextView) v.findViewById(R.id.balloon_name);
		mBikesTextView = (TextView) v.findViewById(R.id.balloon_bikes);
		mSlotsTextView = (TextView) v.findViewById(R.id.balloon_slots);
		mClosedTextView = (TextView) v.findViewById(R.id.balloon_closed);
		mDistanceTextView = (TextView) v.findViewById(R.id.balloon_distance);
		mFavoriteCheckBox = (CheckBox) v
				.findViewById(R.id.balloon_item_favorite);
		/*
		 * final float scale = this.getResources().getDisplayMetrics().density;
		 * favorite.setPadding(favorite.getPaddingLeft(),
		 * favorite.getPaddingTop(), favorite.getPaddingRight() + (int)(10.0f *
		 * scale + 0.5f), favorite.getPaddingBottom());
		 */

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(mLinearLayout, params);

	}

	/**
	 * Sets the view data from a given overlay item.
	 * 
	 * @param item
	 *            - The overlay item containing the relevant view data (title
	 *            and snippet).
	 */
	public void setData(Item item) {
		Cursor station = ((Activity) mContext).managedQuery(Uri
				.withAppendedPath(StationsProvider.CONTENT_URI, String
						.valueOf(((StationsOverlay.StationOverlay) item)
								.getId())), new String[] {
				OpenBikeDBAdapter.KEY_NAME, OpenBikeDBAdapter.KEY_OPEN,
				OpenBikeDBAdapter.KEY_FAVORITE, OpenBikeDBAdapter.KEY_BIKES,
				OpenBikeDBAdapter.KEY_SLOTS }, null, null, null);
		mLinearLayout.setVisibility(VISIBLE);
		String name = station.getString(station
				.getColumnIndex(OpenBikeDBAdapter.KEY_NAME));
		mTextViewTitle.setText(name);

		if (station.getInt(station
				.getColumnIndex(OpenBikeDBAdapter.KEY_FAVORITE)) == 1) {
			mFavoriteCheckBox.setChecked(true);
		} else {
			mFavoriteCheckBox.setChecked(false);
		}

		if (station.getInt(station.getColumnIndex(OpenBikeDBAdapter.KEY_OPEN)) == 1) {
			// Opened station
			int bikes = station.getInt(station
					.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES));
			int slots = station.getInt(station
					.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS));
			mBikesTextView.setText(mContext.getResources().getQuantityString(
					R.plurals.bike, bikes, bikes));
			mSlotsTextView.setText(mContext.getResources().getQuantityString(
					R.plurals.slot, slots, slots));
			mClosedTextView.setVisibility(GONE);
			mBikesTextView.setVisibility(VISIBLE);
			mSlotsTextView.setVisibility(VISIBLE);
		} else {
			// Closed station
			mClosedTextView.setVisibility(VISIBLE);
			mBikesTextView.setVisibility(GONE);
			mSlotsTextView.setVisibility(GONE);
		}
		GeoPoint point = item.getPoint();
		int distance = Utils.computeDistance(point.getLatitudeE6(), point.getLongitudeE6());
		
		if (distance == MyLocationProvider.DISTANCE_UNAVAILABLE) {
			// No distance to show
			mDistanceTextView.setVisibility(GONE);
		} else {
			// Show distance
			mDistanceTextView.setText(mContext.getString(R.string.at) + " " + Utils.formatDistance(distance));
		}
	}
	
	public void setBalloonBottomOffset(int offset) {
		int old = mBottomOffset;
		mBottomOffset = offset;
		if (old != mBottomOffset) {
			setPadding(10, 0, 10, offset);
			invalidate();
		}
	}
}
