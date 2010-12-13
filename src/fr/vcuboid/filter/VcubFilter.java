package fr.vcuboid.filter;

import android.content.SharedPreferences;

public class VcubFilter implements Cloneable {

	private boolean mShowOnlyFavorites = false;
	private boolean mEnableLocation = false;
	private boolean mNeedDbQuery = false;
	
	public void setEnableLocation(boolean mEnableLocation) {
		this.mEnableLocation = mEnableLocation;
	}

	public boolean isEnableLocation() {
		return mEnableLocation;
	}
	
	public void setShowOnlyFavorites(boolean showOnlyFavorites) {
		mShowOnlyFavorites = showOnlyFavorites;
	}

	public boolean isShowOnlyFavorites() {
		return mShowOnlyFavorites;
	}

	public void setNeedDbQuery(VcubFilter actualFilter) {
		if (actualFilter.isShowOnlyFavorites() && !mShowOnlyFavorites)
			mNeedDbQuery = true;
		else
			mNeedDbQuery = false;
	}

	public boolean isNeedDbQuery() {
		return mNeedDbQuery;
	}

	public VcubFilter(SharedPreferences preferences) {
		mShowOnlyFavorites = preferences.getBoolean("favorite_filter", false);
		mEnableLocation = preferences.getBoolean("location_filter", true);
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
		mEnableLocation == that.mEnableLocation;
	}


}
