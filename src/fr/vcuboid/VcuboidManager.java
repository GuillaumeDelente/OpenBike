package fr.vcuboid;

import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import fr.vcuboid.database.VcuboidDBAdapter;

public class VcuboidManager {

	static public boolean isUpdating = false;
	protected VcuboidDBAdapter mVcuboidDBAdapter = null;
	protected Vcuboid mActivity = null;
	protected Cursor mCursor = null;
	private GetAllStationsTask mGetAllStationsTask = null;
	private UpdateAllStationsTask mUpdateAllStationsTask = null;

	public VcuboidManager(Vcuboid activity) {
		Log.e("Vcuboid", "New Manager created");
		mActivity = activity;
		mVcuboidDBAdapter = new VcuboidDBAdapter(mActivity);
		mVcuboidDBAdapter.open();
	}

	void attach(Vcuboid activity) {
		mActivity = activity;
		mActivity.startManagingCursor(mCursor);
		if (mGetAllStationsTask != null) {
			retrieveGetAllStationTask();
		} else if (mUpdateAllStationsTask != null) {
			retrieveUpdateAllStationTask();
		}
	}

	void detach() {
		mActivity.stopManagingCursor(mCursor);
		mActivity = null;
	}

	Cursor getAllStationsCursor() {
		if (mCursor == null) {
			mCursor = mVcuboidDBAdapter.getAllStationsCursor();
			mActivity.startManagingCursor(mCursor);
		} else {
			mCursor.requery();
		}
		if (mCursor.getCount() == 0) {
			executeGetAllStationsTask();
		}
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
		}
	}

	private void retrieveUpdateAllStationTask() {
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
			} else {
				mActivity.finishGetAllStationsOnProgress();
				mGetAllStationsTask = null;
			}
		}

		int getProgress() {
			return (progress);
		}
	}

	private class UpdateAllStationsTask extends AsyncTask<Void, Void, Void> {

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
			isUpdating = false;
			if (mActivity == null) {
				Log
						.w("RotationAsync",
								"onPostExecute() skipped -- no activity");
			} else {
				mActivity.finishUpdateAllStationsOnProgress();
				mUpdateAllStationsTask = null;
			}
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
}
