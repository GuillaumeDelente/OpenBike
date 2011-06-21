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
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
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
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import fr.openbike.ILocationService;
import fr.openbike.ILocationServiceListener;
import fr.openbike.IOpenBikeActivity;
import fr.openbike.LocationService;
import fr.openbike.OpenBikeManager;
import fr.openbike.R;
import fr.openbike.RestClient;
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.filter.BikeFilter;
import fr.openbike.filter.FilterPreferencesActivity;
import fr.openbike.list.OpenBikeListActivity;
import fr.openbike.map.StationsOverlay.StationOverlay;
import fr.openbike.object.Network;
import fr.openbike.utils.Utils;

public class OpenBikeMapActivity extends MapActivity implements
		IOpenBikeActivity, ILocationServiceListener {

	public static String ACTION_DETAIL = "fr.openbike.action.VIEW_STATION";
	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private StationsOverlay mStationsOverlay;
	private boolean mIsFirstFix = true;
	private ProgressDialog mPdialog = null;

	private SharedPreferences mMapPreferences = null;
	private MapView mMapView = null;
	private OpenBikeManager mOpenBikeManager = null;
	private UpdateOverlays mUpdateOverlays = null;
	private PopulateOverlays mPopulateOverlays = null;
	private int mSelected = 0;
	private ServiceConnection mConnection = null;
	private ILocationService mBoundService = null;
	private SharedPreferences mPreferences = null;
	private boolean mIsBound = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_layout);
		mMapView = (MapView) findViewById(R.id.map_view);
		mPdialog = new ProgressDialog(OpenBikeMapActivity.this);
		mMapController = mMapView.getController();
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setBuiltInZoomControls(true);
		mMapView.displayZoomControls(true);
		mOpenBikeManager = OpenBikeManager.getOpenBikeManagerInstance(this);
		mMapPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mMapOverlays = mMapView.getOverlays();
		handleIntent(getIntent());
		OpenBikeManager.setCurrentActivity(this);
		// mOpenBikeManager.startLocation();
		BitmapDrawable marker = (BitmapDrawable) getResources().getDrawable(
				R.drawable.pin);

		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
				.getIntrinsicHeight());
		mMapOverlays.clear();
		mStationsOverlay = new StationsOverlay(getResources(), marker, mMapView);
		mMapOverlays.add(mStationsOverlay);
		if (mMyLocationOverlay == null) {
			mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		}
		mMapOverlays.add(mMyLocationOverlay);
		mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				Log.d("OpenBike", "Service connected");
				mBoundService = ((LocationService.LocationServiceBinder) service)
						.getService();
				mBoundService.addListener(OpenBikeMapActivity.this);
			}

			public void onServiceDisconnected(ComponentName className) {
				mBoundService = null;
				Toast.makeText(OpenBikeMapActivity.this, "Disconnected",
						Toast.LENGTH_SHORT).show();
			}
		};
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.d("OpenBike", "On new intent " + intent.getAction());
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		mMapOverlays.clear();
		// TODO
		// Location location = mOpenBikeManager.getCurrentLocation();
		Location location = null;
		if (ACTION_DETAIL.equals(intent.getAction())) {
			setStation(intent.getData());
			if (mMapPreferences.getBoolean(
					FilterPreferencesActivity.CENTER_PREFERENCE, false)
					&& location != null) {
				zoomAndCenter(location);
			} else {
				// TODO
				/*
				 * zoomAndCenter(((StationOverlay) mMapOverlays.get(0))
				 * .getStation().getGeoPoint());
				 */
			}
		} else {
			setStationList();
			zoomAndCenter(location);
		}
		mMapView.invalidate();
	}

	void doBindService() {
		Log.d("OpenBike", "Service binded");
		bindService(new Intent(this, LocationService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onResume() {
		Log.d("OpenBike", "On resume " + getIntent().getAction());
		OpenBikeManager.setCurrentActivity(this);
		if (mPreferences.getBoolean(
				FilterPreferencesActivity.LOCATION_PREFERENCE, false)) {
			doBindService();
		} else {
			mMyLocationOverlay.setCurrentLocation(null);
			executePopulateOverlays();
		}
		
		/*
		 * 
		 * if (mRetrieveList) { // Know if we passed by onNewIntent() just
		 * before mMapOverlays.clear(); Drawable marker =
		 * getResources().getDrawable(R.drawable.pin);
		 * 
		 * marker.setBounds(0, 0, marker.getIntrinsicWidth(),
		 * marker.getIntrinsicHeight());
		 * 
		 * mMapOverlays.add(new SitesOverlay(marker));
		 * 
		 * if (ACTION_DETAIL.equals(intent.getAction())) {
		 * setStation(intent.getData()); if (mMapPreferences.getBoolean(
		 * FilterPreferencesActivity.CENTER_PREFERENCE, false)) {
		 * zoomAndCenter(OpenBikeManager.getCurrentLocation()); } else {
		 * zoomAndCenter(((StationOverlay) mMapOverlays.get(0))
		 * .getStation().getGeoPoint()); } } else { setStationList(); if
		 * (mMapPreferences.getBoolean(
		 * FilterPreferencesActivity.CENTER_PREFERENCE, false))
		 * zoomAndCenter(OpenBikeManager.getCurrentLocation()); } }
		 * mRetrieveList = true;
		 */
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

		// TODO
		/*
		 * StationOverlay station = getLastStationOverlay(); if (station !=
		 * null) { station.hideBalloon(); mOpenBikeManager.sortStations(); }
		 * mOpenBikeManager.stopLocation(); hideOverlayBalloon();
		 * StationOverlay.setBalloonView(null);
		 */
		mStationsOverlay.hideBalloon();
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (mIsBound)
			doUnbindService();
		super.onStop();
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
		case LocationService.ENABLE_GPS:
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
		case LocationService.NO_LOCATION_PROVIDER:
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
									// TODO
									/*
									 * if (mMapPreferences .getBoolean(
									 * FilterPreferencesActivity
									 * .FAVORITE_FILTER, false)) {
									 * ((StationOverlay) mMapOverlays
									 * .get(mMapOverlays.size() - 2))
									 * .hideBalloon(); mMapOverlays
									 * .remove(mMapOverlays.size() - 2); //
									 * mMapView.invalidate(); } else {
									 * ((StationOverlay) mMapOverlays
									 * .get(mMapOverlays.size() - 2))
									 * .getStation() .setFavorite(false); }
									 */
									dialog.dismiss();
								}
							}).setOnCancelListener(
							new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface arg0) {
									// TODO
									/*
									 * ((StationOverlay) mMapOverlays
									 * .get(mMapOverlays.size() - 2))
									 * .refreshBalloon();
									 */

								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case OpenBikeManager.PROGRESS_DIALOG:
			mPdialog.setCancelable(false);
			return mPdialog;
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
	public void onLocationChanged(Location location, boolean firstFix) {
		Log.d("OpenBike", "Location received");
		mMyLocationOverlay.setCurrentLocation(location);
		mStationsOverlay.setCurrentLocation(location);
		BikeFilter filter = mOpenBikeManager.getOpenBikeFilter();
		if (firstFix || filter.isFilteringByDistance()) {
			executePopulateOverlays();
		} else {
			mMapView.invalidate();
		}
		if ((mIsFirstFix && location != null && !OpenBikeMapActivity.ACTION_DETAIL
				.equals(getIntent().getAction()))
				|| mMapPreferences.getBoolean(
						FilterPreferencesActivity.CENTER_PREFERENCE, false)) {
			zoomAndCenter(location);
		}
		mIsFirstFix = (location == null) ? true : false;
	}

	@Override
	public void onListUpdated() {
		BikeFilter filter = mOpenBikeManager.getOpenBikeFilter();
		if (filter.isFilteringByDistance() || filter.isShowOnlyWithBikes()
				|| filter.isShowOnlyWithSlots()) {
			executePopulateOverlays();
		} else {
			executeUpdateOverlays();
		}
	}

	public void setFavorite(int id, boolean isChecked) {
		// TODO
		/*
		 * mSelected = id; if (isChecked) { mOpenBikeManager.setFavorite(id,
		 * true); int size = mMapOverlays.size(); int baloonPosition = size -
		 * (mMapOverlays.get(size - 1) instanceof MyLocationOverlay ? 2 : 1);
		 * Overlay overlay = mMapOverlays.get(baloonPosition); if (overlay
		 * instanceof StationOverlay) { ((StationOverlay)
		 * overlay).getStation().setFavorite(true); } else { } //
		 * onListUpdated(); } else {
		 * showDialog(OpenBikeManager.REMOVE_FROM_FAVORITE); }
		 */
	}

	private void setStation(Uri uri) {
		// TODO
		/*
		 * Cursor station = managedQuery(uri, null, null, null, null); int
		 * latitude = station.getInt(station
		 * .getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE)); int longitude =
		 * station.getInt(station
		 * .getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE));
		 * mMapOverlays.add(new StationOverlay(new MinimalStation(station
		 * .getInt(station.getColumnIndex(BaseColumns._ID)), station
		 * .getString(station.getColumnIndex(OpenBikeDBAdapter.KEY_NAME)),
		 * longitude, latitude, station.getInt(station
		 * .getColumnIndex(OpenBikeDBAdapter.KEY_BIKES)), station
		 * .getInt(station .getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS)),
		 * station.getInt(station .getColumnIndex(OpenBikeDBAdapter.KEY_OPEN))
		 * == 1, station.getInt(station
		 * .getColumnIndex(OpenBikeDBAdapter.KEY_FAVORITE)) == 1,
		 * Utils.computeDistance(latitude, longitude))));
		 * zoomAndCenter(((StationOverlay) mMapOverlays.get(0)).getStation()
		 * .getGeoPoint());
		 */
	}

	private void setStationList() {
		// TODO
		/*
		 * mMapOverlays.addAll(mOpenBikeManager.getVisibleStations());
		 * Collections.reverse(mMapOverlays); Log.d("OpenBike",
		 * "Number of Overlays : " + mMapOverlays.size());
		 */
	}

	public void hideOverlayBalloon() {
		// TODO
		/*
		 * int position = mMapOverlays.size() - 2; if (position >= 0) { Overlay
		 * overlay = mMapOverlays.get(position); if (overlay instanceof
		 * StationOverlay) { ((StationOverlay) overlay).hideBalloon(); } else {
		 * // Log.e("Balloon", //
		 * "hideOtherBalloons, before last not a StationOverlay"); } }
		 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openbike.IOpenBikeActivity#showChooseNetwork(java.util.ArrayList)
	 */
	@Override
	public void showChooseNetwork(ArrayList<Network> networks) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showProgressDialog(String title, String message) {
		mPdialog.setTitle(title);
		mPdialog.setMessage(message);
		if (!mPdialog.isShowing())
			showDialog(OpenBikeManager.PROGRESS_DIALOG);
	}

	@Override
	public void dismissProgressDialog() {
		// onListUpdated();
		if (mPdialog.isShowing())
			dismissDialog(OpenBikeManager.PROGRESS_DIALOG);
	}

	private void executePopulateOverlays() {
		if (mPopulateOverlays != null) {
			mPopulateOverlays.cancel(true);
		}
		mPopulateOverlays = (PopulateOverlays) new PopulateOverlays().execute();
	}

	private void executeUpdateOverlays() {
		if (mPopulateOverlays != null) {
			mPopulateOverlays.setUpdateOnPostExecute();
			return;
		}
		if (mUpdateOverlays != null) {
			mUpdateOverlays.cancel(true);
			mUpdateOverlays = null;
		}
		mUpdateOverlays = (UpdateOverlays) new UpdateOverlays().execute();
	}

	private class PopulateOverlays extends AsyncTask<Void, Integer, Void> {

		private OpenBikeDBAdapter mOpenBikeDBAdapter = mOpenBikeManager
				.getDbAdapter();
		private BikeFilter mOpenBikeFilter = mOpenBikeManager
				.getOpenBikeFilter();
		private ArrayList<StationOverlay> mOverlays = null;
		private boolean mUpdateOnPostExecute = false;
		private int OK = 0;
		private int CANCELLED = -1;
		private int EMPTY = -2;

		@Override
		protected Void doInBackground(Void... unused) {
			publishProgress(getStationsCursor());
			return null;
		}

		private int getStationsCursor() {
			if (mOpenBikeDBAdapter.getStationCount() == 0) {
				return EMPTY;
			}
			if (isCancelled())
				return CANCELLED;
			Cursor cursor = mOpenBikeDBAdapter.getFilteredStationsCursor(
					new String[] { BaseColumns._ID, OpenBikeDBAdapter.KEY_OPEN,
							OpenBikeDBAdapter.KEY_LATITUDE,
							OpenBikeDBAdapter.KEY_LONGITUDE,
							OpenBikeDBAdapter.KEY_BIKES,
							OpenBikeDBAdapter.KEY_SLOTS }, Utils
							.whereClauseFromFilter(mOpenBikeFilter), null);
			if (isCancelled()) {
				cursor.close();
				return CANCELLED;
			}
			//FIXME
			if (mIsBound && mBoundService == null) {
				Log.e("OpenBike", "mBoundService is null !");
			} else {
				mOverlays = mStationsOverlay.getOverlaysFromCursor(cursor,
					mOpenBikeFilter, mIsBound ? mBoundService.getCurrentLocation() : null);
			}
			cursor.close();
			return OK;
		}

		@Override
		protected void onPreExecute() {
			Log.d("OpenBike", "onPreExecute map");
			showProgressDialog(getString(R.string.loading),
					getString(R.string.showing_load));
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values[0] == CANCELLED) {
				return;
			} else if (values[0] == CANCELLED) {
				//TODO
			} else if (mOverlays != null) {
				mStationsOverlay.setItems(mOverlays);
				mMapView.invalidate();
			} else {
				Log.e("OpenBike", "mOverlays null");
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			Log.d("OpenBike", "onPostExecute map");
			dismissProgressDialog();
			mPopulateOverlays = null;
			if (mUpdateOnPostExecute) {
				Log.d("OpenBike", "updating overlays after populate");
				executeUpdateOverlays();
			}
		}

		@Override
		protected void onCancelled() {
			Log.d("OpenBike", "onCancelled map");
			dismissProgressDialog();
			super.onCancelled();
		}

		private void setUpdateOnPostExecute() {
			mUpdateOnPostExecute = true;
		}
	}

	private class UpdateOverlays extends AsyncTask<Void, Integer, Boolean> {

		private OpenBikeDBAdapter mOpenBikeDBAdapter = mOpenBikeManager
				.getDbAdapter();
		private BikeFilter mOpenBikeFilter = mOpenBikeManager
				.getOpenBikeFilter();
		private List<StationOverlay> mOverlays = null;

		@Override
		protected Boolean doInBackground(Void... unused) {
			Log.d("OpenBike", "updateOverlays");
			return updateListFromDb();
		}

		private boolean updateListFromDb() {
			if (mOpenBikeDBAdapter.getStationCount() == 0 || isCancelled()) {
				return false;
			}
			Cursor cursor = mOpenBikeDBAdapter.getFilteredStationsCursor(
					new String[] { OpenBikeDBAdapter.KEY_BIKES,
							OpenBikeDBAdapter.KEY_SLOTS,
							OpenBikeDBAdapter.KEY_OPEN }, Utils
							.whereClauseFromFilter(mOpenBikeFilter),
					BaseColumns._ID);
			mOverlays = mStationsOverlay.getOverlayList();
			Iterator<StationOverlay> it = mOverlays.iterator();
			StationOverlay stationOverlay;
			while (it.hasNext() && !isCancelled()) {
				stationOverlay = it.next();
				cursor.moveToNext();
				stationOverlay.setBikes(cursor.getInt(cursor
						.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES)));
				stationOverlay.setSlots(cursor.getInt(cursor
						.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS)));
			}
			cursor.close();
			return true;
		}

		@Override
		protected void onPostExecute(Boolean isListCreated) {
			mMapView.invalidate();
			if (mStationsOverlay.isBalloonShowing()) {
				mStationsOverlay.updateBalloonData(mStationsOverlay
						.getItem(mStationsOverlay.getLastFocusedIndex()));
			}
			mUpdateOverlays = null;
			Log.d("OpenBike", "finish updateOverlays");
		}
	}
}
