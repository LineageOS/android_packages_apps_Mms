/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.mms.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.android.mms.LogTag;

public class GroupMembership {
    private static final String TAG = "Mms/GroupMembership";
    private static final boolean DEBUG = false;

    private static final int _ID           = 0;
    private static final int GM_CONTACT_ID = 1;
    private static final int GM_GROUP_ID   = 2;

    private long mId;
    private long mContactId;
    private long mGroupId;

    private GroupMembership(Context context, Cursor c) {
        if (DEBUG) {
            Log.v(TAG, "Recipient constructor cursor");
        }

        // Fill the specified groupMembership with the values from the
        // specified cursor
        mId = c.getLong(_ID);
        mContactId = c.getLong(GM_CONTACT_ID);
        mGroupId = c.getLong(GM_GROUP_ID);

        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            Log.d(TAG, "Create groupMembership: recipient=" + mId
                    + ", groupId=" + mGroupId
                    + ", contactId=" + mId);
        }
    }

    public long getId() {
        return mId;
    }

    public long getContactId() {
        return mContactId;
    }

    public long getGroupId() {
        return mGroupId;
    }

    /**
     * Get all groupMembership
     * @param context
     * @return all groupMembership
     */
    public static ArrayList<GroupMembership> getGroupMemberships(Context context) {
        ArrayList<GroupMembership> groupMemberships = new ArrayList<GroupMembership>();

        final String[] projection = new String[] {
                CommonDataKinds.GroupMembership._ID,
                CommonDataKinds.GroupMembership.CONTACT_ID,
                CommonDataKinds.GroupMembership.GROUP_ROW_ID
        };

        final String selection =
                CommonDataKinds.GroupMembership.MIMETYPE + " = '" +
                CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'";

        final Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI,
                projection, selection, null, null);

        if (cursor == null) {
            return null;
        }

        if (cursor.getCount() == 0) {
            // No results to process
            cursor.close();
            return null;
        }

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            groupMemberships.add(new GroupMembership(context, cursor));
        }
        cursor.close();

        return groupMemberships;
    }
}
