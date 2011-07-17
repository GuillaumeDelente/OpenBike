/*
 * Copyright 2011 Google Inc.
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

package fr.openbike.io;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;

import fr.openbike.database.OpenBikeDBAdapter;

/**
 * Handle a remote {@link XmlPullParser} that defines a set of {@link Speakers}
 * entries. Assumes that the remote source is a Google Spreadsheet.
 */
public class RemoteBikesHandler extends JSONHandler {
	private static final String TAG = "SpeakersHandler";

	/** {@inheritDoc} */
	@Override
	public void parse(JSONArray jsonBikes,
			OpenBikeDBAdapter dbAdapter) throws JSONException, IOException {
		dbAdapter.insertOrUpdateStations(jsonBikes);
	}

	/* (non-Javadoc)
	 * @see fr.openbike.io.JSONHandler#parseForResult(org.json.JSONArray)
	 */
	@Override
	public Object parseForResult(JSONArray json) throws JSONException,
			IOException {
		return null;
	}
}
