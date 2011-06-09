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

import java.util.ArrayList;
import java.util.Iterator;

import fr.openbike.object.MinimalStation;

public class Filtering {

	public Filtering() {

	}

	static public void onlyFavorites() {
	}

	public static void filter(ArrayList<MinimalStation> mVisibleStations,
			BikeFilter mVcubFilter) {
		for (Iterator<MinimalStation> it = mVisibleStations.iterator(); it
				.hasNext();) {
			MinimalStation s = it.next();
			if (mVcubFilter.isShowOnlyFavorites() && !s.isFavorite()) {
				it.remove();
				continue;
			}
			if (mVcubFilter.isShowOnlyWithBikes() && s.getBikes() < 1) {
				it.remove();
				continue;
			}
			if (mVcubFilter.isShowOnlyWithSlots() && s.getSlots() < 1) {
				it.remove();
				continue;
			}
			if (mVcubFilter.isFilteringByDistance() && 
					s.getDistance() > mVcubFilter.getDistanceFilter()) {
				it.remove();
				continue;
			}
			;
		}
	}
}
