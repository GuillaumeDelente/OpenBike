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
package fr.openbike.object;

import android.util.Log;

import com.google.android.maps.GeoPoint;

/**
 * Model class which will store the Station Items
 * 
 * @author Guillaume Delente
 * 
 */

public class MinimalStation {
	private int mId;
	private String mNetwork;
	private String mName;
	private GeoPoint mGeoPoint;
	private int mBikes;
	private int mSlots;
	private int mDistance;
	private boolean mIsOpen;
	private boolean mIsFavorite;
	
	public MinimalStation(int id, String network, String name,
			int longitude, int latitude, int availablesBikes,
			int freeLocations, boolean isOpen, boolean isFavorite, int distance) {
		mId = id;
		mNetwork = network;
		mName = name;
		setGeoPoint(new GeoPoint(latitude, longitude));
		mBikes = availablesBikes;
		mSlots = freeLocations;
		mIsOpen = isOpen;
		mIsFavorite = isFavorite;
		mDistance = distance;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}
	
	public int getBikes() {
		return mBikes;
	}

	public void setBikes(int bikes) {
		mBikes = bikes;
	}

	public int getSlots() {
		return mSlots;
	}

	public void setSlots(int slots) {
		mSlots = slots;
	}

	public boolean isOpen() {
		Log.d("OpenBike", "IsOpen getter : " + mIsOpen);
		return mIsOpen;
	}

	public void setOpen(boolean isOpen) {
		if ((mIsOpen = isOpen) == false) {
			mSlots = 0;
			mBikes = 0;
		}
	}

	public String getNetwork() {
		return mNetwork;
	}

	public void setNetwork(String network) {
		this.mNetwork = network;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public int getId() {
		return this.mId;
	}

	public boolean isFavorite() {
		return mIsFavorite;
	}

	public void setFavorite(boolean isFavorite) {
		mIsFavorite = isFavorite;
	}

	public void setGeoPoint(GeoPoint geoPoint) {
		mGeoPoint = geoPoint;
	}

	public GeoPoint getGeoPoint() {
		return mGeoPoint;
	}

	public void setDistance(int distance) {
		mDistance = distance;
	}

	public int getDistance() {
		return mDistance;
	}
}