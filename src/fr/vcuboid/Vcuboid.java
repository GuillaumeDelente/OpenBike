package fr.vcuboid;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import fr.vcuboid.database.VcuboidDBAdapter;

public class Vcuboid extends ListActivity {
	private VcuboidManager mVcuboidManager = null;
	private ProgressDialog dialog = null;
	private SimpleCursorAdapter mAdapter = null;
	private boolean isConfigurationChanged = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.station_list);
			mVcuboidManager = (VcuboidManager) getLastNonConfigurationInstance();
			// No AsyncTask running
			if (mVcuboidManager == null) {
				mVcuboidManager = new VcuboidManager(this);
			} else {
				mVcuboidManager.attach(this);
				/*
				 * // get back the DB adapter because DB is open
				 * mVcuboidDBAdapter = task.getVcuboidDBAdapter();
				 * task.attach(this); if (task.getProgress() < 100) {
				 * updateProgress(task.getProgress()); }
				 */
			}

			String[] columns = new String[] { VcuboidDBAdapter.KEY_NAME,
					VcuboidDBAdapter.KEY_BIKES, VcuboidDBAdapter.KEY_SLOTS };
			int[] to = new int[] { R.id.name_entry, R.id.bikes_entry,
					R.id.slots_entry };

			// Create the adapter for bounding data to the listView
			mAdapter = new VcuboidSimpleCursorAdaptor(this,
					R.layout.station_list_entry, mVcuboidManager
							.getAllStationsCursor(), columns, to);

			// Apply it
			this.setListAdapter(mAdapter);

			/*
			 * if (task == null && cursor.getCount() == 0) { // If the station
			 * list is empty, retrieve it // from the network task = new
			 * getAllStationsFromNetworkTask(this, mVcuboidDBAdapter);
			 * task.execute(); }
			 */
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
			mVcuboidManager.clearDB();
			mAdapter.getCursor().requery();
			return true;
		case R.id.update_all:
			mVcuboidManager.executeUpdateAllStationsTask();
			/*
			 * new updateAllStationsFromNetworkTask(this, mVcuboidDBAdapter)
			 * .execute();
			 */
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (dialog != null)
			dialog.dismiss();
		mVcuboidManager.detach();
		isConfigurationChanged = true;
		return (mVcuboidManager);
	}

	@Override
	protected void onDestroy() {
		if (!isConfigurationChanged)
			mVcuboidManager.onDestroy();
		super.onDestroy();
	}

	/*
	 * void updateGetAllStationsProgress(int progress) { String title =
	 * "Récuperation de la liste des stations"; String message = progress < 50 ?
	 * "Contact du serveur en cours..." :
	 * "Données recues, sauvegarde dans la base de donnée en cours..."; if
	 * (dialog == null) { dialog = ProgressDialog.show(this, title, message,
	 * true); } else { dialog.setMessage(message); } }
	 * 
	 * void markAsDone() { dialog.dismiss();
	 * 
	 * if (mAdapter.getCursor() != null) mAdapter.getCursor().requery();
	 * 
	 * Log.e("OKAY", "finished onPostExecute"); }
	 * 
	 * void markAsDoneRefreshing() { refreshing = false; if
	 * (mAdapter.getCursor() != null) mAdapter.getCursor().requery();
	 * Log.e("OKAY", "finished onPostExecute"); }
	 */

	/*
	 * public void setUpdatingStations() { for (int i = 0; i <
	 * this.getListView().getChildCount(); i++) { Log.e("setUpdatingStations",
	 * "At " + i);
	 * this.getListView().getChildAt(i).findViewById(R.id.refreshing)
	 * .setVisibility(View.VISIBLE); } }
	 * 
	 * 
	 * private class updateAllStationsFromNetworkTask extends AsyncTask<Void,
	 * Void, Void> {
	 * 
	 * private Vcuboid activity = null; private VcuboidDBAdapter
	 * vcuboidDBAdapter = null; private int progress = 0;
	 * 
	 * public updateAllStationsFromNetworkTask(Vcuboid activity,
	 * VcuboidDBAdapter vcuboidDBAdapter) { this.activity = activity;
	 * this.vcuboidDBAdapter = vcuboidDBAdapter; }
	 * 
	 * protected void onPreExecute() { if (activity == null) {
	 * Log.w("RotationAsync", "onProgressUpdate() skipped -- no activity"); }
	 * else { activity.setUpdatingStations(); } }
	 * 
	 * protected Void doInBackground(Void... unused) {
	 * RestClient.jsonBikesToDb(RestClient
	 * .connect("http://vcuboid.appspot.com/stations"), vcuboidDBAdapter);
	 * Log.i("Finished !", "Ok!"); return (null); }
	 * 
	 * @Override protected void onPostExecute(Void unused) { if (activity ==
	 * null) { Log .w("RotationAsync",
	 * "onPostExecute() skipped -- no activity"); } else {
	 * activity.markAsDoneRefreshing(); } }
	 * 
	 * void detach() { activity = null; }
	 * 
	 * void attach(Vcuboid activity) { this.activity = activity; }
	 * 
	 * int getProgress() { return (progress); }
	 * 
	 * VcuboidDBAdapter getVcuboidDBAdapter() { return vcuboidDBAdapter; } }
	 */

	public void showGetAllStationsOnProgress() {
		String title = "Récuperation de la liste des stations";
		String message = "Contact du serveur en cours...";
		dialog = ProgressDialog.show(this, title, message, true);
	}

	public void updateGetAllStationsOnProgress(int progress) {
		String title = "Récuperation de la liste des stations";
		dialog
				.setMessage("Données recues, sauvegarde dans la base de donnée en cours...");
	}

	public void finishGetAllStationsOnProgress() {
		// TODO : registerDataSetObserver for automatying refresh
		// when cursor changes
		mAdapter.getCursor().requery();
		dialog.dismiss();
	}

	public void showUpdateAllStationsOnProgress() {
		for (int i = 0; i < this.getListView().getChildCount(); i++) {
			Log.e("setUpdatingStations", "At " + i);
			this.getListView().getChildAt(i).findViewById(R.id.refreshing)
					.setVisibility(View.VISIBLE);
		}
	}

	public void finishUpdateAllStationsOnProgress() {
		// TODO : registerDataSetObserver for automatying refresh
		// when cursor changes
		for (int i = 0; i < this.getListView().getChildCount(); i++) {
			Log.e("setUpdatingStations", "At " + i);
			this.getListView().getChildAt(i).findViewById(R.id.refreshing)
					.setVisibility(View.INVISIBLE);
		}
		mAdapter.getCursor().requery();
	}
}