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

package fr.openbike.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.openbike.io.JSONHandler;

/**
 * Various utility methods used by {@link JSONHandler} implementations.
 */
public class ParserUtils {

	/**
	 * Build and return a {@link String} associed to the {@link InputStream}
	 * 
	 * @throws IOException
	 */
	public static String getString(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		StringBuilder json = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			json.append(line);
		}
		reader.close();
		return json.toString();
	}
}
