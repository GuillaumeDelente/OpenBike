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
import android.provider.BaseColumns;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.filter.BikeFilter;
import fr.openbike.filter.FilterPreferencesActivity;
import fr.openbike.filter.Filtering;
import fr.openbike.list.OpenBikeListActivity;
import fr.openbike.map.StationOverlay;
import fr.openbike.object.MinimalStation;
import fr.openbike.utils.Utils;

public class OpenBikeManager {
	
	public static final String SERVER_URL = "http://openbikeserver.appspot.com/stations";
	public static final int RETRIEVE_ALL_STATIONS = 0;
	public static final int REMOVE_FROM_FAVORITE = 1;
	public static final long MIN_UPDATE_TIME = 1 * 1000 * 60;
	protected static OpenBikeDBAdapter mOpenBikeDBAdapter = null;
	protected static Activity mActivity = null;
	protected static MyLocationProvider mLocationProvider = null;
	private static OpenBikeManager mThis;
	private static SharedPreferences mFilterPreferences = null;
	private static ArrayList<StationOverlay> mVisibleStations = null;
	private BikeFilter mOpenBikeFilter = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;
	private CreateVisibleStationsTask mCreateVisibleStationsTask = null;
	
	public BikeFilter getVcubFilter() {
		return mOpenBikeFilter;
	}

	public void setVcubFilter(BikeFilter vcubFilter) {
		mOpenBikeFilter = vcubFilter;
	}

	private OpenBikeManager(Activity activity) {
		//Log.i("OpenBike", "New Manager created");
		mActivity = activity;
		mOpenBikeDBAdapter = new OpenBikeDBAdapter((Context) activity);
		mOpenBikeDBAdapter.open();
		PreferenceManager.setDefaultValues((Context) activity, R.xml.filter_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.map_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.other_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.location_preferences, false);
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) activity);
		if (mFilterPreferences.getBoolean(
				((Context) activity).getString(R.string.use_location), false))
			useLocation();
		initializeFilter();
		//StationOverlay.initialize((Context) activity);
	}
	
	public static synchronized OpenBikeManager getVcuboidManagerInstance(Activity activity) {
		//Log.e("OpenBike", "Getting VcuboidManager instance");
		if (mThis == null) {
			mThis = new OpenBikeManager(activity);
		} else {
			mActivity = activity;
			mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		}
		return mThis;
	}
/*
	public static synchronized OpenBikeManager getVcuboidManagerInstance() {
		if (mThis == null)
			mThis = new OpenBikeManager(null);
		return mThis;
	}
	*/
	
	public OpenBikeDBAdapter getDbAdapter() {
		return mOpenBikeDBAdapter;
	}

	public void setCurrentActivity(Activity activity) {
		setCurrentActivity(activity, false);
	}
	
	public void setCurrentActivity(Activity activity, boolean isShowStationMode) {
		mActivity = activity;
		if (mUpdateAllStationsTask != null && mUpdateAllStationsTask.getProgress() < 50 && activity instanceof IOpenBikeActivity)
			((IOpenBikeActivity) activity).showUpdateAllStationsOnProgress(false);
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
	
	public boolean executeCreateVisibleStationsTask(boolean forceDbQuery) {
		if (mCreateVisibleStationsTask == null) {
			if (mOpenBikeFilter.isNeedDbQuery() || forceDbQuery) {
				mCreateVisibleStationsTask = 
					(CreateVisibleStationsTask) new CreateVisibleStationsTask()
						.execute();
			} else {
				Filtering.filter(mVisibleStations, mOpenBikeFilter);
				if (mActivity instanceof IOpenBikeActivity)
					((IOpenBikeActivity) mActivity).onListUpdated();
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
			mUpdateAllStationsTask = null;
		} else {
			((IOpenBikeActivity) mActivity).showUpdateAllStationsOnProgress(false);
		}
	}
	
	private void initializeFilter() {
		mOpenBikeFilter = new BikeFilter((Context) mActivity);
	}

	public void clearDB() {
		mOpenBikeDBAdapter.reset();
		mOpenBikeDBAdapter.close();
		mOpenBikeDBAdapter.open();
	}
	
	public void setFavorite(int id, boolean isChecked) {
		mOpenBikeDBAdapter.updateFavorite(id, isChecked);
		StationOverlay overlay;
		MinimalStation station;
		Iterator<StationOverlay> it = mVisibleStations.iterator();
		while(it.hasNext()) {
			overlay = it.next();
			station = overlay.getStation();
			if (station.getId() == id) {
				station.setFavorite(isChecked);
				if (mOpenBikeFilter.isShowOnlyFavorites() && !isChecked) {
					it.remove();
				}
				return;
			}
		}
	}
	
	public ArrayList<StationOverlay> getVisibleStations() {
		if (mVisibleStations == null && mCreateVisibleStationsTask == null) {
			mVisibleStations = new ArrayList<StationOverlay>();
			executeCreateVisibleStationsTask(false);
		}
		return mVisibleStations;
	}
	
	private void updateDistance(Location location) {
		if (mVisibleStations == null)
			getVisibleStations();
		StationOverlay overlay;
		MinimalStation station;
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
	
	private void resetDistances() {
		if (mVisibleStations == null)
			return;
		StationOverlay overlay;
		MinimalStation station;
		Iterator<StationOverlay> it = mVisibleStations.iterator();
		while(it.hasNext()) {
			overlay = it.next();
			station = overlay.getStation();
			station.setDistance(-1);
		}
	}
	
	// Only to use after map
	public void sortStations() {
		Utils.sortStationsByDistance(mVisibleStations);
	}
	
	public void onLocationChanged(Location location) {
		if (mGetAllStationsTask != null) {
			//First launch, retrieving station list
			return;
		} else if (mCreateVisibleStationsTask == null) {
			if (location == null) {
				//FIXME:
				//mVcubFilter.setNeedDbQuery();
				executeCreateVisibleStationsTask(false);
				//resetDistances();
				//Utils.sortStationsByName(mVisibleStations);
			} else if (mOpenBikeFilter.isFilteringByDistance()) {
				if (!mOpenBikeFilter.isNeedDbQuery())
					updateDistance(location); 
				executeCreateVisibleStationsTask(false);
			} else {
				updateDistance(location);
				Utils.sortStationsByDistance(mVisibleStations);
			}
		} else { // We received a new location during list creation :
			// cancel the current task and launch a new one.
			mCreateVisibleStationsTask.cancel(true);
			mCreateVisibleStationsTask = null;
			//FIXME:
			//mVcubFilter.setNeedDbQuery(true);
			executeCreateVisibleStationsTask(true);
		}
		if (mActivity instanceof IOpenBikeActivity)
			((IOpenBikeActivity) mActivity).onLocationChanged(location);
	}

	public void useLocation() {
		if (mLocationProvider == null)
			mLocationProvider = new MyLocationProvider((Context) mActivity, this);
	}
	
	public void dontUseLocation() {
		mActivity.removeDialog(MyLocationProvider.ENABLE_GPS);
		mLocationProvider.disableMyLocation();
		mLocationProvider = null;
		//FIXME: Ugly way to know if we need
		// DB query and avoid useless operations
		if (!mOpenBikeFilter.isFilteringByDistance()) {
			resetDistances();
			Utils.sortStationsByName(mVisibleStations);
		}
	}

	public static Location getCurrentLocation() {
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
	/*
	public void showLocationDialogs() {
		if (! mLocationProvider.isProviderEnabled())
			mActivity.showDialog(MyLocationProvider.NO_LOCATION_PROVIDER);
		else if (! mLocationProvider.isGpsEnabled())
			mActivity.showDialog(MyLocationProvider.ENABLE_GPS);
		
	}
	*/
	
	public void showAskForGps() {
		if (mActivity != null && (mActivity instanceof IOpenBikeActivity 
				|| mActivity instanceof FilterPreferencesActivity))
			mActivity.showDialog(MyLocationProvider.ENABLE_GPS);
	}
	
	public void showNoLocationProvider() {
		if (mActivity != null && (mActivity instanceof IOpenBikeActivity 
				|| mActivity instanceof FilterPreferencesActivity))
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
	
	public void resetDb() {
		mOpenBikeDBAdapter.reset();
	}
	
	private boolean isFirstRun() {
		if (mFilterPreferences.getBoolean("firstRun", true)) {
			mFilterPreferences.edit().putBoolean("firstRun", false).commit();
			return true;
		}
		return false;
	}
	
	public ArrayList<StationOverlay> getSearchResults (String query) {
		Cursor cursor = mOpenBikeDBAdapter.getSearchCursor(query);
		ArrayList<StationOverlay> results;
		if (cursor != null && cursor.getCount() != 0) {
			results = new ArrayList<StationOverlay>(cursor.getCount());
		} else {
			return new ArrayList<StationOverlay>(0);
		}
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
			if (stationLocation != null) {
				stationLocation.setLatitude((double) cursor
					.getInt(4)*1E-6);
				stationLocation.setLongitude((double) cursor
					.getInt(5)*1E-6);
				distanceToStation = (int) stationLocation.distanceTo(location);
			}
			stationOverlay = new StationOverlay(
					new MinimalStation(cursor.getInt(0), 
						cursor.getString(6), 
						cursor.getInt(4), 
						cursor.getInt(5),
						cursor.getInt(1), 
						cursor.getInt(2), 
						cursor.getInt(3) == 1 ? true : false,
						cursor.getInt(7) == 1 ? true : false,
						stationLocation != null ? distanceToStation : -1));
			results.add(stationOverlay);
		}
		return results;
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
			if (mActivity != null) {
				((IOpenBikeActivity) mActivity).showGetAllStationsOnProgress();
			}
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
			int result = 1;
			String json = RestClient
					.connect(SERVER_URL);
			if (json == null) {
				publishProgress(RestClient.NETWORK_ERROR);
				return false;
			}
			publishProgress(50);
			result = mOpenBikeDBAdapter.insertStations(json);
			if (result != 1) {
				publishProgress(result);
				return false;
			}
			publishProgress(100);
			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				
				if (progress[0] < 0) {
					((IOpenBikeActivity) mActivity).finishGetAllStationsOnProgress();
					mActivity.showDialog(progress[0]);
				} else if (progress[0] == 50){
					((IOpenBikeActivity) mActivity).updateGetAllStationsOnProgress(50);
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean isListRetrieved) {
			if (!isListRetrieved)
				return;
			mFilterPreferences.edit()
				.putLong("last_update", System.currentTimeMillis()).commit();
			executeCreateVisibleStationsTask(true);
			if (mActivity != null) {
				((IOpenBikeActivity) mActivity).finishGetAllStationsOnProgress();
				mGetAllStationsTask = null;
				//showLocationDialogs();
			}
		}

		int getProgress() {
			return progress;
		}
	}

	private class UpdateAllStationsTask extends AsyncTask<Void, Integer, Boolean> {
		private int mProgress = 0;

		protected void onPreExecute() {
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				((IOpenBikeActivity) mActivity).showUpdateAllStationsOnProgress(true);
			}
		}

		protected Boolean doInBackground(Void... unused) {
			int result = 1;
			String json = RestClient
			.connect(SERVER_URL);
			if (json == null) {
				publishProgress(RestClient.NETWORK_ERROR);
				return false;
			}
			if (!RestClient.updateListFromJson(json,
					mVisibleStations)) {
				publishProgress(OpenBikeDBAdapter.JSON_ERROR);
				return false;
			}
			publishProgress(50);
			result = mOpenBikeDBAdapter.updateStations(json);
			if (result != 1) {
				publishProgress(result);
				return false;
			}
			return true;
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			mProgress = progress[0];
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				if (progress[0] < 0) { // Error
					((IOpenBikeActivity) mActivity).showDialog(progress[0]);
				} else {
					((IOpenBikeActivity) mActivity).onListUpdated();
				}
				// Error or not, dismiss loading dialog
				((IOpenBikeActivity) mActivity).finishUpdateAllStationsOnProgress(true);
			}
		}

		@Override
		protected void onPostExecute(Boolean isSuccess) {
			mProgress = 100;
			if (isSuccess) {
				mFilterPreferences.edit()
					.putLong("last_update", System.currentTimeMillis()).commit();
			}
			mUpdateAllStationsTask = null;
		}

		public int getProgress() {
			return mProgress;
		}
	}
	
	public static boolean isLocationAvailable() {
		return (mLocationProvider != null && mLocationProvider.getMyLocation() != null);
	}
	
	//FIXME : We can avoid some useless list creation when we haven't yet the location
	private class CreateVisibleStationsTask extends AsyncTask<Void, Void, Boolean> {
		
		private boolean updateListFromDb(boolean useList) {
			ArrayList<StationOverlay> stationsList = null;
			if (!useList)
				stationsList = new ArrayList<StationOverlay>(mVisibleStations.size());
			if (mOpenBikeDBAdapter.getStationCount() == 0) {
				return false;
			}
			Cursor cursor = mOpenBikeDBAdapter
					.getFilteredStationsCursor(Utils.whereClauseFromFilter(mOpenBikeFilter), 
							mLocationProvider == null 
								|| mLocationProvider.getMyLocation() == null ?
										OpenBikeDBAdapter.KEY_NAME + " COLLATE NOCASE"
										: null);
			StationOverlay stationOverlay;
			Location stationLocation = null;
			Location location = null;
			int distanceToStation = 0;
			int id_col = cursor.getColumnIndex(BaseColumns._ID);
			int latitude_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE);
			int longitude_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE);
			int name_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_NAME);
			int bikes_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES);
			int slots_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS);
			int open_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_OPEN);
			int favorite_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_FAVORITE);
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
							.getInt(latitude_col)*1E-6);
					stationLocation.setLongitude((double) cursor
							.getInt(longitude_col)*1E-6);
					distanceToStation = (int) stationLocation.distanceTo(location);
					if (mOpenBikeFilter.isFilteringByDistance() && 
							mOpenBikeFilter.getDistanceFilter() < distanceToStation)
						continue;
				}
				stationOverlay = new StationOverlay(
						new MinimalStation(cursor.getInt(id_col),
								cursor.getString(name_col), 
								cursor.getInt(longitude_col), 
								cursor.getInt(latitude_col),
								cursor.getInt(bikes_col), 
								cursor.getInt(slots_col), 
								cursor.getInt(open_col) == 0 ?
										false : true,
								cursor.getInt(favorite_col) == 0 ?
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
				mVisibleStations.clear();
				if (isCancelled())
					return true;
				mVisibleStations.addAll(stationsList);
				stationsList = null;
			}
			//FIXME:
			//mVcubFilter.setNeedDbQuery(true);
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
			// Hack for ArrayAdapter & Background thread
			if (mActivity instanceof OpenBikeListActivity) {
				return updateListFromDb(false);
			} else {
				if (mVisibleStations != null) 
					mVisibleStations.clear();
				return updateListFromDb(true);
			}
		}

		@Override
		protected void onPostExecute(Boolean isListCreated) {
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				if (!isListCreated)
					executeGetAllStationsTask();
				else {
					((IOpenBikeActivity) mActivity).onListUpdated();
					// FIXME : Don't use System.currentTimeMillis()
 					if (System.currentTimeMillis() - 
							mFilterPreferences.getLong("last_update", 0)
							> MIN_UPDATE_TIME) {
						executeUpdateAllStationsTask(false);
					}
					if (mActivity instanceof OpenBikeListActivity) {
						((OpenBikeListActivity) mActivity).setEmptyList();
						if (isFirstRun())
							mActivity.showDialog(OpenBikeListActivity.WELCOME_MESSAGE);
					}
				}
				mCreateVisibleStationsTask = null;
			}
		}
	}
}
