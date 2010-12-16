package fr.vcuboid;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;

import fr.vcuboid.database.VcuboidDBAdapter;
import fr.vcuboid.filter.Filtering;
import fr.vcuboid.filter.VcubFilter;
import fr.vcuboid.list.VcuboidListActivity;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.map.VcuboidMapActivity;
import fr.vcuboid.object.Station;
import fr.vcuboid.utils.Utils;

public class VcuboidManager {

	public static boolean isUpdating = false;
	protected static VcuboidDBAdapter mVcuboidDBAdapter = null;
	protected static IVcuboidActivity mActivity = null;
	private static VcuboidManager mThis;
	private static SharedPreferences mFilterPreferences = null;
	private static ArrayList<StationOverlay> mVisibleStations = null;
	private VcubFilter mVcubFilter = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;
	
	public VcubFilter getVcubFilter() {
		return mVcubFilter;
	}

	public void setVcubFilter(VcubFilter vcubFilter) {
		mVcubFilter = vcubFilter;
	}

	private VcuboidManager(IVcuboidActivity activity) {
		Log.e("Vcuboid", "New Manager created");
		mActivity = activity;
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		initializeFilter();
		mVcuboidDBAdapter = new VcuboidDBAdapter((Context) mActivity);
		mVcuboidDBAdapter.open();
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
			isUpdating = false;
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
		if (mVisibleStations != null) {
			Log.e("Vcuboid", "First in List : " + mVisibleStations.get(0).getStation().getName());
			return mVisibleStations;
		} else {
			mVisibleStations = new ArrayList<StationOverlay>();
			updateListFromDb();
			return mVisibleStations;
		}
	}
	
	private void updateListFromDb() {
		if (mVcuboidDBAdapter.getStationCount() == 0) {
			executeGetAllStationsTask();
			return;
		}
		mVisibleStations.clear();
		Cursor cursor = mVcuboidDBAdapter
				.getFilteredStationsCursor(Utils.whereClauseFromFilter(mVcubFilter));
		StationOverlay stationOverlay;
		Location stationLocation = null;
		Location location = null;
		if (mVcubFilter.isEnableLocation()) {
			location = ((VcuboidMapActivity) mActivity).getCurrentLocation();
			stationLocation = new Location(location);
		}
		while(cursor.moveToNext()) {
			if (mVcubFilter.isEnableLocation()) {
				stationLocation.setLatitude((double) cursor.getInt(VcuboidDBAdapter.LATITUDE_COLUMN)*1E-6);
				stationLocation.setLongitude((double) cursor.getInt(VcuboidDBAdapter.LONGITUDE_COLUMN)*1E-6);
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
							cursor.getInt(VcuboidDBAdapter.OPEN_COLUMN) == 0 ? false : true,
							cursor.getInt(VcuboidDBAdapter.FAVORITE_COLUMN) == 0 ? false : true,
							mVcubFilter.isEnableLocation() ? (int) stationLocation.distanceTo(location) : -1));
			mVisibleStations.add(stationOverlay);
		}
		cursor.close();
		Utils.sortStations(mVisibleStations);
	}
	
	public void updateDistance(Location location) {
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
	
	public ArrayList<StationOverlay> onLocationChanged(Location location, StationOverlay current) {
		updateDistance(location);
		Utils.sortStations(mVisibleStations);
		return mVisibleStations;
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
			isUpdating = true;
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
				isUpdating = false;
				((IVcuboidActivity) mActivity).finishUpdateAllStationsOnProgress();
				mUpdateAllStationsTask = null;
			}
		}

		public int getProgress() {
			return progress;
		}
	}

	public void applyFilter() {
		if (mVcubFilter.isNeedDbQuery())
			updateListFromDb();
		else
			Filtering.filter(mVisibleStations, mVcubFilter);
		Utils.sortStations(mVisibleStations);
		mActivity.onListUpdated();
	}
}
