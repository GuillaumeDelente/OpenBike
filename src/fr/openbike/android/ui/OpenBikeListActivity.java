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
package fr.openbike.android.ui;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.maps.GeoPoint;

import fr.openbike.android.IActivityHelper;
import fr.openbike.android.R;
import fr.openbike.android.database.OpenBikeDBAdapter;
import fr.openbike.android.database.StationsProvider;
import fr.openbike.android.model.MinimalStation;
import fr.openbike.android.service.ILocationServiceListener;
import fr.openbike.android.service.LocationService;
import fr.openbike.android.service.LocationService.LocationBinder;
import fr.openbike.android.ui.OpenBikeArrayAdaptor.ViewHolder;
import fr.openbike.android.ui.widget.SeekBarPreference;
import fr.openbike.android.utils.ActivityHelper;
import fr.openbike.android.utils.DetachableResultReceiver;
import fr.openbike.android.utils.Utils;

public class OpenBikeListActivity extends ListActivity implements
		ILocationServiceListener, DetachableResultReceiver.Receiver,
		IActivityHelper {

	public static final String ACTION_FAVORITE = "fr.openbike.action_favorite";

	private OpenBikeArrayAdaptor mAdapter = null;
	private String mSelected = null;
	private CreateListAdaptorTask mCreateListAdaptorTask = null;
	private UpdateDistanceTask mUpdateDistanceTask = null;
	private SharedPreferences mSharedPreferences = null;
	private OpenBikeDBAdapter mDBAdapter = null;
	private ActivityHelper mActivityHelper = null;
	private ListView mListView = null;
	protected DetachableResultReceiver mReceiver = null;
	private LocationService mService = null;
	private boolean mBound = false;

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocationBinder binder = (LocationBinder) service;
			mService = binder.getService();
			mBound = true;
			mService.addListener(OpenBikeListActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_layout);
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			showStationDetails(getIntent().getData());
			finish();
		}
		mReceiver = DetachableResultReceiver.getInstance(new Handler());
		mActivityHelper = new ActivityHelper(this);
		mActivityHelper.setupActionBar(getString(R.string.station_list));
		mListView = getListView();
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				showStationDetails(String
						.valueOf((Integer) ((OpenBikeArrayAdaptor.ViewHolder) view
								.getTag()).favorite.getTag()));
			}
		});
		registerForContextMenu(mListView);
		mDBAdapter = OpenBikeDBAdapter.getInstance(this);
		startService(new Intent(this, LocationService.class));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActivityHelper.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			showStationDetails(intent.getData());
			finish();
		} else {
			setIntent(intent);
			mActivityHelper.clearActions();
			mActivityHelper.onPostCreate(null); // Change menu based on current
			// action
		}
	}

	@Override
	protected void onResume() {
		mReceiver.setReceiver(this);
		boolean useLocation = mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, true);
		Intent intent = getIntent();
		String action = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(action)) {
			((TextView) findViewById(R.id.empty)).setText(R.string.no_results);
			mActivityHelper
					.setActionBarTitle(getString(R.string.search_results));
			String query = intent.getStringExtra(SearchManager.QUERY);
			showResults(query);
		} else if (ACTION_FAVORITE.equals(action)) {
			((TextView) findViewById(R.id.empty))
					.setText(R.string.no_favorites);
			mActivityHelper
					.setActionBarTitle(getString(R.string.favorite_stations));
			if (!useLocation) {
				executeCreateListAdaptorTask(null, null);
			}
		} else {
			((TextView) findViewById(R.id.empty)).setText(R.string.no_stations);
			mActivityHelper.setActionBarTitle(getString(R.string.station_list));
			if (!useLocation) {
				executeCreateListAdaptorTask(null, null);
			}
		}
		mActivityHelper.onResume();
		super.onResume();
	}

	@Override
	protected void onStart() {
		if (mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, true)) {
			Intent intent = new Intent(this, LocationService.class);
			bindService(intent, mConnection, 0);
		}
		super.onStart();
	}

	@Override
	protected void onStop() {
		if (mBound) {
			mBound = false;
			mService.removeListener(OpenBikeListActivity.this);
			unbindService(mConnection);
		}
		super.onStop();
	}

	@Override
	protected void onPause() {
		mReceiver.clearReceiver();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mActivityHelper.onCreateOptionsMenu(menu);
		String action = getIntent().getAction();
		if (!Intent.ACTION_SEARCH.equals(action))
			getMenuInflater().inflate(R.menu.list_menu, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mActivityHelper.onPrepareOptionsMenu(menu);
		if (ACTION_FAVORITE.equals(getIntent().getAction())) {
			menu.setGroupVisible(R.id.menu_group_global, false);
		} else {
			menu.setGroupVisible(R.id.menu_group_global, true);
		}
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		getActivityHelper().onPrepareDialog(id, dialog);
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		default:
			return mActivityHelper.onOptionsItemSelected(item)
					|| super.onOptionsItemSelected(item);
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
			onStationsUpdated();
			return true;
		case R.id.remove_favorite:
			mDBAdapter.updateFavorite(Integer.parseInt(mSelected), false);
			onStationsUpdated();
			return true;
		case R.id.show_details:
			showStationDetails(mSelected);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.remove_from_favorite:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.remove_favorite)).setMessage(
					(getString(R.string.remove_favorite_sure)))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mDBAdapter.updateFavorite(Integer
											.parseInt(mSelected), false);
									onStationsUpdated();
									dialog.cancel();
								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									onStationsUpdated();
									dialog.cancel();
								}
							}).create();
			/*
			 * case R.id.welcome: return new
			 * AlertDialog.Builder(this).setCancelable(true).setTitle(
			 * getString(R.string.welcome_message_title)).setMessage(
			 * (getString(R.string.welcome_message))).setPositiveButton(
			 * R.string.Ok, new DialogInterface.OnClickListener() { public void
			 * onClick(DialogInterface dialog, int id) { dialog.cancel(); }
			 * }).create();
			 */
		default:
			Dialog dialog = getActivityHelper().onCreateDialog(id);
			if (dialog != null)
				return dialog;
		}
		return super.onCreateDialog(id);
	}

	public void setEmptyList() {
		findViewById(R.id.loading).setVisibility(View.GONE);
		mListView.setEmptyView(findViewById(R.id.empty));
	}

	public void setLoadingList() {
		findViewById(R.id.empty).setVisibility(View.GONE);
		mListView.setEmptyView(findViewById(R.id.loading));
	}

	@Override
	public void onLocationChanged(Location location, boolean firstFix) {
		boolean distanceFiltering = mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER, false);
		if (distanceFiltering || firstFix) {
			String query = null;
			Intent intent = getIntent();
			if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
				query = intent.getStringExtra(SearchManager.QUERY);
			}
			executeCreateListAdaptorTask(location, query);
		} else {
			executeUpdateDistanceTask(location);
		}
		if (!firstFix && location != null) {
			Toast.makeText(this, getString(R.string.position_updated),
					Toast.LENGTH_SHORT).show();
		}
	}

	public void onStationsUpdated() {
		executeCreateListAdaptorTask(mBound ? mService.getMyLocation() : null,
				null);
	}

	public void setFavorite(int id, boolean isChecked) {
		mSelected = String.valueOf(id);
		if (isChecked) {
			OpenBikeDBAdapter.getInstance(this).updateFavorite(id, true);
			onStationsUpdated();
		} else {
			showDialog(R.id.remove_from_favorite);
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

	private void showResults(String query) {
		executeCreateListAdaptorTask(mBound ? mService.getMyLocation() : null,
				query);
	}

	private void executeCreateListAdaptorTask(Location location, String query) {
		if (mCreateListAdaptorTask == null) {
			mCreateListAdaptorTask = (CreateListAdaptorTask) new CreateListAdaptorTask(
					location, query).execute();
		} else {
			mCreateListAdaptorTask.cancel(true);
			mCreateListAdaptorTask = (CreateListAdaptorTask) new CreateListAdaptorTask(
					location, query).execute();
		}
	}

	private void executeUpdateDistanceTask(Location location) {
		if (mCreateListAdaptorTask != null) {
			mCreateListAdaptorTask.setUpdateOnPostExecute();
			return;
		}
		if (mUpdateDistanceTask != null) {
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
		private Location currentLocation = null;
		private ArrayList<MinimalStation> mStations = null;
		private String mQuery = null;

		CreateListAdaptorTask(Location location, String query) {
			currentLocation = location;
			mQuery = query;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setLoadingList();
		}

		@Override
		protected Boolean doInBackground(Void... unused) {
			mStations = Intent.ACTION_SEARCH.equals(getIntent().getAction()) ? createSearchList()
					: createStationList();
			if (isCancelled())
				return false;
			if (mStations != null) {
				if (mAdapter == null) {
					mAdaptorCreated = true;
					mAdapter = new OpenBikeArrayAdaptor(
							(Context) OpenBikeListActivity.this,
							R.layout.station_list_entry, mStations);
				}
				return true;
			} else {
				// No stations in DB
				return false;
			}
		}

		private ArrayList<MinimalStation> createStationList() {
			int size = mOpenBikeDBAdapter.getStationCount();
			if (size == 0) {
				return null;
			}
			final int distanceFilter = mSharedPreferences.getBoolean(
					AbstractPreferencesActivity.ENABLE_DISTANCE_FILTER, false) ? mSharedPreferences
					.getInt(AbstractPreferencesActivity.DISTANCE_FILTER,
							SeekBarPreference.DEFAULT_DISTANCE)
					: 0;
			ArrayList<MinimalStation> stationsList = new ArrayList<MinimalStation>(
					size);
			if (isCancelled())
				return null;
			Cursor cursor = mOpenBikeDBAdapter
					.getFilteredStationsCursor(
							new String[] { BaseColumns._ID,
									OpenBikeDBAdapter.KEY_BIKES,
									OpenBikeDBAdapter.KEY_NETWORK,
									OpenBikeDBAdapter.KEY_SLOTS,
									OpenBikeDBAdapter.KEY_OPEN,
									OpenBikeDBAdapter.KEY_LATITUDE,
									OpenBikeDBAdapter.KEY_LONGITUDE,
									OpenBikeDBAdapter.KEY_NAME,
									OpenBikeDBAdapter.KEY_FAVORITE },
							ACTION_FAVORITE.equals(getIntent().getAction()) ? Utils.FAVORITE_WHERE_CLAUSE
									: Utils
											.whereClauseFromFilter(mSharedPreferences),
							currentLocation == null ? OpenBikeDBAdapter.KEY_NAME
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
			if (currentLocation != null)
				stationLocation = new Location("");
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					cursor.close();
					return null;
				}
				if (currentLocation != null) {
					stationLocation.setLatitude((double) cursor
							.getInt(latitude_col) * 1E-6);
					stationLocation.setLongitude((double) cursor
							.getInt(longitude_col) * 1E-6);
					distanceToStation = (int) stationLocation
							.distanceTo(currentLocation);
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
						currentLocation != null ? distanceToStation : -1);
				stationsList.add(minimalStation);
			}
			cursor.close();
			if (isCancelled())
				return null;
			if (currentLocation != null) {
				Utils.sortStationsByDistance(stationsList);
			}
			return stationsList;
		}

		private ArrayList<MinimalStation> createSearchList() {
			int size = mOpenBikeDBAdapter.getStationCount();
			if (size == 0) {
				return null;
			}
			ArrayList<MinimalStation> stationsList = new ArrayList<MinimalStation>(
					size);
			if (isCancelled())
				return null;
			Cursor cursor = mOpenBikeDBAdapter.getSearchCursor(mQuery);
			MinimalStation minimalStation;
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
			Location stationLocation = new Location("");
			while (cursor.moveToNext()) {
				if (isCancelled()) {
					cursor.close();
					return null;
				}
				if (currentLocation != null) {
					stationLocation.setLatitude((double) cursor
							.getInt(latitude_col) * 1E-6);
					stationLocation.setLongitude((double) cursor
							.getInt(longitude_col) * 1E-6);
					distanceToStation = (int) stationLocation
							.distanceTo(currentLocation);
				}
				minimalStation = new MinimalStation(cursor.getInt(id_col),
						cursor.getString(name_col), cursor
								.getInt(longitude_col), cursor
								.getInt(latitude_col),
						cursor.getInt(bikes_col), cursor.getInt(slots_col),
						cursor.getInt(open_col) == 0 ? false : true, cursor
								.getInt(favorite_col) == 0 ? false : true,
						currentLocation != null ? distanceToStation : -1);
				stationsList.add(minimalStation);
			}
			cursor.close();
			if (isCancelled())
				return null;
			return stationsList;
		}

		@Override
		protected void onPostExecute(Boolean isListCreated) {
			if (!isListCreated) {
				mCreateListAdaptorTask = null;
			} else {
				if (mAdaptorCreated) {
					setListAdapter(mAdapter);
				} else {
					ArrayList<MinimalStation> list = mAdapter.getList();
					list.clear();
					list.addAll(mStations);
				}
				mAdapter.notifyDataSetChanged();
				mCreateListAdaptorTask = null;
				if (mUpdateOnPostExecute)
					executeUpdateDistanceTask(currentLocation);
			}
			setEmptyList();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		private void setUpdateOnPostExecute() {
			mUpdateOnPostExecute = true;
		}
	}

	private class UpdateDistanceTask extends AsyncTask<Void, Void, Void> {

		Location currentLocation = null;

		protected UpdateDistanceTask(Location location) {
			currentLocation = location;
		}

		@Override
		protected Void doInBackground(Void... unused) {
			if (mAdapter == null)
				return null;
			Iterator<MinimalStation> it = mAdapter.getList().iterator();
			if (currentLocation == null) {
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
				station.setDistance((int) currentLocation
						.distanceTo(stationLocation));
			}
			// TODO: Use mAdapter.sort
			Utils.sortStationsByDistance(mAdapter.getList());
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (mAdapter != null)
				mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		mActivityHelper.onReceiveResult(resultCode, resultData);
	}

	@Override
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}

	@Override
	public void onLocationProvidersChanged(int id) {
		if (!isFinishing()) {
			showDialog(id);
		}
	}
}
