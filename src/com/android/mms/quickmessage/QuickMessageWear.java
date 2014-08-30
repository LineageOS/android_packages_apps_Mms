/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2010-2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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
package com.android.mms.quickmessage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.widget.Toast;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Conversation;
import com.android.mms.transaction.SmsMessageSender;


public class QuickMessageWear extends Activity {
    private static boolean DEBUG = MmsConfig.Debug();
    private static String TAG = "QmWear";
    private CharSequence message;
    private String[] dest;
    Context mContext;
    Intent i;
    private WakeLock wakeLock;
    public static final String SMS_SENDER =
            "com.android.mms.SMS_SENDER";
    public static final String SMS_THEAD_ID =
            "com.android.mms.SMS_THREAD_ID";
    public static final String SMS_CONATCT =
            "com.android.mms.CONTACT";


    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(EXTRA_VOICE_REPLY);
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        i = getIntent();
        parseIntent(i.getExtras());

        //Get partial Wakelock so that we can send the message even if phone is locked
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.acquire();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DEBUG)
            Log.d(TAG, "onNewIntent() called");
        // Set new intent
        setIntent(intent);
        // Send new SMS from voice
        parseIntent(intent.getExtras());
    }

    @Override
    protected void onDestroy(){
        if (wakeLock.isHeld()){
            wakeLock.release();
        }
        super.onDestroy();
    }

    private void parseIntent(Bundle extras) {
        if (extras == null) {
            return;
        }
        //parse the remote input into a message that can be sent
        message = getMessageText(i);
        String mSender = extras.getString(SMS_SENDER);
        String mContactName = extras.getString(SMS_CONATCT);
        Long tId = extras.getLong(SMS_THEAD_ID);
        //Send only small messages if you send more then 160 chars it wont work properly
        if (message.length() <= 160) {
            dest = new String[]{
                    mSender
            };
            SmsMessageSender smsMessageSender = new SmsMessageSender(getBaseContext(), dest,
					message.toString(), tId);
            try {
                smsMessageSender.sendMessage(tId);
                Toast.makeText(mContext, R.string.toast_sending_message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "We haz failed to send your voice reply please try again later");
            }
        } else {
            Toast.makeText(mContext, R.string.too_many_chars + " to " + mContactName,
					Toast.LENGTH_LONG).show();
        }
        //gotta mark as read even if it doesn't send since we read the message and tried to respond to it
        Conversation con = Conversation.get(mContext, tId, true);
        if (con != null) {
            con.markAsRead();
            if (DEBUG)
                Log.d(TAG, "markAllMessagesRead(): Marked message " + tId
                        + " as read");
        }
        finish();
    }
}


