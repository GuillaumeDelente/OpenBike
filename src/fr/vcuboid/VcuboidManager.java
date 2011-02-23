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
package fr.vcuboid;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import fr.vcuboid.database.VcuboidDBAdapter;
import fr.vcuboid.filter.Filtering;
import fr.vcuboid.filter.VcubFilter;
import fr.vcuboid.list.VcuboidListActivity;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;
import fr.vcuboid.utils.Utils;

public class VcuboidManager {
	
	public static boolean mIsUpdating = false;
	public static final int RETRIEVE_ALL_STATIONS = 0;
	public static final int REMOVE_FROM_FAVORITE = 1;
	protected static VcuboidDBAdapter mVcuboidDBAdapter = null;
	protected static IVcuboidActivity mActivity = null;
	protected static MyLocationProvider mLocationProvider = null;
	private static VcuboidManager mThis;
	private static SharedPreferences mFilterPreferences = null;
	private static ArrayList<StationOverlay> mVisibleStations = null;
	private VcubFilter mVcubFilter = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;
	private CreateVisibleStationsTask mCreateVisibleStationsTask = null;
	
	public VcubFilter getVcubFilter() {
		return mVcubFilter;
	}

	public void setVcubFilter(VcubFilter vcubFilter) {
		mVcubFilter = vcubFilter;
	}

	private VcuboidManager(IVcuboidActivity activity) {
		Log.e("Vcuboid", "New Manager created");
		mActivity = activity;
		mVcuboidDBAdapter = new VcuboidDBAdapter((Context) activity);
		mVcuboidDBAdapter.open();
		PreferenceManager.setDefaultValues((Context) mActivity, R.xml.location_preferences, false);
		PreferenceManager.setDefaultValues((Context) mActivity, R.xml.filter_preferences, false);
		PreferenceManager.setDefaultValues((Context) mActivity, R.xml.map_preferences, false);
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		if (mFilterPreferences.getBoolean(
				((Context) mActivity).getString(R.string.use_location), true))
			useLocation();
		initializeFilter();
	}
	
	public static synchronized VcuboidManager getVcuboidManagerInstance(IVcuboidActivity activity) {
		Log.e("Vcuboid", "Getting VcuboidManager instance");
		if (mThis == null) {
			mThis = new VcuboidManager(activity);
		} else {
			mActivity = activity;
			mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		}
		return mThis;
	}

	public static synchronized VcuboidManager getVcuboidManagerInstance() {
		return mThis;
	}

	public void setCurrentActivity(IVcuboidActivity activity) {
		mActivity = activity;
		if (mUpdateAllStationsTask != null && mUpdateAllStationsTask.getProgress() < 50)
			activity.showUpdateAllStationsOnProgress(false);
	}

	public void attach(VcuboidListActivity activity) {
		mActivity = activity;
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		if (mGetAllStationsTask != null) {
			mThis.retrieveGetAllStationTask();
		} else if (mUpdateAllStationsTask != null) {
			if (mUpdateAllStationsTask.getProgress() < 50)
				mThis.retrieveUpdateAllStationTask();
		}
	}

	public void detach() {
		mActivity = null;
	}

	public boolean executeGetAllStationsTask() {
		Log.e("Vcuboid", "executeGetAllStationsTask");
		if (mGetAllStationsTask == null) {
			mGetAllStationsTask = (GetAllStationsTask) new GetAllStationsTask()
					.execute();
			return true;
		}
		return false;
	}
	
	public boolean executeCreateVisibleStationsTask() {
		if (mCreateVisibleStationsTask == null) {
			if (mVcubFilter.isNeedDbQuery()) {
			mCreateVisibleStationsTask =  (CreateVisibleStationsTask) new CreateVisibleStationsTask()
					.execute();
			} else {
				Filtering.filter(mVisibleStations, mVcubFilter);
				mActivity.onListUpdated();
			}
			return true;
		} else {
			new Exception("CreateVisibleStation already launched");
		}
		return false;
	}

	public boolean executeUpdateAllStationsTask() {
		if (mUpdateAllStationsTask == null) {
			mUpdateAllStationsTask = (UpdateAllStationsTask) new UpdateAllStationsTask()
					.execute();
			return true;
		}
		return false;
	}

	private void retrieveGetAllStationTask() {
		int progress = mGetAllStationsTask.getProgress();
		if (progress < 100) {
			/*
			((IVcuboidActivity) mActivity).showGetAllStationsOnProgress();
			((IVcuboidActivity) mActivity).updateGetAllStationsOnProgress(mGetAllStationsTask
					.getProgress());
					*/
		} else {
			((IVcuboidActivity) mActivity).finishGetAllStationsOnProgress();
			mGetAllStationsTask = null;
		}
	}

	private void retrieveUpdateAllStationTask() {
		int progress = mUpdateAllStationsTask.getProgress();
		if (progress >= 50) {
			mIsUpdating = false;
			mUpdateAllStationsTask = null;
		} else {
			((IVcuboidActivity) mActivity).showUpdateAllStationsOnProgress(false);
		}
	}
	
	private void initializeFilter() {
		mVcubFilter = new VcubFilter((Context) mActivity);
	}

	public void clearDB() {
		mVcuboidDBAdapter.reset();
		mVcuboidDBAdapter.close();
		mVcuboidDBAdapter.open();
	}
	
	public void setFavorite(int id, boolean isChecked) {
		mVcuboidDBAdapter.updateFavorite(id, isChecked);
		StationOverlay overlay;
		Station station;
		Iterator<StationOverlay> it = mVisibleStations.iterator();
		while(it.hasNext()) {
			overlay = it.next();
			station = overlay.getStation();
			if (station.getId() == id) {
				station.setFavorite(isChecked);
				Log.d("Vcuboid", "Favorite in DB id " + station.getId());
				if (mVcubFilter.isShowOnlyFavorites() && !isChecked) {
					Log.d("Vcuboid", "Removing station");
					it.remove();
				}
				return;
			}
		}
		Log.d("Vcuboid", "List size " + mVisibleStations.size());
	}
	
	public ArrayList<StationOverlay> getVisibleStations() {
		if (mVisibleStations == null && mCreateVisibleStationsTask == null) {
			mVisibleStations = new ArrayList<StationOverlay>();
			executeCreateVisibleStationsTask();
		}
		return mVisibleStations;
	}
	
	private void updateDistance(Location location) {
		if (mVisibleStations == null)
			getVisibleStations();
		StationOverlay overlay;
		Station station;
		Location l = new Location(location);
		GeoPoint point;
		Iterator<StationOverlay> it = mVisibleStations.iterator();
		while(it.hasNext()) {
			overlay = it.next();
			station = overlay.getStation();
			point = station.getGeoPoint();
			l.setLatitude((double) point.getLatitudeE6()*1E-6);
			l.setLongitude((double) point.getLongitudeE6()*1E-6);
			station.setDistance((int) location.distanceTo(l));
		}
	}
	
	private void resetDistances() {
		if (mVisibleStations == null)
			return;
		StationOverlay overlay;
		Station station;
		Iterator<StationOverlay> it = mVisibleStations.iterator();
		while(it.hasNext()) {
			overlay = it.next();
			station = overlay.getStation();
			station.setDistance(-1);
		}
	}
	
	public void onLocationChanged(Location location) {
		if (mCreateVisibleStationsTask == null) {
			if (location == null) {
				mVcubFilter.setNeedDbQuery();
				Log.d("Vcuboid", "Location unavailable, need db = " + mVcubFilter.isNeedDbQuery());
				executeCreateVisibleStationsTask();
				//resetDistances();
				//Utils.sortStationsByName(mVisibleStations);
			} else if (mVcubFilter.isFilteringByDistance()) {
				if (!mVcubFilter.isNeedDbQuery())
					updateDistance(location); 
				executeCreateVisibleStationsTask();
			} else {
				updateDistance(location);
				Utils.sortStationsByDistance(mVisibleStations);
			}
			mActivity.onLocationChanged(location);
		}
	}

	public void useLocation() {
		if (mLocationProvider == null)
			mLocationProvider = new MyLocationProvider((Context) mActivity, this);
	}
	
	public void dontUseLocation() {
		mActivity.removeDialog(MyLocationProvider.ENABLE_GPS);
		mLocationProvider.disableMyLocation();
		mLocationProvider = null;
		resetDistances();
		Utils.sortStationsByName(mVisibleStations);
	}

	public Location getCurrentLocation() {
		if (mLocationProvider != null && mLocationProvider.isLocationAvailable()) {
			return mLocationProvider.getMyLocation();
		} else {
			return null;
		}
	}
	
	public void showAskForGps() {
		mActivity.showDialog(MyLocationProvider.ENABLE_GPS);
	}
	
	public void showNoLocationProvider() {
		mActivity.showDialog(MyLocationProvider.NO_LOCATION_PROVIDER);
	}
	
	public void startLocation() {
		if (mLocationProvider != null)
			mLocationProvider.enableMyLocation();
	}
	
	public void stopLocation() {
		if (mLocationProvider != null)
			mLocationProvider.disableMyLocation();
	}
	
	public Station getStation(int id) {
		return mVcuboidDBAdapter.getStation(id);
	}
	
	/************************************/
	/************************************/
	/*******					*********/
	/*******   Private Classes	*********/
	/*******					*********/
	/************************************/
	/************************************/
	
	private class GetAllStationsTask extends AsyncTask<Void, Void, Void> {

		private int progress = 0;

		@Override
		protected void onPreExecute() {
			Log.d("Vcuboid", "onPreExecute GetAllStations");
			if (mActivity != null) {
				((IVcuboidActivity) mActivity).showGetAllStationsOnProgress();
			}
		}

		@Override
		protected Void doInBackground(Void... unused) {
			String json = RestClient
					.connect("http://vcuboid.appspot.com/stations");
			publishProgress();
			RestClient.jsonStationsToDb(json, mVcuboidDBAdapter);
			publishProgress();
			return (null);
		}

		@Override
		protected void onProgressUpdate(Void... unused) {
			if (mActivity != null) {
				mActivity.updateGetAllStationsOnProgress(progress += 50);
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			executeCreateVisibleStationsTask();
			if (mActivity != null) {
				((IVcuboidActivity) mActivity).finishGetAllStationsOnProgress();
				mGetAllStationsTask = null;
				Log.d("Vcuboid", "Async task get all stations finished");
			}
		}

		int getProgress() {
			return progress;
		}
	}

	private class UpdateAllStationsTask extends AsyncTask<Void, Integer, Void> {
		private int mProgress = 0;

		protected void onPreExecute() {
			mIsUpdating = true;
			if (mActivity != null) {
				mActivity.showUpdateAllStationsOnProgress(true);
			}
		}

		protected Void doInBackground(Void... unused) {
			String json = RestClient
			.connect("http://vcuboid.appspot.com/stations");
			if (json == null) {
				publishProgress(RestClient.NETWORK_ERROR);
				return null;
			}
			if (!RestClient.updateListFromJson(json,
					mVisibleStations)) {
				publishProgress(RestClient.JSON_ERROR);
				return null;
			}
			publishProgress(50);
			if (!RestClient.updateDbFromJson(json,
					mVcuboidDBAdapter)) {
				publishProgress(RestClient.DB_ERROR);
				return null;
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			mProgress = progress[0];
			if (mActivity != null) {
				mActivity.onListUpdated();
				mActivity.finishUpdateAllStationsOnProgress(true);
				if (progress[0] < 0) {
					mActivity.showDialog(progress[0]);
				}
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (mActivity == null) {
				mProgress = 100;
			} else {
				mIsUpdating = false;
				executeCreateVisibleStationsTask();
				mUpdateAllStationsTask = null;
				Log.d("Vcuboid", "Async task update finished");
			}
		}

		public int getProgress() {
			return mProgress;
		}
	}
	
	private class CreateVisibleStationsTask extends AsyncTask<Void, Void, Boolean> {
		
		private boolean updateListFromDb(boolean useList) {
			Log.d("Vcuboid", "UpdatingListFromDb");
			ArrayList<StationOverlay> stationsList = null;
			if (!useList)
				stationsList = new ArrayList<StationOverlay>(mVisibleStations.size());
			if (mVcuboidDBAdapter.getStationCount() == 0) {
				return false;
			}
			Cursor cursor = mVcuboidDBAdapter
					.getFilteredStationsCursor(Utils.whereClauseFromFilter(mVcubFilter), 
							mLocationProvider == null 
								|| mLocationProvider.getMyLocation() == null ?
										VcuboidDBAdapter.KEY_NAME 
										: null);
			StationOverlay stationOverlay;
			Location stationLocation = null;
			Location location = null;
			int distanceToStation = 0;
			if (mLocationProvider != null) {
				location = mLocationProvider.getMyLocation();
				if (location != null)
					stationLocation = new Location(location);
			}
			while(cursor.moveToNext()) {
				if (stationLocation != null) {
					stationLocation.setLatitude((double) cursor
							.getInt(VcuboidDBAdapter.LATITUDE_COLUMN)*1E-6);
					stationLocation.setLongitude((double) cursor
							.getInt(VcuboidDBAdapter.LONGITUDE_COLUMN)*1E-6);
					distanceToStation = (int) stationLocation.distanceTo(location);
					if (mVcubFilter.isFilteringByDistance() && 
							mVcubFilter.getDistanceFilter() < distanceToStation)
						continue;
				}
				stationOverlay = new StationOverlay(
						new Station(cursor.getInt(VcuboidDBAdapter.ID_COLUMN), 
								cursor.getString(VcuboidDBAdapter.NETWORK_COLUMN), 
								cursor.getString(VcuboidDBAdapter.NAME_COLUMN), 
								cursor.getString(VcuboidDBAdapter.ADDRESS_COLUMN), 
								cursor.getInt(VcuboidDBAdapter.LONGITUDE_COLUMN), 
								cursor.getInt(VcuboidDBAdapter.LATITUDE_COLUMN),
								cursor.getInt(VcuboidDBAdapter.BIKES_COLUMN), 
								cursor.getInt(VcuboidDBAdapter.SLOTS_COLUMN), 
								cursor.getInt(VcuboidDBAdapter.OPEN_COLUMN) == 0 ?
										false : true,
								cursor.getInt(VcuboidDBAdapter.FAVORITE_COLUMN) == 0 ?
										false : true,
								cursor.getInt(VcuboidDBAdapter.PAYMENT_COLUMN) == 0 ?
												false : true,
										stationLocation != null ? distanceToStation : -1));
				if (useList)
					mVisibleStations.add(stationOverlay);
				else
					stationsList.add(stationOverlay);
			}
			cursor.close();
			if (mLocationProvider != null) {
				Utils.sortStationsByDistance(useList ? mVisibleStations : stationsList);
			}
			if (!useList) {
				Log.d("Vcuboid", "Create List without mVisibleStation");
				mVisibleStations.clear();
				mVisibleStations.addAll(stationsList);
				stationsList = null;
			}
			mVcubFilter.setNeedDbQuery();
			return true;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mActivity instanceof VcuboidListActivity) {
				((VcuboidListActivity) mActivity).setLoadingList();
			}
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
			if (mVisibleStations == null) {
				Log.e("Vcuboid", "mVisibleStations is null in createVisibleStationList");
				//mVisibleStations = new ArrayList<StationOverlay>();
				//return false;
			}
			Log.d("Vcuboid", "Create Visible station list");
			// Hack for ArrayAdapter & Background thread
			if (mActivity instanceof VcuboidListActivity) {
				return updateListFromDb(false);
			} else {
				mVisibleStations.clear();
				return updateListFromDb(true);
			}
		}

		@Override
		protected void onPostExecute(Boolean isListCreated) {
			if (mActivity == null) {
			} else {
				if (!isListCreated)
					executeGetAllStationsTask();
				else 
					mActivity.onListUpdated();
				if (mActivity instanceof VcuboidListActivity) {
					((VcuboidListActivity) mActivity).setEmptyList();
				}
				mCreateVisibleStationsTask = null;
				Log.d("Vcuboid", "Async task create station list finished");
			}
		}
	}
}
