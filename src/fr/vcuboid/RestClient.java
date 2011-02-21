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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import fr.vcuboid.database.VcuboidDBAdapter;
import fr.vcuboid.map.StationOverlay;
import fr.vcuboid.object.Station;

public class RestClient {
	
	public static final int NETWORK_ERROR = -1;
	public static final int JSON_ERROR = -2;
	public static final int DB_ERROR = -3;

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/*
	 * Connect to the server
	 */
	public static String connect(String url) {
		Log.i("Vcuboid", "connect");
		HttpClient httpclient = new DefaultHttpClient();

		// Prepare a request object
		HttpGet httpget = new HttpGet(url);

		// Execute the request
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			// Examine the response status
			Log.i("JSON", response.getStatusLine().toString());

			// Get hold of the response entity
			HttpEntity entity = response.getEntity();
			// If the response does not enclose an entity, there is no need
			// to worry about connection release

			if (entity != null) {

				// A Simple JSON Response Read
				InputStream instream = entity.getContent();
				String jsonString = convertStreamToString(instream);
				instream.close();
				return jsonString;
			}
			return null;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean jsonStationsToDb(String json,
			VcuboidDBAdapter vcuboidDBAdapter) {
		Log.i("Vcuboid", "jsonStationsToDb");
		try {
			JSONArray jsonArray = new JSONArray(json);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonStation = jsonArray.getJSONObject(i);
				vcuboidDBAdapter.insertStation(jsonStation.getInt("id"),
						jsonStation.getString("name"), jsonStation
								.getString("address"), jsonStation
								.getString("network"), jsonStation
								.getDouble("latitude"), jsonStation
								.getDouble("longitude"), jsonStation
								.getInt("availableBikes"), jsonStation
								.getInt("freeSlots"), jsonStation
								.getBoolean("open"), jsonStation
								.getBoolean("payment"));
			}
			return true;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public static boolean updateListFromJson(String json,
			ArrayList<StationOverlay> mVisibleStations) {
		Log.i("Vcuboid", "updateListFromJson");
		try {
			JSONArray jsonArray = new JSONArray(json);
			JSONObject jsonStation;
			Station station;
			int id = 0;
			for (int i = 0; i < mVisibleStations.size(); i++) {
				station = mVisibleStations.get(i).getStation();
				id = station.getId();
				jsonStation = jsonArray.getJSONObject(id-1);
				Log.e("Vcuboid", "Station : " + id + " " + jsonStation.getInt("id"));
				station.setBikes(jsonStation.getInt("availableBikes"));
				station.setSlots(jsonStation.getInt("freeSlots"));
				station.setOpen(jsonStation.getBoolean("open"));
			}
			return true;
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean updateDbFromJson(String json,
			VcuboidDBAdapter vcuboidDBAdapter) {
		Log.i("Vcuboid", "updateDbFromJson");
		try {
			boolean success = true;
			JSONArray jsonArray = new JSONArray(json);
			JSONObject jsonStation;
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonStation = jsonArray.getJSONObject(i);
				if (!vcuboidDBAdapter.updateStation(jsonStation.getInt("id"),
						jsonStation.getInt("availableBikes"), jsonStation
								.getInt("freeSlots"), jsonStation
								.getBoolean("open"))) {
					success = false;
				}

			}
			return success;
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
}