package fr.vcuboid.map;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import fr.vcuboid.R;

public class MapPreferencesActivity extends PreferenceActivity {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.map_preferences);
        addPreferencesFromResource(R.xml.filter_preferences);
    }
}