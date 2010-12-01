package fr.vcuboid;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import fr.vcuboid.database.VcuboidDBAdapter;
import fr.vcuboid.list.VcuboidListActivity;
import fr.vcuboid.map.StationOverlay;

public class VcuboidManager {

	public static boolean isUpdating = false;
	protected static VcuboidDBAdapter mVcuboidDBAdapter = null;
	protected static IVcuboidActivity mActivity = null;
	private static VcuboidManager mThis;
	private static SharedPreferences mFilterPreferences = null;
	private static ArrayList<StationOverlay> mVisibleStations = null;
	private static VcubFilter mVcubFilter = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;
	private OnVcubFilterChangeListener mOnVcubFilterChangeListener = null;


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
	
	public Cursor getCursor() {
		if (mVcuboidDBAdapter.getStationCount() == 0)
			executeGetAllStationsTask();
		Cursor cursor = mVcuboidDBAdapter
				.getFilteredStationsCursor(mVcubFilter);
		((Activity) mActivity).startManagingCursor(cursor);
		return cursor;
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
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		// Keep a reference on the listener for GC
		mOnVcubFilterChangeListener = new OnVcubFilterChangeListener();
		mFilterPreferences.registerOnSharedPreferenceChangeListener(mOnVcubFilterChangeListener);
		mVcubFilter = new VcubFilter(mFilterPreferences.getBoolean("favorite_filter", false));
	}

	public void clearDB() {
		mVcuboidDBAdapter.reset();
		mVcuboidDBAdapter.close();
		mVcuboidDBAdapter.open();
	}

	public void setFavorite(int id, boolean isChecked) {
		mVcuboidDBAdapter.updateFavorite(id, isChecked);
	}
	
	public ArrayList<StationOverlay> getVisibleStations() {
		mVisibleStations = new ArrayList<StationOverlay>();
		Cursor c = getCursor();
		StationOverlay s;
		int i = 0;
		while(c.moveToNext()) {
			s = new StationOverlay(
					c.getInt(VcuboidDBAdapter.LATITUDE_COLUMN), 
					c.getInt(VcuboidDBAdapter.LONGITUDE_COLUMN), 
					c.getInt(VcuboidDBAdapter.BIKES_COLUMN), 
					c.getInt(VcuboidDBAdapter.SLOTS_COLUMN),
					i
					/*
					c.getInt(VcuboidDBAdapter.ID_COLUMN), 
					c.getString(VcuboidDBAdapter.NETWORK_COLUMN),
					c.getString(VcuboidDBAdapter.NAME_COLUMN), 
					c.getString(VcuboidDBAdapter.ADDRESS_COLUMN), 
					c.getInt(VcuboidDBAdapter.LONGITUDE_COLUMN), 
					c.getInt(VcuboidDBAdapter.LATITUDE_COLUMN), 
					c.getInt(VcuboidDBAdapter.BIKES_COLUMN), 
					c.getInt(VcuboidDBAdapter.SLOTS_COLUMN), 
					c.getInt(VcuboidDBAdapter.OPEN_COLUMN) == 0 ? false : true, 
					c.getInt(VcuboidDBAdapter.FAVORITE_COLUMN) == 0 ? false : true
					*/
					);
			mVisibleStations.add(s);
		}
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
	
	private class OnVcubFilterChangeListener implements OnSharedPreferenceChangeListener {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			Log.e("Vcuboid", "Filter changed");
			if (key.equals("favorite_filter")) {
				mVcubFilter.setShowOnlyFavorites(sharedPreferences.getBoolean(key, false));
				Log.e("Vcuboid", "Filter changed for " + mActivity);
				mActivity.onFilterChanged();
			}
		}
	}
}
