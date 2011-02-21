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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.vcuboid.R;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;
import fr.vcuboid.utils.Utils;

public class VcuboidArrayAdaptor extends ArrayAdapter<StationOverlay> {
	
	private LayoutInflater mInflater;
	private Context mContext;

	public VcuboidArrayAdaptor(Context context, int layout,
			ArrayList<StationOverlay> list) {
		super(context, layout, list);
		mInflater = LayoutInflater.from(context);
		mContext = context;
	}

	public static class ViewHolder {
		TextView name;
		TextView bikes;
		TextView slots;
		TextView distance;
		TextView maintenance;
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
			viewHolder.distance = (TextView) convertView
				.findViewById(R.id.distance);
			viewHolder.maintenance = (TextView) convertView
					.findViewById(R.id.station_maintenance);
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
			viewHolder.maintenance.setVisibility(View.VISIBLE);
			viewHolder.bikes.setVisibility(View.GONE);
			viewHolder.slots.setVisibility(View.GONE);
			viewHolder.distance.setVisibility(View.GONE);
		} else {
			viewHolder.maintenance.setVisibility(View.GONE);
			viewHolder.bikes.setVisibility(View.VISIBLE);
			viewHolder.slots.setVisibility(View.VISIBLE);
			viewHolder.bikes.setText(String.valueOf(station.getBikes()) + " " 
					+ mContext.getString(station.getBikes() == 1 ? 
							R.string.bike : R.string.bikes));
			viewHolder.slots.setText(String.valueOf(station.getSlots()) + " " 
					+ mContext.getString(station.getSlots() == 1 ? 
							R.string.slot : R.string.slots));
			viewHolder.distance.setText(
					String.valueOf(Utils.formatDistance(station.getDistance())));
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

