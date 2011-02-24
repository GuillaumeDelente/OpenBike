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
package fr.vcuboid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import fr.vcuboid.list.VcuboidArrayAdaptor;
import fr.vcuboid.object.Station;
import fr.vcuboid.utils.Utils;

/**
 * @author guitou
 * 
 */
public class StationDetails extends Activity {
	
	public static final int COMPUTE_DISTANCE = -2;

	private Station mStation = null;
	private VcuboidManager mVcuboidManager = null;
	private TextView mName = null;
	private TextView mDistance = null;
	private TextView mBikes = null;
	private TextView mSlots = null;
	private TextView mAddress = null;
	private TextView mCreditCard = null;
	private CheckBox mFavorite = null;
	private ImageButton mNavigate = null;
	private ImageButton mGoogleMaps = null;
	private ImageButton mShowMap = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.station_details_layout);
		mVcuboidManager = VcuboidManager.getVcuboidManagerInstance();
		mStation = mVcuboidManager.getStation(getIntent().getExtras().getInt(
				"id"));
		mName = (TextView) findViewById(R.id.name);
		mFavorite = (CheckBox) findViewById(R.id.favorite);
		mDistance = (TextView) findViewById(R.id.distance);
		mCreditCard = (TextView) findViewById(R.id.cc);
		mAddress = (TextView) findViewById(R.id.address);
		mNavigate = (ImageButton) findViewById(R.id.navigate);
		mGoogleMaps = (ImageButton) findViewById(R.id.google_maps);
		mShowMap = (ImageButton) findViewById(R.id.show_map);
		if (!mStation.isOpen()) {
			findViewById(R.id.open_layout).setVisibility(View.GONE);
			findViewById(R.id.closed_layout).setVisibility(View.VISIBLE);
		} else {
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
		mName.setText(mStation.getId() + " - " + mStation.getName());
		mFavorite.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton button,
					boolean isChecked) {
				mVcuboidManager.setFavorite(mStation.getId(), isChecked);
			}
		});
		if (mStation.getDistance() != MyLocationProvider.DISTANCE_UNAVAILABLE) {
			mDistance.setText(getString(R.string.At) + " "
					+ Utils.formatDistance(mStation.getDistance()));
			mDistance.setVisibility(View.VISIBLE);
		}
		mAddress.setText(getString(R.string.address) + " : "
				+ mStation.getAddress());
		mCreditCard
				.setText(getString(R.string.cc)
						+ " : "
						+ getString(mStation.hasPayment() ? R.string.yes
								: R.string.no));
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
		
		mShowMap.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.putExtra("id", mStation.getId());
				startActivity(intent);
			}
		});
		
		
		mGoogleMaps.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("google.navigation:q="
								+ mStation.getGeoPoint().getLatitudeE6() * 1E-6
								+ "," + mStation.getGeoPoint().getLongitudeE6()
								* 1E-6)));
			}
		});
	}
}
