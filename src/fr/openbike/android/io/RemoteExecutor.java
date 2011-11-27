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
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.util.Log;
import fr.openbike.android.database.OpenBikeDBAdapter;
import fr.openbike.android.io.JSONHandler.HandlerException;
import fr.openbike.android.utils.ParserUtils;

/**
 * Executes an {@link HttpUriRequest} and passes the result as an
 * {@link XmlPullParser} to the given {@link XmlHandler}.
 */
public class RemoteExecutor {
	private final HttpClient mHttpClient;

	public RemoteExecutor(HttpClient httpClient) {
		mHttpClient = httpClient;
	}

	public Object executeGet(String url, JSONHandler handler, Context context)
			throws HandlerException {
		final HttpUriRequest request = new HttpGet(url);
		return execute(request, handler, context);
	}

	public Object execute(HttpUriRequest request, JSONHandler handler,
			Context context) throws HandlerException {
		try {
			final HttpResponse resp = mHttpClient.execute(request);
			final int status = resp.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new HandlerException("Unexpected server response "
						+ resp.getStatusLine() + " for "
						+ request.getRequestLine());
			}

			final InputStream input = resp.getEntity().getContent();
			Object result = null;
			try {
				result = handler.parse(new JSONObject(ParserUtils
						.getString(input)), OpenBikeDBAdapter
						.getInstance(context));
				if (input != null)
					input.close();
				return result;
			} catch (JSONException e) {
				if (input != null)
					input.close();
				throw new HandlerException("Malformed response for "
						+ request.getRequestLine(), e);
			}
		} catch (HandlerException e) {
			throw e;
		} catch (IOException e) {
			throw new HandlerException("Problem reading remote response for "
					+ request.getRequestLine(), e);
		}
	}
}
