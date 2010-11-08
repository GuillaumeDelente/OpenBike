package fr.vcuboid;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SimpleCursorAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.vcuboid.database.VcuboidDBAdapter;

public class Vcuboid extends ListActivity {

	private VcuboidManager mVcuboidManager = null;
	private SimpleCursorAdapter mAdapter = null;
	private boolean isConfigurationChanged = false;
	
	private ProgressDialog mPd = null;
	private int[] mListAdapterTo = new int[] { R.id.name_entry, R.id.bikes_entry,
			R.id.slots_entry };
	String[] mListAdapterFrom = new String[] { VcuboidDBAdapter.KEY_NAME,
			VcuboidDBAdapter.KEY_BIKES, VcuboidDBAdapter.KEY_SLOTS };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.station_list);
			mVcuboidManager = (VcuboidManager) getLastNonConfigurationInstance();
			if (mVcuboidManager == null) { // No AsyncTask running
				mVcuboidManager = new VcuboidManager(this);
			} else {
				mVcuboidManager.attach(this);
			}
			FavoriteListener.vcuboidManager = mVcuboidManager;


			mAdapter = new VcuboidSimpleCursorAdaptor(this,
					R.layout.station_list_entry, mVcuboidManager
							.getCursor(), mListAdapterFrom, mListAdapterTo);
			this.setListAdapter(mAdapter);
		} catch (Exception e) {
			Log.e("ERROR", "ERROR IN CODE:" + e.toString());
		}
	}
	
	

	@Override
	protected void onResume() {
		super.onResume();
		mAdapter = new VcuboidSimpleCursorAdaptor(this,
				R.layout.station_list_entry, mVcuboidManager
						.getCursor(), mListAdapterFrom, mListAdapterTo);
		this.setListAdapter(mAdapter);
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
			finish();
			//mAdapter.getCursor().requery();
			return true;
		case R.id.update_all:
			mVcuboidManager.executeUpdateAllStationsTask();
			return true;		
		case R.id.filters:
			startActivity(new Intent(this, FilterPreferencesActivity.class));
			mAdapter = new VcuboidSimpleCursorAdaptor(this,
					R.layout.station_list_entry, mVcuboidManager
							.getCursor(), mListAdapterFrom, mListAdapterTo);
			this.setListAdapter(mAdapter);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mPd != null)
			mPd.dismiss();
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

	public void showGetAllStationsOnProgress() {
		String title = "Récuperation de la liste des stations";
		String message = "Contact du serveur en cours...";
		mPd = ProgressDialog.show(this, title, message, true);
	}

	public void updateGetAllStationsOnProgress(int progress) {
		String title = "Récuperation de la liste des stations";
		mPd
				.setMessage("Données recues, sauvegarde dans la base de donnée en cours...");
	}

	public void finishGetAllStationsOnProgress() {
		// TODO : registerDataSetObserver for automatying refresh
		// when cursor changes
		//mAdapter.getCursor().requery();
		mPd.dismiss();
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
		// mAdapter.getCursor().requery();
	}
}

class FavoriteListener implements OnCheckedChangeListener {
	
	static public VcuboidManager vcuboidManager = null;

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		vcuboidManager.setFavorite((Integer) buttonView.getTag(), isChecked);
	}
}