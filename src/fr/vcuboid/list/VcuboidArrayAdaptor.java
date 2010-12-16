package fr.vcuboid.list;

import java.util.ArrayList;
import java.util.zip.Inflater;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import fr.vcuboid.R;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;

public class VcuboidArrayAdaptor extends ArrayAdapter<StationOverlay> {
	
	private LayoutInflater mInflater;

	public VcuboidArrayAdaptor(Context context, int layout,
			ArrayList<StationOverlay> list) {
		super(context, layout, list);
		mInflater = LayoutInflater.from(context);
		// TODO Auto-generated constructor stub
	}

	static class ViewHolder {
		TextView name;
		TextView bikes;
		TextView slots;
		TextView maintenance;
		CheckBox favorite;
		ProgressBar refreshing;
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
			viewHolder.maintenance = (TextView) convertView
					.findViewById(R.id.station_maintenance);
			viewHolder.refreshing = (ProgressBar) convertView
					.findViewById(R.id.refreshing);
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
			viewHolder.bikes.setVisibility(View.INVISIBLE);
			viewHolder.slots.setVisibility(View.INVISIBLE);
		} else {
			viewHolder.maintenance.setVisibility(View.INVISIBLE);
			viewHolder.bikes.setVisibility(View.VISIBLE);
			viewHolder.slots.setVisibility(View.VISIBLE);
			viewHolder.bikes.setText(String.valueOf(station.getBikes()));
			viewHolder.slots.setText(String.valueOf(station.getSlots()));
		}
		//bindRefreshing(viewHolder.refreshing);
		viewHolder.favorite.setChecked(station.isFavorite());
		viewHolder.favorite.setOnCheckedChangeListener(new FavoriteListener());
		viewHolder.favorite.setTag(position);
		return convertView;
	}
}