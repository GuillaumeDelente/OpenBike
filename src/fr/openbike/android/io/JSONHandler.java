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

package fr.openbike.android.io;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import android.content.ContentProvider;
import fr.openbike.android.database.OpenBikeDBAdapter;

/**
 * Abstract class that handles reading and parsing an {@link XmlPullParser} into
 * a set of {@link ContentProviderOperation}. It catches recoverable network
 * exceptions and rethrows them as {@link HandlerException}. Any local
 * {@link ContentProvider} exceptions are considered unrecoverable.
 * <p>
 * This class is only designed to handle simple one-way synchronization.
 */
public abstract class JSONHandler {

	public void parseAndSave(JSONArray json, OpenBikeDBAdapter dbAdapter) throws HandlerException {
		try {
			parse(json, dbAdapter);
		} catch (HandlerException e) {
			throw e;
		} catch (JSONException e) {
			throw new HandlerException("Problem parsing JSON response", e);
		} catch (IOException e) {
			throw new HandlerException("Problem reading response", e);
		}
	}
	
	public void parseAndSave(JSONObject json, OpenBikeDBAdapter dbAdapter) throws HandlerException {
		try {
			parse(json, dbAdapter);
		} catch (HandlerException e) {
			throw e;
		} catch (JSONException e) {
			throw new HandlerException("Problem parsing JSON response", e);
		} catch (IOException e) {
			throw new HandlerException("Problem reading response", e);
		}
	}

	/**
	 * Parse the given {@link XmlPullParser}, returning a set of
	 * {@link ContentProviderOperation} that will bring the
	 * {@link ContentProvider} into sync with the parsed data.
	 */
	public abstract void parse(JSONArray json, OpenBikeDBAdapter dbAdapter)
    throws JSONException, IOException;
	
	

	/**
	 * Parse the given {@link XmlPullParser}, returning a set of
	 * {@link ContentProviderOperation} that will bring the
	 * {@link ContentProvider} into sync with the parsed data.
	 */
	public abstract void parse(JSONObject json, OpenBikeDBAdapter dbAdapter)
    throws JSONException, IOException;

	/**
	 * Parse the given {@link XmlPullParser}, returning a set of
	 * {@link ContentProviderOperation} that will bring the
	 * {@link ContentProvider} into sync with the parsed data.
	 */
	public abstract Object parseForResult(JSONArray json)
    throws JSONException, IOException;
	
	/**
	 * General {@link IOException} that indicates a problem occured while
	 * parsing or applying an {@link XmlPullParser}.
	 */
	public static class HandlerException extends IOException {
		/**
		 * 
		 */
		private static final long serialVersionUID = -4445747653914953086L;

		public HandlerException(String message) {
			super(message);
		}

		public HandlerException(String message, Throwable cause) {
			super(message);
			initCause(cause);
		}

		@Override
		public String toString() {
			if (getCause() != null) {
				return getLocalizedMessage() + ": " + getCause();
			} else {
				return getLocalizedMessage();
			}
		}
	}
}
