package fr.vcuboid.filter;

import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;

public class Filtering {

	public Filtering() {

	}

	static public void onlyFavorites() {
	}

	public static void filter(ArrayList<StationOverlay> mVisibleStations,
			VcubFilter mVcubFilter) {
		if (mVcubFilter.isShowOnlyFavorites()) {
			Log.e("Vcuboid", "Filtering List : Only Favorites");
			for (Iterator<StationOverlay> it = mVisibleStations.iterator(); it.hasNext();) {
				Station s = ((StationOverlay) it.next()).getStation();
				if (! s.isFavorite()) {
					it.remove();
				}
			}
			;
		}
	}
}
