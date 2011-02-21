/*
 * Copyright (C) 2010 Guillaume Delente
 *
 * This file is part of .
 *
 * Vcuboid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * Vcuboid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vcuboid.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.vcuboid.list;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.MyLocationProvider;
import fr.vcuboid.R;
import fr.vcuboid.RestClient;
import fr.vcuboid.StationDetails;
import fr.vcuboid.VcuboidManager;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.map.VcuboidMapActivity;

public class VcuboidListActivity extends ListActivity implements
		IVcuboidActivity {

	private VcuboidManager mVcuboidManager = null;
	private VcuboidArrayAdaptor mAdapter = null;
	private ProgressDialog mPdialog = null;
	private int mSelected = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("Vcuboid", "OnCreate");
		setContentView(R.layout.station_list);
		mVcuboidManager = (VcuboidManager) getLastNonConfigurationInstance();
		if (mVcuboidManager == null) { // No AsyncTask running
			Log.d("Vcuboid", "Bundle empty");
			mVcuboidManager = VcuboidManager.getVcuboidManagerInstance(this);
		} else {
			Log.d("Vcuboid", "Recovering from bundle");
			mVcuboidManager.attach(this);
		}
		mAdapter = new VcuboidArrayAdaptor(this, R.layout.station_list_entry,
				mVcuboidManager.getVisibleStations());
		this.setListAdapter(mAdapter);
		final Intent intent = new Intent(this, StationDetails.class);
		final ListView listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			    public void onItemClick(AdapterView<?> parent, View view,
			        int position, long id) {
			    	Log.i("Vcuboid", "Item clicked");
			    	intent.putExtra("id", (Integer) 
			    			((VcuboidArrayAdaptor.ViewHolder) view.getTag()).favorite.getTag())
			    			.putExtra("distance", 
			    					((StationOverlay) listView.getItemAtPosition(position))
			    					.getStation().getDistance());
			    	startActivity(intent);
			    }
			  });
	}

	@Override
	protected void onResume() {
		super.onResume();
		mVcuboidManager.setCurrentActivity(this);
		mVcuboidManager.startLocation();
		onListUpdated();
		Log.i("Vcuboid", "onResume " + this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mVcuboidManager.stopLocation();
		Log.i("Vcuboid", "onPause");
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
			return true;
		case R.id.menu_update_all:
			mVcuboidManager.executeUpdateAllStationsTask();
			return true;
		case R.id.menu_filters:
			startActivity(new Intent(this, ListFilterActivity.class));
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
		mVcuboidManager.detach();
		return (mVcuboidManager);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void showGetAllStationsOnProgress() {
		Log.i("Vcuboid", "showGetAllStationsOnProgress");
		showDialog(VcuboidManager.RETRIEVE_ALL_STATIONS);
	}

	@Override
	public void updateGetAllStationsOnProgress(int progress) {
		Log.i("Vcuboid", "updateGetAllStationsOnProgress");
		mPdialog.setMessage(getString(R.string.saving_db_summary));
	}

	@Override
	public void finishGetAllStationsOnProgress() {
		//onListUpdated();
		dismissDialog(VcuboidManager.RETRIEVE_ALL_STATIONS);
	}

	@Override
	public void showUpdateAllStationsOnProgress(boolean animate) {
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);
		loading.setVisibility(View.VISIBLE);
		if (animate) {
			AnimationSet set = new AnimationSet(true);
			Animation animation = new AlphaAnimation(0.0f, 1.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
					0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, -1.0f,
					Animation.RELATIVE_TO_SELF, 0.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			LayoutAnimationController controller = new LayoutAnimationController(
					set, 0.5f);
			loading.setLayoutAnimation(controller);
		}
	}

	@Override
	public void finishUpdateAllStationsOnProgress() {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, -1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);
		loading.startAnimation(set);
		loading.setVisibility(View.INVISIBLE);
		onListUpdated();
	}

	@Override
	public void onLocationChanged(Location l) {
		onListUpdated();
		Toast.makeText(this, getString(R.string.position_updated), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onListUpdated() {
		Log.i("Vcuboid", "notifyDataSetChanged");
		mAdapter.notifyDataSetChanged();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case RestClient.NETWORK_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.network_error)).setMessage(
					(getString(R.string.network_error_summary)))
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case RestClient.JSON_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.json_error)).setMessage(
					(getString(R.string.json_error_summary)))
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case RestClient.DB_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.db_error)).setMessage(
					(getString(R.string.db_error_summary))).setPositiveButton(
					"Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					}).create();
		case MyLocationProvider.ENABLE_GPS:
			Log.i("Vcuboid", "onPrepareDialog : ENABLE_GPS");
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					getString(R.string.gps_disabled))					
					.setMessage(
							getString(R.string.should_enable_gps) + "\n" +
							getString(R.string.show_location_parameters))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent gpsOptionsIntent = new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivity(gpsOptionsIntent);
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case MyLocationProvider.NO_LOCATION_PROVIDER:
			Log.i("Vcuboid", "onPrepareDialog : NO_LOCATION_PROVIDER");
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					getString(R.string.location_disabled)).setMessage(
					getString(R.string.should_enable_location) + "\n"
							+ getString(R.string.show_location_parameters))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent gpsOptionsIntent = new Intent(
											android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
									startActivity(gpsOptionsIntent);
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case VcuboidManager.RETRIEVE_ALL_STATIONS:
			mPdialog = new ProgressDialog(VcuboidListActivity.this);
			mPdialog.setCancelable(false);
			mPdialog.setTitle(getString(R.string.retrieve_all));
			mPdialog.setMessage((getString(R.string.querying_server_summary)));
			return mPdialog;
		case VcuboidManager.REMOVE_FROM_FAVORITE:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.remove_favorite)).setMessage(
					(getString(R.string.remove_favorite_sure)))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mVcuboidManager.setFavorite(mSelected,
											false);
									onListUpdated();
									dialog.cancel();
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									onListUpdated();
									dialog.cancel();
								}
							}).create();
		}
		return super.onCreateDialog(id);
	}

	public void setFavorite(int id, boolean isChecked) {
		mSelected = id;
		if (isChecked) {
			mVcuboidManager.setFavorite(id, true);
			onListUpdated();
		} else
			showDialog(VcuboidManager.REMOVE_FROM_FAVORITE);
	}
}
