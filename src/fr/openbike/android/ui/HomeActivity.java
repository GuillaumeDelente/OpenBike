/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.openbike.android.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import fr.openbike.android.IActivityHelper;
import fr.openbike.android.R;
import fr.openbike.android.database.OpenBikeDBAdapter;
import fr.openbike.android.model.Network;
import fr.openbike.android.service.ILocationServiceListener;
import fr.openbike.android.service.LocationService;
import fr.openbike.android.service.SyncService;
import fr.openbike.android.service.LocationService.LocationBinder;
import fr.openbike.android.utils.ActivityHelper;
import fr.openbike.android.utils.DetachableResultReceiver;

public class HomeActivity extends Activity implements ILocationServiceListener,
		DetachableResultReceiver.Receiver, IActivityHelper {

	public static final String ACTION_CHOOSE_NETWORK = "action_choose_network";
	private static final String EXTRA_NETWORKS = "extra_networks";

	private AlertDialog mNetworkDialog;
	private NetworkAdapter mNetworkAdapter;
	private SharedPreferences mSharedPreferences;
	private LayoutInflater mLayoutInflater;
	private ActivityHelper mActivityHelper = null;
	protected DetachableResultReceiver mReceiver = null;
	private boolean mBound = false;
	private LocationService mService = null;

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocationBinder binder = (LocationBinder) service;
			mService = binder.getService();
			mBound = true;
			mService.addListener(HomeActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_layout);
		mReceiver = DetachableResultReceiver.getInstance(new Handler());
		mActivityHelper = new ActivityHelper(this);
		mActivityHelper.setupActionBar(null);
		mLayoutInflater = LayoutInflater.from(this);
		mNetworkAdapter = new NetworkAdapter(this);
		if (savedInstanceState != null) {
			ArrayList<Network> networks = (ArrayList<Network>) savedInstanceState
					.getSerializable(EXTRA_NETWORKS);
			if (networks != null)
				mNetworkAdapter.insertAll(networks);
		}
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		setListeners();
		PreferenceManager.setDefaultValues(this, R.xml.filter_preferences,
				false);
		PreferenceManager.setDefaultValues(this, R.xml.map_preferences, false);
		PreferenceManager
				.setDefaultValues(this, R.xml.other_preferences, false);
		PreferenceManager.setDefaultValues(this, R.xml.location_preferences,
				false);
		startService(new Intent(this, LocationService.class));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		super.onNewIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mReceiver.setReceiver(this);
		if (ACTION_CHOOSE_NETWORK.equals(getIntent().getAction())) {
			setIntent(new Intent());
			showChooseNetwork();
			return;
		}
		if (mNetworkDialog == null || !mNetworkDialog.isShowing()) {
			mActivityHelper.onResume();
		}
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
			mService.removeListener(HomeActivity.this);
			unbindService(mConnection);
			mBound = false;
		}
		super.onStop();
	}

	@Override
	protected void onPause() {
		mReceiver.clearReceiver();
		super.onPause();
	}

	private void showChooseNetwork() {
		final Intent intent = new Intent(SyncService.ACTION_CHOOSE_NETWORK,
				null, this, SyncService.class);
		stopService(intent);
		intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mReceiver);
		startService(intent);
	}

	/**
	 * 
	 */
	private void setListeners() {
		findViewById(R.id.home_btn_list).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(HomeActivity.this,
								OpenBikeListActivity.class));
					}

				});

		findViewById(R.id.home_btn_map).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(HomeActivity.this,
								OpenBikeMapActivity.class));
					}
				});

		findViewById(R.id.home_btn_favorite).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(HomeActivity.this,
								OpenBikeListActivity.class)
								.setAction(OpenBikeListActivity.ACTION_FAVORITE));
					}
				});

		findViewById(R.id.home_btn_filters).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(HomeActivity.this,
								FiltersPreferencesActivity.class));
					}
				});

		findViewById(R.id.home_btn_settings).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(HomeActivity.this,
								SettingsPreferencesActivity.class));
					}
				});

		findViewById(R.id.home_btn_search).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						getActivityHelper().goSearch();
					}
				});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mNetworkDialog != null && mNetworkDialog.isShowing()) {
			if (!mNetworkAdapter.isEmpty())
				outState.putSerializable(EXTRA_NETWORKS, mNetworkAdapter
						.getItems());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final SharedPreferences.Editor editor = mSharedPreferences.edit();
		switch (id) {
		case R.id.choose_network:
			final SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			mNetworkDialog = new AlertDialog.Builder(this).setCancelable(false)
					.setTitle(getString(R.string.choose_network_title))
					.setSingleChoiceItems(mNetworkAdapter, -1,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									editor
											.putInt(
													AbstractPreferencesActivity.NETWORK_PREFERENCE,
													((Network) ((AlertDialog) dialog)
															.getListView()
															.getAdapter()
															.getItem(item))
															.getId());
									Button okButton = mNetworkDialog
											.getButton(Dialog.BUTTON_POSITIVE);
									okButton.setEnabled(true);
								}
							}).setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									try {
										ListView listView = ((AlertDialog) dialog)
												.getListView();
										Network network = (Network) listView
												.getItemAtPosition(listView
														.getCheckedItemPosition());
										OpenBikeDBAdapter.getInstance(
												HomeActivity.this)
												.insertNetwork(network);
										editor.commit();
										setCurrentNetwork(editor, network);
										getActivityHelper().startUpdate();
									} catch (SQLiteException e) {
										dismissDialog(R.id.choose_network);
										showDialog(R.id.database_error);
									}
								}

								private void setCurrentNetwork(Editor editor,
										Network network) {
									editor
											.putString(
													AbstractPreferencesActivity.UPDATE_SERVER_URL,
													network.getServerUrl());
									editor
											.putInt(
													AbstractPreferencesActivity.NETWORK_LATITUDE,
													network.getLatitude());
									editor
											.putInt(
													AbstractPreferencesActivity.NETWORK_LONGITUDE,
													network.getLongitude());
									editor
											.putString(
													AbstractPreferencesActivity.NETWORK_NAME,
													network.getName());
									editor
											.putString(
													AbstractPreferencesActivity.NETWORK_CITY,
													network.getCity());
									editor
											.putString(
													AbstractPreferencesActivity.SPECIAL_STATION,
													network.getSpecialName());
									Cursor networkCursor = OpenBikeDBAdapter
											.getInstance(HomeActivity.this)
											.getNetwork(
													network.getId(),
													new String[] { OpenBikeDBAdapter.KEY_VERSION });
									editor
											.putLong(
													AbstractPreferencesActivity.STATIONS_VERSION,
													networkCursor.moveToFirst() ? (long) networkCursor
															.getInt(0)
															: 0);
									editor.commit();
									/*
									 * ErrorReporter.getInstance().putCustomData(
									 * "Network",
									 * String.valueOf(network.getId()));
									 */
								}
							}).setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (preferences
											.getInt(
													AbstractPreferencesActivity.NETWORK_PREFERENCE,
													AbstractPreferencesActivity.NO_NETWORK) == AbstractPreferencesActivity.NO_NETWORK) {
										finish();
									} else {
										dismissDialog(R.id.choose_network);
									}
								}
							}).create();
			return mNetworkDialog;
		default:
			Dialog dialog = getActivityHelper().onCreateDialog(id);
			if (dialog != null)
				return dialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActivityHelper.onPostCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mActivityHelper.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.home_menu_items, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mActivityHelper.onPrepareOptionsMenu(menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		default:
			return mActivityHelper.onOptionsItemSelected(item)
					|| super.onOptionsItemSelected(item);
		}
	}

	private void displayNetworks(ArrayList<Network> networks) {
		mNetworkAdapter.insertAll(networks);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case R.id.choose_network:
			if (((AlertDialog) dialog).getListView().getEmptyView() == null) {
				View emptyView = mLayoutInflater.inflate(R.layout.loading_view,
						null);
				emptyView.setVisibility(View.GONE);
				((ViewGroup) ((AlertDialog) dialog).getListView().getParent())
						.addView(emptyView);
				((AlertDialog) dialog).getListView().setEmptyView(emptyView);
			}
			if (mNetworkAdapter != null)
				mNetworkAdapter.notifyDataSetChanged();
			((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE)
					.setEnabled(
							((AlertDialog) dialog).getListView()
									.getCheckedItemPosition() != -1);
			break;
		default:
			if (mNetworkDialog != null
					&& mNetworkDialog.isShowing()
					&& (id == R.id.json_error || id == R.id.database_error || id == R.id.network_error)) {
				mNetworkDialog.dismiss();
			}
			getActivityHelper().onPrepareDialog(id, dialog);
		}
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if (resultCode == SyncService.STATUS_SYNC_NETWORKS) {
			showDialog(R.id.choose_network);
		} else if (resultCode == SyncService.STATUS_SYNC_NETWORKS_FINISHED) {
			ArrayList<Network> networks = resultData
					.getParcelableArrayList(SyncService.EXTRA_RESULT);
			displayNetworks(networks);
		} else {
			getActivityHelper().onReceiveResult(resultCode, resultData);
		}
	}

	/**
	 * Returns the {@link ActivityHelper} object associated with this activity.
	 */
	@Override
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}

	@Override
	public void onLocationChanged(Location l, boolean alert) {
		// Nothing
	}

	@Override
	public void onLocationProvidersChanged(int id) {
		if (!isFinishing()) {
			showDialog(id);
		}
	}

	@Override
	public void onStationsUpdated() {
		// String a = null;
		// a.length();
	}
}
