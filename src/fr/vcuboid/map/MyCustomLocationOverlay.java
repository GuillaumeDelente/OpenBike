package fr.vcuboid.map;

import android.content.Context;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class MyCustomLocationOverlay extends MyLocationOverlay {

	private MapController mMapController = null;

	public MyCustomLocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
	}

	public MyCustomLocationOverlay(Context context, MapView mapView,
			MapController mapController) {
		super(context, mapView);
		mMapController = mapController;
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
		if (mMapController != null) {
			int latitude = (int) (location.getLatitude() * 1E6);
			int longitude = (int) (location.getLongitude() * 1E6);
			mMapController.animateTo(new GeoPoint(latitude, longitude));
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
