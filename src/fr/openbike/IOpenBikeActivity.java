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
package fr.openbike;

import java.util.ArrayList;

import fr.openbike.object.Network;

public interface IOpenBikeActivity {
	public void showProgressDialog(String title, String message);
	public void dismissProgressDialog();
	public void showUpdateAllStationsOnProgress(boolean animate);
	public void finishUpdateAllStationsOnProgress(boolean animate);
	public void onListUpdated();
	public void showChooseNetwork(ArrayList<Network> networks);
	public void showDialog(int id);
	public void dismissDialog(int id);
}
