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
package fr.openbike.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import fr.openbike.R;

public class BikeFilter implements Cloneable {

	private boolean mShowOnlyFavorites = false;
	private boolean mShowOnlyWithSlots = false;
	private boolean mShowOnlyWithBikes = false;
	private boolean mNeedDbQuery = true;
	private int mDistanceFilter = 0;

	public BikeFilter(Context context) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		mShowOnlyFavorites = preferences.getBoolean(context
				.getString(R.string.favorite_filter), false);
		mShowOnlyWithBikes = preferences.getBoolean(context
				.getString(R.string.bikes_filter), false);
		mShowOnlyWithSlots = preferences.getBoolean(context
				.getString(R.string.slots_filter), false);
		mDistanceFilter = preferences.getBoolean(context
				.getString(R.string.enable_distance_filter), false) ? 
						preferences.getInt(
								context.getString(R.string.distance_filter), 1000)
				: 0;
	}

	public void setShowOnlyFavorites(boolean showOnlyFavorites) {
		mShowOnlyFavorites = showOnlyFavorites;
	}

	public boolean isShowOnlyFavorites() {
		return mShowOnlyFavorites;
	}

	public void setShowOnlyWithSlots(boolean showOnlyWithSlots) {
		mShowOnlyWithSlots = showOnlyWithSlots;
	}

	public boolean isShowOnlyWithSlots() {
		return mShowOnlyWithSlots;
	}

	public void setShowOnlyWithBikes(boolean showOnlyWithBikes) {
		mShowOnlyWithBikes = showOnlyWithBikes;
	}

	public boolean isShowOnlyWithBikes() {
		return mShowOnlyWithBikes;
	}

	public boolean isFilteringByDistance() {
		return mDistanceFilter != 0;
	}

	public int getDistanceFilter() {
		return mDistanceFilter;
	}

	void setDistanceFilter(int distance) {
		mDistanceFilter = distance;
	}

	public void setNeedDbQuery(BikeFilter actualFilter) {
		if (actualFilter.isShowOnlyFavorites() && !mShowOnlyFavorites) {
			mNeedDbQuery = true;
			return;
		}
		if (actualFilter.isShowOnlyWithBikes() && !mShowOnlyWithBikes
				|| actualFilter.isShowOnlyWithSlots() && !mShowOnlyWithSlots) {
			mNeedDbQuery = true;
			return;
		}

		if (mDistanceFilter == 0 && actualFilter.getDistanceFilter() != 0 // Filter disabled
				|| (actualFilter.getDistanceFilter() != 0 &&			  // Distance increased
						actualFilter.getDistanceFilter() < mDistanceFilter)) {
			mNeedDbQuery = true;
			return;
		}
		mNeedDbQuery = false;
	}


	private void setNeedDbQuery() {
		if (isFilteringByDistance())
			mNeedDbQuery = true;
		else
			mNeedDbQuery = false;
	}
	
	/*
	public void setNeedDbQuery(boolean need) {
		mNeedDbQuery = need;
	}
	*/

	public boolean isNeedDbQuery() {
		boolean query = mNeedDbQuery;
		setNeedDbQuery();
		return query;
	}

	@Override
	public BikeFilter clone() throws CloneNotSupportedException {
		return (BikeFilter) super.clone();
	}

	@Override
	public boolean equals(Object aThat) {
		// check for self-comparison
		if (this == aThat)
			return true;

		if (!(aThat instanceof BikeFilter))
			return false;

		BikeFilter that = (BikeFilter) aThat;

		// now a proper field-by-field evaluation can be made
		return mShowOnlyFavorites == that.mShowOnlyFavorites
				&& mShowOnlyWithBikes == that.mShowOnlyWithBikes
				&& mShowOnlyWithSlots == that.mShowOnlyWithSlots
				&& mDistanceFilter == that.mDistanceFilter;
	}
}
