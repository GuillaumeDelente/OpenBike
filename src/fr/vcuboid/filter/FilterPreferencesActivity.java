package fr.vcuboid.filter;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.KeyEvent;
import fr.vcuboid.VcuboidManager;

abstract public class FilterPreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	protected VcubFilter mActualFilter;
    protected VcubFilter mModifiedFilter;
    protected CheckBoxPreference mCheckBoxPreference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
	    mCheckBoxPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("favorite_filter");
        mActualFilter = VcuboidManager.getVcuboidManagerInstance().getVcubFilter();
        try {
			mModifiedFilter = mActualFilter.clone();
		} catch (CloneNotSupportedException e) {
			// Cannot happend
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (android.os.Build.VERSION.SDK_INT < 5
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0) {
	        onBackPressed();
	    }

	    return super.onKeyDown(keyCode, event);
	}
	
	//@Override only >= 5
	public void onBackPressed() {
		if (mModifiedFilter.equals(mActualFilter)) {
			setResult(RESULT_CANCELED);
			Log.e("Vcuboid", "Exiting Preferences : Filter Changed");
		} else {
			Log.e("Vcuboid", "Exiting Preferences : Filter Changed");
			setResult(RESULT_OK);
			mModifiedFilter.setNeedDbQuery(mActualFilter);
			VcuboidManager.getVcuboidManagerInstance().setVcubFilter(mModifiedFilter);
			Log.e("Vcuboid", "Only Favorites ? " + mModifiedFilter.isShowOnlyFavorites());
		}
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    finish();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Let's do something a preference value changes
        if (key.equals("favorite_filter")) {
        	Log.e("Vcuboid", "Favorites changed");
			mModifiedFilter.setShowOnlyFavorites(mCheckBoxPreference.isChecked());
        }
	}
}