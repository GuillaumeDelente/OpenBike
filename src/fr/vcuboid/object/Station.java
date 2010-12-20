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
package fr.vcuboid.object;

import com.google.android.maps.GeoPoint;

/**
 * Model class which will store the Station Items
 * 
 * @author Guillaume Delente
 * 
 */

public class Station {
	private int mId;
	private String mNetwork;
	private String mName;
	private String mAddress;
	private GeoPoint mGeoPoint;
	private int mBikes;
	private int mSlots;
	private int mDistance;
	private boolean mIsOpen;
	private boolean mIsFavorite;

	public Station(int id, String network, String name, String address,
			double longitude, double latitude, int availablesBikes,
			int freeLocations, boolean isOpen, boolean isFavorite) {
		this(id, network, name, address, (int) (longitude * 1E6),
				(int) (latitude * 1E6), availablesBikes, freeLocations,
				isOpen, isFavorite);
	}
	
	public Station(int id, String network, String name, String address,
			int longitude, int latitude, int availablesBikes,
			int freeLocations, boolean isOpen, boolean isFavorite) {
		this(id, network, name, address, longitude,
				latitude, availablesBikes, freeLocations,
				isOpen, isFavorite, -1);
	}

	public Station(int id, String network, String name, String address,
			int longitude, int latitude, int availablesBikes,
			int freeLocations, boolean isOpen, boolean isFavorite, int distance) {
		mId = id;
		mNetwork = network;
		mAddress = address;
		mName = name;
		setGeoPoint(new GeoPoint(latitude, longitude));
		mBikes = availablesBikes;
		mSlots = freeLocations;
		mIsOpen = isOpen;
		mIsFavorite = isFavorite;
		mDistance = distance;
	}

	public Station() {
		// TODO Auto-generated constructor stub
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public void setAddress(String address) {
		mAddress = address;
	}

	public String getAddress() {
		return mAddress;
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