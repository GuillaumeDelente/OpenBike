package fr.vcuboid.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;

public class VcuboidMapActivity extends MapActivity implements IVcuboidActivity {

	private MapController mMapController;
	private MyCustomLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private boolean mIsVeryFirstFix = true;
	private SharedPreferences mMapPreferences = null;
	private MapView mMapView = null;
	private VcuboidManager mVcuboidManager = null;
	
	static final int SET_FILTER = 0;

	/** {@inheritDoc} */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("Vcuboid", "Map on create");
		setContentView(R.layout.map_layout);
		mMapView = (MapView) findViewById(R.id.map_view);
		mMapController = mMapView.getController();
		mMapView.setSatellite(true);
		mMapView.setStreetView(false);
		mMapView.displayZoomControls(true);
		mMapView.setBuiltInZoomControls(true);

		mVcuboidManager = VcuboidManager.getVcuboidManagerInstance(this);
		mMapPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (mMapPreferences.getBoolean("map_always_on_my_position", false)) {
			mMyLocationOverlay = new MyCustomLocationOverlay(this, mMapView,
					mMapController);
		} else {
			mMyLocationOverlay = new MyCustomLocationOverlay(this, mMapView);
		}

		mMyLocationOverlay.runOnFirstFix(new Runnable() {

			@Override
			public void run() {
				if (mIsVeryFirstFix) {
					mMapController
							.animateTo(mMyLocationOverlay.getMyLocation());
					mIsVeryFirstFix = false;
					mMapController.setZoom(17);
				}
			}
		});
		mMyLocationOverlay.enableMyLocation();
		mMapOverlays = mMapView.getOverlays();
		Bitmap marker = BitmapFactory.decodeResource(getResources(),
				R.drawable.v3);
		StationOverlay.setMarker(marker);
		StationOverlay.setMapView(mMapView);
		ArrayList<StationOverlay> stations = mVcuboidManager
				.getVisibleStations();
		mMapOverlays.addAll(stations);
		Collections.reverse(mMapOverlays);
		mMapOverlays.add(mMyLocationOverlay);
	}
	
	public void hideOverlayBalloon() {
		int baloonPosition = mMapOverlays.size() - 2;
		Overlay overlay = mMapOverlays.get(baloonPosition);
		if (overlay instanceof StationOverlay) {
			((StationOverlay) overlay).hideBalloon();
		} else {
			Log.e("Balloon", "hideOtherBalloons, before last not a StationOverlay");
		}
	}

	@Override
	protected boolean isLocationDisplayed() {
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_map_preferences:
			startActivityForResult(new Intent(this, MapFilterActivity.class), SET_FILTER);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onResume() {
		mVcuboidManager.setCurrentActivity(this);
		if (mMapPreferences.getBoolean("map_always_on_my_position", false)) {
			mMyLocationOverlay.setAlwaysCentered(mMapController);
		} else {
			mMyLocationOverlay.unsetAlwaysCentered();
		}
		mMyLocationOverlay.enableMyLocation();
		super.onResume();
	}

	@Override
	protected void onPause() {
		hideOverlayBalloon();
		StationOverlay.balloonView = null;
		mMyLocationOverlay.disableMyLocation();
		super.onPause();
	}
	
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == SET_FILTER) {
            if (resultCode == RESULT_OK) {
				mVcuboidManager.applyFilter();
            }
        }
    }

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setCenteredMap(boolean isCentered) {
		mMyLocationOverlay
				.setmMapController(isCentered ? mMapController : null);
	}

	@Override
	public void finishGetAllStationsOnProgress() {
		// TODO Auto-generated method stub

	}

	@Override
	public void finishUpdateAllStationsOnProgress() {
		// TODO Auto-generated method stub

	}

	@Override
	public void showGetAllStationsOnProgress() {
		// TODO Auto-generated method stub

	}

	@Override
	public void showUpdateAllStationsOnProgress() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateGetAllStationsOnProgress(int progress) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLocationChanged(Location location) {
		StationOverlay current = (StationOverlay) mMapOverlays
		.get(mMapOverlays.size() - 2);
		// TODO: Debuging only -------------------------
		if (mMapOverlays.get(mMapOverlays.size() - 1) instanceof MyCustomLocationOverlay) {
			Log.e("Vcuboid2", "Location OK");
		}
		if (!(mMapOverlays.get(mMapOverlays.size() - 2) instanceof StationOverlay)) {
			Log.e("Vcuboid2", "Pas bon ! ");
		}
		// ---------------------------------------------
		if (!current.isCurrent) {
			current = null;
		}
		mMapOverlays.clear();
		ArrayList<StationOverlay> stations = mVcuboidManager.onLocationChanged(location,
				current);
		mMapOverlays.addAll(stations);
		Collections.reverse(mMapOverlays);
		mMapOverlays.add(mMyLocationOverlay);
	}

	@Override
	public void onListUpdated() {
		mMapOverlays.clear();
		ArrayList<StationOverlay> stations = mVcuboidManager.getVisibleStations();
		mMapOverlays.addAll(stations);
		Collections.reverse(mMapOverlays);
		mMapOverlays.add(mMyLocationOverlay);
	}
	
	public Location getCurrentLocation() {
		return mMyLocationOverlay.getLastFix();
	}
}
