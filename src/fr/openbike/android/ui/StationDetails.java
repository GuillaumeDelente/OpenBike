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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.openbike.android.IActivityHelper;
import fr.openbike.android.R;
import fr.openbike.android.database.OpenBikeDBAdapter;
import fr.openbike.android.database.StationsProvider;
import fr.openbike.android.service.ILocationServiceListener;
import fr.openbike.android.service.LocationService;
import fr.openbike.android.service.LocationService.LocationBinder;
import fr.openbike.android.utils.ActivityHelper;
import fr.openbike.android.utils.Utils;

/**
 * @author guitou
 * 
 */
public class StationDetails extends Activity implements
		ILocationServiceListener, IActivityHelper {

	public static final int COMPUTE_DISTANCE = -2;
	public static final int MAPS_NOT_AVAILABLE = 0;
	public static final int NAVIGATION_NOT_AVAILABLE = 1;

	private Cursor mStation = null;
	private TextView mName = null;
	private TextView mDistance = null;
	private TextView mBikes = null;
	private TextView mSlots = null;
	private TextView mAddress = null;
	private TextView mCreditCard = null;
	private TextView mSpecial = null;
	private CheckBox mFavorite = null;
	private Button mNavigate = null;
	private Button mGoogleMaps = null;
	private Button mShowMap = null;
	private SharedPreferences mSharedPreferences = null;
	private Uri mUri = null;
	private int mLatitude;
	private int mLongitude;
	private ActivityHelper mActivityHelper = null;
	boolean mBound = false;

	private LocationService mService = null;

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocationBinder binder = (LocationBinder) service;
			mService = binder.getService();
			mBound = true;
			mService.addListener(StationDetails.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.station_details_layout);
		mActivityHelper = new ActivityHelper(this);
		mActivityHelper.setupActionBar(getString(R.string.station_details));
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		mName = (TextView) findViewById(R.id.name);
		mFavorite = (CheckBox) findViewById(R.id.favorite);
		mDistance = (TextView) findViewById(R.id.distance);
		mCreditCard = (TextView) findViewById(R.id.cc);
		mSpecial = (TextView) findViewById(R.id.special);
		mAddress = (TextView) findViewById(R.id.address);
		mNavigate = (Button) findViewById(R.id.navigate);
		mGoogleMaps = (Button) findViewById(R.id.show_google_maps);
		mShowMap = (Button) findViewById(R.id.show_map);
		mGoogleMaps.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startMaps();
			}
		});
		mNavigate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				startNavigation();
			}
		});
		mShowMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				showOnMap(mStation.getString(mStation
						.getColumnIndex(BaseColumns._ID)));
			}
		});
	}

	private void showOnMap(Uri uri) {
		Intent intent = new Intent(this, OpenBikeMapActivity.class)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setAction(OpenBikeMapActivity.ACTION_DETAIL);
		intent.setData(uri);
		startActivity(intent);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActivityHelper.onPostCreate(savedInstanceState);
	}

	private void showOnMap(String id) {
		showOnMap(Uri.withAppendedPath(StationsProvider.CONTENT_URI, id));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mActivityHelper.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.station_details_menu, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_show_on_map:
			showOnMap(mStation.getString(mStation
					.getColumnIndex(BaseColumns._ID)));
			return true;
		case R.id.menu_show_on_google_maps:
			startMaps();
			return true;
		case R.id.menu_navigate_to:
			startNavigation();
			return true;
		default:
			return mActivityHelper.onOptionsItemSelected(item)
					|| super.onOptionsItemSelected(item);
		}
	}

	void startMaps() {
		Intent intent = Utils.getMapsIntent(mStation.getInt(mStation
				.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE)), mStation
				.getInt(mStation
						.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE)),
				mStation.getString(mStation
						.getColumnIndex(OpenBikeDBAdapter.KEY_NAME)));
		if (Utils.isIntentAvailable(intent, this))
			startActivity(intent);
		else
			showDialog(MAPS_NOT_AVAILABLE);
	}

	void startNavigation() {
		Intent intent = Utils.getNavigationIntent(mStation.getInt(mStation
				.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE)), mStation
				.getInt(mStation
						.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE)));
		if (Utils.isIntentAvailable(intent, this))
			startActivity(intent);
		else
			showDialog(NAVIGATION_NOT_AVAILABLE);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case MAPS_NOT_AVAILABLE:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.not_available)).setMessage(
					(getString(R.string.maps_not_available_summary)))
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		case NAVIGATION_NOT_AVAILABLE:
			return new AlertDialog.Builder(this).setCancelable(true).setTitle(
					getString(R.string.not_available)).setMessage(
					(getString(R.string.navigation_not_available_summary)))
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		default:
			Dialog dialog = getActivityHelper().onCreateDialog(id);
			if (dialog != null)
				return dialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		getActivityHelper().onPrepareDialog(id, dialog);
		super.onPrepareDialog(id, dialog);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onPause() {
		mFavorite.setOnCheckedChangeListener(null);
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (mBound) {
			mService.removeListener(StationDetails.this);
			unbindService(mConnection);
			mBound = false;
		}
		super.onStop();
	}

	@Override
	protected void onStart() {
		if (mSharedPreferences.getBoolean(
				AbstractPreferencesActivity.LOCATION_PREFERENCE, true)) {
			Intent intent = new Intent(this, LocationService.class);
			bindService(intent, mConnection, 0);
		}
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		handleIntent();
		mStation = managedQuery(mUri, null, null, null, null);
		if (mStation == null) {
			finish();
			return;
		} else {
			mStation.moveToFirst();
		}
		mLatitude = mStation.getInt(mStation
				.getColumnIndex(OpenBikeDBAdapter.KEY_LATITUDE));
		mLongitude = mStation.getInt(mStation
				.getColumnIndex(OpenBikeDBAdapter.KEY_LONGITUDE));
		mName.setText(mStation.getInt(mStation.getColumnIndex(BaseColumns._ID))
				+ " - "
				+ mStation.getString(mStation
						.getColumnIndex(OpenBikeDBAdapter.KEY_NAME)));
		mAddress.setText(getString(R.string.address)
				+ " : "
				+ mStation.getString(mStation
						.getColumnIndex(OpenBikeDBAdapter.KEY_ADDRESS)));
		mCreditCard
				.setText(getString(R.string.cc)
						+ " : "
						+ getString(mStation.getInt(mStation
								.getColumnIndex(OpenBikeDBAdapter.KEY_PAYMENT)) == 1 ? R.string.yes
								: R.string.no));
		mSpecial
				.setText(PreferenceManager.getDefaultSharedPreferences(this)
						.getString(AbstractPreferencesActivity.SPECIAL_STATION,
								"")
						+ " : "
						+ getString(mStation.getInt(mStation
								.getColumnIndex(OpenBikeDBAdapter.KEY_SPECIAL)) == 1 ? R.string.yes
								: R.string.no));
		if (mStation
				.getInt(mStation.getColumnIndex(OpenBikeDBAdapter.KEY_OPEN)) == 0) {
			findViewById(R.id.open_layout).setVisibility(View.GONE);
			findViewById(R.id.closed_layout).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.open_layout).setVisibility(View.VISIBLE);
			findViewById(R.id.closed_layout).setVisibility(View.GONE);
			mBikes = (TextView) findViewById(R.id.bikes);
			mSlots = (TextView) findViewById(R.id.slots);
			int bikes = mStation.getInt(mStation
					.getColumnIndex(OpenBikeDBAdapter.KEY_BIKES));
			int slots = mStation.getInt(mStation
					.getColumnIndex(OpenBikeDBAdapter.KEY_SLOTS));
			if (bikes == 0) {
				((ImageView) findViewById(R.id.bike_sign))
						.setImageDrawable(getResources().getDrawable(
								R.drawable.no_bike_sign));
			}
			if (slots == 0) {
				((ImageView) findViewById(R.id.parking_sign))
						.setImageDrawable(getResources().getDrawable(
								R.drawable.no_parking_sign));
			}
			mBikes.setText(getResources().getQuantityString(R.plurals.bike,
					bikes, bikes));
			mSlots.setText(getResources().getQuantityString(R.plurals.slot,
					slots, slots));
			mFavorite.setChecked(mStation.getInt(mStation
					.getColumnIndex(OpenBikeDBAdapter.KEY_FAVORITE)) == 1);
		}
		if (mStation != null)
			mStation.moveToFirst();
		mFavorite.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				OpenBikeDBAdapter.getInstance(StationDetails.this)
						.updateFavorite(
								mStation.getInt(mStation
										.getColumnIndex(BaseColumns._ID)),
								isChecked);
			}
		});
	}

	private void handleIntent() {
		mUri = getIntent().getData();
	}

	@Override
	public void onLocationChanged(Location location, boolean alert) {
		int distance = Utils.computeDistance(mLatitude, mLongitude, location);
		if (distance != LocationService.DISTANCE_UNAVAILABLE) {
			mDistance.setText(getString(R.string.upper_at) + " "
					+ Utils.formatDistance(distance));
			mDistance.setVisibility(View.VISIBLE);
		} else {
			mDistance.setVisibility(View.GONE);
		}
	}

	@Override
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mActivityHelper.onPrepareOptionsMenu(menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public void onLocationProvidersChanged(int id) {
		showDialog(id);
	}
}
