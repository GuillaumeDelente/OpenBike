package fr.vcuboid;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import fr.vcuboid.database.VcuboidDBAdapter;

public class VcuboidSimpleCursorAdaptor extends SimpleCursorAdapter {

	/*
	@Override
	public boolean setViewValue(View view, final Cursor cursor, int columnIndex) {
    	Log.e("Eh ! ", " setViewValue!! ");
		switch (columnIndex) {
		case VcuboidDBAdapter.NAME_COLUMN:
			bindName((TextView) view, cursor);
			break;
		case VcuboidDBAdapter.BIKES_COLUMN:
			bindBikes((TextView) view, cursor);
			break;
		case VcuboidDBAdapter.SLOTS_COLUMN:
			bindSlots((TextView) view, cursor);
			break;
		}
		return true;
	}
	*/

	public VcuboidSimpleCursorAdaptor(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
		// TODO Auto-generated constructor stub
	}

	static class ViewHolder {
		TextView name;
		TextView bikes;
		TextView slots;
		ProgressBar refreshing;
	}
	
	@Override
    public void bindView (View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if (viewHolder == null) {
                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) view.findViewById(R.id.name_entry);
                viewHolder.bikes = (TextView) view.findViewById(R.id.bikes_entry);
                viewHolder.slots = (TextView) view.findViewById(R.id.slots_entry);
                viewHolder.refreshing = (ProgressBar) view.findViewById(R.id.refreshing);
                view.setTag(viewHolder);
        }
        bindName(viewHolder.name, cursor);
        bindBikes(viewHolder.bikes, cursor);
        bindSlots(viewHolder.slots, cursor);
        bindRefreshing(viewHolder.refreshing);
}

	private void bindName(TextView view, final Cursor cursor) {
		view.setText(cursor.getString(VcuboidDBAdapter.NAME_COLUMN));
	}
	
	private void bindBikes(TextView view, final Cursor cursor) {
		view.setText(cursor.getString(VcuboidDBAdapter.BIKES_COLUMN));
	}
	
	private void bindSlots(TextView view, final Cursor cursor) {
		view.setText(cursor.getString(VcuboidDBAdapter.SLOTS_COLUMN));
	}
	
	private void bindRefreshing(ProgressBar refreshing) {
		refreshing.setVisibility(VcuboidManager.isUpdating ? View.VISIBLE : View.INVISIBLE);
	}
}