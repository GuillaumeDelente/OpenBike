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

package fr.openbike.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import fr.openbike.IActivityHelper;
import fr.openbike.R;
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.model.Network;
import fr.openbike.service.SyncService;
import fr.openbike.utils.ActivityHelper;
import fr.openbike.utils.DetachableResultReceiver;

public class HomeActivity extends Activity implements DetachableResultReceiver.Receiver, IActivityHelper {

	public static final String ACTION_CHOOSE_NETWORK = "action_choose_network";
	private static final String EXTRA_NETWORKS = "extra_networks";

	private AlertDialog mNetworkDialog;
	private NetworkAdapter mNetworkAdapter;
	private SharedPreferences mSharedPreferences;
	private LayoutInflater mLayoutInflater;
	private ProgressDialog mPdialog = null;
	private ActivityHelper mActivityHelper = null;
	protected DetachableResultReceiver mReceiver = null;

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
		mPdialog = new ProgressDialog(this);
		if (savedInstanceState != null) {
			ArrayList<Network> networks = (ArrayList<Network>) savedInstanceState
					.getSerializable(EXTRA_NETWORKS);
			if (networks != null)
				mNetworkAdapter.insertAll(networks);
		}
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		setListeners();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction() != null
				&& intent.getAction().equals(ACTION_CHOOSE_NETWORK)) {
			showChooseNetwork();
		}
		super.onNewIntent(intent);
	}

	@Override
	protected void onResume() {
		mReceiver.setReceiver(this);
		if (getIntent().getAction().equals(ACTION_CHOOSE_NETWORK)) {
			showChooseNetwork();
			return;
		}
		if (mSharedPreferences.getInt(
				AbstractPreferencesActivity.NETWORK_PREFERENCE, 0) == AbstractPreferencesActivity.NO_NETWORK
				&& (mNetworkDialog == null || !mNetworkDialog.isShowing())) {
			showChooseNetwork();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		mReceiver.clearReceiver();
		super.onPause();
	}

	private void showChooseNetwork() {
		final Intent intent = new Intent(SyncService.ACTION_CHOOSE_NETWORK,
				null, this, SyncService.class);
		intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mReceiver);
		startService(intent);
	}

	private void startSync() {
		if (mSharedPreferences.getInt(
				AbstractPreferencesActivity.NETWORK_PREFERENCE, 0) == 0)
			return;
		final Intent intent = new Intent(SyncService.ACTION_SYNC, null, this,
				SyncService.class);
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
								OpenBikeListActivity.class));
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
		case R.id.progress:
			mPdialog.setCancelable(false);
			return mPdialog;
		case R.id.database_error:
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					R.string.db_error).setMessage(R.string.db_error_summary)
					.setPositiveButton(R.string.Ok, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					}).create();
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
										showProgressDialog(R.string.loading,
												R.string.loading);
										startSync();
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
													network.getServerUrl()
															+ network.getId());
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
									editor.commit();
								}
							}).setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (preferences
											.getInt(
													AbstractPreferencesActivity.NETWORK_PREFERENCE,
													0) == 0) {
										finish();
									} else {
										dismissDialog(R.id.choose_network);
									}
								}
							}).create();
			return mNetworkDialog;
		}
		return super.onCreateDialog(id);
	}

	public void showProgressDialog(int titleRes, int messageRes) {
		mPdialog.setTitle(titleRes);
		mPdialog.setMessage(getString(messageRes));
		if (!mPdialog.isShowing())
			showDialog(R.id.progress);
	}

	public void dismissProgressDialog() {
		if (mPdialog.isShowing())
			dismissDialog(R.id.progress);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActivityHelper.onPostCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mActivityHelper.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.refresh_menu_items, menu);
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
		case R.id.action_refresh:
			startSync();
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
			Log.d("OpenBike", "Enabled : "
					+ ((AlertDialog) dialog).getListView()
							.getCheckedItemPosition());

			((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE)
					.setEnabled(
							((AlertDialog) dialog).getListView()
									.getCheckedItemPosition() != -1);
		}
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		Log.d("OpenBike", "OnReceiveResult " + resultCode);
		if (resultCode == SyncService.STATUS_SYNC_NETWORKS) {
			showDialog(R.id.choose_network);
		} else if (resultCode == SyncService.STATUS_SYNC_NETWORKS_FINISHED) {
			ArrayList<Network> networks = resultData
					.getParcelableArrayList(SyncService.EXTRA_RESULT);
			displayNetworks(networks);
		} else if (resultCode == SyncService.STATUS_SYNC_STATIONS) {
		} else if (resultCode == SyncService.STATUS_SYNC_STATIONS_FINISHED) {
			dismissProgressDialog();
		}
	}

	/**
	 * Returns the {@link ActivityHelper} object associated with this activity.
	 */
	@Override
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}

}
