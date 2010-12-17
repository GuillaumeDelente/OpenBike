package fr.vcuboid.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import com.google.android.maps.Overlay;

import fr.vcuboid.database.VcuboidDBAdapter;
import fr.vcuboid.filter.VcubFilter;
import fr.vcuboid.map.MyLocationOverlay;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;

public class Utils {

	static public void sortStationsByDistance(List<? extends Overlay> list) {
		Collections.sort(list, new Comparator<Overlay>() {
			@Override
			public int compare(Overlay o1, Overlay o2) {
				if (o1 instanceof StationOverlay
						&& o2 instanceof StationOverlay) {
					if (((StationOverlay) o1).isCurrent)
						return -1;
					if (((StationOverlay) o2).isCurrent)
						return 1;
					Station s1 = ((StationOverlay) o1).getStation();
					Station s2 = ((StationOverlay) o2).getStation();
					if (s1.getDistance() < s2.getDistance()) {
						return -1;
					} else if (s1.getDistance() > s2.getDistance()) {
						return 1;
					} else {
						return 0;
					}
				} else if (o1 instanceof MyLocationOverlay) {
					return -1;
				} else if (o2 instanceof MyLocationOverlay) {
					return 1;
				}
				return 0;
			}
		});
	}

	static public void sortStationsByName(List<? extends Overlay> list) {
		Collections.sort(list, new Comparator<Overlay>() {
			@Override
			public int compare(Overlay o1, Overlay o2) {
				if (((StationOverlay) o1).isCurrent)
					return -1;
				if (((StationOverlay) o2).isCurrent)
					return 1;
				Station s1 = ((StationOverlay) o1).getStation();
				Station s2 = ((StationOverlay) o2).getStation();
				return s1.getName().compareToIgnoreCase(s2.getName());
			}
		});
	}

	static public String whereClauseFromFilter(VcubFilter filter) {
		Vector<String> selection = new Vector<String>();
		if (filter.isShowOnlyFavorites())
			selection.add("(" + VcuboidDBAdapter.KEY_FAVORITE + " = 1 )");
		if (filter.isShowOnlyWithBikes())
			selection.add("(" + VcuboidDBAdapter.KEY_BIKES + " > 1 )");
		else if (filter.isShowOnlyWithSlots())
			selection.add("(" + VcuboidDBAdapter.KEY_SLOTS + " > 1 )");
		int size = selection.size();
		if (size == 0)
			return null;
		String where = selection.firstElement();
		for (int i = 1; i < selection.size(); i++) {
			where += " AND " + selection.elementAt(i);
		}
		return where;
	}
}
