package com.android.mms.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.android.mms.LogTag;

public class GMembership {
    private static final String TAG = "Mms/GMembership";
    private static final boolean DEBUG = false;

    private static final int _ID                = 0;
    private static final int GM_CONTACT_ID      = 1;
    private static final int GM_GROUP_ID        = 2;

    private long mId;                      // The ID of the groupMembership
    private long mContactId;               // The ID of the contact
    private long mGroupId;                 // The ID of the group

    private GMembership(Context context, long id, long contactId, long groupId) {
        mId = id;
        mContactId = contactId;
        mGroupId = groupId;
    }

    private GMembership(Context context, Cursor cursor) {
        if (DEBUG) {
            Log.v(TAG, "Recipient constructor cursor");
        }

        fillFromCursor(context, this, cursor);
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
     * Fill the specified groupMembership with the values from the specified
     * cursor
     */
    private static void fillFromCursor(Context context, GMembership groupMembership, Cursor c) {
        groupMembership.mId = c.getLong(_ID);
        groupMembership.mContactId = c.getLong(GM_CONTACT_ID);
        groupMembership.mGroupId = c.getLong(GM_GROUP_ID);

        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            Log.d(TAG, "fillFromCursor: recipient=" + groupMembership.mId
                    + ", groupId=" + groupMembership.mGroupId
                    + ", contactId=" + groupMembership.mId);
        }
    }

    /**
     * Get all groupMembership
     * @param context
     * @return all groupMembership
     */
    public static ArrayList<GMembership> getGroupMemberships(Context context) {
        ArrayList<GMembership> groupMemberships = new ArrayList<GMembership>();

        final String[] groupMembershipProjection = new String[] {
                GroupMembership._ID,
                GroupMembership.CONTACT_ID,
                GroupMembership.GROUP_ROW_ID
        };

        final String groupMembershipSelection = 
                GroupMembership.MIMETYPE + " = '" + GroupMembership.CONTENT_ITEM_TYPE + "'";

        final Cursor groupMembershipCursor = context.getContentResolver().query(Data.CONTENT_URI,
                groupMembershipProjection, groupMembershipSelection, null, null);

        if (groupMembershipCursor == null) {
            return null;
        }

        final int groupMembershipCount = groupMembershipCursor.getCount();
        if (groupMembershipCount == 0) {
            groupMembershipCursor.close();
            return null;
        }

        for (int i = 0; i < groupMembershipCount; i++) {
            groupMembershipCursor.moveToPosition(i);

            GMembership groupMembership = new GMembership(context, groupMembershipCursor);

            groupMemberships.add(groupMembership);
        }

        groupMembershipCursor.close();

        return groupMemberships;
    }
}
