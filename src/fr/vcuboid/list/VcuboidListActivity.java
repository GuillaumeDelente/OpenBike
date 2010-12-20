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
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.MyLocationProvider;
import fr.vcuboid.R;
import fr.vcuboid.RestClient;
import fr.vcuboid.VcuboidManager;
import fr.vcuboid.map.VcuboidMapActivity;

public class VcuboidListActivity extends ListActivity implements
		IVcuboidActivity {

	private VcuboidManager mVcuboidManager = null;
	private VcuboidArrayAdaptor mAdapter = null;
	private ProgressDialog mPd = null;

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
		mAdapter = new VcuboidArrayAdaptor(this, R.layout.station_list_entry,
				mVcuboidManager.getVisibleStations());
		this.setListAdapter(mAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mVcuboidManager.setCurrentActivity(this);
		mVcuboidManager.startLocation();
		onListUpdated();
		Log.e("Vcuboid", "onResume " + this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mVcuboidManager.stopLocation();
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
		onListUpdated();
		mPd.dismiss();
	}

	@Override
	public void showUpdateAllStationsOnProgress() {
		AnimationSet set = new AnimationSet(true);
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				-1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(500);
		set.addAnimation(animation);
		LayoutAnimationController controller = new LayoutAnimationController(
				set, 0.5f);
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);
		loading.setVisibility(View.VISIBLE);
		loading.setLayoutAnimation(controller);
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
	}

	@Override
	public void onListUpdated() {
		Log.e("Vcuboid", "notifyDataSetChanged");
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog dialog;
		switch (id) {
		case RestClient.NETWORK_ERROR:
			builder.setMessage(getString(R.string.network_error_summary))
					.setTitle(getString(R.string.network_error)).setCancelable(
							true).setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			break;
		case RestClient.JSON_ERROR:
			builder.setMessage(R.string.json_error_summary).setTitle(
					getString(R.string.json_error)).setCancelable(true)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			break;
		case RestClient.DB_ERROR:
			builder.setMessage(R.string.db_error_summary).setTitle(
					getString(R.string.db_error)).setCancelable(true)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			break;
			case MyLocationProvider.ENABLE_GPS:
			builder.setTitle(getString(R.string.gps_disabled)).setMessage(
					getString(R.string.show_location_parameters)).setCancelable(
					false).setPositiveButton(getString(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent gpsOptionsIntent = new Intent(
									android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							startActivity(gpsOptionsIntent);
						}
					});
			builder.setNegativeButton(getString(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			break;
		default:
			return super.onCreateDialog(id);
		}
		dialog = builder.create();
		return dialog;
	}
}

class FavoriteListener implements OnCheckedChangeListener {

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		buttonView.getTag();
		VcuboidManager.getVcuboidManagerInstance().setFavorite(
				(Integer) buttonView.getTag(), isChecked);
	}
}