/*
 * Copyright (C) 2011 Guillaume Delente
 *
 * This file is part of OpenBike.
 *
 * OpenBike is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * OpenBike is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenBike.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.openbike.list;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import fr.openbike.IOpenBikeActivity;
import fr.openbike.MyLocationProvider;
import fr.openbike.OpenBikeManager;
import fr.openbike.R;
import fr.openbike.RestClient;
import fr.openbike.StationDetails;
import fr.openbike.list.OpenBikeArrayAdaptor.ViewHolder;
import fr.openbike.map.OpenBikeMapActivity;
import fr.openbike.object.Station;

public class OpenBikeListActivity extends ListActivity implements
		IOpenBikeActivity {

	private OpenBikeManager mVcuboidManager = null;
	private OpenBikeArrayAdaptor mAdapter = null;
	private ProgressDialog mPdialog = null;
	private int mSelected = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.i("OpenBike", "OnCreate");
		setContentView(R.layout.station_list);
		mVcuboidManager = (OpenBikeManager) getLastNonConfigurationInstance();
		if (mVcuboidManager == null) { // No AsyncTask running
			//Log.d("OpenBike", "Bundle empty");
			mVcuboidManager = OpenBikeManager.getVcuboidManagerInstance(this);
		} else {
			//Log.d("OpenBike", "Recovering from bundle");
			mVcuboidManager.attach(this);
		}
		mAdapter = new OpenBikeArrayAdaptor(this, R.layout.station_list_entry,
				mVcuboidManager.getVisibleStations());
		this.setListAdapter(mAdapter);
		//Maybe mAdapter was null when calling onListUpdated
		//so do it now
		onListUpdated();
		final Intent intent = new Intent(this, StationDetails.class);
		final ListView listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				//Log.i("OpenBike", "Item clicked");
				intent.putExtra("id",
						(Integer) ((OpenBikeArrayAdaptor.ViewHolder) view
								.getTag()).favorite.getTag());
				startActivity(intent);
			}
		});
		registerForContextMenu(listView);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mVcuboidManager.setCurrentActivity(this);
		mVcuboidManager.startLocation();
		// Cannot remove update even
		// if it's sometime useless
		onListUpdated();
		//Log.i("OpenBike", "onResume " + this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		finishUpdateAllStationsOnProgress(false);
		mVcuboidManager.stopLocation();
		//Log.i("OpenBike", "onPause");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.station_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_update_all:
			mVcuboidManager.executeUpdateAllStationsTask(true);
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, ListFilterActivity.class));
			return true;
		case R.id.menu_map:
			startActivity(new Intent(this, OpenBikeMapActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_context_menu, menu);
		ViewHolder holder = ((ViewHolder) ((AdapterContextMenuInfo) menuInfo).targetView
				.getTag());
		menu.removeItem(holder.favorite.isChecked() ? R.id.add_favorite
				: R.id.remove_favorite);
		menu.setHeaderTitle(holder.name.getText());
		// FIXME : If listView change, we don't get the good
		// id in onContextItemSelected
		mSelected = (Integer) holder.favorite.getTag();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent intent;
		Station station;
		switch (item.getItemId()) {
		case R.id.show_on_map:
			intent = new Intent(this, OpenBikeMapActivity.class);
			intent.putExtra("id", mSelected);
			startActivity(intent);
			return true;
		case R.id.show_on_google_maps:
			station = mVcuboidManager.getStation(mSelected);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="
					+ station.getGeoPoint().getLatitudeE6() * 1E-6 + ","
					+ station.getGeoPoint().getLongitudeE6() * 1E-6 + " ("
					+ station.getName() + ")")));
			return true;
		case R.id.navigate:
			station = mVcuboidManager.getStation(mSelected);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse("google.navigation:q="
							+ station.getGeoPoint().getLatitudeE6() * 1E-6
							+ "," + station.getGeoPoint().getLongitudeE6()
							* 1E-6)));
			return true;
		case R.id.add_favorite:
			mVcuboidManager.setFavorite(mSelected, true);
			onListUpdated();
			return true;
		case R.id.remove_favorite:
			mVcuboidManager.setFavorite(mSelected, false);
			onListUpdated();
			return true;
		case R.id.show_details:
			intent = new Intent(this, StationDetails.class);
			intent.putExtra("id", mSelected);
			startActivity(intent);
			onListUpdated();
			return true;
		default:
			return super.onContextItemSelected(item);
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
		//Log.i("OpenBike", "showGetAllStationsOnProgress");
		showDialog(OpenBikeManager.RETRIEVE_ALL_STATIONS);
	}

	@Override
	public void updateGetAllStationsOnProgress(int progress) {
		//Log.i("OpenBike", "updateGetAllStationsOnProgress");
		mPdialog.setMessage(getString(R.string.saving_db_summary));
	}

	@Override
	public void finishGetAllStationsOnProgress() {
		// onListUpdated();
		dismissDialog(OpenBikeManager.RETRIEVE_ALL_STATIONS);
	}

	public void setEmptyList() {
		findViewById(R.id.loading).setVisibility(View.GONE);
		getListView().setEmptyView(findViewById(R.id.empty));
	}

	public void setLoadingList() {
		findViewById(R.id.empty).setVisibility(View.GONE);
		getListView().setEmptyView(findViewById(R.id.loading));
	}

	@Override
	public void showUpdateAllStationsOnProgress(boolean animate) {
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.updating);
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
	public void finishUpdateAllStationsOnProgress(boolean animate) {
		RelativeLayout loading = (RelativeLayout) findViewById(R.id.updating);
		loading.setVisibility(View.INVISIBLE);
		if (animate) {
			AnimationSet set = new AnimationSet(true);
			Animation animation = new AlphaAnimation(1.0f, 0.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
					0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, 0.0f,
					Animation.RELATIVE_TO_SELF, -1.0f);
			animation.setDuration(500);
			set.addAnimation(animation);
			loading.startAnimation(set);
			loading.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onLocationChanged(Location l) {
		onListUpdated();
		if (l == null)
			return;
		Toast.makeText(this, getString(R.string.position_updated),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onListUpdated() {
		//Log.i("OpenBike", "notifyDataSetChanged");
		if (mAdapter == null) //OnCreate
			return;
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
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					getString(R.string.gps_disabled)).setMessage(
					getString(R.string.should_enable_gps) + "\n"
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
		case MyLocationProvider.NO_LOCATION_PROVIDER:
			//Log.i("OpenBike", "onPrepareDialog : NO_LOCATION_PROVIDER");
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
		case OpenBikeManager.RETRIEVE_ALL_STATIONS:
			mPdialog = new ProgressDialog(OpenBikeListActivity.this);
			mPdialog.setCancelable(false);
			mPdialog.setTitle(getString(R.string.retrieve_all));
			mPdialog.setMessage((getString(R.string.querying_server_summary)));
			return mPdialog;
		case OpenBikeManager.REMOVE_FROM_FAVORITE:
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
		} else {
			showDialog(OpenBikeManager.REMOVE_FROM_FAVORITE);
		}
	}
}
