package fr.vcuboid.map;

import android.os.Bundle;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;
import fr.vcuboid.filter.FilterPreferencesActivity;

public class MapFilterActivity extends FilterPreferencesActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.map_preferences);
	    addPreferencesFromResource(R.xml.location_preferences);
	    addPreferencesFromResource(R.xml.filter_preferences);
	}
}
