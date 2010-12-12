package fr.vcuboid.map;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import fr.vcuboid.R;
import fr.vcuboid.filter.FilterPreferencesActivity;

public class MapFilterActivity extends FilterPreferencesActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.map_preferences);
	    addPreferencesFromResource(R.xml.filter_preferences);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	
}
