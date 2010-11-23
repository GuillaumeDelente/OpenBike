package fr.vcuboid.map;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;

public class VcuboidMapActivity extends MapActivity implements IVcuboidActivity {

	private MapController mMapController;
	private MyCustomLocationOverlay mMyLocationOverlay;
	private boolean mIsVeryFirstFix = true;
	private SharedPreferences mMapPreferences = null;
	//private MapView mMapView = null;
	private VcuboidManager mVcuboidManager = null;

	// TODO: Globalize Shared preferences

	/** {@inheritDoc} */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_layout);

		MapView mMapView = (MapView) findViewById(R.id.map_view);
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
		mMapView.getOverlays().add(mMyLocationOverlay);
		Cursor c = mVcuboidManager.getCursor();
		Log.e("Vcuboid", "Cursor est de taille " + c.getCount());
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
			startActivity(new Intent(this, MapPreferencesActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onResume() {
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
		mMyLocationOverlay.disableMyLocation();
		super.onPause();
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
}
