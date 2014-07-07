/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.mms.transaction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.util.MultiSimUtility;

/**
 * MmsSystemEventReceiver receives the
 * {@link android.content.intent.ACTION_BOOT_COMPLETED},
 * {@link com.android.internal.telephony.TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED}
 * and performs a series of operations which may include:
 * <ul>
 * <li>Show/hide the icon in notification area which is used to indicate
 * whether there is new incoming message.</li>
 * <li>Resend the MM's in the outbox.</li>
 * </ul>
 */
public class MmsSystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSystemEventReceiver";
    private static ConnectivityManager mConnMgr = null;
    private Context mContext;

    public static void wakeUpService(Context context) {
        Log.d(TAG, "wakeUpService: start service ...");
        MultiSimUtility.startSelectMmsSubsciptionServ(
                context,
                new Intent(context, TransactionService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (mConnMgr == null) {
            mConnMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "Intent received: " + intent);
        }

        String action = intent.getAction();
        if (action.equals(Mms.Intents.CONTENT_CHANGED_ACTION)) {
            Uri changed = (Uri) intent.getParcelableExtra(Mms.Intents.DELETED_CONTENTS);
            MmsApp.getApplication().getPduLoaderManager().removePdu(changed);
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (!isNetworkAvailable()) {
                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "mobile data unavailable, bailing");
                }
                return;
            }
            NetworkInfo mmsNetworkInfo = mConnMgr
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            boolean available = false;
            boolean isConnected = false;

            // Check that mobile data connection is available
            if (mmsNetworkInfo != null) {
                available = mmsNetworkInfo.isAvailable();
                isConnected = mmsNetworkInfo.isConnected();
            }

            Log.d(TAG, "TYPE_MOBILE_MMS available = " + available +
                           ", isConnected = " + isConnected);

            // Wake up transact service when MMS data is available and isn't connected.
            if (isNetworkAvailable() && !isConnected) {
                wakeUpService(context);
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // We should check whether there are unread incoming
            // messages in the Inbox and then update the notification icon.
            // Called on the UI thread so don't block.
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                    context, MessagingNotification.THREAD_NONE, false);

            // Scan and send pending Mms once after boot completed since
            // ACTION_ANY_DATA_CONNECTION_STATE_CHANGED wasn't registered in a whole life cycle
            wakeUpService(context);
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            // Wake up transact service upon leaving airplane mode if auto-enable data
            if (isAutoEnableData() && !isAirplaneModeOn()) {
                wakeUpService(context);
            }
        }
    }

    private boolean isAutoEnableData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean result = prefs.getBoolean(MessagingPreferenceActivity.AUTO_ENABLE_DATA, false);
        Log.d(TAG, "isAutoEnableData=" + result);
        return result;
    }

    private boolean isAirplaneModeOn() {
        boolean result = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        Log.d(TAG, "isAirplaneModeOn=" + result);
        return result;
    }

    private boolean isNetworkAvailable() {
        boolean result = false;
        if (mConnMgr == null) {
            result = false;
        } else {
            if (mConnMgr.getMobileDataEnabled()) {
                NetworkInfo ni = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
                result = (ni == null ? false : ni.isAvailable());
            } else {
                // we can auto-enable data, so report available
                result = !isAirplaneModeOn() && isAutoEnableData();
            }
        }
        Log.d(TAG, "isNetworkAvailable=" + result);
        return result;
    }
}
