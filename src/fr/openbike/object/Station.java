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



/**
 * Model class which will store the Station Items
 * 
 * @author Guillaume Delente
 * 
 */

public class Station extends MinimalStation{
	private String mAddress;
	private boolean mHasPayment;
	private boolean mIsSpecial;
	

	public static final String ADDRESS = "address";
	public static final String PAYMENT = "payment";
	public static final String SPECIAL = "special";

	public Station(int id, String name, String address,
			double longitude, double latitude, int availablesBikes,
			int freeLocations, boolean isOpen, boolean isFavorite,
			boolean hasPayment, boolean isSpecial) {
		this(id, name, address, (int) (longitude * 1E6),
				(int) (latitude * 1E6), availablesBikes, freeLocations,
				isOpen, isFavorite, hasPayment, isSpecial);
	}
	
	public Station(int id, String name, String address,
			int longitude, int latitude, int availablesBikes,
			int freeLocations, boolean isOpen, boolean isFavorite, 
			boolean hasPayment, boolean isSpecial) {
		this(id, name, address, longitude,
				latitude, availablesBikes, freeLocations,
				isOpen, isFavorite, hasPayment, isSpecial, -1);
	}

	public Station(int id, String name, String address,
			int longitude, int latitude, int availablesBikes,
			int freeLocations, boolean isOpen, boolean isFavorite, 
			boolean hasPayment, boolean isSpecial, int distance) {
		
		super(id, name, longitude, latitude, availablesBikes,
				freeLocations, isOpen, isFavorite, distance);
		mAddress = address;
		mHasPayment = hasPayment;
		mIsSpecial = isSpecial;
	}

	public void setAddress(String address) {
		mAddress = address;
	}

	public String getAddress() {
		return mAddress;
	}

	public boolean hasPayment() {
		return mHasPayment;
	}
	
	public void setPayment(boolean hasPayment) {
		mHasPayment = hasPayment;
	}	
	
	public boolean isSpecial() {
		return mIsSpecial;
	}
	
	public void setSpecial(boolean isSpecial) {
		mIsSpecial = isSpecial;
	}
}