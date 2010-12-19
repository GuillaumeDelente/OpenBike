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
	 * This is a test function which will connects to a given rest service and
	 * prints it's response to Android Log with labels "Praeda".
	 */
	public static String connect(String url) {

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
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static boolean jsonStationsToDb(String json,
			VcuboidDBAdapter vcuboidDBAdapter) {
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
								.getBoolean("open"));
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
		try {
			JSONArray jsonArray = new JSONArray(json);
			JSONObject jsonStation;
			Station station;
			int id = 0;
			// FIXME Maybe JSON are in station id order
			// and we should access them for each item in the list
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonStation = jsonArray.getJSONObject(i);
				for (int j = 0; j < mVisibleStations.size(); j++) {
					id = jsonStation.getInt("id");
					station = mVisibleStations.get(j).getStation();
					if (id == station.getId()) {
						station.setBikes(jsonStation.getInt("availableBikes"));
						station.setSlots(jsonStation.getInt("freeSlots"));
						station.setOpen(jsonStation.getBoolean("open"));
						break;
					}
				}
			}
			return true;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public static boolean updateDbFromJson(String json,
			VcuboidDBAdapter vcuboidDBAdapter) {
		try {
			JSONArray jsonArray = new JSONArray(json);
			JSONObject jsonStation;
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonStation = jsonArray.getJSONObject(i);
				vcuboidDBAdapter.updateStation(jsonStation.getInt("id"),
						jsonStation.getInt("availableBikes"), jsonStation
								.getInt("freeSlots"), jsonStation
								.getBoolean("open"));

			}
			return true;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}