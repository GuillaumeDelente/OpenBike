/*
 * Copyright (C) 2010 Guillaume Delente
 *
 * This file is part of .
 *
 * Vcuboid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * Vcuboid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vcuboid.  If not, see <http://www.gnu.org/licenses/>.
 */
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
