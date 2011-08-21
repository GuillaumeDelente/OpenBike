/*
 * Copyright (C) 2011 Guillaume Delente
 *
 * This file is part of OpenBike.
 *
 * OpenBike is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * OpenBike is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenBike.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.openbike.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import fr.openbike.R;

public class SettingsPreferencesActivity extends AbstractPreferencesActivity {

	protected CheckBoxPreference mDistanceFilterCb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.location_preferences);
		addPreferencesFromResource(R.xml.other_preferences);
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		SharedPreferences preferences = preferenceScreen.getSharedPreferences();
		mNetworkPreference = preferenceScreen
				.findPreference(AbstractPreferencesActivity.NETWORK_PREFERENCE);
		mReportBugPreference = preferenceScreen
				.findPreference(AbstractPreferencesActivity.REPORT_BUG_PREFERENCE);
		mNetworkPreference.setSummary(preferences.getString(NETWORK_NAME, "")
				+ " : " + preferences.getString(NETWORK_CITY, ""));
		preferences.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(AbstractPreferencesActivity.LOCATION_PREFERENCE)) {
			if (!sharedPreferences.getBoolean(
					AbstractPreferencesActivity.LOCATION_PREFERENCE, false)) {
				mDistanceFilterCb.setChecked(false);
			}
		}
		super.onSharedPreferenceChanged(sharedPreferences, key);
	}
	

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mNetworkPreference) {
			startActivity(new Intent(this, HomeActivity.class).setAction(
					HomeActivity.ACTION_CHOOSE_NETWORK).setFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP));
		} else if (preference == mReportBugPreference) {
			final Intent emailIntent = new Intent(
					android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
					new String[] { "contact@openbike.fr" });
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
					"Bug OpenBike");
			startActivity(Intent.createChooser(emailIntent, "Signaler un bug"));
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

}
