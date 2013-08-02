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
import android.provider.ContactsContract.Groups;
import android.util.Log;

import com.android.mms.LogTag;

public class Group {
    private static final String TAG = "Mms/Group";
    private static final boolean DEBUG = false;

    private static final int _ID           = 0;
    private static final int GROUP_TITLE   = 1;
    private static final int ACCOUNT_NAME  = 2;
    private static final int ACCOUNT_TYPE  = 3;
    private static final int DATA_SET      = 4;
    private static final int SUMMARY_COUNT = 5;

    private long mId;
    private String mTitle;
    private String mAccountName;
    private String mAccountType;
    private String mDataSet;
    private int mSummaryCount;
    private ArrayList<PhoneNumber> mPhoneNumbers;

    private boolean mIsChecked;

    private Group(Context context, Cursor c) {
        if (DEBUG) {
            Log.v(TAG, "Recipient constructor cursor");
        }

        mId = c.getLong(_ID);
        mTitle = c.getString(GROUP_TITLE);
        mAccountName = c.getString(ACCOUNT_NAME);
        mAccountType = c.getString(ACCOUNT_TYPE);
        mDataSet = c.getString(DATA_SET);
        mSummaryCount = c.getInt(SUMMARY_COUNT);
        mPhoneNumbers = new ArrayList<PhoneNumber>();

        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            Log.d(TAG, "Create Group: recipient=" + mTitle + ", groupId=" + mId);
        }
    }

    public long getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public String getDataSet() {
        return mDataSet;
    }

    public int getSummaryCount() {
        return mSummaryCount;
    }

    public ArrayList<PhoneNumber> getPhoneNumbers() {
        return mPhoneNumbers;
    }

    public void addPhoneNumber(PhoneNumber phoneNumber) {
        if (!mPhoneNumbers.contains(phoneNumber)) {
            mPhoneNumbers.add(phoneNumber);
        }
    }

    /**
     * Returns true if this group is selected for a multi-operation.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
    }

    /**
     * Get all groups
     */
    public static ArrayList<Group> getGroups(Context context) {
        ArrayList<Group> groups = new ArrayList<Group>();

        final String[] projection = new String[] {
                Groups._ID,
                Groups.TITLE,
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.DATA_SET,
                Groups.SUMMARY_COUNT,
        };

        final String selection = Groups.ACCOUNT_TYPE + " NOT NULL AND "
                + Groups.ACCOUNT_NAME + " NOT NULL AND "
                + Groups.AUTO_ADD + "=0 AND "
                + Groups.DELETED + "=0 AND "
                + Groups.SUMMARY_COUNT + "!=0";

        final String sort = Groups.ACCOUNT_TYPE + ", "
                + Groups.ACCOUNT_NAME + ", "
                + Groups.DATA_SET + ", "
                + Groups.TITLE + " COLLATE LOCALIZED ASC";

        final Cursor cursor = context.getContentResolver().query(Groups.CONTENT_SUMMARY_URI,
                projection, selection, null, sort);

        if (cursor == null) {
            return null;
        }

        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            Group group = new Group(context, cursor);
            groups.add(group);
        }
        cursor.close();

        return groups;
    }

    public static Group get(Context context, long id) {
        final String[] projection = new String[] {
                Groups._ID,
                Groups.TITLE,
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.DATA_SET,
                Groups.SUMMARY_COUNT,
        };

        final String selection = Groups.ACCOUNT_TYPE + " NOT NULL AND "
                + Groups.ACCOUNT_NAME + " NOT NULL AND "
                + Groups.AUTO_ADD + "=0 AND "
                + Groups.DELETED + "=0 AND "
                + Groups.SUMMARY_COUNT + "!=0"
                + " AND " + Groups._ID + " = " + id;

        final Cursor cursor = context.getContentResolver().query(Groups.CONTENT_SUMMARY_URI,
                projection, selection, null, null);

        if (cursor == null) {
            return null;
        }

        if (cursor.getCount() != 1) {
            // No match found
            cursor.close();
            return null;
        }

        // Match found, create and return result
        Group group = new Group(context, cursor);
        cursor.close();
        return group;
    }
}
