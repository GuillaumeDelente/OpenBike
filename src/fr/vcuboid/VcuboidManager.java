package fr.vcuboid;

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

public class VcuboidManager {

	public static boolean isUpdating = false;
	protected static VcuboidDBAdapter mVcuboidDBAdapter = null;
	protected static IVcuboidActivity mActivity = null;
	private static Cursor mCursor = null;
	private static VcuboidManager mThis;
	private static SharedPreferences mFilterPreferences = null;
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
			mVcuboidDBAdapter.open();
			mActivity = activity;
			mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		}
		return mThis;
	}
	
	public static synchronized VcuboidManager getVcuboidManagerInstance() {
		return mThis;
	}

	public void attach(VcuboidListActivity activity) {
		mActivity = activity;
		((Activity) mActivity).startManagingCursor(mCursor);
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences((Context) mActivity);
		if (mGetAllStationsTask != null) {
			mThis.retrieveGetAllStationTask();
		} else if (mUpdateAllStationsTask != null) {
			mThis.retrieveUpdateAllStationTask();
		}
	}

	public void detach() {
		((Activity) mActivity).stopManagingCursor(mCursor);
		mActivity = null;
	}
	
	public Cursor getCursor() {
		if (mCursor != null) {
			((Activity) mActivity).stopManagingCursor(mCursor);
			mCursor.close();
		}

		mCursor = mVcuboidDBAdapter
				.getFilteredStationsCursor(mVcubFilter);
		
		((Activity) mActivity).startManagingCursor(mCursor);

		 if (mCursor.getCount() == 0)
		 	if (mVcuboidDBAdapter.getStationCount() == 0 && mGetAllStationsTask == null) // Because of filters, check the whole table
		 		mThis.executeGetAllStationsTask();	
		 mCursor.requery();
		 
		return mCursor;
	}

	private boolean executeGetAllStationsTask() {
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

	public void onDestroy() {
		mVcuboidDBAdapter.close();
		/*
		 * if (mGetAllStationsTask == null && mUpdateAllStationsTask == null)
		 * mVcuboidDBAdapter.close();
		 */
	}

	public void clearDB() {
		mVcuboidDBAdapter.reset();
		mVcuboidDBAdapter.close();
		mVcuboidDBAdapter.open();
	}

	public void setFavorite(int id, boolean isChecked) {
		mVcuboidDBAdapter.updateFavorite(id, isChecked);
		mCursor.requery();
	}
	
	public void onPause() {
		mVcuboidDBAdapter.close();
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
				mCursor.requery();
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
				mCursor.requery();
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
			}
		}
	}
}
