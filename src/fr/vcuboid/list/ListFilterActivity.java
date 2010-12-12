package fr.vcuboid.list;

import fr.vcuboid.R;
import fr.vcuboid.R.xml;
import fr.vcuboid.filter.FilterPreferencesActivity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;

public class ListFilterActivity extends FilterPreferencesActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.filter_preferences);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
}