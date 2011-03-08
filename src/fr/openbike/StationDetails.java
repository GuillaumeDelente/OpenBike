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
package fr.openbike;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.openbike.map.OpenBikeMapActivity;
import fr.openbike.object.Station;
import fr.openbike.utils.Utils;

/**
 * @author guitou
 * 
 */
public class StationDetails extends Activity {

	public static final int COMPUTE_DISTANCE = -2;

	private Station mStation = null;
	private OpenBikeManager mVcuboidManager = null;
	private TextView mName = null;
	private TextView mDistance = null;
	private TextView mBikes = null;
	private TextView mSlots = null;
	private TextView mAddress = null;
	private TextView mCreditCard = null;
	private TextView mSpecial = null;
	private CheckBox mFavorite = null;
	private ImageButton mNavigate = null;
	private ImageButton mGoogleMaps = null;
	private ImageButton mShowMap = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.station_details_layout);
		mVcuboidManager = OpenBikeManager.getVcuboidManagerInstance();
		mName = (TextView) findViewById(R.id.name);
		mFavorite = (CheckBox) findViewById(R.id.favorite);
		mDistance = (TextView) findViewById(R.id.distance);
		mCreditCard = (TextView) findViewById(R.id.cc);
		mSpecial = (TextView) findViewById(R.id.special);
		mAddress = (TextView) findViewById(R.id.address);
		mNavigate = (ImageButton) findViewById(R.id.navigate);
		mGoogleMaps = (ImageButton) findViewById(R.id.show_google_maps);
		mShowMap = (ImageButton) findViewById(R.id.show_map);
		mGoogleMaps.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("geo:0,0?q="
								+ mStation.getGeoPoint().getLatitudeE6() * 1E-6
								+ "," + mStation.getGeoPoint().getLongitudeE6()
								* 1E-6 + " (" + mStation.getName() + ")")));
			}
		});

		mNavigate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("google.navigation:q="
								+ mStation.getGeoPoint().getLatitudeE6() * 1E-6
								+ "," + mStation.getGeoPoint().getLongitudeE6()
								* 1E-6)));
			}
		});
		final Intent intent = new Intent(this, OpenBikeMapActivity.class);
		mShowMap.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				intent.putExtra("id", mStation.getId());
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Log.i("OpenBike", "onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.station_details_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_show_on_map:
			intent = new Intent(this, OpenBikeMapActivity.class);
			intent.putExtra("id", mStation.getId());
			startActivity(intent);
			return true;
		case R.id.menu_show_on_google_maps:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="
					+ mStation.getGeoPoint().getLatitudeE6() * 1E-6 + ","
					+ mStation.getGeoPoint().getLongitudeE6() * 1E-6 + " ("
					+ mStation.getName() + ")")));
			return true;
		case R.id.menu_navigate_to:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse("google.navigation:q="
							+ mStation.getGeoPoint().getLatitudeE6() * 1E-6
							+ "," + mStation.getGeoPoint().getLongitudeE6()
							* 1E-6)));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mStation = mVcuboidManager.getStation(getIntent().getExtras().getInt(
				"id"));
		mName.setText(mStation.getId() + " - " + mStation.getName());
		mAddress.setText(getString(R.string.address) + " : "
				+ mStation.getAddress());
		mCreditCard
				.setText(getString(R.string.cc)
						+ " : "
						+ getString(mStation.hasPayment() ? R.string.yes
								: R.string.no));
		mSpecial
		.setText(getString(R.string.special)
				+ " : "
				+ getString(mStation.isSpecial() ? R.string.yes
						: R.string.no));
		if (mStation.getDistance() != MyLocationProvider.DISTANCE_UNAVAILABLE) {
			mDistance.setText(getString(R.string.upper_at) + " "
					+ Utils.formatDistance(mStation.getDistance()));
			mDistance.setVisibility(View.VISIBLE);
		}
		if (!mStation.isOpen()) {
			findViewById(R.id.open_layout).setVisibility(View.GONE);
			findViewById(R.id.closed_layout).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.open_layout).setVisibility(View.VISIBLE);
			findViewById(R.id.closed_layout).setVisibility(View.GONE);
			mBikes = (TextView) findViewById(R.id.bikes);
			mSlots = (TextView) findViewById(R.id.slots);
			if (mStation.getBikes() == 0) {
				((ImageView) findViewById(R.id.bike_sign))
						.setImageDrawable(getResources().getDrawable(
								R.drawable.no_bike_sign));
			}
			if (mStation.getSlots() == 0) {
				((ImageView) findViewById(R.id.parking_sign))
						.setImageDrawable(getResources().getDrawable(
								R.drawable.no_parking_sign));
			}
			mBikes.setText(mStation.getBikes()
					+ " "
					+ getString(mStation.getBikes() == 1 ? R.string.bike
							: R.string.bikes));
			mSlots.setText(mStation.getSlots()
					+ " "
					+ getString(mStation.getSlots() == 1 ? R.string.slot
							: R.string.slots));
			mFavorite.setChecked(mStation.isFavorite());
		}

		mFavorite.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				mVcuboidManager.setFavorite(mStation.getId(), isChecked);
			}
		});
	}

	@Override
	protected void onPause() {
		mFavorite.setOnCheckedChangeListener(null);
		super.onPause();
	}
}
