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

package fr.vcuboid.map;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.vcuboid.R;
import fr.vcuboid.object.Station;
import fr.vcuboid.utils.Utils;

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
	private TextView mDistance;

	/**
	 * Create a new BalloonOverlayView.
	 * 
	 * @param context - The activity context.
	 * @param balloonBottomOffset - The bottom padding (in pixels) to be applied
	 * when rendering this view.
	 */
	public BalloonOverlayView(Context context, int balloonBottomOffset, int ballonLeftOffset) {
		super(context);
		mContext = context;
		setPadding(ballonLeftOffset, 0, 0, balloonBottomOffset);
		mLayout = new RelativeLayout(context);
		mLayout.setVisibility(VISIBLE);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.balloon_overlay, mLayout);
		mFavorite = (CheckBox) v.findViewById(R.id.favorite);
		mName = (TextView) v.findViewById(R.id.balloon_item_name);
		mBikes = (TextView) v.findViewById(R.id.balloon_item_bikes);
		mSlots = (TextView) v.findViewById(R.id.balloon_item_slots);
		mOpened = (TextView) v.findViewById(R.id.balloon_item_opened);
		mDistance = (TextView) v.findViewById(R.id.balloon_item_distance);
		
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(mLayout, params);

	}
	
	/**
	 * Sets the view data from a given overlay item.
	 * 
	 * @param item - The overlay item containing the relevant view data 
	 * (title and snippet). 
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
			mBikes.setText(station.getBikes() + " vélos");
			mSlots.setText(station.getSlots() + " places");
		}
		if (station.getDistance() != -1) {
			mDistance.setText("Distance : " + Utils.formatDistance(station.getDistance()));
			mDistance.setVisibility(VISIBLE);
		} else {
			mDistance.setVisibility(GONE);
		}
	}
	
	public void disableListeners() {
		mFavorite.setOnCheckedChangeListener(null);
	}
	
	public void refreshData(Station station) {
		setData(station);
	}
	
	//TODO : same as for the list
	class FavoriteListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			((VcuboidMapActivity) mContext).setFavorite((Integer) buttonView.getTag(), isChecked);
		}
	}
}
