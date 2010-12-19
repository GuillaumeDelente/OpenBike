package fr.vcuboid;

import android.location.Location;


public interface IVcuboidActivity {

	public void showGetAllStationsOnProgress();
	public void updateGetAllStationsOnProgress(int progress);
	public void finishGetAllStationsOnProgress();
	public void showUpdateAllStationsOnProgress();
	public void finishUpdateAllStationsOnProgress();
	public void showAskForGps();
	public void onLocationChanged(Location location);
	public void onListUpdated();
	
}
