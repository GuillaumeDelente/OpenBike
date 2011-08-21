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

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;

import fr.openbike.R;
import fr.openbike.ui.HomeActivity;

/**
 * A class that handles some common activity-related functionality in the app,
 * such as setting up the action bar. This class provides functioanlity useful
 * for both phones and tablets, and does not require any Android 3.0-specific
 * features.
 */
public class ActivityHelper {
	protected Activity mActivity;
	private Animation mRefreshAnimation = null;
	private static final int[] mVisibleInBar = { R.id.action_refresh,
			R.id.menu_search };

	public ActivityHelper(Activity activity) {
		mActivity = activity;
		mRefreshAnimation = AnimationUtils.loadAnimation(activity, R.anim.clockwise_rotation);
		mRefreshAnimation.setRepeatCount(Animation.INFINITE);
	}

	public void onPostCreate(Bundle savedInstanceState) {
		// Create the action bar
		SimpleMenu menu = new SimpleMenu(mActivity);
		mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
		// TODO: call onPreparePanelMenu here as well
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			int id = item.getItemId();
			for (int j = 0; j < mVisibleInBar.length; j++) {
				if (mVisibleInBar[j] == id) {
					addActionButtonFromMenuItem(item);
				}
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		mActivity.getMenuInflater().inflate(R.menu.default_menu_items, menu);
		return false;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.action_refresh);
		if (item != null)
			item.setVisible(!getActionBarCompat().isRefreshAnimating());
		return false;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_search:
			goSearch();
			return true;
		}
		return false;
	}

	/**
	 * Invoke "home" action, returning to
	 * {@link com.google.android.apps.iosched.ui.HomeActivity}.
	 */
	public Intent getHomeIntent() {
		final Intent intent = new Intent(mActivity, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	/**
	 * Invoke "search" action, triggering a default search.
	 */
	public void goSearch() {
		mActivity.startSearch(null, false, Bundle.EMPTY, false);
	}

	/**
	 * Sets up the action bar with the given title and accent color. If title is
	 * null, then the app logo will be shown instead of a title. Otherwise, a
	 * home button and title are visible. If color is null, then the default
	 * colorstrip is visible.
	 */
	public void setupActionBar(CharSequence title) {
		final ActionBar actionBar = getActionBarCompat();
		if (actionBar == null) {
			Log.d("OpenBike", "ActionBar is null");
			return;
		}
		if (title != null) {
			actionBar.setHomeAction(new IntentAction(mActivity,
					getHomeIntent(), R.drawable.icon_home));
			actionBar.setTitle(title);
		} else {
			actionBar.setHomeLogo(R.drawable.actionbar_logo);
		}
	}

	/**
	 * Sets the action bar title to the given string.
	 */
	public void setActionBarTitle(CharSequence title) {

		ActionBar actionBar = getActionBarCompat();
		if (actionBar == null) {
			Log.d("OpenBike", "Title bar is null");
			return;
		}
		if (title != null) {
			actionBar.setTitle(title);
		}
	}

	/**
	 * Returns the {@link ViewGroup} for the action bar on phones
	 */
	public ActionBar getActionBarCompat() {
		return (ActionBar) mActivity.findViewById(R.id.actionbar);
	}

	/**
	 * Adds an action button to the compatibility action bar, using menu
	 * information from a {@link MenuItem}. If the menu item ID is
	 * <code>menu_refresh</code>, the menu item's state can be changed to show a
	 * loading spinner using
	 * {@link ActivityHelper#setRefreshActionButtonCompatState(boolean)}.
	 */
	private void addActionButtonFromMenuItem(final MenuItem item) {
		final ActionBar actionBar = getActionBarCompat();
		actionBar.addAction(new Action() {

			@Override
			public void performAction(View view) {
				mActivity
						.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
			}

			@Override
			public int getIconResId() {
				return 0;
			}

			@Override
			public Drawable getIconDrawable() {
				return item.getIcon();
			}

			@Override
			public int getId() {
				return item.getItemId();
			}
		});
	}

	/**
	 * Sets the indeterminate loading state of a refresh button added with
	 * {@link ActivityHelper#addActionButtonCompatFromMenuItem(android.view.MenuItem)}
	 * (where the item ID was menu_refresh).
	 */
	public void setRefreshActionButtonCompatState(boolean refreshing) {
		ActionBar actionBar = getActionBarCompat();
		if (refreshing) {
			actionBar.startRefreshAnimation();
		} else {
			actionBar.stopRefreshAnimation();
		}
	}
	
	public void clearActions() {
		getActionBarCompat().removeAllActions();
	}
}
