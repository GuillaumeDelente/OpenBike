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
package fr.vcuboid.list;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
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
import fr.vcuboid.R;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;
import fr.vcuboid.utils.Utils;

public class VcuboidArrayAdaptor extends ArrayAdapter<StationOverlay> {
	
	private LayoutInflater mInflater;
	private Context mContext;
	private Drawable mRedBike;
	private Drawable mGreenBike;

	public VcuboidArrayAdaptor(Context context, int layout,
			ArrayList<StationOverlay> list) {
		super(context, layout, list);
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
		StationOverlay overlay = getItem(position);
		if (overlay == null) {
			Log.e("Vcuboid", "Invalid position: " + position);
		}
		Station station = overlay.getStation();
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
	
	class FavoriteListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			((VcuboidListActivity) mContext).setFavorite((Integer) buttonView.getTag(), isChecked);
		}
	}
}

