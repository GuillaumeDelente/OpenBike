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
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
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
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.database.SuggestionProvider;
import fr.openbike.list.OpenBikeArrayAdaptor.ViewHolder;
import fr.openbike.map.OpenBikeMapActivity;
import fr.openbike.utils.Utils;

public class OpenBikeListActivity extends ListActivity implements
		IOpenBikeActivity {

	public static final int WELCOME_MESSAGE = 2;
	private OpenBikeManager mOpenBikeManager = null;
	private OpenBikeArrayAdaptor mAdapter = null;
	private ProgressDialog mPdialog = null;
	private String mSelected = null;
	private boolean mBackToList = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.i("OpenBike", "OnCreate");
		setContentView(R.layout.station_list);
		mOpenBikeManager = (OpenBikeManager) getLastNonConfigurationInstance();
		if (mOpenBikeManager == null) { // No AsyncTask running
			mOpenBikeManager = OpenBikeManager.getVcuboidManagerInstance(this);
		} else {
			mOpenBikeManager.attach(this);
		}

		final ListView listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Log.i("OpenBike", "Item clicked");
				showStationDetails(String
						.valueOf((Integer) ((OpenBikeArrayAdaptor.ViewHolder) view
								.getTag()).favorite.getTag()));
			}
		});
		registerForContextMenu(listView);
		handleIntent(getIntent());
	}

	private void showStationDetails(Uri uri) {
		Intent intent = new Intent(this, StationDetails.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(uri);
		startActivity(intent);
	}

	private void showStationDetails(String id) {
		showStationDetails(Uri.withAppendedPath(SuggestionProvider.CONTENT_URI,
				id));
	}

	private void showOnMap(Uri uri) {
		Intent intent = new Intent(this, OpenBikeMapActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setAction(OpenBikeMapActivity.ACTION_DETAIL);
		intent.setData(uri);
		startActivity(intent);
	}

	private void showOnMap(String id) {
		showOnMap(Uri.withAppendedPath(SuggestionProvider.CONTENT_URI, id));
	}

	@Override
	protected void onResume() {
		super.onResume();
		mOpenBikeManager.setCurrentActivity(this);
		mOpenBikeManager.startLocation();
		// Cannot remove update even
		// if it's sometime useless
		onListUpdated();
		// Log.i("OpenBike", "onResume " + this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		finishUpdateAllStationsOnProgress(false);
		mOpenBikeManager.stopLocation();
		// Log.i("OpenBike", "onPause");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			findViewById(R.id.search_results).setVisibility(View.VISIBLE);
			// handles a search query
			String query = intent.getStringExtra(SearchManager.QUERY);
			showResults(query);
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			showStationDetails(intent.getData());
		} else {
			findViewById(R.id.search_results).setVisibility(View.GONE);
			mAdapter = new OpenBikeArrayAdaptor(this,
					R.layout.station_list_entry, mOpenBikeManager
							.getVisibleStations());
			this.setListAdapter(mAdapter);
			// Maybe mAdapter was null when calling onListUpdated
			// so do it now
			onListUpdated();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)
				&& Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			if (goBack())
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private boolean goBack() {
		if (mBackToList) {
			mBackToList = false;
			startActivity(new Intent(this, OpenBikeListActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).setClass(this,
							OpenBikeListActivity.class));
			return true;
		} else {
			finish();
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
			menu.setGroupVisible(R.id.menu_group_list_search, true);
			menu.setGroupVisible(R.id.menu_group_list_view, false);
		} else {
			menu.setGroupVisible(R.id.menu_group_list_search, false);
			menu.setGroupVisible(R.id.menu_group_list_view, true);
		}
		return true;
	}

	@Override
	public boolean onSearchRequested() {
		mBackToList = true;
		return super.onSearchRequested();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_search:
			onSearchRequested();
			return true;
		case R.id.menu_update_all:
			mOpenBikeManager.executeUpdateAllStationsTask(true);
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, ListFilterActivity.class));
			return true;
		case R.id.menu_map:
			startActivity(new Intent(this, OpenBikeMapActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return true;
		case R.id.menu_back:
			goBack();
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
		if (!Utils.isIntentAvailable(new Intent(Intent.ACTION_VIEW, Uri
				.parse("geo:0,0?q=bibi")), this)) {
			menu.removeItem(R.id.show_on_google_maps);
		}
		if (!Utils.isIntentAvailable(new Intent(Intent.ACTION_VIEW, Uri
				.parse("google.navigation:q=bibi")), this)) {
			menu.removeItem(R.id.navigate);
		}
		menu.removeItem(holder.favorite.isChecked() ? R.id.add_favorite
				: R.id.remove_favorite);
		mSelected = (String.valueOf((Integer) holder.favorite.getTag()));
		menu.setHeaderTitle(holder.name.getText());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Cursor station;
		switch (item.getItemId()) {
		case R.id.show_on_map:
			showOnMap(String.valueOf(mSelected));
			return true;
		case R.id.show_on_google_maps:
			station = managedQuery(Uri.withAppendedPath(
					SuggestionProvider.CONTENT_URI, mSelected), null, null,
					null, null);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="
					+ station.getInt(station
							.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE))
					* 1E-6
					+ ","
					+ station.getInt(station
							.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE))
					* 1E-6
					+ " ("
					+ station.getString(station
							.getColumnIndex(OpenBikeDBAdapter.KEY_NAME)) + ")")));
			return true;
		case R.id.navigate:
			station = managedQuery(Uri.withAppendedPath(
					SuggestionProvider.CONTENT_URI, mSelected), null, null,
					null, null);
			startActivity(new Intent(
					Intent.ACTION_VIEW,
					Uri
							.parse("google.navigation:q="
									+ station
											.getInt(station
													.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE))
									* 1E-6
									+ ","
									+ station
											.getInt(station
													.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE))
									* 1E-6)));
			return true;
		case R.id.add_favorite:
			mOpenBikeManager.setFavorite(Integer.parseInt(mSelected), true);
			onListUpdated();
			return true;
		case R.id.remove_favorite:
			mOpenBikeManager.setFavorite(Integer.parseInt(mSelected), false);
			onListUpdated();
			return true;
		case R.id.show_details:
			showStationDetails(mSelected);
			onListUpdated();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		mOpenBikeManager.detach();
		return (mOpenBikeManager);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void showGetAllStationsOnProgress() {
		// Log.i("OpenBike", "showGetAllStationsOnProgress");
		showDialog(OpenBikeManager.RETRIEVE_ALL_STATIONS);
	}

	@Override
	public void updateGetAllStationsOnProgress(int progress) {
		// Log.i("OpenBike", "updateGetAllStationsOnProgress");
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
		// Log.i("OpenBike", "notifyDataSetChanged");
		if (mAdapter == null) // OnCreate
			return;
		mAdapter.notifyDataSetChanged();
	}

	/**
	 * Searches the dictionary and displays results for the given query.
	 * 
	 * @param query
	 *            The search query
	 */
	private void showResults(String query) {

		OpenBikeArrayAdaptor adapter = new OpenBikeArrayAdaptor(this,
				R.layout.station_list_entry, mOpenBikeManager
						.getSearchResults(query));
		getListView().setAdapter(adapter);
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
		case OpenBikeDBAdapter.JSON_ERROR:
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
		case OpenBikeDBAdapter.DB_ERROR:
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
			// Log.i("OpenBike", "onPrepareDialog : NO_LOCATION_PROVIDER");
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
									mOpenBikeManager.setFavorite(Integer
											.parseInt(mSelected), false);
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
		case WELCOME_MESSAGE:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.welcome_message_title)).setMessage(
					(getString(R.string.welcome_message))).setPositiveButton(
					"Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					}).create();
		}
		return super.onCreateDialog(id);
	}

	public void setFavorite(int id, boolean isChecked) {
		mSelected = String.valueOf(id);
		if (isChecked) {
			mOpenBikeManager.setFavorite(id, true);
			onListUpdated();
		} else {
			showDialog(OpenBikeManager.REMOVE_FROM_FAVORITE);
		}
	}
}
