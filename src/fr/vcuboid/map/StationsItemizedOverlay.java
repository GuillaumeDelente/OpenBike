package fr.vcuboid.map;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

public class StationsItemizedOverlay extends
		ItemizedOverlay<StationOverlayItem> {

	private static Drawable mMarker;
	private ArrayList<StationOverlayItem> mOverlays = null;
	private Context mContext;

	public StationsItemizedOverlay(Drawable marker, Context context,
			ArrayList<StationOverlayItem> overlays) {
		super(boundCenterBottom(marker));
		mMarker = marker;
		mContext = context;
		mOverlays = overlays;
		populate();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
	}

	public void addOverlayItem(StationOverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected StationOverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		/*
		 * StationOverlay item = mOverlays.get(index); AlertDialog.Builder
		 * dialog = new AlertDialog.Builder(mContext);
		 * dialog.setIcon(IconsUtil.getLineIconResource(item.getTitle()));
		 * dialog.setTitle(item.getTitle());
		 * dialog.setMessage(item.getSnippet());
		 * 
		 * AlertDialog dialogInstance = dialog.create(); dialogInstance.show();
		 * dialogInstance.setCancelable(true);
		 * dialogInstance.setCanceledOnTouchOutside(true);
		 */
		return false;
	}

	public void addStation(StationOverlayItem station) {
		/*
		 * GeoPoint point = new
		 * GeoPoint(ss.getLatitudeAsMicroDegrees(),ss.getLongitudeAsMicroDegrees
		 * ()); SubwayOverlayItem stationItem = new SubwayOverlayItem(point,
		 * subwayLine.getName(), ss.getName()); addOverlayItem(stationItem);
		 */
	}
}
