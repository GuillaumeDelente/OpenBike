package fr.vcuboid.list;

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
import fr.vcuboid.FilterPreferencesActivity;
import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;
import fr.vcuboid.database.VcuboidDBAdapter;
import fr.vcuboid.map.VcuboidMapActivity;

public class VcuboidListActivity extends ListActivity implements
		IVcuboidActivity {

	private VcuboidManager mVcuboidManager = null;
	private SimpleCursorAdapter mAdapter = null;

	private ProgressDialog mPd = null;
	private int[] mListAdapterTo = new int[] { R.id.name_entry,
			R.id.bikes_entry, R.id.slots_entry };
	String[] mListAdapterFrom = new String[] { VcuboidDBAdapter.KEY_NAME,
			VcuboidDBAdapter.KEY_BIKES, VcuboidDBAdapter.KEY_SLOTS };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("Vcuboid", "OnCreate");
		setContentView(R.layout.station_list);
		mVcuboidManager = (VcuboidManager) getLastNonConfigurationInstance();
		if (mVcuboidManager == null) { // No AsyncTask running
			Log.e("Vcuboid", "No AsyncTask running");
			mVcuboidManager = VcuboidManager.getVcuboidManagerInstance(this);
		} else {
			Log.e("Vcuboid", "AsyncTask running, attaching it to the activity");
			mVcuboidManager.attach(this);
		}
		mAdapter = new VcuboidSimpleCursorAdaptor(this,
				R.layout.station_list_entry, mVcuboidManager.getCursor(),
				mListAdapterFrom, mListAdapterTo);
		this.setListAdapter(mAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mVcuboidManager.setCurrentActivity(this);
		Log.e("Vcuboid", "onResume " + this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.e("Vcuboid", "onPause");
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
		case R.id.menu_clear_db:
			mVcuboidManager.clearDB();
			finish();
			// mAdapter.getCursor().requery();
			return true;
		case R.id.menu_update_all:
			mVcuboidManager.executeUpdateAllStationsTask();
			return true;
		case R.id.menu_filters:
			startActivity(new Intent(this, FilterPreferencesActivity.class));
			return true;
		case R.id.menu_map:
			startActivity(new Intent(this, VcuboidMapActivity.class));
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
		return (mVcuboidManager);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void showGetAllStationsOnProgress() {
		String title = "Récuperation de la liste des stations";
		String message = "Contact du serveur en cours...";
		mPd = ProgressDialog.show(this, title, message, true);
	}

	@Override
	public void updateGetAllStationsOnProgress(int progress) {
		// String title = "Récuperation de la liste des stations";
		mPd
				.setMessage("Données recues, sauvegarde dans la base de donnée en cours...");
	}

	@Override
	public void finishGetAllStationsOnProgress() {
		// TODO : registerDataSetObserver for automatying refresh
		// when cursor changes
		mAdapter.getCursor().requery();
		mPd.dismiss();
	}

	@Override
	public void showUpdateAllStationsOnProgress() {
		for (int i = 0; i < this.getListView().getChildCount(); i++) {
			this.getListView().getChildAt(i).findViewById(R.id.refreshing)
					.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void finishUpdateAllStationsOnProgress() {
		mAdapter.getCursor().requery();
		for (int i = 0; i < this.getListView().getChildCount(); i++) {
			this.getListView().getChildAt(i).findViewById(R.id.refreshing)
					.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onFilterChanged() {
		Log.e("Vcuboid", "Changing cursor");
		mAdapter.changeCursor(mVcuboidManager.getCursor());
	}
}

class FavoriteListener implements OnCheckedChangeListener {

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		VcuboidManager.getVcuboidManagerInstance().setFavorite(
				(Integer) buttonView.getTag(), isChecked);
	}
}