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
package fr.vcuboid.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.RelativeLayout;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import fr.vcuboid.IVcuboidActivity;
import fr.vcuboid.MyLocationProvider;
import fr.vcuboid.R;
import fr.vcuboid.RestClient;
import fr.vcuboid.VcuboidManager;

public class VcuboidMapActivity extends MapActivity implements IVcuboidActivity {

	private MapController mMapController;
	private MyLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private boolean mIsFirstFix = true;
	private SharedPreferences mMapPreferences = null;
	private MapView mMapView = null;
	private VcuboidManager mVcuboidManager = null;
	private int mSelected = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("Vcuboid", "Map on create");
		setContentView(R.layout.map_layout);
		mMapView = (MapView) findViewById(R.id.map_view);
		mMapController = mMapView.getController();
		mMapView.setSatellite(false);
		mMapView.setStreetView(false);
		mMapView.setBuiltInZoomControls(true);
		mVcuboidManager = VcuboidManager.getVcuboidManagerInstance(this);
		mVcuboidManager.setCurrentActivity(this);
		mMapView.displayZoomControls(true);
		mMapView.invalidate();
		mMapPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mMapOverlays = mMapView.getOverlays();
		Bitmap marker = BitmapFactory.decodeResource(getResources(),
				R.drawable.v3);
		StationOverlay.setMarker(marker);
		StationOverlay.setMapView(mMapView);
	}

	public void hideOverlayBalloon() {
		int position = mMapOverlays.size()
				- (mMyLocationOverlay == null ? 1 : 2);
		if (position >= 0) {
			Overlay overlay = mMapOverlays.get(position);
			if (overlay instanceof StationOverlay) {
				((StationOverlay) overlay).hideBalloon();
			} else {
				Log.e("Balloon",
						"hideOtherBalloons, before last not a StationOverlay");
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_map_preferences:
			startActivity(new Intent(this, MapFilterActivity.class));
			return true;
		case R.id.menu_update_all:
			mVcuboidManager.executeUpdateAllStationsTask();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		mVcuboidManager.startLocation();
		mMapOverlays.clear();
		mMapOverlays.addAll(mVcuboidManager.getVisibleStations());
		Collections.reverse(mMapOverlays);
		if (mMapPreferences.getBoolean(getString(R.string.use_location), true)) {
			if (mMyLocationOverlay == null) {
				// FIXME centered map on location
				mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
			}
			mMapOverlays.add(mMyLocationOverlay);
			if (!mMyLocationOverlay.isMyLocationDrawn()) {
				mMyLocationOverlay.setCurrentLocation(mVcuboidManager
						.getCurrentLocation());
				mMapView.invalidate();
			}
		} else {
			mMyLocationOverlay = null;
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		mVcuboidManager.stopLocation();
		hideOverlayBalloon();
		StationOverlay.balloonView = null;
		super.onPause();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setCenteredMap(boolean isCentered) {
		// mMyLocationOverlay
		// .setmMapController(isCentered ? mMapController : null);
	}

	@Override
	public void finishGetAllStationsOnProgress() {
		// TODO Auto-generated method stub

	}

	@Override
	public void showGetAllStationsOnProgress() {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateGetAllStationsOnProgress(int progress) {
		// TODO Auto-generated method stub

	}

	public void setFavorite(int id, boolean isChecked) {
		mSelected = id;
		if (isChecked) {
			mVcuboidManager.setFavorite(id, true);
			int size = mMapOverlays.size();
			int baloonPosition = size
					- (mMapOverlays.get(size - 1) instanceof MyLocationOverlay ? 2
							: 1);
			Overlay overlay = mMapOverlays.get(baloonPosition);
			if (overlay instanceof StationOverlay) {
				((StationOverlay) overlay).getStation().setFavorite(true);
			} else {
				Log.d("Vcuboid", "before last not a StationOverlay");
			}
			// onListUpdated();
		} else {
			showDialog(VcuboidManager.REMOVE_FROM_FAVORITE);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		mMyLocationOverlay.setCurrentLocation(location);
		onListUpdated();
		if (mMapPreferences.getBoolean(getString(R.string.center_on_location),
				false)
				|| mIsFirstFix) {
			mMapController.animateTo(new GeoPoint(
					(int) (location.getLatitude() * 1E6), (int) (location
							.getLongitude() * 1E6)));
			mIsFirstFix = false;
		}
	}

	@Override
	public void onListUpdated() {
		mMapOverlays.clear();
		ArrayList<StationOverlay> stations = mVcuboidManager
				.getVisibleStations();
		mMapOverlays.addAll(stations);
		Collections.reverse(mMapOverlays);
		if (mMyLocationOverlay != null)
			mMapOverlays.add(mMyLocationOverlay);
		mMapView.invalidate();
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
	public Dialog onCreateDialog(int id) {
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
					getString(R.string.gps_disabled)).setMessage(
					(getString(R.string.show_location_parameters)))
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
									if (mMapPreferences
											.getBoolean(
													getString(R.string.favorite_filter),
													false)) {
										((StationOverlay) mMapOverlays
												.get(mMapOverlays.size()
														- (mMyLocationOverlay == null ? 1
																: 2)))
												.hideBalloon();
										mMapOverlays
												.remove(mMapOverlays.size()
														- (mMyLocationOverlay == null ? 1
																: 2));
										mMapView.invalidate();
									} else {
										((StationOverlay) mMapOverlays
												.get(mMapOverlays.size()
														- (mMyLocationOverlay == null ? 1
																: 2)))
												.getStation()
												.setFavorite(false);
									}
									dialog.dismiss();
								}
							}).setOnCancelListener(
							new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface arg0) {
									((StationOverlay) mMapOverlays
											.get(mMapOverlays.size()
													- (mMyLocationOverlay == null ? 1
															: 2)))
											.refreshBalloon();

								}
							}).setNegativeButton(getString(R.string.no),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).create();
		}
		return super.onCreateDialog(id);
	}
}
