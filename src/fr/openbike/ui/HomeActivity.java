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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import fr.openbike.R;
import fr.openbike.database.OpenBikeDBAdapter;
import fr.openbike.filter.FilterPreferencesActivity;
import fr.openbike.list.OpenBikeListActivity;
import fr.openbike.map.OpenBikeMapActivity;
import fr.openbike.object.Network;
import fr.openbike.service.SyncService;

public class HomeActivity extends BaseActivity {

	public static final int CHOOSE_NETWORK = 2;
	public static final String ACTION_CHOOSE_NETWORK = "action_choose_network";
	private static final String EXTRA_NETWORKS = "extra_networks";

	private BroadcastReceiver mBroadcastReceiver;
	private AlertDialog mNetworkDialog;
	private NetworkAdapter mNetworkAdapter;
	private SharedPreferences mSharedPreferences;
	private LayoutInflater mLayoutInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_layout);
		getActivityHelper().setupActionBar(null, 0);
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
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction().equals(ACTION_CHOOSE_NETWORK)) {
			showChooseNetwork();
		}
		super.onNewIntent(intent);
	}

	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(SyncService.ACTION_RESULT_NETWORK);
		registerReceiver(mBroadcastReceiver, filter);
		if (getIntent().getAction().equals(ACTION_CHOOSE_NETWORK)) {
			showChooseNetwork();
			return;
		}
		if (mSharedPreferences.getInt(
				FilterPreferencesActivity.NETWORK_PREFERENCE, 0) 
				== FilterPreferencesActivity.NO_NETWORK
				&& (mNetworkDialog == null || !mNetworkDialog.isShowing())) {
			showChooseNetwork();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		unregisterReceiver(mBroadcastReceiver);
		super.onPause();
	}

	private void showChooseNetwork() {
		final Intent intent = new Intent(SyncService.ACTION_CHOOSE_NETWORK,
				null, this, SyncService.class);
		startService(intent);
		showDialog(CHOOSE_NETWORK);
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
								FilterPreferencesActivity.class));
					}
				});

		findViewById(R.id.home_btn_settings).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						startActivity(new Intent(HomeActivity.this,
								FilterPreferencesActivity.class));
					}
				});

		findViewById(R.id.home_btn_search).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
					}
				});
		mBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("OpenBike", "intent received");
				ArrayList<Network> networks = intent
						.getParcelableArrayListExtra(SyncService.EXTRA_RESULT);
				displayNetworks(networks);
			}
		};
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
		final Context context = this;
		final SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		switch (id) {
		case ERROR_DATABASE:
			return new AlertDialog.Builder(this).setCancelable(false).setTitle(
					R.string.db_error).setMessage(R.string.db_error_summary)
					.setPositiveButton(R.string.Ok, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					}).create();
		case CHOOSE_NETWORK:
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
													FilterPreferencesActivity.NETWORK_PREFERENCE,
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
									} catch (SQLiteException e) {
										dismissDialog(CHOOSE_NETWORK);
										showDialog(ERROR_DATABASE);
									}
								}

								private void setCurrentNetwork(Editor editor,
										Network network) {
									editor
											.putString(
													FilterPreferencesActivity.UPDATE_SERVER_URL,
													network.getServerUrl());
									editor
											.putInt(
													FilterPreferencesActivity.NETWORK_LATITUDE,
													network.getLatitude());
									editor
											.putInt(
													FilterPreferencesActivity.NETWORK_LONGITUDE,
													network.getLongitude());
									editor
											.putString(
													FilterPreferencesActivity.NETWORK_NAME,
													network.getName());
									editor
											.putString(
													FilterPreferencesActivity.NETWORK_CITY,
													network.getCity());
									editor
											.putString(
													FilterPreferencesActivity.SPECIAL_STATION,
													network.getSpecialName());
									editor.commit();
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
	public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
		// TODO Auto-generated method stub
		return super.onCreateThumbnail(outBitmap, canvas);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getActivityHelper().setupHomeActivity();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO
		// getMenuInflater().inflate(R.menu.refresh_menu_items, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO
		/*
		 * if (item.getItemId() == R.id.menu_refresh) { triggerRefresh(); return
		 * true; }
		 */
		return super.onOptionsItemSelected(item);
	}

	private void displayNetworks(ArrayList<Network> networks) {
		mNetworkAdapter.insertAll(networks);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case CHOOSE_NETWORK:
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

}
