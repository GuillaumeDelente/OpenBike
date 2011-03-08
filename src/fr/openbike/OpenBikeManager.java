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
package fr.openbike;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.filter.BikeFilter;
import fr.openbike.filter.Filtering;
import fr.openbike.list.OpenBikeListActivity;
import fr.openbike.map.StationOverlay;
import fr.openbike.object.Station;
import fr.openbike.utils.Utils;

public class OpenBikeManager {
	
	public static final String SERVER_URL = "http://openbikeserver.appspot.com/stations";
	public static boolean mIsUpdating = false;
	public static final int RETRIEVE_ALL_STATIONS = 0;
	public static final int REMOVE_FROM_FAVORITE = 1;
	public static final long MIN_UPDATE_TIME = 1 * 1000 * 60;
	public static boolean mIsShowStationMode = false;
	protected static OpenBikeDBAdapter mOpenBikeDBAdapter = null;
	protected static IOpenBikeActivity mActivity = null;
	protected static MyLocationProvider mLocationProvider = null;
	private static OpenBikeManager mThis;
	private static SharedPreferences mFilterPreferences = null;
	private static ArrayList<StationOverlay> mVisibleStations = null;
	private BikeFilter mVcubFilter = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;
	private CreateVisibleStationsTask mCreateVisibleStationsTask = null;
	
	public BikeFilter getVcubFilter() {
		return mVcubFilter;
	}

	public void setVcubFilter(BikeFilter vcubFilter) {
		mVcubFilter = vcubFilter;
	}

	private OpenBikeManager(IOpenBikeActivity activity) {
		//Log.i("OpenBike", "New Manager created");
		mActivity = activity;
		mOpenBikeDBAdapter = new OpenBikeDBAdapter((Context) activity);
		mOpenBikeDBAdapter.open();
		PreferenceManager.setDefaultValues((Context) activity, R.xml.location_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.filter_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.map_preferences, false);
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) activity);
		if (mFilterPreferences.getBoolean(
				((Context) activity).getString(R.string.use_location), true))
			useLocation();
		initializeFilter();
		StationOverlay.initialize((Context) activity);
	}
	
	public static synchronized OpenBikeManager getVcuboidManagerInstance(IOpenBikeActivity activity) {
		//Log.e("OpenBike", "Getting VcuboidManager instance");
		if (mThis == null) {
			mThis = new OpenBikeManager(activity);
		} else {
			mActivity = activity;
			mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		}
		return mThis;
	}

	public static synchronized OpenBikeManager getVcuboidManagerInstance() {
		return mThis;
	}

	public void setCurrentActivity(IOpenBikeActivity activity) {
		setCurrentActivity(activity, false);
	}
	
	public void setCurrentActivity(IOpenBikeActivity activity, boolean isShowStationMode) {
		mActivity = activity;
		if (mUpdateAllStationsTask != null && mUpdateAllStationsTask.getProgress() < 50)
			activity.showUpdateAllStationsOnProgress(false);
	}

	public void attach(OpenBikeListActivity activity) {
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
		//Log.e("OpenBike", "executeGetAllStationsTask");
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
			//Log.e("OpenBike", "VisibleStationsTask already launched");
		}
		return false;
	}

	public boolean executeUpdateAllStationsTask(boolean showToast) {
		if (mActivity == null)
			return false;
		if (mUpdateAllStationsTask == null && (System.currentTimeMillis() 
				- mFilterPreferences.getLong("last_update", 0) > MIN_UPDATE_TIME)) {
			mUpdateAllStationsTask = (UpdateAllStationsTask) new UpdateAllStationsTask()
					.execute();
			return true;
		} else if (showToast) {
			Toast.makeText(((Activity) mActivity), ((Activity) mActivity)
					.getString(R.string.already_uptodate),
					Toast.LENGTH_SHORT).show();
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
			((IOpenBikeActivity) mActivity).finishGetAllStationsOnProgress();
			mGetAllStationsTask = null;
		}
	}

	private void retrieveUpdateAllStationTask() {
		int progress = mUpdateAllStationsTask.getProgress();
		if (progress >= 50) {
			mIsUpdating = false;
			mUpdateAllStationsTask = null;
		} else {
			((IOpenBikeActivity) mActivity).showUpdateAllStationsOnProgress(false);
		}
	}
	
	private void initializeFilter() {
		mVcubFilter = new BikeFilter((Context) mActivity);
	}
	
	public void setShowStationMode(int id) {
		if (id == -1) {
			mIsShowStationMode = false;
			mVcubFilter.setNeedDbQuery(true);
			executeCreateVisibleStationsTask();
		} else {
			mIsShowStationMode = true;
			mVisibleStations.clear();
			mVisibleStations.add(new StationOverlay(getStation(id)));
		}
	}

	public void clearDB() {
		mOpenBikeDBAdapter.reset();
		mOpenBikeDBAdapter.close();
		mOpenBikeDBAdapter.open();
	}
	
	public void setFavorite(int id, boolean isChecked) {
		mOpenBikeDBAdapter.updateFavorite(id, isChecked);
		StationOverlay overlay;
		Station station;
		Iterator<StationOverlay> it = mVisibleStations.iterator();
		while(it.hasNext()) {
			overlay = it.next();
			station = overlay.getStation();
			if (station.getId() == id) {
				station.setFavorite(isChecked);
				//Log.d("OpenBike", "Favorite in DB id " + station.getId());
				if (mVcubFilter.isShowOnlyFavorites() && !isChecked) {
					//Log.d("OpenBike", "Removing station");
					it.remove();
				}
				return;
			}
		}
		//Log.d("OpenBike", "List size " + mVisibleStations.size());
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
		Location l = new Location("");
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
	
	private void computeDistance(Station station) {
		Location location = null;
		if (mLocationProvider == null || 
				(location = mLocationProvider.getMyLocation()) == null) {
			station.setDistance(MyLocationProvider.DISTANCE_UNAVAILABLE);
			return;
		}
		Location l = new Location("");
		GeoPoint point = station.getGeoPoint();
		l.setLatitude((double) point.getLatitudeE6()*1E-6);
		l.setLongitude((double) point.getLongitudeE6()*1E-6);
		station.setDistance((int) location.distanceTo(l));
		//Log.d("OpenBike", "Compute distance : " + l.getLongitude() + l.getLatitude()
		//		+ location.getLongitude() + location.getLatitude() + (int) location.distanceTo(l));
		return;
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
		if (mIsShowStationMode) {
			computeDistance(mVisibleStations.get(0).getStation());
			mActivity.onLocationChanged(location);
			return;
		}
		if (mGetAllStationsTask != null) {
			//First launch, retrieving station list
			return;
		} else if (mCreateVisibleStationsTask == null) {
			if (location == null) {
				mVcubFilter.setNeedDbQuery();
				//Log.d("OpenBike", "Location unavailable, need db = " + mVcubFilter.isNeedDbQuery());
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
		} else { // We received a new location during list creation :
			// cancel the current task and launch a new one.
			mCreateVisibleStationsTask.cancel(true);
			mCreateVisibleStationsTask = null;
			mVcubFilter.setNeedDbQuery(true);
			executeCreateVisibleStationsTask();
		}
		mActivity.onLocationChanged(location);
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
	
	// Only at first launch, as we
	// haven't show location related dialog,
	// check if we should now that we 
	// have retrieved the list
	public void showLocationDialogs() {
		if (! mLocationProvider.isProviderEnabled())
			mActivity.showDialog(MyLocationProvider.NO_LOCATION_PROVIDER);
		else if (! mLocationProvider.isGpsEnabled())
			mActivity.showDialog(MyLocationProvider.ENABLE_GPS);
		
	}
	
	public void showAskForGps() {
		if (!mOpenBikeDBAdapter.isFirstUse())
			mActivity.showDialog(MyLocationProvider.ENABLE_GPS);
	}
	
	public void showNoLocationProvider() {
		if (!mOpenBikeDBAdapter.isFirstUse())
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
		Station station = mOpenBikeDBAdapter.getStation(id);
		computeDistance(station);
		return station;
	}
	
	public void resetDb() {
		mOpenBikeDBAdapter.reset();
	}
	
	/************************************/
	/************************************/
	/*******					*********/
	/*******   Private Classes	*********/
	/*******					*********/
	/************************************/
	/************************************/
	
	private class GetAllStationsTask extends AsyncTask<Void, Integer, Boolean> {

		private int progress = 0;

		@Override
		protected void onPreExecute() {
			//Log.d("OpenBike", "onPreExecute GetAllStations");
			if (mActivity != null) {
				((IOpenBikeActivity) mActivity).showGetAllStationsOnProgress();
			}
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
			String json = RestClient
					.connect(SERVER_URL);
			if (json == null) {
				publishProgress(RestClient.NETWORK_ERROR);
				return false;
			}
			publishProgress(50);
			if (!RestClient.jsonStationsToDb(json, mOpenBikeDBAdapter)) {
				publishProgress(RestClient.JSON_ERROR);
				return false;
			}
			publishProgress(100);
			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			if (mActivity != null) {
				if (progress[0] < 0) {
					mActivity.finishGetAllStationsOnProgress();
					mActivity.showDialog(progress[0]);
				} else if (progress[0] == 50){
					mActivity.updateGetAllStationsOnProgress(50);
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean isListRetrieved) {
			if (!isListRetrieved)
				return;
			mFilterPreferences.edit()
				.putLong("last_update", System.currentTimeMillis()).commit();
			executeCreateVisibleStationsTask();
			if (mActivity != null) {
				((IOpenBikeActivity) mActivity).finishGetAllStationsOnProgress();
				mGetAllStationsTask = null;
				showLocationDialogs();
				//Log.d("OpenBike", "Async task get all stations finished");
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
			.connect(SERVER_URL);
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
					mOpenBikeDBAdapter)) {
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
				//Log.d("OpenBike", "Async task update finished");
			}
			mFilterPreferences.edit()
				.putLong("last_update", System.currentTimeMillis()).commit();
		}

		public int getProgress() {
			return mProgress;
		}
	}
	
	private class CreateVisibleStationsTask extends AsyncTask<Void, Void, Boolean> {
		
		private boolean updateListFromDb(boolean useList) {
			//Log.d("OpenBike", "UpdatingListFromDb");
			ArrayList<StationOverlay> stationsList = null;
			if (!useList)
				stationsList = new ArrayList<StationOverlay>(mVisibleStations.size());
			if (mOpenBikeDBAdapter.getStationCount() == 0) {
				return false;
			}
			Cursor cursor = mOpenBikeDBAdapter
					.getFilteredStationsCursor(Utils.whereClauseFromFilter(mVcubFilter), 
							mLocationProvider == null 
								|| mLocationProvider.getMyLocation() == null ?
										OpenBikeDBAdapter.KEY_NAME 
										: null);
			StationOverlay stationOverlay;
			Location stationLocation = null;
			Location location = null;
			int distanceToStation = 0;
			if (mLocationProvider != null) {
				location = mLocationProvider.getMyLocation();
				if (location != null)
					stationLocation = new Location("");
			}
			while(cursor.moveToNext()) {
				if (isCancelled())
					return true;
				if (stationLocation != null) {
					stationLocation.setLatitude((double) cursor
							.getInt(OpenBikeDBAdapter.LATITUDE_COLUMN)*1E-6);
					stationLocation.setLongitude((double) cursor
							.getInt(OpenBikeDBAdapter.LONGITUDE_COLUMN)*1E-6);
					distanceToStation = (int) stationLocation.distanceTo(location);
					if (mVcubFilter.isFilteringByDistance() && 
							mVcubFilter.getDistanceFilter() < distanceToStation)
						continue;
				}
				stationOverlay = new StationOverlay(
						new Station(cursor.getInt(OpenBikeDBAdapter.ID_COLUMN), 
								cursor.getString(OpenBikeDBAdapter.NETWORK_COLUMN), 
								cursor.getString(OpenBikeDBAdapter.NAME_COLUMN), 
								cursor.getString(OpenBikeDBAdapter.ADDRESS_COLUMN), 
								cursor.getInt(OpenBikeDBAdapter.LONGITUDE_COLUMN), 
								cursor.getInt(OpenBikeDBAdapter.LATITUDE_COLUMN),
								cursor.getInt(OpenBikeDBAdapter.BIKES_COLUMN), 
								cursor.getInt(OpenBikeDBAdapter.SLOTS_COLUMN), 
								cursor.getInt(OpenBikeDBAdapter.OPEN_COLUMN) == 0 ?
										false : true,
								cursor.getInt(OpenBikeDBAdapter.FAVORITE_COLUMN) == 0 ?
										false : true,
								cursor.getInt(OpenBikeDBAdapter.PAYMENT_COLUMN) == 0 ?
												false : true,
								cursor.getInt(OpenBikeDBAdapter.SPECIAL_COLUMN) == 0 ?
												false : true,
										stationLocation != null ? distanceToStation : -1));
				if (useList) {
					mVisibleStations.add(stationOverlay);
				} else {
					stationsList.add(stationOverlay);
				}
			}
			cursor.close();
			if (isCancelled())
				return true;
			if (mLocationProvider != null) {
				Utils.sortStationsByDistance(useList ? mVisibleStations : stationsList);
			}
			if (!useList) {
				//Log.d("OpenBike", "Create List without mVisibleStation");
				mVisibleStations.clear();
				if (isCancelled())
					return true;
				mVisibleStations.addAll(stationsList);
				stationsList = null;
			}
			mVcubFilter.setNeedDbQuery();
			return true;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mActivity instanceof OpenBikeListActivity) {
				((OpenBikeListActivity) mActivity).setLoadingList();
			}
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
			if (mVisibleStations == null) {
				//Log.e("OpenBike", "mVisibleStations is null in createVisibleStationList");
				//mVisibleStations = new ArrayList<StationOverlay>();
				//return false;
			}
			//Log.d("OpenBike", "Create Visible station list");
			// Hack for ArrayAdapter & Background thread
			if (mActivity instanceof OpenBikeListActivity) {
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
				else {
					mActivity.onListUpdated();
					// FIXME : Don't use System.currentTimeMillis()
					if (System.currentTimeMillis() - mFilterPreferences.getLong("last_update", 0)
							> MIN_UPDATE_TIME) {
						executeUpdateAllStationsTask(false);
					}
				}
				if (mActivity instanceof OpenBikeListActivity) {
					((OpenBikeListActivity) mActivity).setEmptyList();
				}
				mCreateVisibleStationsTask = null;
				//Log.d("OpenBike", "Async task create station list finished");
			}
		}
	}
}
