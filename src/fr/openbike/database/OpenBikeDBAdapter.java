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
package fr.openbike.database;

import org.acra.ErrorReporter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import fr.openbike.model.Network;
import fr.openbike.model.Station;
import fr.openbike.ui.AbstractPreferencesActivity;

public class OpenBikeDBAdapter {

	public static final int JSON_ERROR = -2;
	public static final int DB_ERROR = -3;
	private static final String DATABASE_NAME = "openbike.db";
	public static final String STATIONS_TABLE = "stations";
	private static final String STATIONS_VIRTUAL_TABLE = "virtual_stations";
	private static final String NETWORKS_TABLE = "networks";
	private static final int DATABASE_VERSION = 3;

	private SQLiteDatabase mDb;
	private OpenBikeDBOpenHelper mDbHelper;
	private static OpenBikeDBAdapter mInstance = null;
	public static final String KEY_ADDRESS = "address";
	public static final String KEY_BIKES = "availableBikes";
	public static final String KEY_SLOTS = "freeSlots";
	public static final String KEY_OPEN = "isOpen";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_NAME = "name";
	public static final String KEY_NETWORK = "network";
	public static final String KEY_FAVORITE = "isFavorite";
	public static final String KEY_PAYMENT = "hasPayment";
	public static final String KEY_SPECIAL = "isSpecial";
	public static final String KEY_SPECIAL_NAME = "specialName";
	public static final String KEY_CITY = "city";
	public static final String KEY_SERVER = "server";

	private static SharedPreferences mPreferences;

	private static final String CREATE_STATIONS_TABLE = "create table "
			+ STATIONS_TABLE + " (" + BaseColumns._ID + " integer not null, "
			+ KEY_NAME + " text not null COLLATE NOCASE, " + KEY_OPEN
			+ " integer not null, " + KEY_BIKES + " integer not null, "
			+ KEY_SLOTS + " integer not null, " + KEY_ADDRESS
			+ " text not null COLLATE NOCASE, " + KEY_LATITUDE
			+ " integer not null, " + KEY_LONGITUDE + " integer not null, "
			+ KEY_NETWORK + " integer not null, " + KEY_FAVORITE
			+ " integer not null, " + KEY_PAYMENT + " integer not null, "
			+ KEY_SPECIAL + " integer not null );";

	private static final String CREATE_VIRTUAL_TABLE = "CREATE VIRTUAL TABLE "
			+ STATIONS_VIRTUAL_TABLE + " USING fts3 (" + BaseColumns._ID
			+ " integer not null, " + KEY_NETWORK + " integer not null, "
			+ KEY_NAME + " text not null COLLATE NOCASE);";

	private static final String CREATE_NETWORKS_TABLE = "CREATE TABLE "
			+ NETWORKS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key, " + KEY_NAME + " text not null, "
			+ KEY_CITY + " text not null, " + KEY_LATITUDE
			+ " integer not null, " + KEY_LONGITUDE + " integer not null, "
			+ KEY_SERVER + " text not null, " + KEY_SPECIAL_NAME
			+ " text not null);";

	private OpenBikeDBAdapter(Context context) {
		// mContext = context;
		mDbHelper = new OpenBikeDBOpenHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static synchronized OpenBikeDBAdapter getInstance(Context context) {
		if (mInstance == null)
			mInstance = new OpenBikeDBAdapter(context);
		return mInstance;
	}

	public void close() {
		mDb.close();
	}

	public void open() throws SQLiteException {
		try {
			mDb = mDbHelper.getWritableDatabase();
		} catch (SQLiteException ex) {
			mDb = mDbHelper.getReadableDatabase();
		}
	}

	public void insertStations(JSONArray jsonArray) throws JSONException,
			SQLiteException {

		InsertHelper stationsInsertHelper = new InsertHelper(mDb,
				STATIONS_TABLE);
		InsertHelper virtualInsertHelper = new InsertHelper(mDb,
				STATIONS_VIRTUAL_TABLE);

		final int idColumn = stationsInsertHelper
				.getColumnIndex(BaseColumns._ID);
		final int nameColumn = stationsInsertHelper.getColumnIndex(KEY_NAME);
		final int openColumn = stationsInsertHelper.getColumnIndex(KEY_OPEN);
		final int bikesColumn = stationsInsertHelper.getColumnIndex(KEY_BIKES);
		final int slotsColumn = stationsInsertHelper.getColumnIndex(KEY_SLOTS);
		final int addressColumn = stationsInsertHelper
				.getColumnIndex(KEY_ADDRESS);
		final int latitudeColumn = stationsInsertHelper
				.getColumnIndex(KEY_LATITUDE);
		final int longitudeColumn = stationsInsertHelper
				.getColumnIndex(KEY_LONGITUDE);
		final int paymentColumn = stationsInsertHelper
				.getColumnIndex(KEY_PAYMENT);
		final int specialColumn = stationsInsertHelper
				.getColumnIndex(KEY_SPECIAL);
		final int networkColumn = stationsInsertHelper
				.getColumnIndex(KEY_NETWORK);
		final int favoriteColumn = stationsInsertHelper
				.getColumnIndex(KEY_FAVORITE);

		final int virtualIdColumn = virtualInsertHelper
				.getColumnIndex(BaseColumns._ID);
		final int virtualNameColumn = virtualInsertHelper
				.getColumnIndex(KEY_NAME);
		final int virtualNetworkColumn = virtualInsertHelper
				.getColumnIndex(KEY_NETWORK);

		final int networkId = jsonArray.getJSONObject(0)
				.getInt(Station.NETWORK);
		final int size = jsonArray.length();

		final String sql = "INSERT INTO " + STATIONS_TABLE
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?);";

		final String sql_virtual = "INSERT INTO " + STATIONS_VIRTUAL_TABLE
				+ " VALUES (?,?,?);";

		try {
			mDb.beginTransaction();
			SQLiteStatement insert = mDb.compileStatement(sql);
			SQLiteStatement insert_virtual = mDb.compileStatement(sql_virtual);
			insert.bindLong(networkColumn, networkId);
			insert.bindLong(favoriteColumn, 0); // Favorite
			insert_virtual.bindLong(virtualNetworkColumn, networkId);
			int id;
			String name;
			for (int i = 0; i < size; i++) {
				JSONObject jsonStation = jsonArray.getJSONObject(i);
				id = jsonStation.getInt(Station.ID);
				name = jsonStation.getString(Station.NAME);
				insert.bindLong(idColumn, id);
				insert.bindString(nameColumn, name);
				insert.bindLong(openColumn, jsonStation
						.getBoolean(Station.OPEN) ? 1 : 0);
				insert.bindLong(bikesColumn, jsonStation.getInt(Station.BIKES));
				insert.bindLong(slotsColumn, jsonStation.getInt(Station.SLOTS));
				insert.bindString(addressColumn, jsonStation
						.getString(Station.ADDRESS));
				insert.bindLong(latitudeColumn, (int) (jsonStation
						.getDouble(Station.LATITUDE) * 1E6));
				insert.bindLong(longitudeColumn, (int) (jsonStation
						.getDouble(Station.LONGITUDE) * 1E6));
				insert.bindLong(paymentColumn, jsonStation
						.getBoolean(Station.PAYMENT) ? 1 : 0);
				insert.bindLong(specialColumn, jsonStation
						.getBoolean(Station.SPECIAL) ? 1 : 0);
				insert.executeInsert();

				insert_virtual.bindLong(virtualIdColumn, id);
				insert_virtual.bindString(virtualNameColumn, name);
				insert_virtual.executeInsert();
			}
			mDb.setTransactionSuccessful();
		} catch (JSONException e) {
			throw e;
		} catch (SQLException e) {
			throw e;
		} finally {
			mDb.endTransaction();
		}
	}

	public boolean insertNetwork(Network network) throws SQLiteException {
		if (network == null)
			Log.d("OpenBike", "Network is null");
		if (getNetwork(network.getId(), new String[] { BaseColumns._ID }) != null) {
			Log.d("OpenBike", "Network already in db");
			return false;
		}
		Log.d("OpenBike", "Need inserting Network in db");
		ContentValues newValues = new ContentValues();
		newValues.put(BaseColumns._ID, network.getId());
		newValues.put(KEY_NAME, network.getName());
		newValues.put(KEY_CITY, network.getCity());
		newValues.put(KEY_SERVER, network.getServerUrl());
		newValues.put(KEY_LONGITUDE, network.getLongitude());
		newValues.put(KEY_LATITUDE, network.getLatitude());
		newValues.put(KEY_SPECIAL_NAME, network.getSpecialName());
		mDb.insertOrThrow(NETWORKS_TABLE, null, newValues);
		return true;
	}

	public void insertOrUpdateStations(JSONArray jsonArray)
			throws SQLiteException, JSONException {
		if (!updateStations(jsonArray)) {
			Log.d("OpenBike", "Inserting stations");
			insertStations(jsonArray);
		}
	}

	public boolean updateStations(JSONArray jsonArray) throws SQLiteException,
			JSONException {
		boolean updateOnly = true;
		final int size = jsonArray.length();
		JSONObject jsonStation;
		try {
			mDb.beginTransaction();
			ContentValues contentValues = new ContentValues();
			final int networkId = jsonArray.getJSONObject(0).getInt(
					Station.NETWORK);
			for (int i = 0; i < size; i++) {
				jsonStation = jsonArray.getJSONObject(i);
				contentValues.put(OpenBikeDBAdapter.KEY_BIKES, jsonStation
						.getInt(Station.BIKES));
				contentValues.put(OpenBikeDBAdapter.KEY_SLOTS, jsonStation
						.getInt(Station.SLOTS));
				contentValues.put(OpenBikeDBAdapter.KEY_OPEN, jsonStation
						.getBoolean(Station.OPEN));
				if (mDb.update(STATIONS_TABLE, contentValues, BaseColumns._ID
						+ " = " + jsonStation.getInt(Station.ID) + " AND "
						+ KEY_NETWORK + " = " + networkId, null) == 0) {
					updateOnly = false;
					break;
				}

			}
		} catch (SQLiteException e) {
			mDb.endTransaction();
			throw e;
		} catch (JSONException e) {
			mDb.endTransaction();
			throw e;
		}
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
		return updateOnly;
	}

	/*
	 * public boolean updateStation(int id, int availableBikes, int freeSlots,
	 * boolean isOpen) { ContentValues newValues = new ContentValues();
	 * newValues.put(KEY_BIKES, availableBikes); newValues.put(KEY_SLOTS,
	 * freeSlots); newValues.put(KEY_OPEN, isOpen); return
	 * mDb.update(DATABASE_TABLE, newValues, KEY_ID + "=" + id, null) > 0; }
	 */

	public boolean updateFavorite(int id, boolean isFavorite) {
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_FAVORITE, isFavorite ? 1 : 0);
		return mDb.update(STATIONS_TABLE, newValues, BaseColumns._ID
				+ " = ? AND " + KEY_NETWORK + " = ?;", new String[] {
				String.valueOf(id),
				String.valueOf(mPreferences.getInt(
						AbstractPreferencesActivity.NETWORK_PREFERENCE,
						AbstractPreferencesActivity.NO_NETWORK)) }) > 0;
	}

	/*
	 * 
	 * KEY_ID KEY_ADDRESS KEY_BIKES KEY_SLOTS KEY_OPEN KEY_LATITUDE
	 * KEY_LONGITUDE KEY_NAME KEY_NETWORK
	 * 
	 * 
	 * public Cursor getAllStationsCursor() { return mDb.query(DATABASE_TABLE,
	 * new String[] { KEY_ID, KEY_ADDRESS, KEY_BIKES, KEY_SLOTS, KEY_OPEN,
	 * KEY_LATITUDE, KEY_LONGITUDE, KEY_NAME, KEY_NETWORK, KEY_FAVORITE,
	 * KEY_PAYMENT, KEY_SPECIAL }, null, null, null, null, null); }
	 */

	public Cursor getFilteredStationsCursor(String[] projection, String where,
			String orderBy) {
		String nWhere;
		if (where == null)
			nWhere = KEY_NETWORK + " = ?";
		else
			nWhere = where + " AND " + KEY_NETWORK + " = ?";
		return mDb.query(STATIONS_TABLE, projection, nWhere,
				new String[] { String.valueOf(mPreferences.getInt(
						AbstractPreferencesActivity.NETWORK_PREFERENCE,
						AbstractPreferencesActivity.NO_NETWORK)) }, null, null,
				orderBy);
	}

	// Search results
	public Cursor getSearchCursor(String query) {
		String table = STATIONS_VIRTUAL_TABLE;
		try {
			Integer.parseInt(query);
			table = "vs." + BaseColumns._ID;
		} catch (NumberFormatException ex) {
		}
		query += "*";

		// TODO: Put network id as argument in rawQuery(), doesn't work
		String s = "SELECT vs._id, ob.availableBikes, ob.freeSlots, ob.isOpen, "
				+ "ob.latitude, ob.longitude, ob.name, ob.isFavorite "
				+ "FROM virtual_stations vs "
				+ "INNER JOIN stations ob "
				+ "ON (ob._id = vs._id AND vs.network = ob.network) "
				+ "WHERE "
				+ table
				+ " match ? AND vs.network = "
				+ String.valueOf(mPreferences.getInt(
						AbstractPreferencesActivity.NETWORK_PREFERENCE,
						AbstractPreferencesActivity.NO_NETWORK));

		Cursor cursor = mDb.rawQuery(s, new String[] { query });
		return cursor;
	}

	// Search suggestions
	public Cursor getStationsMatches(String query, String[] columns) {
		String table = STATIONS_VIRTUAL_TABLE;
		try {
			Integer.parseInt(query);
			table = BaseColumns._ID;
		} catch (NumberFormatException ex) {
		}
		query += "*";
		// Network is not in argument list because when I do so, it doesn't work
		// !
		String s = "SELECT vs._id, vs._id as "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
				+ ", 'n° ' || vs._id as "
				+ SearchManager.SUGGEST_COLUMN_TEXT_2
				+ ", vs.name as "
				+ SearchManager.SUGGEST_COLUMN_TEXT_1
				+ " FROM"
				+ " virtual_stations vs WHERE "
				+ table
				+ " MATCH ? AND vs.network = "
				+ mPreferences.getInt(
						AbstractPreferencesActivity.NETWORK_PREFERENCE,
						AbstractPreferencesActivity.NO_NETWORK) + ";";
		Cursor cursor = mDb.rawQuery(s, new String[] { query });
		/*
		 * Cursor cursor = mDb.query(STATIONS_VIRTUAL_TABLE, new String[] {
		 * BaseColumns._ID, "'n° ' || " + BaseColumns._ID + " as " +
		 * SearchManager.SUGGEST_COLUMN_TEXT_2, BaseColumns._ID + " as " +
		 * SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, KEY_NAME + " as " +
		 * SearchManager.SUGGEST_COLUMN_TEXT_1 }, table + " MATCH ? AND " +
		 * KEY_NETWORK + " = ?", new String[] { query,
		 * String.valueOf(mCurrentNetwork) }, null, null, null);
		 */
		/*
		 * Cursor cursor = mDb.rawQuery( "SELECT _id " +
		 * SearchManager.SUGGEST_COLUMN_TEXT_2 + ", _id " +
		 * SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID + ", name " +
		 * SearchManager.SUGGEST_COLUMN_TEXT_1 + " FROM virtual_stations;"
		 * "WHERE " + table + " MATCH ?;", null new String[] { query,
		 * String.valueOf(mCurrentNetwork) });
		 */
		if (cursor == null) {
			return null;
		} /*
		 * else if (!cursor.moveToFirst()) { cursor.close(); return null;
		 * 
		 * }
		 */
		return cursor;
	}

	/*
	 * public Station getStation(int id) throws SQLException { Cursor cursor =
	 * mDb.query(true, DATABASE_TABLE, new String[] { BaseColumns._ID,
	 * KEY_ADDRESS, KEY_BIKES, KEY_SLOTS, KEY_OPEN, KEY_LATITUDE, KEY_LONGITUDE,
	 * KEY_NAME, KEY_NETWORK, KEY_FAVORITE, KEY_PAYMENT, KEY_SPECIAL },
	 * BaseColumns._ID + "=?", new String[] { String.valueOf(id) }, null, null,
	 * null, null); if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
	 * throw new SQLException("No Station found with ID " + id); }
	 * 
	 * Station result = new Station(id, cursor.getString(NAME_COLUMN), cursor
	 * .getString(ADDRESS_COLUMN), cursor.getInt(LONGITUDE_COLUMN),
	 * cursor.getInt(LATITUDE_COLUMN), cursor.getInt(BIKES_COLUMN),
	 * cursor.getInt(SLOTS_COLUMN), cursor.getInt(OPEN_COLUMN) != 0,
	 * cursor.getInt(FAVORITE_COLUMN) != 0, cursor .getInt(PAYMENT_COLUMN) != 0,
	 * cursor .getInt(SPECIAL_COLUMN) != 0); return result; }
	 */
	public Cursor getStation(int id, String[] columns) throws SQLException {
		Cursor cursor = mDb.query(true, STATIONS_TABLE, columns,
				BaseColumns._ID + " = ? AND " + KEY_NETWORK + " = ?",
				new String[] {
						String.valueOf(id),
						String.valueOf(mPreferences.getInt(
								AbstractPreferencesActivity.NETWORK_PREFERENCE,
								AbstractPreferencesActivity.NO_NETWORK)) },
				null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No Station found with ID " + id);
		}
		return cursor;
	}

	public Cursor getNetwork(int id, String[] columns) {
		Log.d("OpenBike", "getting Network : " + id);
		try {
			Cursor cursor = mDb
					.query(true, NETWORKS_TABLE, columns, BaseColumns._ID
							+ " = ?", new String[] { String.valueOf(id) },
							null, null, null, null);
			if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
				Log.d("OpenBike", "No Network : " + id + " in db");
				return null;
			}
			return cursor;
		} catch (Exception e) {
			Log.d("OpenBike", "getNetwork exception : " + e.getMessage());
		}
		return null;
	}

	public int getStationCount() throws SQLException {
		Cursor cursor = mDb.rawQuery("SELECT COUNT(*) AS count FROM "
				+ STATIONS_TABLE + " WHERE " + KEY_NETWORK + " = ?",
				new String[] { String.valueOf(mPreferences.getInt(
						AbstractPreferencesActivity.NETWORK_PREFERENCE,
						AbstractPreferencesActivity.NO_NETWORK)) });
		cursor.moveToNext();
		int count = cursor.getInt(0);
		cursor.close();
		return count;
	}

	//For debug purpose
	/*
	public Cursor getStations() throws SQLException {
		Cursor cursor = mDb.rawQuery("SELECT " + BaseColumns._ID + ", "
				+ KEY_LATITUDE + ", " + KEY_LONGITUDE + ", " + KEY_BIKES + ", "
				+ KEY_NAME + ", " + KEY_SLOTS + " FROM " + STATIONS_TABLE
				+ " WHERE " + KEY_NETWORK + " = ?", new String[] { String
				.valueOf(mPreferences.getInt(
						AbstractPreferencesActivity.NETWORK_PREFERENCE,
						AbstractPreferencesActivity.NO_NETWORK)) });
		return cursor;
	}*/

	private static class OpenBikeDBOpenHelper extends SQLiteOpenHelper {
		public OpenBikeDBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		/*
		 * // SQL Statement to create a new database. private static final
		 * String DATABASE_CREATE = "create table " + DATABASE_TABLE + " (" +
		 * KEY_ID + " integer primary key, " + KEY_NAME + " text not null, " +
		 * KEY_OPEN + " integer not null, " + KEY_BIKES + " integer not null, "
		 * + KEY_SLOTS + " integer not null, " + KEY_ADDRESS +
		 * " text not null, " + KEY_LATITUDE + " real not null, " +
		 * KEY_LONGITUDE + " real not null, " + KEY_NETWORK +
		 * " text not null);";
		 */

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_STATIONS_TABLE);
			db.execSQL(CREATE_VIRTUAL_TABLE);
			db.execSQL(CREATE_NETWORKS_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == 1) {
				try {
					db.beginTransaction();
					// Create temporary table
					db.execSQL("CREATE TEMPORARY TABLE stations_backup ("
							+ BaseColumns._ID + " integer primary key, "
							+ KEY_NAME + " text not null COLLATE NOCASE, "
							+ KEY_OPEN + " integer not null, " + KEY_BIKES
							+ " integer not null, " + KEY_SLOTS
							+ " integer not null, " + KEY_ADDRESS
							+ " text not null COLLATE NOCASE, " + KEY_LATITUDE
							+ " integer not null, " + KEY_LONGITUDE
							+ " integer not null, " + KEY_NETWORK
							+ " text not null COLLATE NOCASE, " + KEY_FAVORITE
							+ " integer not null, " + KEY_PAYMENT
							+ " integer not null, " + KEY_SPECIAL
							+ " integer not null );");

					// Fill backup
					db.execSQL("INSERT INTO stations_backup SELECT "
							+ BaseColumns._ID + ", " + KEY_NAME + ", "
							+ KEY_OPEN + ", " + KEY_BIKES + ", " + KEY_SLOTS
							+ ", " + KEY_ADDRESS + ", " + KEY_LATITUDE + ", "
							+ KEY_LONGITUDE + ", " + KEY_NETWORK + ", "
							+ KEY_FAVORITE + ", " + KEY_PAYMENT + ", "
							+ KEY_SPECIAL + " FROM openbike;");

					// Drop old table
					db.execSQL("DROP TABLE openbike;");

					// Create new collate nocase table
					db.execSQL("create table stations (" + BaseColumns._ID
							+ " integer primary key, " + KEY_NAME
							+ " text not null COLLATE NOCASE, " + KEY_OPEN
							+ " integer not null, " + KEY_BIKES
							+ " integer not null, " + KEY_SLOTS
							+ " integer not null, " + KEY_ADDRESS
							+ " text not null COLLATE NOCASE, " + KEY_LATITUDE
							+ " integer not null, " + KEY_LONGITUDE
							+ " integer not null, " + KEY_NETWORK
							+ " text not null COLLATE NOCASE, " + KEY_FAVORITE
							+ " integer not null, " + KEY_PAYMENT
							+ " integer not null, " + KEY_SPECIAL
							+ " integer not null );");

					// Fill new table from backup
					db.execSQL("INSERT INTO stations SELECT " + BaseColumns._ID
							+ ", " + KEY_NAME + ", " + KEY_OPEN + ", "
							+ KEY_BIKES + ", " + KEY_SLOTS + ", " + KEY_ADDRESS
							+ ", " + KEY_LATITUDE + ", " + KEY_LONGITUDE + ", "
							+ KEY_NETWORK + ", " + KEY_FAVORITE + ", "
							+ KEY_PAYMENT + ", " + KEY_SPECIAL
							+ " FROM stations_backup;");

					// Drop old table
					db.execSQL("DROP TABLE stations_backup;");

					// Create and fill virtual table
					db
							.execSQL("CREATE VIRTUAL TABLE virtual_stations USING fts3 ("
									+ BaseColumns._ID
									+ " integer primary key, "
									+ KEY_NAME
									+ " text not null COLLATE NOCASE);");
					db.execSQL("INSERT INTO virtual_stations ("
							+ BaseColumns._ID + ", " + KEY_NAME + ") SELECT "
							+ BaseColumns._ID + ", " + KEY_NAME
							+ " FROM stations;");
					oldVersion++;
					db.setTransactionSuccessful();
				} catch (Exception e) {
					ErrorReporter.getInstance().handleException(e);
				} finally {
					db.endTransaction();
				}
			}
			if (oldVersion == 2) {
				try {
					db.beginTransaction();
					// Create temporary table
					db.execSQL(CREATE_NETWORKS_TABLE);

					// Fill Network table with BORDEAUX
					db
							.execSQL("INSERT INTO networks "
									+ "VALUES (1, 'VCub', 'Bordeaux', 44837368, -576144, "
									+ "'http://openbikeserver-2.appspot.com/stations/','VCub +');");

					// Create temporary table
					db.execSQL("CREATE TEMPORARY TABLE stations_backup ("
							+ BaseColumns._ID + " integer not null, "
							+ KEY_NAME + " text not null COLLATE NOCASE, "
							+ KEY_OPEN + " integer not null, " + KEY_BIKES
							+ " integer not null, " + KEY_SLOTS
							+ " integer not null, " + KEY_ADDRESS
							+ " text not null COLLATE NOCASE, " + KEY_LATITUDE
							+ " integer not null, " + KEY_LONGITUDE
							+ " integer not null, " + KEY_FAVORITE
							+ " integer not null, " + KEY_PAYMENT
							+ " integer not null, " + KEY_SPECIAL
							+ " integer not null );");

					// Fill backup
					db.execSQL("INSERT INTO stations_backup SELECT "
							+ BaseColumns._ID + ", " + KEY_NAME + ", "
							+ KEY_OPEN + ", " + KEY_BIKES + ", " + KEY_SLOTS
							+ ", " + KEY_ADDRESS + ", " + KEY_LATITUDE + ", "
							+ KEY_LONGITUDE + ", " + KEY_FAVORITE + ", "
							+ KEY_PAYMENT + ", " + KEY_SPECIAL
							+ " FROM stations;");

					// Drop old table
					db.execSQL("DROP TABLE stations;");

					// Create table with network as integer
					db.execSQL(CREATE_STATIONS_TABLE);

					// Fill new table from backup
					db.execSQL("INSERT INTO " + STATIONS_TABLE + " SELECT "
							+ BaseColumns._ID + ", " + KEY_NAME + ", "
							+ KEY_OPEN + ", " + KEY_BIKES + ", " + KEY_SLOTS
							+ ", " + KEY_ADDRESS + ", " + KEY_LATITUDE + ", "
							+ KEY_LONGITUDE + ", 1, " + KEY_FAVORITE + ", "
							+ KEY_PAYMENT + ", " + KEY_SPECIAL
							+ " FROM stations_backup;");

					// Drop temporary table
					db.execSQL("DROP TABLE stations_backup;");

					// Drop virtual table without backup because
					// we fill it with stations_table
					db.execSQL("DROP TABLE virtual_stations;");

					// Re create virtual table whith _id not longer
					// primary key and add a column network
					db.execSQL(CREATE_VIRTUAL_TABLE);

					// Fill it
					db
							.execSQL("INSERT INTO " + STATIONS_VIRTUAL_TABLE
									+ " SELECT " + BaseColumns._ID + ", "
									+ KEY_NETWORK + ", " + KEY_NAME + " FROM "
									+ STATIONS_TABLE + ";");

					oldVersion++;
					db.setTransactionSuccessful();
				} catch (Exception e) {
					ErrorReporter.getInstance().handleException(e);
				} finally {
					db.endTransaction();
				}
			}
		}
	}
}
