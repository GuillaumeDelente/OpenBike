package fr.vcuboid;

public class VcubFilter {
	
	private boolean mShowOnlyFavorites = false;
	
	public VcubFilter() {
	}
	
	public VcubFilter(boolean showOnlyFavorites) {
		setShowOnlyFavorites(showOnlyFavorites);
	}

	public void setShowOnlyFavorites(boolean showOnlyFavorites) {
		mShowOnlyFavorites = showOnlyFavorites;
	}

	public boolean getShowOnlyFavorites() {
		return mShowOnlyFavorites;
	}

}
