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
package fr.openbike.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import fr.openbike.IActivityHelper;
import fr.openbike.R;
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.service.ILocationService;
import fr.openbike.service.ILocationServiceListener;
import fr.openbike.service.LocationService;
import fr.openbike.ui.StationsOverlay.StationOverlay;
import fr.openbike.utils.ActivityHelper;
import fr.openbike.utils.DetachableResultReceiver;
import fr.openbike.utils.Utils;

public class OpenBikeMapActivity extends MapActivity implements
		ILocationServiceListener, DetachableResultReceiver.Receiver,
		IActivityHelper {

	public static String ACTION_DETAIL = "fr.openbike.action.VIEW_STATION";
	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private StationsOverlay mStationsOverlay;
	private boolean mIsFirstFix = true;
	private ProgressDialog mPdialog = null;
	private ActivityHelper mActivityHelper = null;
	protected DetachableResultReceiver mReceiver = null;

	private SharedPreferences mSharedPreferences = null;
	private MapView mMapView = null;
	private UpdateOverlays mUpdateOverlays = null;
	private PopulateOverlays mPopulateOverlays = null;
	private int mSelected = 0;
	private ServiceConnection mConnection = null;
	private ILocationService mBoundService = null;
	private boolean mIsBound = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_layout);
		mReceiver = DetachableResultReceiver.getInstance(new Handler());
		mActivityHelper = new ActivityHelper(this);
		mActivityHelper.setupActionBar(getString(R.string.station_map));
		mMapView = (MapView) findViewById(R.id.map_view);
		mPdialog = new ProgressDialog(OpenBikeMapActivity.this);
		mMapController = mMapView.getController();
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setBuiltInZoomControls(true);
		mMapView.displayZoomControls(true);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		BitmapDrawable marker = (BitmapDrawable) getResources().getDrawable(
				R.drawable.pin);

		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker
				.getIntrinsicHeight());
		mStationsOverlay = new StationsOverlay(getResources(), marker,
				mMapView, this);
		mMapOverlays = mMapView.getOverlays();
		mMapOverlays.add(mStationsOverlay);
		if (mMyLocationOverlay == null) {
			mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		}
		mMapOverlays.add(mMyLocationOverlay);
		mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
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
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	void doBindService() {
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
		mReceiver.setReceiver(this);
		Intent intent = getIntent();
		mStationsOverlay.setCurrentLocation(null);
		mMyLocationOverlay.setCurrentLocation(null);
		boolean useLocation = mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, false);
		if (useLocation) {
			doBindService();
		}
		if (ACTION_DETAIL.equals(intent.getAction())) {
			setStation(intent.getData());
		} else {
			if (!useLocation) {
				executePopulateOverlays();
			}
		}
		mActivityHelper.onResume();
		super.onResume();
	}

	@Override
	protected void onPause() {
		mReceiver.clearReceiver();
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
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActivityHelper.onPostCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mActivityHelper.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.map_menu, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mActivityHelper.onPrepareOptionsMenu(menu);
		if (OpenBikeMapActivity.ACTION_DETAIL.equals(getIntent().getAction())) {
			menu.setGroupVisible(R.id.map_menu_global, false);
			menu.setGroupVisible(R.id.map_menu_detail, true);
		} else {
			menu.setGroupVisible(R.id.map_menu_global, true);
			menu.setGroupVisible(R.id.map_menu_detail, false);
		}

		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_show_global_map:
			startActivity(new Intent(this, OpenBikeMapActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		default:
			return mActivityHelper.onOptionsItemSelected(item)
					|| super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.progress:
			mPdialog.setCancelable(false);
			return mPdialog;
		default:
			Dialog dialog = getActivityHelper().onCreateDialog(id);
			if (dialog != null)
				return dialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		getActivityHelper().onPrepareDialog(id, dialog);
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public void onLocationChanged(Location location, boolean firstFix) {
		mMyLocationOverlay.setCurrentLocation(location);
		mStationsOverlay.setCurrentLocation(location);
		boolean isDistanceFiltering = mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER, false);
		if (firstFix || isDistanceFiltering) {
			executePopulateOverlays();
		} else {
			mMapView.invalidate();
		}
		if ((mIsFirstFix && location != null && !OpenBikeMapActivity.ACTION_DETAIL
				.equals(getIntent().getAction()))
				|| mSharedPreferences.getBoolean(
						AbstractPreferencesActivity.CENTER_PREFERENCE, false)) {
			zoomAndCenter(location);
		}
		mIsFirstFix = (location == null) ? true : false;
	}

	public void onListUpdated() {
		boolean needUpdate = mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER, false)
				|| mSharedPreferences.getBoolean(
						AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER,
						false)
				|| mSharedPreferences.getBoolean(
						AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER,
						false);
		if (needUpdate) {
			executePopulateOverlays();
		} else {
			executeUpdateOverlays();
		}
	}

	private void setStation(Uri uri) {
		Cursor station = managedQuery(uri, null, null, null, null);
		int latitude = station.getInt(station
				.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE));
		int longitude = station.getInt(station
				.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE));
		mStationsOverlay.setItems(mStationsOverlay.getOverlaysFromCursor(
				station, mIsBound ? mBoundService.getCurrentLocation() : null,
				0));
		zoomAndCenter(new GeoPoint(latitude, longitude));
		mMapView.invalidate();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
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
			mMapController.animateTo(new GeoPoint(mSharedPreferences.getInt(
					AbstractPreferencesActivity.NETWORK_LATITUDE, 0),
					mSharedPreferences.getInt(
							AbstractPreferencesActivity.NETWORK_LONGITUDE, 0)));
			return;
		}
		mMapController.setZoom(16);
		mMapController.animateTo(geoPoint);
	}

	public void showProgressDialog(String title, String message) {
		mPdialog.setTitle(title);
		mPdialog.setMessage(message);
		if (!mPdialog.isShowing())
			showDialog(R.id.progress);
	}

	public void dismissProgressDialog() {
		// onListUpdated();
		if (mPdialog.isShowing())
			dismissDialog(R.id.progress);
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

		private OpenBikeDBAdapter mOpenBikeDBAdapter = OpenBikeDBAdapter
				.getInstance(OpenBikeMapActivity.this);
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
							.whereClauseFromFilter(mSharedPreferences), null);
			if (isCancelled()) {
				cursor.close();
				return CANCELLED;
			}
			// FIXME
			if (mIsBound && mBoundService == null) {
				Log.e("OpenBike", "mBoundService is null !");
			} else {
				mOverlays = mStationsOverlay
						.getOverlaysFromCursor(
								cursor,
								mIsBound ? mBoundService.getCurrentLocation()
										: null,
								mSharedPreferences
										.getBoolean(
												AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER,
												false) ? mSharedPreferences
										.getInt(
												AbstractPreferencesActivity.DISTANCE_FILTER,
												1000)
										: 0);
			}
			cursor.close();
			return OK;
		}

		@Override
		protected void onPreExecute() {
			showProgressDialog(getString(R.string.loading),
					getString(R.string.showing_load));
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values[0] == CANCELLED) {
				return;
			} else if (mOverlays != null) {
				mStationsOverlay.setItems(mOverlays);
				mMapView.invalidate();
			} else {
				Log.e("OpenBike", "mOverlays null");
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			dismissProgressDialog();
			mPopulateOverlays = null;
			if (mUpdateOnPostExecute) {
				executeUpdateOverlays();
			}
		}

		@Override
		protected void onCancelled() {
			dismissProgressDialog();
			super.onCancelled();
		}

		private void setUpdateOnPostExecute() {
			mUpdateOnPostExecute = true;
		}
	}

	private class UpdateOverlays extends AsyncTask<Void, Integer, Boolean> {

		private OpenBikeDBAdapter mOpenBikeDBAdapter = OpenBikeDBAdapter
				.getInstance(OpenBikeMapActivity.this);
		private List<StationOverlay> mOverlays = null;

		@Override
		protected Boolean doInBackground(Void... unused) {
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
							.whereClauseFromFilter(mSharedPreferences),
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
		}
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openbike.IActivityHelper#getActivityHelper()
	 */
	@Override
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.openbike.service.ILocationServiceListener#onLocationProvidersChanged
	 * (int)
	 */
	@Override
	public void onLocationProvidersChanged(int id) {
		showDialog(id);
	}
}
