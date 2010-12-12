package fr.vcuboid.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.maps.Overlay;

import fr.vcuboid.map.MyCustomLocationOverlay;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;

public class Utils {

	static public void sortStations(List<? extends Overlay> list) {
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
				} else if (o1 instanceof MyCustomLocationOverlay) {
					return -1;
				} else if (o2 instanceof MyCustomLocationOverlay) {
					return 1;
				}
				return 0;
			}
		});
	}
}
