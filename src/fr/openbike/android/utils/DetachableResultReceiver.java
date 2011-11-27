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

package fr.openbike.android.utils;

import android.app.Activity;
import android.app.Service;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import fr.openbike.android.IActivityHelper;
import fr.openbike.android.R;
import fr.openbike.android.service.SyncService;

/**
 * Proxy {@link ResultReceiver} that offers a listener interface that can be
 * detached. Useful for when sending callbacks to a {@link Service} where a
 * listening {@link Activity} can be swapped out during configuration changes.
 */
public class DetachableResultReceiver extends ResultReceiver {
	private static final String TAG = "DetachableResultReceiver";

	private Receiver mReceiver;
	private boolean mIsSync = false;
	private Bundle mBundleData = null;
	private int mResultCode = -1;
	private static DetachableResultReceiver mThis = null;

	public static DetachableResultReceiver getInstance(Handler handler) {
		if (mThis == null)
			mThis = new DetachableResultReceiver(handler);
		return mThis;
	}

	private DetachableResultReceiver(Handler handler) {
		super(handler);
	}

	public void clearReceiver() {
		mReceiver = null;
	}

	public void setReceiver(Receiver receiver) {
		mReceiver = receiver;
		((IActivityHelper) receiver).getActivityHelper()
				.setRefreshActionButtonCompatState(mIsSync);
		if (mResultCode != -1) {
			mReceiver.onReceiveResult(mResultCode, mBundleData);
			mBundleData = null;
			mResultCode = -1;
		}
	}

	public boolean isSync() {
		return mIsSync;
	}

	public interface Receiver {
		public void onReceiveResult(int resultCode, Bundle resultData);

		public void onStationsUpdated();
	}

	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		if (resultCode == SyncService.STATUS_SYNC_STATIONS) {
			mIsSync = true;
			if (mReceiver != null) {
				((IActivityHelper) mReceiver).getActivityHelper()
						.setRefreshActionButtonCompatState(true);
			}
		} else if (resultCode == SyncService.STATUS_SYNC_STATIONS_FINISHED) {
			mIsSync = false;
			if (mReceiver != null) {
				((IActivityHelper) mReceiver).getActivityHelper()
						.setRefreshActionButtonCompatState(false);
				mReceiver.onStationsUpdated();
			}
		}  else if (resultCode == SyncService.STATUS_UPDATE_STATIONS_FINISHED) {
			mIsSync = false;
			if (mReceiver != null) {
				((IActivityHelper) mReceiver).getActivityHelper()
						.setRefreshActionButtonCompatState(false);
				mReceiver.onStationsUpdated();
			}
		} else if (resultCode == SyncService.STATUS_ERROR) {
			mIsSync = false;
			if (mReceiver != null) {
				((IActivityHelper) mReceiver).getActivityHelper()
						.setRefreshActionButtonCompatState(false);
				((Activity) mReceiver).showDialog(R.id.network_error);
			}
		} else if (resultCode == R.id.enable_gps) {
			if (mReceiver != null) {
				((Activity) mReceiver).showDialog(R.id.enable_gps);
			}
		} else if (resultCode == R.id.no_location_provider) {
			if (mReceiver != null) {
				((Activity) mReceiver).showDialog(R.id.no_location_provider);
			}
		}
		if (mReceiver != null) {
			mReceiver.onReceiveResult(resultCode, resultData);
		} else {
			mResultCode = resultCode;
			mBundleData = resultData;
		}
	}
}
