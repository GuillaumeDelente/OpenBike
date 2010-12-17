package fr.vcuboid.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import fr.vcuboid.R;

public class VcubFilter implements Cloneable {

	private boolean mShowOnlyFavorites = false;
	private boolean mShowOnlyWithSlots = false;
	private boolean mShowOnlyWithBikes = false;
	private boolean mNeedDbQuery = false;
	
	public void setShowOnlyFavorites(boolean showOnlyFavorites) {
		mShowOnlyFavorites = showOnlyFavorites;
	}

	public boolean isShowOnlyFavorites() {
		return mShowOnlyFavorites;
	}
	
	public void setShowOnlyWithSlots(boolean showOnlyWithSlots) {
		mShowOnlyWithSlots = showOnlyWithSlots;
	}

	public boolean isShowOnlyWithSlots() {
		return mShowOnlyWithSlots;
	}

	public void setShowOnlyWithBikes(boolean showOnlyWithBikes) {
		mShowOnlyWithBikes = showOnlyWithBikes;
	}

	public boolean isShowOnlyWithBikes() {
		return mShowOnlyWithBikes;
	}

	public void setNeedDbQuery(VcubFilter actualFilter) {
		if (actualFilter.isShowOnlyFavorites() && !mShowOnlyFavorites) {
			mNeedDbQuery = true;
			return;
		} if (actualFilter.isShowOnlyWithBikes() && !mShowOnlyWithBikes ||
				actualFilter.isShowOnlyWithSlots() && !mShowOnlyWithSlots) {
			mNeedDbQuery = true;
			return;
		}
		mNeedDbQuery = false;
	}

	public boolean isNeedDbQuery() {
		return mNeedDbQuery;
	}

	public VcubFilter(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		mShowOnlyFavorites = preferences.getBoolean(context.getString(R.string.favorite_filter), false);
		mShowOnlyWithBikes = preferences.getBoolean(context.getString(R.string.bikes_filter), false);
		mShowOnlyWithSlots = preferences.getBoolean(context.getString(R.string.slots_filter), false);
	}


	@Override
	public VcubFilter clone() throws CloneNotSupportedException {
		return (VcubFilter) super.clone();
	}

	@Override
	public boolean equals(Object aThat) {
		// check for self-comparison
		if (this == aThat)
			return true;

		if (!(aThat instanceof VcubFilter))
			return false;

		VcubFilter that = (VcubFilter) aThat;

		// now a proper field-by-field evaluation can be made
		return mShowOnlyFavorites == that.mShowOnlyFavorites && 
		mShowOnlyWithBikes == that.mShowOnlyWithBikes &&
		mShowOnlyWithSlots == that.mShowOnlyWithSlots;
	}
}
