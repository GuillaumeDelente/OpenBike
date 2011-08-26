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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.openbike.R;
import fr.openbike.model.MinimalStation;
import fr.openbike.utils.Utils;

public class OpenBikeArrayAdaptor extends ArrayAdapter<MinimalStation> {
	
	private LayoutInflater mInflater;
	private Context mContext;
	private static Drawable mRedBike;
	private static Drawable mGreenBike;
	private ArrayList<MinimalStation> mStations = null;

	public OpenBikeArrayAdaptor(Context context, int layout,
			ArrayList<MinimalStation> list) {
		super(context, layout, list);
		mStations = list;
		mInflater = LayoutInflater.from(context);
		mContext = context;
		mRedBike = context.getResources().getDrawable(R.drawable.red_list);
		mGreenBike = context.getResources().getDrawable(R.drawable.green_list);
	}

	public static class ViewHolder {
		TextView name;
		RelativeLayout openLayout;
		TextView bikes;
		TextView slots;
		TextView distance;
		LinearLayout closed;
		CheckBox favorite;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.station_list_entry, null);
			viewHolder.name = (TextView) convertView
					.findViewById(R.id.name_entry);
			viewHolder.bikes = (TextView) convertView
					.findViewById(R.id.bikes_entry);
			viewHolder.slots = (TextView) convertView
					.findViewById(R.id.slots_entry);
			viewHolder.openLayout = (RelativeLayout) convertView
				.findViewById(R.id.open_layout);
			viewHolder.distance = (TextView) convertView
				.findViewById(R.id.distance);
			viewHolder.closed = (LinearLayout) convertView
					.findViewById(R.id.closed_layout);
			viewHolder.favorite = (CheckBox) convertView
					.findViewById(R.id.favorite);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
			viewHolder.favorite.setOnCheckedChangeListener(null);
		}
		MinimalStation station = getItem(position);
		viewHolder.name.setText(station.getName());
		if (!station.isOpen()) {
			viewHolder.closed.setVisibility(View.VISIBLE);
			viewHolder.openLayout.setVisibility(View.GONE);
			viewHolder.distance.setVisibility(View.GONE);
		} else {
			viewHolder.closed.setVisibility(View.GONE);
			viewHolder.openLayout.setVisibility(View.VISIBLE);
			viewHolder.distance.setVisibility(View.VISIBLE);
			viewHolder.bikes.setText(String.valueOf(station.getBikes()));
			viewHolder.bikes.setBackgroundDrawable(station.getBikes() == 0 ?
					mRedBike : mGreenBike);
			viewHolder.slots.setBackgroundDrawable(station.getSlots() == 0 ?
					mRedBike : mGreenBike);
			viewHolder.slots.setText(String.valueOf(station.getSlots()));
			/*
			viewHolder.bikes.setText(String.valueOf(station.getBikes()) + " " 
					+ mContext.getString(station.getBikes() == 1 ? 
							R.string.bike : R.string.bikes));
			viewHolder.slots.setText(String.valueOf(station.getSlots()) + " " 
					+ mContext.getString(station.getSlots() == 1 ? 
							R.string.slot : R.string.slots));
							*/
		}
		if (station.getDistance() != -1) {
			viewHolder.distance.setVisibility(View.VISIBLE);
			viewHolder.distance.setText(
				String.valueOf(Utils.formatDistance(station.getDistance())));
		} else {
			viewHolder.distance.setVisibility(View.GONE);
		}
		viewHolder.favorite.setChecked(station.isFavorite());
		viewHolder.favorite.setOnCheckedChangeListener(new FavoriteListener());
		viewHolder.favorite.setTag(station.getId());
		return convertView;
	}
	
	public ArrayList<MinimalStation> getList() {
		return mStations;
	}
	
	class FavoriteListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			((OpenBikeListActivity) mContext).setFavorite((Integer) buttonView.getTag(), isChecked);
		}
	}
}

