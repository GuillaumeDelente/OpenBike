package fr.vcuboid.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.maps.Overlay;

import fr.vcuboid.map.MyCustomLocationOverlay;
import fr.vcuboid.map.StationOverlay;

public class Utils {

	static public void sortStations(List<? extends Overlay> list) {
		Collections.sort(list, new Comparator<Overlay>() {
			@Override
			public int compare(Overlay s1, Overlay s2) {
				if (s1 instanceof StationOverlay
						&& s2 instanceof StationOverlay) {
					if (((StationOverlay) s1).isCurrent)
						return -1;
					if (((StationOverlay) s2).isCurrent)
						return 1;
					if (((StationOverlay) s1).getDistance() < ((StationOverlay) s2)
							.getDistance()) {
						return -1;
					} else if (((StationOverlay) s1).getDistance() > ((StationOverlay) s2)
							.getDistance()) {
						return 1;
					} else {
						return 0;
					}
				} else if (s1 instanceof MyCustomLocationOverlay) {
					return -1;
				} else if (s2 instanceof MyCustomLocationOverlay) {
					return 1;
				}
				return 0;
			}
		});
	}
}
