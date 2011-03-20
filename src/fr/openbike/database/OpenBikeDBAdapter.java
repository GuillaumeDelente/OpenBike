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
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.provider.BaseColumns;

public class OpenBikeDBAdapter {

	public static final int JSON_ERROR = -2;
	public static final int DB_ERROR = -3;
	private static final String DATABASE_NAME = "openbike.db";
	private static final String STATIONS_TABLE = "stations";
	private static final String STATIONS_VIRTUAL_TABLE = "virtual_stations";
	private static final int DATABASE_VERSION = 2;
	public static final int ID_COLUMN = 0;
	public static final int ADDRESS_COLUMN = 1;
	public static final int BIKES_COLUMN = 2;
	public static final int SLOTS_COLUMN = 3;
	public static final int OPEN_COLUMN = 4;
	public static final int LATITUDE_COLUMN = 5;
	public static final int LONGITUDE_COLUMN = 6;
	public static final int NAME_COLUMN = 7;
	public static final int NETWORK_COLUMN = 8;
	public static final int FAVORITE_COLUMN = 9;
	public static final int PAYMENT_COLUMN = 10;
	public static final int SPECIAL_COLUMN = 11;

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

	private static final String CREATE_STATIONS_TABLE = "create table "
			+ STATIONS_TABLE + " (" + BaseColumns._ID
			+ " integer primary key, " + KEY_NAME
			+ " text not null COLLATE NOCASE, " + KEY_OPEN
			+ " integer not null, " + KEY_BIKES + " integer not null, "
			+ KEY_SLOTS + " integer not null, " + KEY_ADDRESS
			+ " text not null COLLATE NOCASE, " + KEY_LATITUDE
			+ " integer not null, " + KEY_LONGITUDE + " integer not null, "
			+ KEY_NETWORK + " text not null COLLATE NOCASE, " + KEY_FAVORITE
			+ " integer not null, " + KEY_PAYMENT + " integer not null, "
			+ KEY_SPECIAL + " integer not null );";

	private static final String DATABASE_CREATE_VIRTUAL = "CREATE VIRTUAL TABLE "
			+ STATIONS_VIRTUAL_TABLE
			+ " USING fts3 ("
			+ BaseColumns._ID
			+ " integer primary key, "
			+ KEY_NAME
			+ " text not null COLLATE NOCASE);";

	public OpenBikeDBAdapter(Context context) {
		// mContext = context;
		mDbHelper = new OpenBikeDBOpenHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
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
		String sql = "INSERT INTO " + STATIONS_TABLE + " (" + BaseColumns._ID
				+ "," + KEY_ADDRESS + "," + KEY_BIKES + "," + KEY_SLOTS + ","
				+ KEY_OPEN + "," + KEY_LATITUDE + "," + KEY_LONGITUDE + ","
				+ KEY_NAME + "," + KEY_NETWORK + "," + KEY_FAVORITE + ","
				+ KEY_PAYMENT + "," + KEY_SPECIAL
				+ ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?);";

		String sql_virtual = "INSERT INTO " + STATIONS_VIRTUAL_TABLE + " ("
				+ BaseColumns._ID + "," + KEY_NAME + ") VALUES (?,?);";
		try {
			mDb.beginTransaction();
			SQLiteStatement insert = mDb.compileStatement(sql);
			SQLiteStatement insert_virtual = mDb.compileStatement(sql_virtual);
			JSONArray jsonArray = new JSONArray(json);
			int id;
			String name;
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonStation = jsonArray.getJSONObject(i);
				id = jsonStation.getInt("id");
				name = jsonStation.getString("name");
				insert.bindLong(1, id);
				insert.bindString(2, jsonStation.getString("address"));
				insert.bindLong(3, jsonStation.getInt("availableBikes"));
				insert.bindLong(4, jsonStation.getInt("freeSlots"));
				insert.bindLong(5, jsonStation.getBoolean("open") ? 1 : 0);
				insert.bindLong(6,
						(int) (jsonStation.getDouble("latitude") * 1E6));
				insert.bindLong(7,
						(int) (jsonStation.getDouble("longitude") * 1E6));
				insert.bindString(8, name);
				insert.bindString(9, jsonStation.getString("network"));
				insert.bindLong(10, 0);
				insert.bindLong(11, jsonStation.getBoolean("payment") ? 1 : 0);
				insert.bindLong(12, jsonStation.getBoolean("special") ? 1 : 0);
				insert.executeInsert();

				insert_virtual.bindLong(1, id);
				insert_virtual.bindString(2, name);
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

	public int updateStations(String json) {
		int success = 1;
		String sql = "UPDATE " + STATIONS_TABLE + " SET " + KEY_BIKES
				+ " = ?, " + KEY_SLOTS + " = ?, " + KEY_OPEN + " = ? "
				+ " WHERE " + BaseColumns._ID + " = ?;";
		try {
			JSONArray jsonArray = new JSONArray(json);
			JSONObject jsonStation;
			mDb.beginTransaction();
			SQLiteStatement update = mDb.compileStatement(sql);
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
			mDb.endTransaction();
		}
		return success;
	}

	public boolean removeStation(int id) {
		return mDb.delete(STATIONS_TABLE, BaseColumns._ID + "=" + id, null) > 0;
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
		return mDb.update(STATIONS_TABLE, newValues,
				BaseColumns._ID + "=" + id, null) > 0;
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

	public Cursor getFilteredStationsCursor(String where, String orderBy) {
		return mDb.query(STATIONS_TABLE, new String[] { BaseColumns._ID,
				KEY_BIKES, KEY_SLOTS, KEY_OPEN, KEY_LATITUDE, KEY_LONGITUDE,
				KEY_NAME, KEY_FAVORITE, KEY_SPECIAL }, where, null, null, null,
				orderBy);
	}

	// Search results
	public Cursor getSearchCursor(String like) {
		String table = STATIONS_VIRTUAL_TABLE;
		try {
			Integer.parseInt(like);
			table = "vs." + BaseColumns._ID;
		} catch (NumberFormatException ex) {
			like += "*";
		}

		Cursor cursor = mDb.query( STATIONS_TABLE + " ob," + STATIONS_VIRTUAL_TABLE + " vs",
				new String[] { "ob." + BaseColumns._ID, "ob." + KEY_BIKES,
						"ob." + KEY_SLOTS, "ob." + KEY_OPEN,
						"ob." + KEY_LATITUDE, "ob." + KEY_LONGITUDE,
						"ob." + KEY_NAME, "ob." + KEY_FAVORITE }, table
						+ " MATCH ? AND ob.rowid = vs.rowid",
				new String[] { like }, null, null, null);
		/*
		 * Cursor cursor = mDb.rawQuery("SELECT ob.* " +
		 * "FROM openbike ob, virtual_stations vs " +
		 * "WHERE vs.name MATCH ? AND ob.rowid = vs.rowid", new String[] {like +
		 * "*"});
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

	// Search suggestions
	public Cursor getStationsMatches(String query, String[] columns) {
		String table = STATIONS_VIRTUAL_TABLE;
		try {
			Integer.parseInt(query);
			table = BaseColumns._ID;
		} catch (NumberFormatException ex) {
			query += "*";
		}

		Cursor cursor = mDb.query(STATIONS_VIRTUAL_TABLE, new String[] {
				BaseColumns._ID,
				"'n° ' || " + BaseColumns._ID + " as "
						+ SearchManager.SUGGEST_COLUMN_TEXT_2,
				BaseColumns._ID + " as "
						+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
				KEY_NAME + " as " + SearchManager.SUGGEST_COLUMN_TEXT_1 },
				table + " MATCH ?", new String[] { query }, null, null, null);
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
				BaseColumns._ID + "=?", new String[] { String.valueOf(id) },
				null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No Station found with ID " + id);
		}
		return cursor;
	}

	public int getStationCount() throws SQLException {
		Cursor cursor = mDb.rawQuery("SELECT COUNT(*) AS count FROM "
				+ STATIONS_TABLE, null);
		cursor.moveToNext();
		int count = cursor.getInt(0);
		cursor.close();
		return count;
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
			db.execSQL(DATABASE_CREATE_VIRTUAL);
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
					
					//Fill backup
					db.execSQL("INSERT INTO stations_backup SELECT "
							+ BaseColumns._ID + ", " + KEY_NAME + ", "
							+ KEY_OPEN + ", " + KEY_BIKES + ", " + KEY_SLOTS
							+ ", " + KEY_ADDRESS + ", " + KEY_LATITUDE + ", "
							+ KEY_LONGITUDE + ", " + KEY_NETWORK + ", "
							+ KEY_FAVORITE + ", " + KEY_PAYMENT + ", "
							+ KEY_SPECIAL + " FROM openbike;");
					
					//Drop old table
					db.execSQL("DROP TABLE openbike;");
					
					//Create new collate nocase table
					db.execSQL(CREATE_STATIONS_TABLE);
					
					//Fill new table from backup
					db.execSQL("INSERT INTO " + STATIONS_TABLE + " SELECT "
							+ BaseColumns._ID + ", " + KEY_NAME + ", "
							+ KEY_OPEN + ", " + KEY_BIKES + ", " + KEY_SLOTS
							+ ", " + KEY_ADDRESS + ", " + KEY_LATITUDE + ", "
							+ KEY_LONGITUDE + ", " + KEY_NETWORK + ", "
							+ KEY_FAVORITE + ", " + KEY_PAYMENT + ", "
							+ KEY_SPECIAL + " FROM stations_backup;");
					
					//Drop old table
					db.execSQL("DROP TABLE stations_backup;");
					
					//Create and fill virtual table
					db.execSQL(DATABASE_CREATE_VIRTUAL);
					db.execSQL("INSERT INTO " + STATIONS_VIRTUAL_TABLE + "("
							+ BaseColumns._ID + ", " + KEY_NAME + ") SELECT "
							+ BaseColumns._ID + ", " + KEY_NAME + " FROM "
							+ STATIONS_TABLE + ";");
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
