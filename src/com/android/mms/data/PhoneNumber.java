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

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Preferences;
import android.provider.Settings;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.ui.AddRecipientsList;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * An interface for finding information about phone numbers
 */
public class PhoneNumber {
    private static final String TAG = "Mms/PhoneNumber";

    private static final String[] PROJECTION = new String[] {
        Phone._ID,
        Phone.NUMBER,
        Phone.TYPE,
        Phone.LABEL,
        Phone.DISPLAY_NAME_PRIMARY,
        Phone.IS_SUPER_PRIMARY,
        Phone.CONTACT_ID
    };

    private static final String[] PROJECTION_ALT = new String[] {
        Phone._ID,
        Phone.NUMBER,
        Phone.TYPE,
        Phone.LABEL,
        Phone.DISPLAY_NAME_ALTERNATIVE,
        Phone.IS_SUPER_PRIMARY,
        Phone.CONTACT_ID
    };

    private static final String SELECTION = Phone.NUMBER + " NOT NULL";

    private static final String SORT_COMMON =
            "CASE WHEN " + Phone.IS_SUPER_PRIMARY + "=0 THEN 1 ELSE 0 END";

    private static final String SORT = Phone.DISPLAY_NAME_PRIMARY + ", " + SORT_COMMON;
    private static final String SORT_ALT = Phone.DISPLAY_NAME_ALTERNATIVE + ", " + SORT_COMMON;

    private static final int COLUMN_ID               = 0;
    private static final int COLUMN_NUMBER           = 1;
    private static final int COLUMN_TYPE             = 2;
    private static final int COLUMN_LABEL            = 3;
    private static final int COLUMN_DISPLAY_NAME     = 4;
    private static final int COLUMN_IS_SUPER_PRIMARY = 5;
    private static final int COLUMN_CONTACT_ID       = 6;

    private long mId;
    private String mNumber;
    private int mType;
    private String mLabel;
    private String mName;
    private boolean mIsDefault;
    private boolean mIsFirst;
    private long mContactId;
    private ArrayList<Group> mGroups;
    private boolean mIsChecked;

    private PhoneNumber(Context context, Cursor c) {
        mId = c.getLong(COLUMN_ID);
        mNumber = c.getString(COLUMN_NUMBER);
        mType = c.getInt(COLUMN_TYPE);
        mLabel = c.getString(COLUMN_LABEL);
        mName = c.getString(COLUMN_DISPLAY_NAME);
        mContactId = c.getLong(COLUMN_CONTACT_ID);
        mIsDefault = c.getInt(COLUMN_IS_SUPER_PRIMARY) != 0;
        mGroups = new ArrayList<Group>();
        mIsFirst = true;

        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            Log.d(TAG, "Create phone number: recipient=" + mName + ", recipientId="
                + mId + ", recipientNumber=" + mNumber);
        }
    }

    public long getId() {
        return mId;
    }

    public String getNumber() {
        return mNumber;
    }

    public int getType() {
        return mType;
    }

    public String getLabel() {
        return mLabel;
    }

    public String getName() {
        return mName;
    }

    public boolean isDefault() {
        return mIsDefault;
    }

    public boolean isFirst() {
        return mIsFirst;
    }

    public long getContactId() {
        return mContactId;
    }

    public ArrayList<Group> getGroups() {
        return mGroups;
    }

    public void addGroup(Group group) {
        if (!mGroups.contains(group)) {
            mGroups.add(group);
        }
    }

    /**
     * Returns true if this phone number is selected for a multi-operation.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
    }

    /**
     * The primary key of a recipient is its number
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhoneNumber) {
            PhoneNumber other = (PhoneNumber) obj;
            return mNumber.equals(other.mNumber);
        }
        return false;
    }

    /**
     * Get all possible recipients (groups and contacts with phone number(s) only)
     * @param context
     * @return all possible recipients
     */
    public static ArrayList<PhoneNumber> getPhoneNumbers(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        boolean useAlternative = Settings.System.getInt(resolver, Preferences.DISPLAY_ORDER,
                Preferences.DISPLAY_ORDER_PRIMARY) == Preferences.DISPLAY_ORDER_ALTERNATIVE;
        final Cursor cursor = resolver.query(Phone.CONTENT_URI,
                useAlternative ? PROJECTION_ALT : PROJECTION,
                SELECTION, null, useAlternative ? SORT_ALT : SORT);

        if (cursor == null) {
            return null;
        }

        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }

        // Load the required preference values and get things ready
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mobileOnly = prefs.getBoolean(AddRecipientsList.MOBILE_NUMBERS_ONLY, true);
        ArrayList<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();
        final HashSet<Long> contactIds = new HashSet<Long>();

        // Add the phone numbers to the list
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (mobileOnly && cursor.getInt(COLUMN_TYPE) != Phone.TYPE_MOBILE) {
                continue;
            }
            PhoneNumber number = new PhoneNumber(context, cursor);
            if (contactIds.contains(number.mContactId)) {
                number.mIsFirst = false;
            }
            contactIds.add(number.mContactId);
            phoneNumbers.add(number);
        }
        cursor.close();

        return phoneNumbers;
    }
}
