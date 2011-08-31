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
package fr.openbike.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import fr.openbike.android.IActivityHelper;
import fr.openbike.android.R;
import fr.openbike.android.utils.ActivityHelper;

/**
 * @author guitou
 * 
 */
public class AboutActivity extends Activity implements IActivityHelper {
	
	private ActivityHelper mActivityHelper = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_layout);
		mActivityHelper = new ActivityHelper(this);
		mActivityHelper.setupActionBar(getText(R.string.about));
	}

	@Override
	protected void onResume() {
		WebView myWebView = (WebView) findViewById(R.id.webview);
		myWebView.loadUrl("http://openbike.fr/about.html");
		super.onResume();
	}

	@Override
	public ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}
}
