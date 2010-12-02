package fr.vcuboid.map;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class MyCustomLocationOverlay extends MyLocationOverlay {

	private MapController mMapController = null;
	private Context mContext = null;
	private Location mLastLocation = null;

	public MyCustomLocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
		mContext = context;
	}

	public MyCustomLocationOverlay(Context context, MapView mapView,
			MapController mapController) {
		super(context, mapView);
		mMapController = mapController;
		mContext = context;
	}

	public MapController getmMapController() {
		return mMapController;
	}

	public void setmMapController(MapController mapController) {
		this.mMapController = mapController;
	}

	@Override
	public void onLocationChanged(Location location) {
		super.onLocationChanged(location);
		if (mLastLocation == null || location.distanceTo(mLastLocation) > 10) {
			mLastLocation = location;
			if (mMapController != null) {
				int latitude = (int) (location.getLatitude() * 1E6);
				int longitude = (int) (location.getLongitude() * 1E6);
				mMapController.animateTo(new GeoPoint(latitude, longitude));
			}
			((VcuboidMapActivity) mContext).onLocationChanged(location);
		} else {
			Log.e("Vcuboid2", "Location changed but not enough to update");
		}
	}

	public void setAlwaysCentered(MapController mapController) {
		if (mMapController == null)
			this.mMapController = mapController;
	}

	public void unsetAlwaysCentered() {
		this.mMapController = null;
	}
}
