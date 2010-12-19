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
	protected static VcuboidDBAdapter mVcuboidDBAdapter = null;
	protected static IVcuboidActivity mActivity = null;
	protected static MyLocationProvider mLocationProvider = null;
	private static VcuboidManager mThis;
	private static SharedPreferences mFilterPreferences = null;
	private static ArrayList<StationOverlay> mVisibleStations = null;
	private VcubFilter mVcubFilter = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;
	private CreateVisibleStationsTask mCreateVisibleStationsTask= null;
	
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
	}

	public void attach(VcuboidListActivity activity) {
		mActivity = activity;
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		if (mGetAllStationsTask != null) {
			mThis.retrieveGetAllStationTask();
		} else if (mUpdateAllStationsTask != null) {
			mThis.retrieveUpdateAllStationTask();
		}
	}

	public void detach() {
		mActivity = null;
	}

	public boolean executeGetAllStationsTask() {
		Log.e("executeGetAllStationsTask", "Ok");
		if (mGetAllStationsTask == null) {
			mGetAllStationsTask = (GetAllStationsTask) new GetAllStationsTask()
					.execute();
			return true;
		}
		return false;
	}
	
	public boolean executeCreateVisibleStationsTask() {
		if (mCreateVisibleStationsTask == null) {
			mCreateVisibleStationsTask =  (CreateVisibleStationsTask) new CreateVisibleStationsTask()
					.execute();
			return true;
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
			((IVcuboidActivity) mActivity).showGetAllStationsOnProgress();
			((IVcuboidActivity) mActivity).updateGetAllStationsOnProgress(mGetAllStationsTask
					.getProgress());
		} else {
			((IVcuboidActivity) mActivity).finishGetAllStationsOnProgress();
			mGetAllStationsTask = null;
		}
	}

	private void retrieveUpdateAllStationTask() {
		int progress = mUpdateAllStationsTask.getProgress();
		if (progress >= 100) {
			((IVcuboidActivity) mActivity).finishUpdateAllStationsOnProgress();
			mIsUpdating = false;
			mUpdateAllStationsTask = null;
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

	public void setFavorite(int position, boolean isChecked) {
		Station s = mVisibleStations.get(position).getStation();
		s.setFavorite(isChecked);
		mVcuboidDBAdapter.updateFavorite(s.getId(), isChecked);
		if (mVcubFilter.isShowOnlyFavorites()) {
			mVisibleStations.remove(position);
			mActivity.onListUpdated();
		}
	}
	
	public ArrayList<StationOverlay> getVisibleStations() {
		if (mVisibleStations == null && mCreateVisibleStationsTask == null) {
			mVisibleStations = new ArrayList<StationOverlay>();
			executeCreateVisibleStationsTask();
		} 
		return mVisibleStations;
	}
	
	public void createVisibleStationList() {
		if (mVisibleStations == null) {
			mVisibleStations = new ArrayList<StationOverlay>();
			executeCreateVisibleStationsTask();
		}
		if (mVcubFilter.isNeedDbQuery()) {
			mVisibleStations.clear();
			// Hack for ArrayAdapter & Background thread
			if (mActivity instanceof VcuboidListActivity)
				mActivity.onListUpdated();
			executeCreateVisibleStationsTask();
		} else {
			Filtering.filter(mVisibleStations, mVcubFilter);
		}
	}
	
	private void updateListFromDb() {
		if (mVcuboidDBAdapter.getStationCount() == 0) {
			executeGetAllStationsTask();
			return;
		}
		Cursor cursor = mVcuboidDBAdapter
				.getFilteredStationsCursor(Utils.whereClauseFromFilter(mVcubFilter), 
						mLocationProvider == null ? VcuboidDBAdapter.KEY_NAME : null);
		StationOverlay stationOverlay;
		Location stationLocation = null;
		Location location = null;
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
									stationLocation != null ?
											(int) stationLocation.distanceTo(location) : -1));
			mVisibleStations.add(stationOverlay);
		}
		cursor.close();
		if (mLocationProvider != null)
			Utils.sortStationsByDistance(mVisibleStations);
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
			updateDistance(location);
			Utils.sortStationsByDistance(mVisibleStations);
			mActivity.onLocationChanged(location);
		}
	}

	public boolean useLocation() {
		if (mLocationProvider == null)
			mLocationProvider = new MyLocationProvider((Context) mActivity, this);
		else
			Log.e("Vcuboid", "Location Manager !!!!!");
		return mLocationProvider.enableMyLocation();
	}
	
	public void dontUseLocation() {
		mLocationProvider.disableMyLocation();
		mLocationProvider = null;
		resetDistances();
		Utils.sortStationsByName(mVisibleStations);
	}
	
	public boolean isMyLocationAvailable() {
		if (mLocationProvider != null)
			return mLocationProvider.isLocationAvailable();
		return false;
	}
	
	public Location getLocation() {
		if (mLocationProvider != null)
			return mLocationProvider.getMyLocation();
		return null;
	}
	
	public boolean askForGps() {
		if (mLocationProvider != null)
			return mLocationProvider.isAskForGps();
		return false;
	}
	
	public void showAskForGps() {
		mActivity.showAskForGps();
	}
	
	public void setAskForGps(boolean ask) {
		if (mLocationProvider != null)
			mLocationProvider.setAskForGps(ask);
	}
	
	public void startLocation() {
		mLocationProvider.enableMyLocation();
	}
	
	public void stopLocation() {
		mLocationProvider.disableMyLocation();
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

		protected void onPreExecute() {
			if (mActivity != null) {
				((IVcuboidActivity) mActivity).showGetAllStationsOnProgress();
			}
		}

		protected Void doInBackground(Void... unused) {
			String json = RestClient
					.connect("http://vcuboid.appspot.com/stations");
			publishProgress();
			RestClient.jsonStationsToDb(json, mVcuboidDBAdapter);
			publishProgress();
			Log.i("Vcuboid", "Async task finished");
			return (null);
		}

		protected void onProgressUpdate(Void... unused) {
			if (mActivity != null) {
				((IVcuboidActivity) mActivity).updateGetAllStationsOnProgress(progress += 50);
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			updateListFromDb();
			if (mActivity != null) {
				((IVcuboidActivity) mActivity).finishGetAllStationsOnProgress();
				mGetAllStationsTask = null;
			}
		}

		int getProgress() {
			return progress;
		}
	}

	private class UpdateAllStationsTask extends AsyncTask<Void, Void, Void> {

		private int progress = 0;

		protected void onPreExecute() {
			mIsUpdating = true;
			if (mActivity != null) {
				((IVcuboidActivity) mActivity).showUpdateAllStationsOnProgress();
			}
		}

		protected Void doInBackground(Void... unused) {
			RestClient.jsonBikesToDb(RestClient
					.connect("http://vcuboid.appspot.com/stations"),
					mVcuboidDBAdapter);
			Log.i("Vcuboid", "Async task finished");
			return (null);
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (mActivity == null) {
				progress = 100;
			} else {
				mIsUpdating = false;
				((IVcuboidActivity) mActivity).finishUpdateAllStationsOnProgress();
				mUpdateAllStationsTask = null;
			}
		}

		public int getProgress() {
			return progress;
		}
	}
	
	private class CreateVisibleStationsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		protected Void doInBackground(Void... unused) {
			updateListFromDb();
			Log.i("Vcuboid", "Async task finished");
			return (null);
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (mActivity == null) {
			} else {
				mActivity.onListUpdated();
				mCreateVisibleStationsTask = null;
			}
		}
	}
}
