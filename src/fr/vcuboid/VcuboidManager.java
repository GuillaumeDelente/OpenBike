package fr.vcuboid;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import fr.vcuboid.database.VcuboidDBAdapter;

public class VcuboidManager {

	static public boolean isUpdating = false;
	protected VcuboidDBAdapter mVcuboidDBAdapter = null;
	protected VcuboidListActivity mActivity = null;
	protected Cursor mCursor = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;
	private SharedPreferences mFilterPreferences = null;

	public VcuboidManager(VcuboidListActivity activity) {
		Log.e("Vcuboid", "New Manager created");
		mActivity = activity;
		mVcuboidDBAdapter = new VcuboidDBAdapter(mActivity);
		mVcuboidDBAdapter.open();
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
	}

	public void attach(VcuboidListActivity activity) {
		mActivity = activity;
		mActivity.startManagingCursor(mCursor);
		mFilterPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		if (mGetAllStationsTask != null) {
			retrieveGetAllStationTask();
		} else if (mUpdateAllStationsTask != null) {
			retrieveUpdateAllStationTask();
		}
	}

	public void detach() {
		mActivity.stopManagingCursor(mCursor);
		mActivity = null;
	}
	
	public Cursor getCursor() {
		if (mCursor != null)
			mCursor.close();

		mCursor = mVcuboidDBAdapter
				.getFilteredStationsCursor(
						mFilterPreferences.getBoolean("favorite_filter", false));
						
		mActivity.startManagingCursor(mCursor);

		 if (mCursor.getCount() == 0)
		 	if (mVcuboidDBAdapter.getStationCount() == 0) // Because of filters, check the whole table
		 		executeGetAllStationsTask();	
		 mCursor.requery();
		 
		return mCursor;
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
			mActivity.showGetAllStationsOnProgress();
			mActivity.updateGetAllStationsOnProgress(mGetAllStationsTask
					.getProgress());
		} else {
			mActivity.finishGetAllStationsOnProgress();
			mGetAllStationsTask = null;
		}
	}

	private void retrieveUpdateAllStationTask() {
		int progress = mUpdateAllStationsTask.getProgress();
		if (progress >= 100) {
			mActivity.finishUpdateAllStationsOnProgress();
			isUpdating = false;
			mUpdateAllStationsTask = null;
		}
	}

	private class GetAllStationsTask extends AsyncTask<Void, Void, Void> {

		private int progress = 0;

		protected void onPreExecute() {
			if (mActivity == null) {
				Log.w("RotationAsync",
						"onProgressUpdate() skipped -- no activity");
			} else {
				Log.w("GetAllStationsTask", "GOOOOOO");
				mActivity.showGetAllStationsOnProgress();
			}
		}

		protected Void doInBackground(Void... unused) {
			String json = RestClient
					.connect("http://vcuboid.appspot.com/stations");
			publishProgress();
			RestClient.jsonStationsToDb(json, mVcuboidDBAdapter);
			publishProgress();
			Log.i("Finished !", "Ok!");
			return (null);
		}

		protected void onProgressUpdate(Void... unused) {
			if (mActivity == null) {
				Log.w("RotationAsync",
						"onProgressUpdate() skipped -- no activity");
			} else {
				mActivity.updateGetAllStationsOnProgress(progress += 50);
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (mActivity == null) {
				Log
						.w("RotationAsync",
								"onPostExecute() skipped -- no activity");
				progress = 100;
			} else {
				mActivity.finishGetAllStationsOnProgress();
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
			if (mActivity == null) {
				Log.w("RotationAsync",
						"onProgressUpdate() skipped -- no activity");
			} else {
				mActivity.showUpdateAllStationsOnProgress();
			}
		}

		protected Void doInBackground(Void... unused) {
			RestClient.jsonBikesToDb(RestClient
					.connect("http://vcuboid.appspot.com/stations"),
					mVcuboidDBAdapter);
			Log.i("Finished !", "Ok!");
			return (null);
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (mActivity == null) {
				progress = 100;
				Log
						.w("RotationAsync",
								"onPostExecute() skipped -- no activity");
			} else {
				isUpdating = false;
				mActivity.finishUpdateAllStationsOnProgress();
				mUpdateAllStationsTask = null;
				mCursor.requery();
			}
		}

		public int getProgress() {
			return progress;
		}
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
		Log.e("Vcuboid", "setFavorite");
		mVcuboidDBAdapter.updateFavorite(id, isChecked);
		mCursor.requery();
	}
}
