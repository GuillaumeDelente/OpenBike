package fr.vcuboid;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;
import fr.vcuboid.database.VcuboidDBAdapter;

public class Vcuboid extends ListActivity {

	private VcuboidDBAdapter mVcuboidDBAdapter = null;
	private ProgressDialog dialog = null;
	private getAllStationsFromNetworkTask task = null;
	private SimpleCursorAdapter mAdapter = null;
	private Cursor cursor = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.station_list);
			task = (getAllStationsFromNetworkTask) getLastNonConfigurationInstance();
			// No AsyncTask running
			if (task == null) {
				mVcuboidDBAdapter = new VcuboidDBAdapter(this);
				// Open or create the database
				mVcuboidDBAdapter.open();
			} else {
				// get back the DB adapter because DB is open
				mVcuboidDBAdapter = task.getVcuboidDBAdapter();
				task.attach(this);
				if (task.getProgress() < 100) {
					updateProgress(task.getProgress());
				}
			}
			cursor = mVcuboidDBAdapter.getAllStationsCursor();
			startManagingCursor(cursor);
			String[] columns = new String[] { VcuboidDBAdapter.KEY_NAME,
					VcuboidDBAdapter.KEY_BIKES, VcuboidDBAdapter.KEY_SLOTS };
			int[] to = new int[] { R.id.name_entry, R.id.bikes_entry, R.id.slots_entry };
			// Create the adapter for bounding data to the listView
			mAdapter = new SimpleCursorAdapter(this,
					R.layout.station_list_entry, cursor, columns, to);
			// Apply it
			this.setListAdapter(mAdapter);
			if (task == null && cursor.getCount() == 0) {
				// If the station list is empty, retrieve it
				// from the network
				task = new getAllStationsFromNetworkTask(this,
						mVcuboidDBAdapter);
				task.execute();
			}
		} catch (Exception e) {
			Log.e("ERROR", "ERROR IN CODE:" + e.toString());
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.all_stations_list_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.clear_db:
	        clearDB();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (dialog != null)
			dialog.dismiss();
		if (task != null)
			task.detach();
		return (task);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (task == null)
			mVcuboidDBAdapter.close();
	}

	void updateProgress(int progress) {
		String title = "Récuperation de la liste des stations";
		String message = progress < 50 ? "Contact du serveur en cours..."
				: "Données recues, sauvegarde dans la base de donnée en cours...";
		if (dialog == null) {
			dialog = ProgressDialog.show(this, title, message, true);
		} else {
			dialog.setMessage(message);
		}
	}

	void markAsDone() {
		dialog.dismiss();
		if (mAdapter.getCursor() != null)
			mAdapter.getCursor().requery();
		Log.e("OKAY", "finished onPostExecute");
	}
	
	void clearDB() {
		mVcuboidDBAdapter.reset();
		mVcuboidDBAdapter.close();
		mVcuboidDBAdapter.open();
	}

	private class getAllStationsFromNetworkTask extends
			AsyncTask<Void, Void, Void> {

		private Vcuboid activity = null;
		private VcuboidDBAdapter vcuboidDBAdapter = null;
		private int progress = 0;

		public getAllStationsFromNetworkTask(Vcuboid activity,
				VcuboidDBAdapter vcuboidDBAdapter) {
			this.activity = activity;
			this.vcuboidDBAdapter = vcuboidDBAdapter;
		}

		protected void onPreExecute() {
			if (activity == null) {
				Log.w("RotationAsync",
						"onProgressUpdate() skipped -- no activity");
			} else {
				activity.updateProgress(progress);
			}
		}

		protected Void doInBackground(Void... unused) {
			String json = RestClient
					.connect("http://vcuboid.appspot.com/stations");
			publishProgress();
			RestClient.jsonStationsToDb(json, vcuboidDBAdapter);
			publishProgress();
			Log.i("Finished !", "Ok!");
			return (null);
		}

		protected void onProgressUpdate(Void... unused) {
			if (activity == null) {
				Log.w("RotationAsync",
						"onProgressUpdate() skipped -- no activity");
			} else {
				activity.updateProgress(progress += 50);
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (activity == null) {
				Log
						.w("RotationAsync",
								"onPostExecute() skipped -- no activity");
			} else {
				activity.markAsDone();
			}
		}

		void detach() {
			activity = null;
		}

		void attach(Vcuboid activity) {
			this.activity = activity;
		}

		int getProgress() {
			return (progress);
		}

		VcuboidDBAdapter getVcuboidDBAdapter() {
			return vcuboidDBAdapter;
		}
	}
}