package fr.vcuboid.list;

import android.os.Bundle;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;
import fr.vcuboid.filter.FilterPreferencesActivity;

public class ListFilterActivity extends FilterPreferencesActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.filter_preferences);
	    addPreferencesFromResource(R.xml.location_preferences);
	}
}