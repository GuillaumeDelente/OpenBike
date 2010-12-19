package fr.vcuboid.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;

public class VcuboidMapActivity extends MapActivity implements IVcuboidActivity {

	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private boolean mIsFirstFix = true;
	private SharedPreferences mMapPreferences = null;
	private MapView mMapView = null;
	private VcuboidManager mVcuboidManager = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("Vcuboid", "Map on create");
		setContentView(R.layout.map_layout);
		mMapView = (MapView) findViewById(R.id.map_view);
		mMapController = mMapView.getController();
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setBuiltInZoomControls(true);
		mVcuboidManager = VcuboidManager.getVcuboidManagerInstance(this);
		mVcuboidManager.setCurrentActivity(this);
		mMapView.displayZoomControls(true);
		mMapView.invalidate();
		mMapPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mMapOverlays = mMapView.getOverlays();
		Bitmap marker = BitmapFactory.decodeResource(getResources(),
				R.drawable.v3);
		StationOverlay.setMarker(marker);
		StationOverlay.setMapView(mMapView);
	}

	public void hideOverlayBalloon() {
		Overlay overlay = mMapOverlays.get(mMapOverlays.size()
				- (mMyLocationOverlay == null ? 1 : 2));
		if (overlay instanceof StationOverlay) {
			((StationOverlay) overlay).hideBalloon();
		} else {
			Log.e("Balloon",
					"hideOtherBalloons, before last not a StationOverlay");
		}
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
			startActivity(new Intent(this, MapFilterActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		mMapOverlays.clear();
		mMapOverlays.addAll(mVcuboidManager.getVisibleStations());
		Collections.reverse(mMapOverlays);
		if (mMapPreferences.getBoolean(getString(R.string.use_location), true)) {
			if (mMyLocationOverlay == null) {
				// FIXME centered map on location
				mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
			}
			mMapOverlays.add(mMyLocationOverlay);
		} else {
			mMyLocationOverlay = null;
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		hideOverlayBalloon();
		StationOverlay.balloonView = null;
		super.onPause();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setCenteredMap(boolean isCentered) {
		// mMyLocationOverlay
		// .setmMapController(isCentered ? mMapController : null);
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
		if (mMyLocationOverlay == null)
			mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		mMyLocationOverlay.setCurrentLocation(location);
		StationOverlay current = (StationOverlay) mMapOverlays.get(mMapOverlays
				.size() - 2);
		if (!current.isCurrent) {
			current = null;
		}
		mMapOverlays.clear();
		ArrayList<StationOverlay> stations = mVcuboidManager
				.getVisibleStations();
		mMapOverlays.addAll(stations);
		Collections.reverse(mMapOverlays);
		mMapOverlays.add(mMyLocationOverlay);
		mMapView.invalidate();
		if (mMapPreferences.getBoolean(getString(R.string.center_on_location),
				false) || mIsFirstFix) {
			mMapController.animateTo(new GeoPoint(
					(int) (location.getLatitude() * 1E6), (int) (location
							.getLongitude() * 1E6)));
			mIsFirstFix = false;
		}
	}

	@Override
	public void onListUpdated() {
		mMapOverlays.clear();
		ArrayList<StationOverlay> stations = mVcuboidManager
				.getVisibleStations();
		mMapOverlays.addAll(stations);
		Collections.reverse(mMapOverlays);
		if (mMyLocationOverlay != null)
			mMapOverlays.add(mMyLocationOverlay);
		mMapView.invalidate();
	}

	@Override
	public void showAskForGps() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.gps_disabled)).setMessage(
				getString(R.string.show_location_parameters)).setCancelable(
				false).setPositiveButton(getString(R.string.yes),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent gpsOptionsIntent = new Intent(
								android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(gpsOptionsIntent);
					}
				});
		builder.setNegativeButton(getString(R.string.no),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
