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
package fr.openbike.filter;

import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;
import fr.openbike.map.StationOverlay;
import fr.openbike.object.Station;

public class Filtering {

	public Filtering() {

	}

	static public void onlyFavorites() {
	}

	public static void filter(ArrayList<StationOverlay> mVisibleStations,
			BikeFilter mVcubFilter) {
		Log.d("OpenBike", "Filtering stations");
		for (Iterator<StationOverlay> it = mVisibleStations.iterator(); it
				.hasNext();) {
			Station s = ((StationOverlay) it.next()).getStation();
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
