/*
 * Copyright (C) 2011 Guillaume Delente
 *
 * This file is part of OpenBike.
 *
 * OpenBike is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * OpenBike is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenBike.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.openbike.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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

import fr.openbike.IOpenBikeActivity;
import fr.openbike.MyLocationProvider;
import fr.openbike.OpenBikeManager;
import fr.openbike.R;
import fr.openbike.RestClient;
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.filter.FilterPreferencesActivity;
import fr.openbike.list.OpenBikeListActivity;
import fr.openbike.object.MinimalStation;
import fr.openbike.object.Network;
import fr.openbike.utils.Utils;

public class OpenBikeMapActivity extends MapActivity implements
		IOpenBikeActivity {

	public static String ACTION_DETAIL = "fr.openbike.action.VIEW_STATION";
	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private boolean mIsFirstFix = true;

	private SharedPreferences mMapPreferences = null;
	private MapView mMapView = null;
	private OpenBikeManager mOpenBikeManager = null;
	private int mSelected = 0;
	private boolean mRetrieveList = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_layout);
		mMapView = (MapView) findViewById(R.id.map_view);
		mMapController = mMapView.getController();
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setBuiltInZoomControls(true);
		mMapView.displayZoomControls(true);
		mOpenBikeManager = OpenBikeManager.getVcuboidManagerInstance(this);
		mMapPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mMapOverlays = mMapView.getOverlays();
		Bitmap marker = BitmapFactory.decodeResource(getResources(),
				R.drawable.pin);
		StationOverlay.init(marker, mMapView, this);
		handleIntent(getIntent());
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		mRetrieveList = false;
		mMapOverlays.clear();
		if (ACTION_DETAIL.equals(intent.getAction())) {
			setStation(intent.getData());
			if (mMapPreferences.getBoolean(
					FilterPreferencesActivity.CENTER_PREFERENCE, false)
					&& OpenBikeManager.getCurrentLocation() != null) {
				zoomAndCenter(OpenBikeManager.getCurrentLocation());
			} else {
				zoomAndCenter(((StationOverlay) mMapOverlays.get(0))
						.getStation().getGeoPoint());
			}
		} else {
			setStationList();
			zoomAndCenter(OpenBikeManager.getCurrentLocation());
		}
		mMapView.invalidate();
	}

	@Override
	protected void onResume() {
		OpenBikeManager.setCurrentActivity(this);
		mOpenBikeManager.startLocation();
		Intent intent = getIntent();
		if (mRetrieveList) {
			mMapOverlays.clear();
			if (ACTION_DETAIL.equals(intent.getAction())) {
				setStation(intent.getData());
				if (mMapPreferences.getBoolean(
						FilterPreferencesActivity.CENTER_PREFERENCE, false)) {
					zoomAndCenter(OpenBikeManager.getCurrentLocation());
				} else {
					zoomAndCenter(((StationOverlay) mMapOverlays.get(0))
							.getStation().getGeoPoint());
				}
			} else {
				setStationList();
				if (mMapPreferences.getBoolean(
						FilterPreferencesActivity.CENTER_PREFERENCE, false))
					zoomAndCenter(OpenBikeManager.getCurrentLocation());
			}
		}
		mRetrieveList = true;

		if (mMyLocationOverlay == null) {
			mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		}
		mMapOverlays.add(mMyLocationOverlay);
		if (mMapPreferences.getBoolean(
				FilterPreferencesActivity.LOCATION_PREFERENCE, false)) {
			if (!mMyLocationOverlay.isMyLocationDrawn()) {
				mMyLocationOverlay.setCurrentLocation(OpenBikeManager
						.getCurrentLocation());
				// mMapView.invalidate();
			}
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		// FIXME: Hack to avoid current station first in the list
		// This appends only when localisation without filtering,
		// we should find why instead of doing fix !
		// Because we don't want any current station in listActivity,
		// and because such an element is placed in first position by
		// sorts, set all stations not current an sort them
		// (to set any current station at its position.
		StationOverlay station = getLastStationOverlay();
		if (station != null) {
			station.hideBalloon();
			mOpenBikeManager.sortStations();
		}
		mOpenBikeManager.stopLocation();
		hideOverlayBalloon();
		StationOverlay.setBalloonView(null);
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (OpenBikeMapActivity.ACTION_DETAIL.equals(getIntent().getAction())) {
			menu.setGroupVisible(R.id.menu_group_map_station, true);
		} else {
			menu.setGroupVisible(R.id.menu_group_map_station, false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_search:
			onSearchRequested();
			return true;
		case R.id.menu_update_all:
			mOpenBikeManager.executeUpdateAllStationsTask(true);
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this,
					OpenBikeMapActivity.ACTION_DETAIL.equals(getIntent()
							.getAction()) ? StationMapFilterActivity.class
							: MapFilterActivity.class));
			return true;
		case R.id.menu_map:
			startActivity(new Intent(this, OpenBikeMapActivity.class).setFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP).setClass(this,
					OpenBikeMapActivity.class));
			return true;
		case R.id.menu_list:
			startActivity(new Intent(this, OpenBikeListActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).setClass(this,
							OpenBikeListActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
		case OpenBikeDBAdapter.JSON_ERROR:
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
		case OpenBikeDBAdapter.DB_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.db_error)).setMessage(
					(getString(R.string.db_error_summary))).setPositiveButton(
					"Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					}).create();
		case MyLocationProvider.ENABLE_GPS:
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
		case OpenBikeManager.REMOVE_FROM_FAVORITE:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.remove_favorite)).setMessage(
					(getString(R.string.remove_favorite_sure)))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mOpenBikeManager.setFavorite(mSelected,
											false);
									if (mMapPreferences
											.getBoolean(
													FilterPreferencesActivity.FAVORITE_FILTER,
													false)) {
										((StationOverlay) mMapOverlays
												.get(mMapOverlays.size()
														- 2))
												.hideBalloon();
										mMapOverlays
												.remove(mMapOverlays.size()
														- 2);
										// mMapView.invalidate();
									} else {
										((StationOverlay) mMapOverlays
												.get(mMapOverlays.size()
														- 2))
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
													- 2))
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

	@Override
	public void showUpdateAllStationsOnProgress(boolean animate) {
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.updating);
		if (loading.getVisibility() == View.VISIBLE)
			return;
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
		if (loading.getVisibility() == View.INVISIBLE)
			return;
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
	public void onLocationChanged(Location location) {
		/*
		if (mMyLocationOverlay == null) {
			mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
			mMapOverlays.add(mMyLocationOverlay);
		}
		*/
		mMyLocationOverlay.setCurrentLocation(location);
		// Because when distance fitler enabled, onListUpdated is called
		if (!mMapPreferences.getBoolean(
				FilterPreferencesActivity.ENABLE_DISTANCE_FILTER, false)) {
			Utils.sortStationsByDistance(mMapOverlays);
			Collections.reverse(mMapOverlays);
		}
		if (OpenBikeMapActivity.ACTION_DETAIL.equals(getIntent().getAction())) {
			MinimalStation station = ((StationOverlay) mMapOverlays.get(0))
					.getStation();
			station.setDistance(Utils.computeDistance(station.getGeoPoint()
					.getLatitudeE6(), station.getGeoPoint().getLongitudeE6()));
		}
		StationOverlay station = getLastStationOverlay();
		if (station != null)
			station.refreshBalloon();
		if ((mIsFirstFix && location != null && !OpenBikeMapActivity.ACTION_DETAIL
				.equals(getIntent().getAction()))
				|| mMapPreferences.getBoolean(
						FilterPreferencesActivity.CENTER_PREFERENCE, false)) {
			zoomAndCenter(location);
		}
		mIsFirstFix = (location == null) ? true : false;
		mMapView.invalidate();
	}

	@Override
	public void onListUpdated() {
		int currentId = -1;
		StationOverlay stationOverlay = null;
		boolean hasCurrent = false;
		BalloonOverlayView balloon = null;
		StationOverlay station = getLastStationOverlay();
		if (station != null && station.isCurrent()) {
			currentId = station.getStation().getId();
			balloon = station.getBallonView();
		}
		mMapOverlays.clear();
		if (ACTION_DETAIL.equals(getIntent().getAction())) {
			setStation(getIntent().getData());
		} else {
			setStationList();
		}
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
		mMapView.invalidate();
	}

	public void setFavorite(int id, boolean isChecked) {
		mSelected = id;
		if (isChecked) {
			mOpenBikeManager.setFavorite(id, true);
			int size = mMapOverlays.size();
			int baloonPosition = size
					- (mMapOverlays.get(size - 1) instanceof MyLocationOverlay ? 2
							: 1);
			Overlay overlay = mMapOverlays.get(baloonPosition);
			if (overlay instanceof StationOverlay) {
				((StationOverlay) overlay).getStation().setFavorite(true);
			} else {
			}
			// onListUpdated();
		} else {
			showDialog(OpenBikeManager.REMOVE_FROM_FAVORITE);
		}
	}

	private void setStation(Uri uri) {
		Cursor station = managedQuery(uri, null, null, null, null);
		int latitude = station.getInt(station
				.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE));
		int longitude = station.getInt(station
				.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE));
		mMapOverlays.add(new StationOverlay(new MinimalStation(station
				.getInt(station.getColumnIndex(BaseColumns._ID)), station
				.getString(station.getColumnIndex(OpenBikeDBAdapter.KEY_NAME)),
				longitude, latitude, station.getInt(station
						.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES)), station
						.getInt(station
								.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS)),
				station.getInt(station
						.getColumnIndex(OpenBikeDBAdapter.KEY_OPEN)) == 1,
				station.getInt(station
						.getColumnIndex(OpenBikeDBAdapter.KEY_FAVORITE)) == 1,
				Utils.computeDistance(latitude, longitude))));
		zoomAndCenter(((StationOverlay) mMapOverlays.get(0)).getStation()
				.getGeoPoint());
	}

	private void setStationList() {
		mMapOverlays.addAll(mOpenBikeManager.getVisibleStations());
		Collections.reverse(mMapOverlays);
	}

	public void hideOverlayBalloon() {
		int position = mMapOverlays.size() - 2;
		if (position >= 0) {
			Overlay overlay = mMapOverlays.get(position);
			if (overlay instanceof StationOverlay) {
				((StationOverlay) overlay).hideBalloon();
			} else {
				// Log.e("Balloon",
				// "hideOtherBalloons, before last not a StationOverlay");
			}
		}
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
			mMapController.animateTo(new GeoPoint(
					OpenBikeManager.NETWORK_LATITUDE,
					OpenBikeManager.NETWORK_LONGITUDE));
			return;
		}
		mMapController.setZoom(16);
		mMapController.animateTo(geoPoint);
	}

	private StationOverlay getLastStationOverlay() {
		int i = mMapOverlays.size() - 2;
		if (i < 0)
			return null;
		return ((StationOverlay) mMapOverlays.get(i));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openbike.IOpenBikeActivity#dismissProgressDialog()
	 */
	@Override
	public void dismissProgressDialog() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openbike.IOpenBikeActivity#showChooseNetwork(java.util.ArrayList)
	 */
	@Override
	public void showChooseNetwork(ArrayList<Network> networks) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openbike.IOpenBikeActivity#showProgressDialog(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void showProgressDialog(String title, String message) {
		// TODO Auto-generated method stub

	}
}
