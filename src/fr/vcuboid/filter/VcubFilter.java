package fr.vcuboid.filter;

public class VcubFilter implements Cloneable {

	private boolean mShowOnlyFavorites = false;
	private boolean mNeedDbQuery = false;

	public VcubFilter() {
	}

	public VcubFilter(boolean showOnlyFavorites) {
		setShowOnlyFavorites(showOnlyFavorites);
	}

	@Override
	public VcubFilter clone() throws CloneNotSupportedException {
		return (VcubFilter) super.clone();
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

	@Override
	public boolean equals(Object aThat) {
		// check for self-comparison
		if (this == aThat)
			return true;

		if (!(aThat instanceof VcubFilter))
			return false;

		VcubFilter that = (VcubFilter) aThat;

		// now a proper field-by-field evaluation can be made
		return mShowOnlyFavorites == that.mShowOnlyFavorites;
	}
}
