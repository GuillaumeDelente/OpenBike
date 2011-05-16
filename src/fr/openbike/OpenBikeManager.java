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
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;

import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.filter.BikeFilter;
import fr.openbike.filter.FilterPreferencesActivity;
import fr.openbike.filter.Filtering;
import fr.openbike.list.OpenBikeListActivity;
import fr.openbike.map.StationOverlay;
import fr.openbike.object.MinimalStation;
import fr.openbike.object.Network;
import fr.openbike.utils.Utils;

public class OpenBikeManager {
	
	public static String UPDATE_SERVER_URL = ""; //"http://openbikeserver.appspot.com/stations";
	public static final String SERVER_NETWORKS = "http://openbikeserver-2.appspot.com/networks";
	public static int NETWORK_LATITUDE = 0;
	public static int NETWORK_LONGITUDE = 0;
	public static String NETWORK_NAME = "";
	public static String NETWORK_CITY = "";
	public static String SPECIAL_STATION = "";
	public static final String LAST_UPDATE = "last_update";
	public static final int PROGRESS_DIALOG = 0;
	public static final int REMOVE_FROM_FAVORITE = 1;
	public static final long MIN_UPDATE_TIME = 1 * 1000 * 60;
	
	protected static OpenBikeDBAdapter mOpenBikeDBAdapter = null;
	protected static Activity mActivity = null;
	protected static MyLocationProvider mLocationProvider = null;
	private static OpenBikeManager mThis;
	private static SharedPreferences mFilterPreferences = null;
	private static ArrayList<StationOverlay> mVisibleStations = null;
	private BikeFilter mOpenBikeFilter = null;
	private static GetAllStationsTask mGetAllStationsTask = null;
	private static UpdateAllStationsTask mUpdateAllStationsTask = null;
	private CreateVisibleStationsTask mCreateVisibleStationsTask = null;
	private static ShowNetworksTask mShowNetworksTask = null;
	
	public BikeFilter getVcubFilter() {
		return mOpenBikeFilter;
	}

	public void setVcubFilter(BikeFilter vcubFilter) {
		mOpenBikeFilter = vcubFilter;
	}

	private OpenBikeManager(Activity activity) {
		//Log.i("OpenBike", "New Manager created");
		mActivity = activity;
		PreferenceManager.setDefaultValues((Context) activity, R.xml.filter_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.map_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.other_preferences, false);
		PreferenceManager.setDefaultValues((Context) activity, R.xml.location_preferences, false);
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) activity);
		mOpenBikeDBAdapter = new OpenBikeDBAdapter((Context) activity);
		mOpenBikeDBAdapter.open();
		if (mFilterPreferences.getBoolean(
				FilterPreferencesActivity.LOCATION_PREFERENCE, false))
			useLocation();
		initializeFilter();
		initializeNetwork();		
		//StationOverlay.initialize((Context) activity);
	}
	
	public static synchronized OpenBikeManager getVcuboidManagerInstance(Activity activity) {
		//Log.e("OpenBike", "Getting VcuboidManager instance");
		if (mThis == null) {
			mThis = new OpenBikeManager(activity);
		} else {
			setCurrentActivity(activity);
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
	
	public static void setCurrentActivity(Activity activity) {
		if (mActivity == activity) {
			Log.d("OpenBike", "setCurrentActivity but same activity");
			return;
		}

		mActivity = activity;
		if (activity instanceof IOpenBikeActivity) {
			Log.d("OpenBike", "setCurrentActivity !");
			if (mGetAllStationsTask != null) {
				//mGetAllStationsTask in progress
				mGetAllStationsTask.retrieveTask();
			} else if (mShowNetworksTask != null) {
				//mShowNetworksTask in progress
				mShowNetworksTask.retrieveTask();
			} else if (mUpdateAllStationsTask != null) {
				//mUpdateAllStationsTask in progress
				mUpdateAllStationsTask.retrieveTask();
			}
		}
	}

	public void detach() {
		if (mActivity instanceof IOpenBikeActivity) {
			((IOpenBikeActivity) mActivity).finishUpdateAllStationsOnProgress(false);
		}
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
				- mFilterPreferences.getLong(LAST_UPDATE, 0) > MIN_UPDATE_TIME)) {
			Log.i("OpenBike", "Executing updateAllStations");
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
	
	public boolean executeShowNetworksTask() {
		if (mActivity == null)
			return false;
		if (mShowNetworksTask == null) {
			mShowNetworksTask = (ShowNetworksTask) new ShowNetworksTask()
					.execute();
			return true;
		}
		return false;
	}
	
	private void initializeFilter() {
		mOpenBikeFilter = new BikeFilter((Context) mActivity);
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
	
	private void initializeNetwork () {
		int networkId = mFilterPreferences.getInt(FilterPreferencesActivity.NETWORK_PREFERENCE, 0);
		if (networkId != 0 ) {
			Cursor network = mOpenBikeDBAdapter.getNetwork(networkId, 
								new String[] {OpenBikeDBAdapter.KEY_NAME, OpenBikeDBAdapter.KEY_CITY, OpenBikeDBAdapter.KEY_SERVER, 
															OpenBikeDBAdapter.KEY_LONGITUDE, OpenBikeDBAdapter.KEY_LATITUDE, OpenBikeDBAdapter.KEY_SPECIAL_NAME});
			initializeNetwork(new Network(networkId, network.getString(0), network.getString(1), network.getString(2), network.getString(5), network.getInt(3), network.getInt(4)));
		}
	}
	
	private void initializeNetwork (Network network) {
		UPDATE_SERVER_URL = network.getServerUrl();
		NETWORK_LATITUDE = network.getLatitude();
		NETWORK_LONGITUDE = network.getLongitude();
		NETWORK_NAME = network.getName();
		NETWORK_CITY = network.getCity();
		SPECIAL_STATION = network.getSpecialName();
	}
	
	public boolean updateNetworkTable(ArrayList<Network> networks) {
		Network network = null;
		int choosenNetwork = mFilterPreferences.getInt(FilterPreferencesActivity.NETWORK_PREFERENCE, 0);
		for (Network n : networks) {
			if (n.getId() == choosenNetwork) {
				network = n;
				break;
			}
		}
		SharedPreferences.Editor editor = mFilterPreferences.edit();
		editor.putLong(LAST_UPDATE, 0).commit();
		initializeNetwork(network);
		return mOpenBikeDBAdapter.insertNetwork(network);
	}
	
	public ArrayList<StationOverlay> getVisibleStations() {
		Log.d("OpenBike", "getVisibleStations");
		if (mFilterPreferences.getInt(FilterPreferencesActivity.NETWORK_PREFERENCE, 0) == 0) {
			Log.d("OpenBike", "executeShowNetworksTask");
			mVisibleStations = new ArrayList<StationOverlay>();
			executeShowNetworksTask();
		} else if (mVisibleStations == null && mCreateVisibleStationsTask == null) {
			mVisibleStations = new ArrayList<StationOverlay>();
			Log.d("OpenBike", "getVisibleStations : executeCreateVisibleStationsTask");
			executeCreateVisibleStationsTask(true);
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
				Log.d("OpenBike", "onLocationChanged executeCreateVisibleStationsTask");
				executeCreateVisibleStationsTask(false);
				//resetDistances();
				//Utils.sortStationsByName(mVisibleStations);
			} else if (mOpenBikeFilter.isFilteringByDistance()) {
				if (!mOpenBikeFilter.isNeedDbQuery())
					updateDistance(location);
				Log.d("OpenBike", "onLocationChanged executeCreateVisibleStationsTask");
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
			Log.d("OpenBike", "onLocationChanged executeCreateVisibleStationsTask");
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
	
	public boolean isStationListRetrieved() {
		return mOpenBikeDBAdapter.getStationCount() != 0;
	}
	
	
	/************************************/
	/************************************/
	/*******					*********/
	/*******   Private Classes	*********/
	/*******					*********/
	/************************************/
	/************************************/
	
	
	/******************************************************************/
	/******* GetAllStationsTask
	/******************************************************************/
	
	private class GetAllStationsTask extends AsyncTask<Void, Integer, Boolean> {
		
		private int mProgress = 0;
		
		@Override
		protected void onPreExecute() {
			if (mActivity != null) {
				((IOpenBikeActivity) mActivity).showProgressDialog(mActivity.getString(R.string.retrieve_all), 
						mActivity.getString(R.string.querying_server_summary));
			}
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
			int result = 1;
			String json = RestClient
					.connect(UPDATE_SERVER_URL + String.valueOf(mFilterPreferences.getInt(FilterPreferencesActivity.NETWORK_PREFERENCE, 0)));
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
			mProgress = progress[0];
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				if (progress[0] < 0) { // Error
					((IOpenBikeActivity) mActivity).dismissProgressDialog();
					mActivity.showDialog(progress[0]);
				} else if (progress[0] == 50){
					((IOpenBikeActivity) mActivity).showProgressDialog(mActivity.getString(R.string.retrieve_all), 
							mActivity.getString(R.string.saving_db_summary));
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean isListRetrieved) {
			if (!isListRetrieved) {
				Log.d("OpenBike", "List not retrieved, nulling visibleStations");
				mVisibleStations = null;
				if (mActivity != null)
					mGetAllStationsTask = null;
				return;
			}
			mFilterPreferences.edit()
				.putLong(LAST_UPDATE, System.currentTimeMillis()).commit();
			if (mActivity != null) {
				Log.d("OpenBike", "retrieve getAllStations executeCreateVisibleStationsTask");
				executeCreateVisibleStationsTask(true);
				((IOpenBikeActivity) mActivity).dismissProgressDialog();
				mGetAllStationsTask = null;
				//showLocationDialogs();
			}
		}
		
		protected void retrieveTask() {
			Log.d("OpenBike", "retrieveTask, progress " + mProgress);
			if (mProgress < 0) {
				((IOpenBikeActivity) mActivity).dismissProgressDialog();
				mActivity.showDialog(mProgress);
				mGetAllStationsTask = null;
			} else if (mProgress < 50) {
				((IOpenBikeActivity) mActivity).showProgressDialog(mActivity.getString(R.string.retrieve_all), 
						mActivity.getString(R.string.querying_server_summary));
			} else if (mProgress < 100) {
				((IOpenBikeActivity) mActivity).showProgressDialog(mActivity.getString(R.string.retrieve_all), 
						mActivity.getString(R.string.saving_db_summary));
			} else if (mProgress == 100) {
				Log.d("OpenBike", "onLocationChanged executeCreateVisibleStationsTask");
				executeCreateVisibleStationsTask(true);
				((IOpenBikeActivity) mActivity).dismissProgressDialog();
				mGetAllStationsTask = null;
			}
		}
	}

	/******************************************************************/
	/*******   UpdateAllStationsTask
	/******************************************************************/
	
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
			.connect(UPDATE_SERVER_URL + String.valueOf(mFilterPreferences.getInt(FilterPreferencesActivity.NETWORK_PREFERENCE, 0)));
			if (json == null) {
				publishProgress(RestClient.NETWORK_ERROR);
				return false;
			}/*
			if (!RestClient.updateListFromJson(json,
					mVisibleStations)) {
				publishProgress(OpenBikeDBAdapter.JSON_ERROR);
				return false;
			}*/
			publishProgress(50);
			result = mOpenBikeDBAdapter.updateStations(json);
			if (result != 1) {
				publishProgress(result);
				return false;
			}
			publishProgress(100);
			return true;
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			mProgress = progress[0];
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				if (progress[0] < 0) { // Error
					((IOpenBikeActivity) mActivity).showDialog(progress[0]);
					((IOpenBikeActivity) mActivity).finishUpdateAllStationsOnProgress(true);
				} else if (progress[0] == 50) {
					Log.d("OpenBike", "update received");
					mFilterPreferences.edit().putLong(LAST_UPDATE, System.currentTimeMillis()).commit();
					((IOpenBikeActivity) mActivity).finishUpdateAllStationsOnProgress(true);
				} else if (progress[0] == 100) {
					Log.d("OpenBike", "update saved in DB");
					executeCreateVisibleStationsTask(true);
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean isSuccess) {
			if (mActivity != null) {
				mUpdateAllStationsTask = null;
			}
		}

		public int getProgress() {
			return mProgress;
		}
		
		protected void retrieveTask() {
			Log.d("OpenBike", "retrieveTask, progress " + mProgress);
			if (mProgress < 0) { // Error
				((IOpenBikeActivity) mActivity).finishUpdateAllStationsOnProgress(true);
				((IOpenBikeActivity) mActivity).showDialog(mProgress);
			} else if (mProgress >= 50) {
				((IOpenBikeActivity) mActivity).finishUpdateAllStationsOnProgress(true);
				if (mProgress == 100) {
					mUpdateAllStationsTask = null;
				}
			} else {
				((IOpenBikeActivity) mActivity).showUpdateAllStationsOnProgress(false);
			}
		}
	}
	
	public static boolean isLocationAvailable() {
		return (mLocationProvider != null && mLocationProvider.getMyLocation() != null);
	}
	
	
	/******************************************************************/
	/*******   CreateVisibleStationsTask
	/******************************************************************/
	
	//FIXME : We can avoid some useless list creation when we haven't yet the location
	private class CreateVisibleStationsTask extends AsyncTask<Void, Void, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mActivity instanceof OpenBikeListActivity) {
				Log.d("OpenBike", "onPreExecute createVisibleStationsTask");
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
										OpenBikeDBAdapter.KEY_NAME : null);
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
		protected void onPostExecute(Boolean isListCreated) {
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				if (!isListCreated) {
					executeGetAllStationsTask();
				} else {
					((IOpenBikeActivity) mActivity).onListUpdated();
					// FIXME : Don't use System.currentTimeMillis()
 					if (System.currentTimeMillis() - 
							mFilterPreferences.getLong(LAST_UPDATE, 0)
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
			Log.i("OpenBike", "onPostExecute CreateVisibleStationList");
		}
	}
	
	/******************************************************************/
	/*******   ShowNetworksTask
	/******************************************************************/
	
	private class ShowNetworksTask extends AsyncTask<Void, Integer, Boolean> {
		ArrayList<Network> networks;
		private int mProgress = 0;
		
		@Override
		protected void onPreExecute() {
			if (mActivity != null) {
				((IOpenBikeActivity) mActivity)
					.showProgressDialog(mActivity.getString(R.string.retrieve_networks),
						mActivity.getString(R.string.querying_server_summary));
			}
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
				String json = RestClient.connect(SERVER_NETWORKS);
				if (json == null) {
					publishProgress(RestClient.NETWORK_ERROR);
					return false;
				}
				networks = new ArrayList<Network>();
				networks.addAll(RestClient.getNetworkList(json));
				if (networks.size() == 0) {
					publishProgress(OpenBikeDBAdapter.JSON_ERROR);
					return false;
				}
				publishProgress(100);
				return true;
			
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			mProgress = progress[0];
			if (mActivity != null && mActivity instanceof IOpenBikeActivity) {
				if (progress[0] < 0) { // Error
					((IOpenBikeActivity) mActivity).showDialog(progress[0]);
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean isListRetrieved) {
			if (mActivity != null) {
				if (mProgress == 100) {
					((IOpenBikeActivity) mActivity).dismissProgressDialog();
					((IOpenBikeActivity) mActivity).showChooseNetwork(networks);
				}
				mShowNetworksTask = null;
			}
		}
		
		protected void retrieveTask() {
			Log.d("OpenBike", "retrieveTask, progress " + mProgress);
			((IOpenBikeActivity) mActivity).dismissProgressDialog();
			if (mProgress < 100) {
				((IOpenBikeActivity) mActivity)
					.showProgressDialog(mActivity.getString(R.string.retrieve_networks),
						mActivity.getString(R.string.querying_server_summary));
			} else if (mProgress == 100) {
				((IOpenBikeActivity) mActivity).showChooseNetwork(networks);
				mShowNetworksTask = null;
			} else if (mProgress < 0) { // Error
				((IOpenBikeActivity) mActivity).showDialog(mProgress);
				mShowNetworksTask = null;
			}
		}
	}
}
