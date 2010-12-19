package fr.vcuboid.filter;

import java.util.ArrayList;
import java.util.Iterator;

import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;

public class Filtering {

	public Filtering() {

	}

	static public void onlyFavorites() {
	}

	public static void filter(ArrayList<StationOverlay> mVisibleStations,
			VcubFilter mVcubFilter) {
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
			;
		}
	}
}
