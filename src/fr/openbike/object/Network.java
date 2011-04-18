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

public class Network {
	private int mId;
	private String mName;
	private String mCity;
	private String mServerUrl;
	private int mLongitude;
	private int mLatitude;
	
	public Network(int id, String name, String city, String serverUrl,
			double longitude, double latitude) {
		this(id, name, city, serverUrl, (int) (longitude*1E6), (int) (latitude*1E6));
	}
	
	public Network(int id, String name, String city, String serverUrl,
			int longitude, int latitude) {
		mId = id;
		mName = name;
		mServerUrl = serverUrl;
		mLongitude = longitude;
		mLatitude = latitude;
		mCity = city;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}
	
	public String getCity() {
		return mCity;
	}

	public void setCity(String city) {
		mCity = city;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public int getId() {
		return this.mId;
	}
	
	public void setLongitude(int longitude) {
		mLongitude = longitude;
	}

	public int getLongitude() {
		return mLongitude;
	}
	
	public void setLatitude(int latitude) {
		mLatitude = latitude;
	}

	public int getLatitude() {
		return mLatitude;
	}

	public void setServerUrl(String serverUrl) {
		mServerUrl = serverUrl;
	}

	public String getServerUrl() {
		return mServerUrl;
	}
}