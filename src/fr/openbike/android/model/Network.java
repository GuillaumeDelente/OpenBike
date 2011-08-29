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
package fr.openbike.android.model;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Model class which will store the Station Items
 * 
 * @author Guillaume Delente
 * 
 */

public class Network implements Parcelable {
	private int mId;
	private String mName;
	private String mCity;
	private String mServerUrl;
	private String mSpecialName;
	private int mLongitude;
	private int mLatitude;

	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String CITY = "city";
	public static final String SERVER = "server";
	public static final String SPECIAL_NAME = "specialName";
	public static final String LONGITUDE = "longitude";
	public static final String LATITUDE = "latitude";
	
	public Network(int id, String name, String city, String serverUrl, String specialName,
			double longitude, double latitude) {
		this(id, name, city, serverUrl, specialName, (int) (longitude*1E6), (int) (latitude*1E6));
	}
	
	public Network(int id, String name, String city, String serverUrl, String specialName,
			int longitude, int latitude) {
		mId = id;
		mName = name;
		mServerUrl = serverUrl;
		mSpecialName = specialName;
		mLongitude = longitude;
		mLatitude = latitude;
		mCity = city;
	}
	
	public Network(Parcel parcel) {
		mId = parcel.readInt();
		mName = parcel.readString();
		mServerUrl = parcel.readString();
		mSpecialName = parcel.readString();
		mLongitude = parcel.readInt();
		mLatitude = parcel.readInt();
		mCity = parcel.readString();
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
	
	public String getSpecialName() {
		return mSpecialName;
	}

	public void setSpecialName(String specialName) {
		mSpecialName = specialName;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mId);
		dest.writeString(mName);
		dest.writeString(mServerUrl);
		dest.writeString(mSpecialName);
		dest.writeInt(mLongitude);
		dest.writeInt(mLatitude);
		dest.writeString(mCity);
	}
	
    public static final Parcelable.Creator<Network> CREATOR =
    	new Parcelable.Creator<Network>() {
            public Network createFromParcel(Parcel in) {
                return new Network(in);
            }
 
            public Network[] newArray(int size) {
                return new Network[size];
            }
        };
}