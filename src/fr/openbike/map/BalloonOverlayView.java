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

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.openbike.R;
import fr.openbike.StationDetails;
import fr.openbike.object.Station;
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
public class BalloonOverlayView extends FrameLayout {

	private Context mContext;
	private RelativeLayout mLayout;
	private CheckBox mFavorite;
	private TextView mName;
	private TextView mOpened;
	private TextView mBikes;
	private TextView mSlots;
	private TextView mDistanceTextView;

	/**
	 * Create a new BalloonOverlayView.
	 * 
	 * @param context
	 *            - The activity context.
	 * @param balloonBottomOffset
	 *            - The bottom padding (in pixels) to be applied when rendering
	 *            this view.
	 */
	public BalloonOverlayView(Context context, int balloonBottomOffset,
			int ballonLeftOffset) {
		super(context);
		mContext = context;
		setPadding(ballonLeftOffset, 0, 0, balloonBottomOffset);
		mLayout = new RelativeLayout(context);
		mLayout.setVisibility(VISIBLE);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.balloon_overlay, mLayout);
		final Intent intent = new Intent(mContext, StationDetails.class);
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Log.i("OpenBike", "Item clicked");
				intent.putExtra("id", (Integer) mFavorite.getTag());
				mContext.startActivity(intent);
			}
		});
		mFavorite = (CheckBox) v.findViewById(R.id.favorite);
		mName = (TextView) v.findViewById(R.id.balloon_item_name);
		mBikes = (TextView) v.findViewById(R.id.balloon_item_bikes);
		mSlots = (TextView) v.findViewById(R.id.balloon_item_slots);
		mOpened = (TextView) v.findViewById(R.id.balloon_item_opened);
		mDistanceTextView = (TextView) v
				.findViewById(R.id.balloon_item_distance);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(mLayout, params);

	}

	/**
	 * Sets the view data from a given overlay item.
	 * 
	 * @param item
	 *            - The overlay item containing the relevant view data (title
	 *            and snippet).
	 */
	public void setData(Station station) {
		mLayout.setVisibility(VISIBLE);
		mName.setText(station.getName());
		mFavorite.setChecked(station.isFavorite());
		mFavorite.setTag(station.getId());
		mFavorite.setOnCheckedChangeListener(new FavoriteListener());
		if (!station.isOpen()) {
			mOpened.setVisibility(VISIBLE);
			mBikes.setVisibility(INVISIBLE);
			mSlots.setVisibility(INVISIBLE);
		} else {
			mOpened.setVisibility(INVISIBLE);
			mBikes.setVisibility(VISIBLE);
			mSlots.setVisibility(VISIBLE);
			mBikes
					.setText(station.getBikes() + " " + mContext
							.getString(station.getBikes() > 1 ? R.string.bikes : R.string.bike));
			mSlots
			.setText(station.getSlots() + " " + mContext
					.getString(station.getSlots() > 1 ? R.string.slots : R.string.slot));
			if (station.getDistance() != -1) {
				mDistanceTextView.setText(mContext.getString(R.string.at) + " "
						+ Utils.formatDistance(station.getDistance()));
				mDistanceTextView.setVisibility(VISIBLE);
			} else {
				mDistanceTextView.setVisibility(GONE);
			}
		}
	}

	public void disableListeners() {
		mFavorite.setOnCheckedChangeListener(null);

	}

	public void refreshData(Station station) {
		setData(station);
	}

	// TODO : same as for the list
	class FavoriteListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			Log.d("OpenBike", "Checked : " + buttonView.getTag());
			((OpenBikeMapActivity) mContext).setFavorite((Integer) buttonView
					.getTag(), isChecked);
		}
	}
}
