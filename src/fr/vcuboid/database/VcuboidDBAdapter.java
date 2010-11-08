package fr.vcuboid.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import fr.vcuboid.object.Station;

public class VcuboidDBAdapter {
	private static final String DATABASE_NAME = "vcuboid.db";
	private static final String DATABASE_TABLE = "vcuboid";
	private static final int DATABASE_VERSION = 1;
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
	private SQLiteDatabase mDb;
	private final Context mContext;
	private VcuboidDBOpenHelper mDbHelper;

	public static final String KEY_ID = "_id";
	public static final String KEY_ADDRESS = "address";
	public static final String KEY_BIKES = "availableBikes";
	public static final String KEY_SLOTS = "freeSlots";
	public static final String KEY_OPEN = "isOpen";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_NAME = "name";
	public static final String KEY_NETWORK = "network";
	public static final String KEY_FAVORITE = "isFavorite";
	
	//TODO: remove this, only for debugging
	private static final String DATABASE_CREATE = "create table "
		+ DATABASE_TABLE + " (" 
		+ KEY_ID + " integer primary key, "
		+ KEY_NAME + " text not null, " 
		+ KEY_OPEN + " integer not null, " 
		+ KEY_BIKES + " integer not null, "
		+ KEY_SLOTS + " integer not null, " 
		+ KEY_ADDRESS + " text not null, " 
		+ KEY_LATITUDE + " real not null, "
		+ KEY_LONGITUDE + " real not null, " 
		+ KEY_NETWORK + " text not null, " 
		+ KEY_FAVORITE + " integer not null );";

	public VcuboidDBAdapter(Context context) {
		mContext = context;
		mDbHelper = new VcuboidDBOpenHelper(context, DATABASE_NAME, null,
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
	//TODO: remove this, only for debugging
	public void reset() throws SQLException {
		try {
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			mDb.execSQL(DATABASE_CREATE);
		} catch (SQLException e) {
		}
	}

	// Insert a new task
	public long insertStation(Station station) {
		// Create a new row of values to insert.
		ContentValues newVcubValues = new ContentValues();
		// Assign values for each row.
		newVcubValues.put(KEY_ID, station.getId());
		newVcubValues.put(KEY_ADDRESS, station.getAddress());
		newVcubValues.put(KEY_BIKES, station.getAvailableBikes());
		newVcubValues.put(KEY_SLOTS, station.getFreeSlots());
		newVcubValues.put(KEY_OPEN, station.isOpen() ? 1 : 0);
		newVcubValues.put(KEY_LATITUDE, station.getLatitude());
		newVcubValues.put(KEY_LONGITUDE, station.getLongitude());
		newVcubValues.put(KEY_NAME, station.getName());
		newVcubValues.put(KEY_NETWORK, station.getNetwork());
		newVcubValues.put(KEY_FAVORITE, false);
		// Insert the row.
		return mDb.insert(DATABASE_TABLE, null, newVcubValues);
	}

	public boolean removeStation(int id) {
		return mDb.delete(DATABASE_TABLE, KEY_ID + "=" + id, null) > 0;
	}

	public boolean updateStation(int id, int availableBikes, int freeSlots,
			boolean isOpen) {
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_BIKES, availableBikes);
		newValues.put(KEY_SLOTS, freeSlots);
		newValues.put(KEY_OPEN, isOpen);
		return mDb.update(DATABASE_TABLE, newValues, KEY_ID + "=" + id, null) > 0;
	}
	
	public boolean updateFavorite(int id, boolean isFavorite) {
		Log.e("Vcuboid", "updateFavorite " + id + " est " + isFavorite);
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_FAVORITE, isFavorite ? 1 : 0);
		return mDb.update(DATABASE_TABLE, newValues, KEY_ID + "=" + id, null) > 0;
	}

	/*
	 * 
	 * KEY_ID KEY_ADDRESS KEY_BIKES KEY_SLOTS KEY_OPEN KEY_LATITUDE
	 * KEY_LONGITUDE KEY_NAME KEY_NETWORK
	 */

	public Cursor getAllStationsCursor() {
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_ADDRESS,
				KEY_BIKES, KEY_SLOTS, KEY_OPEN, KEY_LATITUDE, KEY_LONGITUDE,
				KEY_NAME, KEY_NETWORK, KEY_FAVORITE }, null, null, null, null, null);
	}
	
	public Cursor getFilteredStationsCursor(boolean isOnlyFavorite) {
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_ADDRESS,
				KEY_BIKES, KEY_SLOTS, KEY_OPEN, KEY_LATITUDE, KEY_LONGITUDE,
				KEY_NAME, KEY_NETWORK, KEY_FAVORITE }, isOnlyFavorite ? KEY_FAVORITE + " = 1" : null, null, null, null, null);
	}

	public Station getStation(int id) throws SQLException {
		Cursor cursor = mDb.query(true, DATABASE_TABLE, new String[] { KEY_ID,
				KEY_ADDRESS, KEY_BIKES, KEY_SLOTS, KEY_OPEN, KEY_LATITUDE,
				KEY_LONGITUDE, KEY_NAME, KEY_NETWORK, KEY_FAVORITE }, KEY_ID + "=" + id,
				null, null, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No Station found with ID " + id);
		}

		Station result = new Station(id, cursor.getString(NETWORK_COLUMN),
				cursor.getString(NAME_COLUMN),
				cursor.getString(ADDRESS_COLUMN), 
				cursor.getDouble(LONGITUDE_COLUMN), 
				cursor.getDouble(LATITUDE_COLUMN), 
				cursor.getInt(BIKES_COLUMN), 
				cursor.getInt(SLOTS_COLUMN),
				cursor.getInt(OPEN_COLUMN) != 0,
				cursor.getInt(FAVORITE_COLUMN) != 0);
		return result;
	}
	
	public int getStationCount()  throws SQLException {
		Cursor cursor = mDb.rawQuery("SELECT COUNT(*) AS count FROM " + DATABASE_TABLE, null);
		cursor.moveToNext();
		int count = cursor.getInt(0);
		cursor.close();
		return count;
	}

	private static class VcuboidDBOpenHelper extends SQLiteOpenHelper {
		public VcuboidDBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
/*
		// SQL Statement to create a new database.
		private static final String DATABASE_CREATE = "create table "
				+ DATABASE_TABLE + " (" + KEY_ID + " integer primary key, "
				+ KEY_NAME + " text not null, " + KEY_OPEN
				+ " integer not null, " + KEY_BIKES + " integer not null, "
				+ KEY_SLOTS + " integer not null, " + KEY_ADDRESS
				+ " text not null, " + KEY_LATITUDE + " real not null, "
				+ KEY_LONGITUDE + " real not null, " + KEY_NETWORK
				+ " text not null);";
*/
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log
					.w("VcuboidDBAdapter", "Upgrading from version "
							+ oldVersion + " to " + newVersion
							+ ", which will destroy all old data");
			// Drop the old table.
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			// Create a new one.
			onCreate(db);
		}
	}
}
