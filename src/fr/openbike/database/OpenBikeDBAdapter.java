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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import fr.openbike.filter.FilterPreferencesActivity;
import fr.openbike.object.Network;

public class OpenBikeDBAdapter {

	public static final int JSON_ERROR = -2;
	public static final int DB_ERROR = -3;
	private static final String DATABASE_NAME = "openbike.db";
	private static final String STATIONS_TABLE = "stations";
	private static final String STATIONS_VIRTUAL_TABLE = "virtual_stations";
	private static final String NETWORKS_TABLE = "networks";
	private static final int DATABASE_VERSION = 3;

	private SQLiteDatabase mDb;
	private OpenBikeDBOpenHelper mDbHelper;
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

	public OpenBikeDBAdapter(Context context) {
		// mContext = context;
		mDbHelper = new OpenBikeDBOpenHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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

	public int insertStations(String json) {
		int success = 1;
		String sql = "INSERT INTO " + STATIONS_TABLE
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?);";

		String sql_virtual = "INSERT INTO " + STATIONS_VIRTUAL_TABLE
				+ " VALUES (?,?,?);";
		try {
			mDb.beginTransaction();
			SQLiteStatement insert = mDb.compileStatement(sql);
			SQLiteStatement insert_virtual = mDb.compileStatement(sql_virtual);
			JSONArray jsonArray = new JSONArray(json);
			int id;
			String name;
			int network = mPreferences.getInt(
					FilterPreferencesActivity.NETWORK_PREFERENCE, 0);
			insert.bindLong(9, network);
			insert.bindLong(10, 0); // Favorite
			insert_virtual.bindLong(2, network);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonStation = jsonArray.getJSONObject(i);
				id = jsonStation.getInt("id");
				name = jsonStation.getString("name");
				insert.bindLong(1, id);
				insert.bindString(2, jsonStation.getString("name"));
				insert.bindLong(3, jsonStation.getBoolean("open") ? 1 : 0);
				insert.bindLong(4, jsonStation.getInt("availableBikes"));
				insert.bindLong(5, jsonStation.getInt("freeSlots"));
				insert.bindString(6, jsonStation.getString("address"));
				insert.bindLong(7,
						(int) (jsonStation.getDouble("latitude") * 1E6));
				insert.bindLong(8,
						(int) (jsonStation.getDouble("longitude") * 1E6));
				insert.bindLong(11, jsonStation.getBoolean("payment") ? 1 : 0);
				insert.bindLong(12, jsonStation.getBoolean("special") ? 1 : 0);
				insert.executeInsert();

				insert_virtual.bindLong(1, id);
				insert_virtual.bindString(3, name);
				insert_virtual.executeInsert();
			}
			mDb.setTransactionSuccessful();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success = JSON_ERROR;
		} catch (Exception e) {
			success = DB_ERROR;
		} finally {
			mDb.endTransaction();
		}
		return success;
	}

	public boolean insertNetwork(Network network) {
		try {
			/*
			 * mDb.execSQL("INSERT OR IGNORE INTO " + NETWORKS_TABLE +
			 * " VALUES (?,?,?);", new String[] {
			 * String.valueOf(network.getId()), network.getCity(),
			 * network.getName() });
			 */
			// mDb.execSQL("INSERT INTO networks(_id, name, city) VALUES (1,'Vcub','Bordeaux');");
			if (getStationCount() != 0)
				return false;
			ContentValues newValues = new ContentValues();
			newValues.put(BaseColumns._ID, network.getId());
			newValues.put(KEY_NAME, network.getName());
			newValues.put(KEY_CITY, network.getCity());
			newValues.put(KEY_SERVER, network.getServerUrl());
			newValues.put(KEY_LONGITUDE, network.getLongitude());
			newValues.put(KEY_LATITUDE, network.getLatitude());
			newValues.put(KEY_SPECIAL_NAME, network.getSpecialName());
			mDb.insert(NETWORKS_TABLE, null, newValues);
		} catch (Exception e) {
			ErrorReporter.getInstance().handleException(e);
			return false;
		}
		return true;
	}

	public int updateStations(String json) {
		int success = 1;
		String sql = "UPDATE " + STATIONS_TABLE + " SET " + KEY_BIKES
				+ " = ?, " + KEY_SLOTS + " = ?, " + KEY_OPEN + " = ? "
				+ " WHERE " + BaseColumns._ID + " = ? AND " + KEY_NETWORK
				+ " = ?;";
		try {
			mDb.beginTransaction();
			JSONArray jsonArray = new JSONArray(json);
			JSONObject jsonStation;
			SQLiteStatement update = mDb.compileStatement(sql);
			update.bindLong(5, mPreferences.getInt(
					FilterPreferencesActivity.NETWORK_PREFERENCE, 0));
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonStation = jsonArray.getJSONObject(i);
				update.bindLong(1, jsonStation.getInt("availableBikes"));
				update.bindLong(2, jsonStation.getInt("freeSlots"));
				update.bindLong(3, jsonStation.getBoolean("open") ? 1 : 0);
				update.bindLong(4, jsonStation.getInt("id"));
				update.execute();
			}
			mDb.setTransactionSuccessful();
		} catch (JSONException e) {
			success = JSON_ERROR;
		} catch (Exception e) {
			success = DB_ERROR;
		} finally {
			if (mDb.inTransaction())
				mDb.endTransaction();
			else
				Log.d("OpenBike", "DB Not in transaction");
		}
		return success;
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
						FilterPreferencesActivity.NETWORK_PREFERENCE, 0)) }) > 0;
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

	public Cursor getFilteredStationsCursor(String[] projection, String where, String orderBy) {
		String nWhere;
		if (where == null)
			nWhere = KEY_NETWORK + " = ?";
		else
			nWhere = where + " AND " + KEY_NETWORK + " = ?";
		return mDb.query(STATIONS_TABLE, projection, nWhere,
				new String[] { String.valueOf(mPreferences.getInt(
						FilterPreferencesActivity.NETWORK_PREFERENCE, 0)) },
				null, null, orderBy);
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

		// FIXME: Put network id as argument in rawQuery(), doesn't work
		String s = "SELECT vs._id, ob.availableBikes, ob.freeSlots, ob.isOpen, "
				+ "ob.latitude, ob.longitude, ob.name, ob.isFavorite "
				+ "FROM virtual_stations vs "
				+ "INNER JOIN stations ob "
				+ "ON (ob._id = vs._id AND vs.network = ob.network) "
				+ "WHERE "
				+ table
				+ " match ? AND vs.network = "
				+ String.valueOf(mPreferences.getInt(
						FilterPreferencesActivity.NETWORK_PREFERENCE, 0));

		Cursor cursor = mDb.rawQuery(s, new String[] { query
		// ,
		// String.valueOf(mPreferences.getInt(FilterPreferencesActivity.NETWORK_PREFERENCE,
		// 0))
				});

		/*
		 * if (cursor == null) { return null; } /* else if
		 * (!cursor.moveToFirst()) { cursor.close(); return null;
		 * 
		 * }
		 */

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
						FilterPreferencesActivity.NETWORK_PREFERENCE, 0) + ";";
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
		Cursor cursor = mDb
				.query(
						true,
						STATIONS_TABLE,
						columns,
						BaseColumns._ID + " = ? AND " + KEY_NETWORK + " = ?",
						new String[] {
								String.valueOf(id),
								String
										.valueOf(mPreferences
												.getInt(
														FilterPreferencesActivity.NETWORK_PREFERENCE,
														0)) }, null, null,
						null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No Station found with ID " + id);
		}
		return cursor;
	}

	public Cursor getNetwork(int id, String[] columns) throws SQLException {
		Cursor cursor = mDb.query(true, NETWORKS_TABLE, columns,
				BaseColumns._ID + " = ?", new String[] { String.valueOf(id), },
				null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No Network found with ID " + id);
		}
		return cursor;
	}

	public int getStationCount() throws SQLException {
		Cursor cursor = mDb.rawQuery("SELECT COUNT(*) AS count FROM "
				+ STATIONS_TABLE + " WHERE " + KEY_NETWORK + " = ?",
				new String[] { String.valueOf(mPreferences.getInt(
						FilterPreferencesActivity.NETWORK_PREFERENCE, 0)) });
		cursor.moveToNext();
		int count = cursor.getInt(0);
		cursor.close();
		return count;
	}

	// FIXME : to remove
	public Cursor getStations() throws SQLException {
		Cursor cursor = mDb.rawQuery("SELECT " +
				BaseColumns._ID + ", " +
				KEY_LATITUDE + ", " +
				KEY_LONGITUDE + ", " +
				KEY_BIKES + ", " +
				KEY_NAME + ", " +
				KEY_SLOTS + " FROM " +
				STATIONS_TABLE + " WHERE " + KEY_NETWORK + " = ?",
				new String[] { String.valueOf(mPreferences.getInt(
						FilterPreferencesActivity.NETWORK_PREFERENCE, 0)) });
		return cursor;
	}

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
