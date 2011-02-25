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
package fr.vcuboid.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.MyLocationProvider;
import fr.vcuboid.R;
import fr.vcuboid.RestClient;
import fr.vcuboid.VcuboidManager;
import fr.vcuboid.list.VcuboidListActivity;

public class VcuboidMapActivity extends MapActivity implements IVcuboidActivity {

	private boolean mIsShowStationMode = false;
	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private boolean mIsFirstFix = true;
	private SharedPreferences mMapPreferences = null;
	private MapView mMapView = null;
	private VcuboidManager mVcuboidManager = null;
	private int mSelected = 0;
	private boolean mRefreshMenu = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("Vcuboid", "Map on create");
		setContentView(R.layout.map_layout);
		mMapView = (MapView) findViewById(R.id.map_view);
		mMapController = mMapView.getController();
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setBuiltInZoomControls(true);
		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.containsKey("id"))
			mIsShowStationMode = true;
		mVcuboidManager = VcuboidManager.getVcuboidManagerInstance(this);
		if (mIsShowStationMode) {
			mVcuboidManager.setShowStationMode(bundle.getInt("id"));
		}
		mMapView.displayZoomControls(true);
		mMapPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (!mIsShowStationMode
				&& !mMapPreferences.getBoolean(
						getString(R.string.use_location), true))
			zoomAndCenter((GeoPoint) null);
		mMapOverlays = mMapView.getOverlays();
		Bitmap marker = BitmapFactory.decodeResource(getResources(),
				R.drawable.v3);
		StationOverlay.setMarker(marker);
		StationOverlay.setMapView(mMapView);
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mRefreshMenu = true;
		Bundle bundle = intent.getExtras();
		if (bundle == null || !bundle.containsKey("id")) {
			mIsShowStationMode = false;
			mVcuboidManager.setShowStationMode(-1);
			Log.d("Vcuboid", "No key !");
		} else if (bundle.containsKey("id")) {
			setIntent(intent);
			mIsShowStationMode = true;
			mVcuboidManager.setShowStationMode(bundle.getInt("id"));
			Log.d("Vcuboid", "Key = " + bundle.getInt("id"));
		}
	}

	public void hideOverlayBalloon() {
		int position = mMapOverlays.size()
				- (mMyLocationOverlay == null ? 1 : 2);
		if (position >= 0) {
			Overlay overlay = mMapOverlays.get(position);
			if (overlay instanceof StationOverlay) {
				((StationOverlay) overlay).hideBalloon();
			} else {
				Log.e("Balloon",
						"hideOtherBalloons, before last not a StationOverlay");
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.i("Vcuboid", "onPrepareOptionsMenu");
		if (mRefreshMenu) {
			MenuInflater inflater = getMenuInflater();
			menu.clear();
			inflater.inflate((mIsShowStationMode ? R.menu.station_map_menu
					: R.menu.map_menu), menu);
			mRefreshMenu = false;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i("Vcuboid", "onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate((mIsShowStationMode ? R.menu.station_map_menu
				: R.menu.map_menu), menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_map_preferences:
			startActivity(new Intent(this, MapFilterActivity.class));
			return true;
		case R.id.menu_list:
			startActivity(new Intent(this, VcuboidListActivity.class));
			return true;
		case R.id.menu_map:
			startActivity(new Intent(this, VcuboidMapActivity.class));
			return true;
		case R.id.menu_update_all:
			mVcuboidManager.executeUpdateAllStationsTask();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		if (mIsShowStationMode)
			mVcuboidManager.setShowStationMode(getIntent().getExtras().getInt("id"));
		mVcuboidManager.setCurrentActivity(this, true);
		mVcuboidManager.startLocation();
		mMapOverlays.clear();
		mMapOverlays.addAll(mVcuboidManager.getVisibleStations());
		if (!mIsShowStationMode) {
			Collections.reverse(mMapOverlays);
		}
		if (mMapPreferences.getBoolean(getString(R.string.use_location), true)) {
			if (mMyLocationOverlay == null) {
				mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
			}
			mMapOverlays.add(mMyLocationOverlay);
			if (!mMyLocationOverlay.isMyLocationDrawn()) {
				mMyLocationOverlay.setCurrentLocation(mVcuboidManager
						.getCurrentLocation());
				// mMapView.invalidate();
			}
			if (mIsShowStationMode) {
				zoomAndCenter(((StationOverlay) mMapOverlays.get(0))
						.getStation().getGeoPoint());
			} else {
				zoomAndCenter(mVcuboidManager.getCurrentLocation());
			}
		} else {
			mMyLocationOverlay = null;
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (mIsShowStationMode) {
			mVcuboidManager.setShowStationMode(-1);
		}
		finishUpdateAllStationsOnProgress(false);
		mVcuboidManager.stopLocation();
		hideOverlayBalloon();
		StationOverlay.setBalloonView(null);
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.i("Vcuboid", "Map : onDestroy");
		super.onDestroy();
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
	public void showGetAllStationsOnProgress() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateGetAllStationsOnProgress(int progress) {
		// TODO Auto-generated method stub

	}

	public void setFavorite(int id, boolean isChecked) {
		mSelected = id;
		if (isChecked) {
			mVcuboidManager.setFavorite(id, true);
			int size = mMapOverlays.size();
			int baloonPosition = size
					- (mMapOverlays.get(size - 1) instanceof MyLocationOverlay ? 2
							: 1);
			Overlay overlay = mMapOverlays.get(baloonPosition);
			if (overlay instanceof StationOverlay) {
				((StationOverlay) overlay).getStation().setFavorite(true);
			} else {
				Log.d("Vcuboid", "before last not a StationOverlay");
			}
			// onListUpdated();
		} else {
			showDialog(VcuboidManager.REMOVE_FROM_FAVORITE);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (mMyLocationOverlay == null) {
			mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
			mMapOverlays.add(mMyLocationOverlay);
		}
		mMyLocationOverlay.setCurrentLocation(location);
		if (!mIsShowStationMode && location != null
				|| !mMyLocationOverlay.isMyLocationDrawn()) {
			StationOverlay station = getLastStationOverlay();
			if (station != null)
				station.refreshBalloon();
			zoomAndCenter(location);
		}
	}

	private void zoomAndCenter(Location location) {
		if (location == null) {
			zoomAndCenter((GeoPoint) null);
			return;
		}
		zoomAndCenter(new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6)));
	}

	private void zoomAndCenter(GeoPoint geoPoint) {
		if (geoPoint == null) {
			mMapController.setZoom(14);
			mMapController.animateTo(new GeoPoint(44840290, -572662));
			return;
		}
		if (mIsShowStationMode) {
			mMapController.setZoom(16);
			mMapController.animateTo(geoPoint);
		}
		if (mMapPreferences.getBoolean(getString(R.string.center_on_location),
				false)
				|| mIsFirstFix) {
			mMapController.setZoom(16);
			mMapController.animateTo(geoPoint);
			mIsFirstFix = false;
		}
	}

	private StationOverlay getLastStationOverlay() {
		int i = mMapOverlays.size() - (mMyLocationOverlay == null ? 1 : 2);
		if (i < 0)
			return null;
		return ((StationOverlay) mMapOverlays.get(i));
	}

	@Override
	public void onListUpdated() {
		int currentId = -1;
		StationOverlay stationOverlay = null;
		boolean hasCurrent = false;
		boolean useLocation = mMyLocationOverlay != null;
		int size = mMapOverlays.size();
		BalloonOverlayView balloon = null;
		if (size >= 2 || (size >= 1 && !useLocation)) {
			StationOverlay station = getLastStationOverlay();
			if (station.isCurrent()) {
				currentId = station.getStation().getId();
				balloon = station.getBallonView();
			}
		}
		mMapOverlays.clear();
		ArrayList<StationOverlay> stations = mVcuboidManager
				.getVisibleStations();
		mMapOverlays.addAll(stations);
		Collections.reverse(mMapOverlays);
		if (currentId != -1) {
			for (Iterator<Overlay> it = mMapOverlays.iterator(); it.hasNext();) {
				stationOverlay = (StationOverlay) it.next();
				if (stationOverlay.getStation().getId() == currentId) {
					it.remove();
					hasCurrent = true;
					break;
				}
			}
			if (hasCurrent) {
				stationOverlay.setCurrent();
				mMapOverlays.add(stationOverlay);
				StationOverlay.setBalloonView(balloon);
				stationOverlay.refreshBalloon();
			} else if (currentId != -1) {
				StationOverlay.hideBalloonWithNoStation();
			} else {
				StationOverlay.setBalloonView(null);
			}
		}
		if (mMyLocationOverlay != null) {
			mMapOverlays.add(mMyLocationOverlay);
		}
		// mMapView.invalidate();
	}

	@Override
	public void showUpdateAllStationsOnProgress(boolean animate) {
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.updating);
		loading.setVisibility(View.VISIBLE);
		if (animate) {
			AnimationSet set = new AnimationSet(true);
			Animation animation = new AlphaAnimation(0.0f, 1.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
					0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, -1.0f,
					Animation.RELATIVE_TO_SELF, 0.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			LayoutAnimationController controller = new LayoutAnimationController(
					set, 0.5f);
			loading.setLayoutAnimation(controller);
		}
	}

	@Override
	public void finishUpdateAllStationsOnProgress(boolean animate) {
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.updating);
		loading.setVisibility(View.INVISIBLE);
		if (animate) {
			AnimationSet set = new AnimationSet(true);
			Animation animation = new AlphaAnimation(1.0f, 0.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
					0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, -1.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			loading.startAnimation(set);
			loading.setVisibility(View.INVISIBLE);
		}
		// onListUpdated();
	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case RestClient.NETWORK_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.network_error)).setMessage(
					(getString(R.string.network_error_summary)))
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case RestClient.JSON_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.json_error)).setMessage(
					(getString(R.string.json_error_summary)))
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case RestClient.DB_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.db_error)).setMessage(
					(getString(R.string.db_error_summary))).setPositiveButton(
					"Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					}).create();
		case MyLocationProvider.ENABLE_GPS:
			Log.i("Vcuboid", "onPrepareDialog : ENABLE_GPS");
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					getString(R.string.gps_disabled)).setMessage(
					getString(R.string.should_enable_gps) + "\n"
							+ getString(R.string.show_location_parameters))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent gpsOptionsIntent = new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivity(gpsOptionsIntent);
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case MyLocationProvider.NO_LOCATION_PROVIDER:
			Log.i("Vcuboid", "onPrepareDialog : NO_LOCATION_PROVIDER");
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					getString(R.string.location_disabled)).setMessage(
					getString(R.string.should_enable_location) + "\n"
							+ getString(R.string.show_location_parameters))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent gpsOptionsIntent = new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivity(gpsOptionsIntent);
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case VcuboidManager.REMOVE_FROM_FAVORITE:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.remove_favorite)).setMessage(
					(getString(R.string.remove_favorite_sure)))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mVcuboidManager.setFavorite(mSelected,
											false);
									if (mMapPreferences
											.getBoolean(
													getString(R.string.favorite_filter),
													false)) {
										((StationOverlay) mMapOverlays
												.get(mMapOverlays.size()
														- (mMyLocationOverlay == null ? 1
																: 2)))
												.hideBalloon();
										mMapOverlays
												.remove(mMapOverlays.size()
														- (mMyLocationOverlay == null ? 1
																: 2));
										// mMapView.invalidate();
									} else {
										((StationOverlay) mMapOverlays
												.get(mMapOverlays.size()
														- (mMyLocationOverlay == null ? 1
																: 2)))
												.getStation()
												.setFavorite(false);
									}
									dialog.dismiss();
								}
							}).setOnCancelListener(
							new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface arg0) {
									((StationOverlay) mMapOverlays
											.get(mMapOverlays.size()
													- (mMyLocationOverlay == null ? 1
															: 2)))
											.refreshBalloon();

								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		}
		return super.onCreateDialog(id);
	}
}
