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

import java.util.ArrayList;
import java.util.Iterator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.maps.GeoPoint;

import fr.openbike.ILocationService;
import fr.openbike.ILocationServiceListener;
import fr.openbike.IOpenBikeActivity;
import fr.openbike.LocationService;
import fr.openbike.OpenBikeManager;
import fr.openbike.R;
import fr.openbike.RestClient;
import fr.openbike.StationDetails;
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.database.StationsProvider;
import fr.openbike.filter.BikeFilter;
import fr.openbike.filter.FilterPreferencesActivity;
import fr.openbike.list.OpenBikeArrayAdaptor.ViewHolder;
import fr.openbike.map.OpenBikeMapActivity;
import fr.openbike.object.MinimalStation;
import fr.openbike.object.Network;
import fr.openbike.utils.Utils;

public class OpenBikeListActivity extends ListActivity implements
		IOpenBikeActivity, ILocationServiceListener {

	public static final int WELCOME_MESSAGE = 2;
	public static final int CHOOSE_NETWORK = 3;
	public static final String ACTION_CHOOSE_NETWORK = "android.intent.action.choose_network";
	private OpenBikeArrayAdaptor mAdapter = null;
	private ProgressDialog mPdialog = null;
	private AlertDialog mNetworkDialog = null;
	private String mSelected = null;
	private boolean mBackToList = false;
	private boolean mIsBound = false;
	private static ArrayList<Network> mNetworks;
	private CreateListAdaptorTask mCreateListAdaptorTask = null;
	private UpdateDistanceTask mUpdateDistanceTask = null;
	private ILocationService mBoundService = null;
	private SharedPreferences mPreferences = null;
	private ServiceConnection mConnection = null;
	private OpenBikeDBAdapter mDBAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.station_list);
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			showStationDetails(getIntent().getData());
			finish();
		}
		mPdialog = new ProgressDialog(OpenBikeListActivity.this);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		final ListView listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				showStationDetails(String
						.valueOf((Integer) ((OpenBikeArrayAdaptor.ViewHolder) view
								.getTag()).favorite.getTag()));
			}
		});
		registerForContextMenu(listView);
		mConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				Log.d("OpenBike", "Service connected");
				mBoundService = ((LocationService.LocationServiceBinder) service)
						.getService();
				mBoundService.addListener(OpenBikeListActivity.this);
			}

			public void onServiceDisconnected(ComponentName className) {
				mBoundService = null;
				Toast.makeText(OpenBikeListActivity.this, "Disconnected",
						Toast.LENGTH_SHORT).show();
			}
		};
		mDBAdapter = OpenBikeDBAdapter.getInstance(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			showStationDetails(intent.getData());
		} else {
			setIntent(intent);
		}
		// handleIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		boolean useLocation = mPreferences.getBoolean(
				FilterPreferencesActivity.LOCATION_PREFERENCE, false);
		OpenBikeManager.setCurrentActivity(this);
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			findViewById(R.id.search_results).setVisibility(View.VISIBLE);
			// handles a search query
			String query = intent.getStringExtra(SearchManager.QUERY);
			showResults(query);
		} else if (ACTION_CHOOSE_NETWORK.equals(intent.getAction())) {
			// TODO
			// mOpenBikeManager.executeShowNetworksTask();
		} else {
			findViewById(R.id.search_results).setVisibility(View.GONE);
			if (useLocation) {
				// Create the list when receiving location
				doBindService();
			} else {
				executeCreateListAdaptorTask(null);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// mOpenBikeManager.detach();
	}

	@Override
	protected void onStop() {
		if (mIsBound)
			doUnbindService();
		super.onStop();
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
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_search:
			onSearchRequested();
			return true;
		case R.id.menu_update_all:
			// TODO
			// mOpenBikeManager.executeUpdateAllStationsTask(true);
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
					StationsProvider.CONTENT_URI, mSelected), null, null, null,
					null);
			startActivity(Utils.getMapsIntent(station.getInt(station
					.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE)), station
					.getInt(station
							.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE)),
					station.getString(station
							.getColumnIndex(OpenBikeDBAdapter.KEY_NAME))));
			return true;
		case R.id.navigate:
			station = managedQuery(Uri.withAppendedPath(
					StationsProvider.CONTENT_URI, mSelected), null, null, null,
					null);
			startActivity(Utils.getNavigationIntent(station.getInt(station
					.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE)), station
					.getInt(station
							.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE))));
			return true;
		case R.id.add_favorite:
			mDBAdapter.updateFavorite(Integer.parseInt(mSelected), true);
			onListUpdated();
			return true;
		case R.id.remove_favorite:
			mDBAdapter.updateFavorite(Integer.parseInt(mSelected), false);
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
	public boolean onSearchRequested() {
		mBackToList = true;
		return super.onSearchRequested();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Context context = this;
		final SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		switch (id) {
		case RestClient.NETWORK_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.network_error)).setMessage(
					(getString(R.string.network_error_summary)))
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (PreferenceManager
											.getDefaultSharedPreferences(
													context)
											.getInt(
													FilterPreferencesActivity.NETWORK_PREFERENCE,
													0) == 0
									// TODO
									// ||
									// !mOpenBikeManager.isStationListRetrieved()
									) {
										mAdapter = null;
										finish();
									} else {
										dialog.cancel();
									}
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
									if (PreferenceManager
											.getDefaultSharedPreferences(
													context)
											.getInt(
													FilterPreferencesActivity.NETWORK_PREFERENCE,
													0) == 0
									// TODO
									// ||
									// !mOpenBikeManager.isStationListRetrieved()
									) {
										mAdapter = null;
										finish();
									} else {
										dialog.cancel();
									}
								}
							}).create();
		case OpenBikeDBAdapter.DB_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.db_error)).setMessage(
					(getString(R.string.db_error_summary))).setPositiveButton(
					"Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (PreferenceManager
									.getDefaultSharedPreferences(context)
									.getInt(
											FilterPreferencesActivity.NETWORK_PREFERENCE,
											0) == 0
							// TODO
							// || !mOpenBikeManager.isStationListRetrieved()
							) {
								mAdapter = null;
								finish();
							} else {
								dialog.cancel();
							}
						}
					}).create();
		case RestClient.URL_ERROR:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.url_error)).setMessage(
					(getString(R.string.url_error_summary))).setPositiveButton(
					"Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							editor
									.putInt(
											FilterPreferencesActivity.NETWORK_PREFERENCE,
											0);
							editor.commit();
							startActivity(new Intent(context,
									OpenBikeListActivity.class)
									.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
						}
					}).create();
		case LocationService.ENABLE_GPS:
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
		case LocationService.NO_LOCATION_PROVIDER:
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
		case OpenBikeManager.PROGRESS_DIALOG:
			mPdialog.setCancelable(false);
			return mPdialog;
		case OpenBikeManager.REMOVE_FROM_FAVORITE:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.remove_favorite)).setMessage(
					(getString(R.string.remove_favorite_sure)))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mDBAdapter.updateFavorite(Integer
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
		case CHOOSE_NETWORK:
			final SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			final CharSequence[] items = { "" };
			mNetworkDialog = new AlertDialog.Builder(this).setCancelable(false)
					.setTitle(getString(R.string.choose_network_title))
					.setSingleChoiceItems(items, -1,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									editor
											.putInt(
													FilterPreferencesActivity.NETWORK_PREFERENCE,
													mNetworks.get(item).getId());
									Button okButton = mNetworkDialog
											.getButton(Dialog.BUTTON_POSITIVE);
									okButton.setEnabled(true);
								}
							}).setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									editor.commit();
									// TODO:
									// mOpenBikeManager.updateNetworkTable(mNetworks);
									// mOpenBikeManager.executeCreateVisibleStationsTask(true);

									startActivity(new Intent(context,
											OpenBikeListActivity.class)
											.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

								}
							}).setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (preferences
											.getInt(
													FilterPreferencesActivity.NETWORK_PREFERENCE,
													0) == 0) {
										finish();
									} else {
										dismissDialog(CHOOSE_NETWORK);
										startActivity(new Intent(context,
												OpenBikeListActivity.class)
												.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
									}
								}
							}).create();
			return mNetworkDialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case CHOOSE_NETWORK:
			int size = 0;
			if (mNetworks != null)
				size = mNetworks.size();
			String[] items = new String[size];
			for (int i = 0; i < size; i++) {
				items[i] = mNetworks.get(i).getName() + " - "
						+ mNetworks.get(i).getCity();
			}
			((AlertDialog) dialog)
					.getListView()
					.setAdapter(
							new ArrayAdapter<String>(
									this,
									android.R.layout.select_dialog_singlechoice,
									items));
		}
		super.onPrepareDialog(id, dialog);
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

	@Override
	public void showProgressDialog(String title, String message) {
		mPdialog.setTitle(title);
		mPdialog.setMessage(message);
		if (!mPdialog.isShowing())
			showDialog(OpenBikeManager.PROGRESS_DIALOG);
	}

	@Override
	public void dismissProgressDialog() {
		// onListUpdated();
		if (mPdialog.isShowing())
			dismissDialog(OpenBikeManager.PROGRESS_DIALOG);
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
		if (loading.getVisibility() == View.VISIBLE)
			return;
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
		if (loading.getVisibility() == View.INVISIBLE)
			return;
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
	public void onLocationChanged(Location location, boolean firstFix) {
		boolean distanceFiltering = mPreferences.getBoolean(
				FilterPreferencesActivity.ENABLE_DISTANCE_FILTER, false);
		if (distanceFiltering || firstFix) {
			executeCreateListAdaptorTask(location);
		} else {
			executeUpdateDistanceTask(location);
		}
		if (!firstFix && location != null) {
			Toast.makeText(this, getString(R.string.position_updated),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onListUpdated() {
		executeCreateListAdaptorTask(mIsBound ? mBoundService
				.getCurrentLocation() : null);
	}

	void doBindService() {
		Log.d("OpenBike", "Service binded");
		bindService(new Intent(this, LocationService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	public void setFavorite(int id, boolean isChecked) {
		mSelected = String.valueOf(id);
		if (isChecked) {
			OpenBikeDBAdapter.getInstance(this).updateFavorite(id, true);
			onListUpdated();
		} else {
			showDialog(OpenBikeManager.REMOVE_FROM_FAVORITE);
		}
	}

	private void showStationDetails(Uri uri) {
		Intent intent = new Intent(this, StationDetails.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(uri);
		startActivity(intent);
	}

	private void showStationDetails(String id) {
		showStationDetails(Uri.withAppendedPath(StationsProvider.CONTENT_URI,
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
		showOnMap(Uri.withAppendedPath(StationsProvider.CONTENT_URI, id));
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

	private void showResults(String query) {
		// TODO: asyncTask ?
		/*
		 * OpenBikeArrayAdaptor adapter = new OpenBikeArrayAdaptor(this,
		 * R.layout.station_list_entry, mOpenBikeManager
		 * .getSearchResults(query)); getListView().setAdapter(adapter);
		 */
	}

	@Override
	public void showChooseNetwork(ArrayList<Network> networks) {
		mNetworks = networks;
		showDialog(CHOOSE_NETWORK);
		Button okButton = mNetworkDialog.getButton(Dialog.BUTTON_POSITIVE);
		okButton.setEnabled(false);
	}

	private void executeCreateListAdaptorTask(Location location) {
		if (mCreateListAdaptorTask == null) {
			Log.d("OpenBike", "executeCreateListAdaptorTask");
			mCreateListAdaptorTask = (CreateListAdaptorTask) new CreateListAdaptorTask(
					location).execute();
		} else {

			Log.d("OpenBike", "Already launched executeCreateListAdaptorTask");
		}
	}

	private void executeUpdateDistanceTask(Location location) {
		Log.d("OpenBike", "executingUpdateDistanceTask");
		if (mCreateListAdaptorTask != null) {
			Log.d("OpenBike", "Setting update distance onPostExecute");
			mCreateListAdaptorTask.setUpdateOnPostExecute();
			return;
		}
		if (mUpdateDistanceTask != null) {
			Log.d("OpenBike", "Cancelling current updateDistanceTask");
			mUpdateDistanceTask.cancel(true);
		}
		mUpdateDistanceTask = (UpdateDistanceTask) new UpdateDistanceTask(
				location).execute();
	}

	private class CreateListAdaptorTask extends AsyncTask<Void, Void, Boolean> {

		private OpenBikeDBAdapter mOpenBikeDBAdapter = OpenBikeDBAdapter
				.getInstance(OpenBikeListActivity.this);
		private boolean mUpdateOnPostExecute = false;
		private boolean mAdaptorCreated = false;
		private ArrayList<MinimalStation> mStations = null;
		private Location mCurrentLocation = null;

		CreateListAdaptorTask(Location location) {
			mCurrentLocation = location;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setLoadingList();
			Log.d("OpenBike", "onPreExecute CreateListAdaptorTask");
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
			mStations = createStationList();
			if (mStations != null) {
				if (mAdapter == null) {
					mAdaptorCreated = true;
					mAdapter = new OpenBikeArrayAdaptor(
							(Context) OpenBikeListActivity.this,
							R.layout.station_list_entry, mStations);
				} else {
				}
				return true;
			} else {
				// TODO: error
				return false;
			}
		}

		private ArrayList<MinimalStation> createStationList() {
			int size = mOpenBikeDBAdapter.getStationCount();
			if (size == 0) {
				//TODO
				// retrieve from network
			}
			final int distanceFilter = mPreferences.getBoolean(
					FilterPreferencesActivity.ENABLE_DISTANCE_FILTER, false) ? mPreferences.getInt(
					FilterPreferencesActivity.DISTANCE_FILTER, 0) : 0;
			ArrayList<MinimalStation> stationsList = new ArrayList<MinimalStation>(
					size);

			Cursor cursor = mOpenBikeDBAdapter.getFilteredStationsCursor(
					new String[] { BaseColumns._ID,
							OpenBikeDBAdapter.KEY_BIKES,
							OpenBikeDBAdapter.KEY_SLOTS,
							OpenBikeDBAdapter.KEY_OPEN,
							OpenBikeDBAdapter.KEY_LATITUDE,
							OpenBikeDBAdapter.KEY_LONGITUDE,
							OpenBikeDBAdapter.KEY_NAME,
							OpenBikeDBAdapter.KEY_FAVORITE }, Utils
							.whereClauseFromFilter(BikeFilter.getInstance(OpenBikeListActivity.this)),
					mCurrentLocation == null ? OpenBikeDBAdapter.KEY_NAME
							: null);
			MinimalStation minimalStation;
			Location stationLocation = null;
			int distanceToStation = 0;
			int id_col = cursor.getColumnIndex(BaseColumns._ID);
			int latitude_col = cursor
					.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE);
			int longitude_col = cursor
					.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE);
			int name_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_NAME);
			int bikes_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES);
			int slots_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS);
			int open_col = cursor.getColumnIndex(OpenBikeDBAdapter.KEY_OPEN);
			int favorite_col = cursor
					.getColumnIndex(OpenBikeDBAdapter.KEY_FAVORITE);
			if (mCurrentLocation != null)
				stationLocation = new Location("");
			while (cursor.moveToNext()) {
				if (isCancelled())
					return null;
				if (mCurrentLocation != null) {
					stationLocation.setLatitude((double) cursor
							.getInt(latitude_col) * 1E-6);
					stationLocation.setLongitude((double) cursor
							.getInt(longitude_col) * 1E-6);
					distanceToStation = (int) stationLocation
							.distanceTo(mCurrentLocation);
					if (distanceFilter != 0
							&& distanceToStation > distanceFilter)
						continue;
				}
				minimalStation = new MinimalStation(cursor.getInt(id_col),
						cursor.getString(name_col), cursor
								.getInt(longitude_col), cursor
								.getInt(latitude_col),
						cursor.getInt(bikes_col), cursor.getInt(slots_col),
						cursor.getInt(open_col) == 0 ? false : true, cursor
								.getInt(favorite_col) == 0 ? false : true,
						mCurrentLocation != null ? distanceToStation : -1);
				stationsList.add(minimalStation);
			}
			cursor.close();
			if (isCancelled())
				return null;
			if (mCurrentLocation != null) {
				Utils.sortStationsByDistance(stationsList);
			}
			return stationsList;
		}

		@Override
		protected void onPostExecute(Boolean isListCreated) {
			Log.d("OpenBike", "onPostExecute CreateListAdaptorTask");
			if (!isListCreated) {
				// TODO
				// executeGetAllStationsTask();
			} else {
				if (mAdaptorCreated) {
					setListAdapter(mAdapter);
				} else {
					Log.d("OpenBike", "mAdapter not null, setting list : "
							+ mStations.size());
					ArrayList<MinimalStation> list = mAdapter.getList();
					list.clear();
					list.addAll(mStations);
				}
				mAdapter.notifyDataSetChanged();
				// TODO: first run dialog
				mCreateListAdaptorTask = null;
				if (mUpdateOnPostExecute)
					executeUpdateDistanceTask(mIsBound ? mBoundService
							.getCurrentLocation() : null);
			}
			setEmptyList();
		}

		private void setUpdateOnPostExecute() {
			mUpdateOnPostExecute = true;
		}
	}

	private class UpdateDistanceTask extends AsyncTask<Void, Void, Void> {

		Location mLocation = null;

		protected UpdateDistanceTask(Location location) {
			Log.d("OpenBike", "New UpdateDistanceTask");
			mLocation = location;
		}

		@Override
		protected Void doInBackground(Void... unused) {
			Iterator<MinimalStation> it = mAdapter.getList().iterator();
			if (mLocation == null) {
				while (it.hasNext()) {
					it.next().setDistance(LocationService.DISTANCE_UNAVAILABLE);
				}
				return null;
			}
			MinimalStation station;
			Location stationLocation = new Location("");
			GeoPoint geoPoint = null;
			while (it.hasNext()) {
				station = it.next();
				geoPoint = station.getGeoPoint();
				stationLocation.setLatitude(geoPoint.getLatitudeE6() * 1E-6);
				stationLocation.setLongitude(geoPoint.getLongitudeE6() * 1E-6);
				station
						.setDistance((int) mLocation
								.distanceTo(stationLocation));
			}
			// FIXME:
			// Use mAdapter.sort
			Utils.sortStationsByDistance(mAdapter.getList());
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			Log.d("OpenBike", "onPostExecute UpdateDistanceTask");
			mAdapter.notifyDataSetChanged();
		}
	}
}
