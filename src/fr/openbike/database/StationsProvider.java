/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.openbike.database;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Provides access to the dictionary database.
 */
public class StationsProvider extends ContentProvider {

	public static String AUTHORITY = "fr.openbike.StationsProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/station");

	// MIME types used for searching words or looking up a single definition
	public static final String STATION_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/vnd.openbike.station";

	private OpenBikeDBAdapter mDb;

	// UriMatcher stuff
	private static final int SEARCH_SUGGEST = 0;
	private static final int GET_STATION = 1;
	private static final UriMatcher sURIMatcher = buildUriMatcher();

	@Override
	public boolean onCreate() {
		mDb = new OpenBikeDBAdapter(getContext());
		mDb.open();
		return true;
	}

	/**
	 * Builds up a UriMatcher for search suggestion and shortcut refresh
	 * queries.
	 */
	private static UriMatcher buildUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		// to get suggestions...
		matcher.addURI(AUTHORITY, "station/#", GET_STATION);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
				SEARCH_SUGGEST);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
				SEARCH_SUGGEST);
		return matcher;
	}

	/**
	 * Handles all the dictionary searches and suggestion queries from the
	 * Search Manager. When requesting a specific word, the uri alone is
	 * required. When searching all of the dictionary for matches, the
	 * selectionArgs argument must carry the search query as the first element.
	 * All other arguments are ignored.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Use the UriMatcher to see what kind of query we have and format the
		// db query accordingly
		switch (sURIMatcher.match(uri)) {
		case SEARCH_SUGGEST:
			if (selectionArgs == null) {
				throw new IllegalArgumentException(
						"selectionArgs must be provided for the Uri: " + uri);
			}
			return getSuggestions(selectionArgs[0]);
		case GET_STATION:
			return getStation(uri);
		default:
			throw new IllegalArgumentException("Unknown Uri: " + uri);
		}
	}

	private Cursor getSuggestions(String query) {
		query = query.toLowerCase();
		String[] columns = new String[] { BaseColumns._ID,
				OpenBikeDBAdapter.KEY_NAME };
		return mDb.getStationsMatches(query, columns);
	}

	private Cursor getStation(Uri uri) {
		String rowId = uri.getLastPathSegment();
		String[] columns = new String[] { BaseColumns._ID,
				OpenBikeDBAdapter.KEY_ADDRESS, OpenBikeDBAdapter.KEY_BIKES,
				OpenBikeDBAdapter.KEY_SLOTS, OpenBikeDBAdapter.KEY_OPEN,
				OpenBikeDBAdapter.KEY_LATITUDE,
				OpenBikeDBAdapter.KEY_LONGITUDE, OpenBikeDBAdapter.KEY_NAME,
				OpenBikeDBAdapter.KEY_NETWORK, OpenBikeDBAdapter.KEY_FAVORITE,
				OpenBikeDBAdapter.KEY_PAYMENT, OpenBikeDBAdapter.KEY_SPECIAL };
		return mDb.getStation(Integer.parseInt(rowId), columns);
	}

	@Override
	public String getType(Uri uri) {
		switch (sURIMatcher.match(uri)) {
		case GET_STATION:
			return STATION_MIME_TYPE;
		case SEARCH_SUGGEST:
			return SearchManager.SUGGEST_MIME_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}

	// Other required implementations...

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}
}
