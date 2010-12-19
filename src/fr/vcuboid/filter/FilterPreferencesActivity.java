package fr.vcuboid.filter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.util.Log;
import fr.vcuboid.R;
import fr.vcuboid.VcuboidManager;

abstract public class FilterPreferencesActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener {

	protected VcubFilter mActualFilter;
	protected VcubFilter mModifiedFilter;
	protected CheckBoxPreference mCheckBoxFavorite;
	protected CheckBoxPreference mCheckBoxBikes;
	protected CheckBoxPreference mCheckBoxSlots;
	protected CheckBoxPreference mCheckBoxLocation;
	protected ProgressDialog mPd = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCheckBoxFavorite = (CheckBoxPreference) getPreferenceScreen()
				.findPreference(getString(R.string.favorite_filter));
		mCheckBoxBikes = (CheckBoxPreference) getPreferenceScreen()
				.findPreference(getString(R.string.bikes_filter));
		mCheckBoxSlots = (CheckBoxPreference) getPreferenceScreen()
				.findPreference(getString(R.string.slots_filter));
		mCheckBoxLocation = (CheckBoxPreference) getPreferenceScreen()
				.findPreference(getString(R.string.use_location));
		mActualFilter = VcuboidManager.getVcuboidManagerInstance()
				.getVcubFilter();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		try {
			mModifiedFilter = mActualFilter.clone();
		} catch (CloneNotSupportedException e) {
			// Cannot happend
		}
	}

	@Override
	public void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		if (mModifiedFilter.equals(mActualFilter)) {
			setResult(RESULT_CANCELED);
			Log.e("Vcuboid", "Exiting Preferences : Filter not changed");
		} else {
			Log.e("Vcuboid", "Exiting Preferences : Filter Changed");
			setResult(RESULT_OK);
			mModifiedFilter.setNeedDbQuery(mActualFilter);
			VcuboidManager.getVcuboidManagerInstance().setVcubFilter(
					mModifiedFilter);
			VcuboidManager.getVcuboidManagerInstance().createVisibleStationList();
			Log.e("Vcuboid", "Only Favorites ? "
					+ mModifiedFilter.isShowOnlyFavorites());
		}
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(getString(R.string.favorite_filter))) {
			Log.e("Vcuboid", "Favorites changed");
			mModifiedFilter.setShowOnlyFavorites(mCheckBoxFavorite.isChecked());
		} else if (key.equals(getString(R.string.bikes_filter))) {
			Log.e("Vcuboid", "Bikes changed");
			mModifiedFilter.setShowOnlyWithBikes(mCheckBoxBikes.isChecked());
		} else if (key.equals(getString(R.string.slots_filter))) {
			Log.e("Vcuboid", "Slots changed");
			mModifiedFilter.setShowOnlyWithSlots(mCheckBoxSlots.isChecked());
		} else if (key.equals(getString(R.string.use_location))) {
			Log.e("Vcuboid", "Location changed");
			VcuboidManager vcubManager = VcuboidManager.getVcuboidManagerInstance();
			if (mCheckBoxLocation.isChecked()) {
				Log.e("Vcuboid", "useLocation");
				vcubManager.useLocation();
			} else {
				Log.e("Vcuboid", "dontUseLocation");
				vcubManager.dontUseLocation();
			}
		}
	}
}
