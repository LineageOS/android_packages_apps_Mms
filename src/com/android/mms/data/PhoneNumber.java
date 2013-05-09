package com.android.mms.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

import com.android.mms.LogTag;

/**
 * An interface for finding information about recipients
 */
public class PhoneNumber {
    private static final String TAG = "Mms/recipient";
    private static final boolean DEBUG = false;

    private static final int _ID                = 0;
    private static final int PHONE_NUMBER       = 1;
    private static final int PHONE_TYPE         = 2;
    private static final int PHONE_LABEL        = 3;
    private static final int PHONE_DISPLAY_NAME = 4;
    private static final int PHONE_IS_PRIMARY   = 5;
    private static final int PHONE_CONTACT_ID   = 6;

    private long mId;
    private String mNumber;                     // The number of the recipient.
    private int mType;                          // The type of the number of the recipient.
    private String mLabel;                      // The label of the number of the recipient.
    private String mName;                       // The name of the recipient.
    private String mIsPrimary;                  // non-0 if number is the primary of the contact.
    private long mContactId;                    // The ID of the contact
    private ArrayList<Group> mGroups;           // The groups of the contact
    private boolean mIsChecked;                 // True if user has selected the recipient for a
                                                // multi-operation such as add recipient.

    private PhoneNumber(Context context, long id, String number, int type, String label,
                      String name, String isPrimary, long contactId, ArrayList<Group> groups, boolean isChecked) {
        mId = id;
        mNumber = number;
        mType = type;
        mLabel = label;
        mName = name;
        mIsPrimary = isPrimary;
        mContactId = contactId;
        mGroups = groups;
        mIsChecked = isChecked;
    }

    private PhoneNumber(Context context, Cursor cursor) {
        if (DEBUG) {
            Log.v(TAG, "Recipient constructor cursor");
        }

        fillFromCursor(context, this, cursor);
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

    public String getIsPrimary() {
        return mIsPrimary;
    }

    public long getContactId() {
        return mContactId;
    }

    public Contact getContact() {
        return Contact.get(mNumber, false);
    }

    public ArrayList<Group> getGroups() {
        return mGroups;
    }

    public void addGroup(Group group) {
        mGroups.add(group);
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

    /*
     * The primary key of a recipient is its number
     */
    @Override
    public boolean equals(Object obj) {
        try {
            PhoneNumber other = (PhoneNumber)obj;
            return (mNumber.equals(other.mNumber));
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Fill the specified phoneNumber with the values from the specified
     * cursor
     */
    private static void fillFromCursor(Context context, PhoneNumber phoneNumber,
                                       Cursor c) {
        phoneNumber.mId = c.getLong(_ID);
        phoneNumber.mNumber = c.getString(PHONE_NUMBER);
        phoneNumber.mType = c.getInt(PHONE_TYPE);
        phoneNumber.mLabel = c.getString(PHONE_LABEL);
        phoneNumber.mIsPrimary = c.getString(PHONE_IS_PRIMARY);
        phoneNumber.mName = c.getString(PHONE_DISPLAY_NAME);
        phoneNumber.mContactId = c.getLong(PHONE_CONTACT_ID);
        phoneNumber.mGroups = new ArrayList<Group>();

        if (Log.isLoggable(LogTag.THREAD_CACHE, Log.VERBOSE)) {
            Log.d(TAG, "fillFromCursor: recipient=" + phoneNumber + ", recipientId=" + phoneNumber.mId
                    + ", recipientNumber=" + phoneNumber.mNumber);
        }
    }

    /**
     * Get all possible recipients (groups and contacts with phone number(s) only)
     * @param context
     * @return all possible recipients
     */
    public static ArrayList<PhoneNumber> getPhoneNumbers(Context context) {
        ArrayList<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();

        final String[] phonesProjection = new String[] {
            Phone._ID,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.DISPLAY_NAME,
            Phone.IS_PRIMARY,
            Phone.CONTACT_ID
        };

        final String phonesSelection = Phone.NUMBER + " NOT NULL";

        final String phonesSort = Phone.DISPLAY_NAME + ", "
              + "CASE WHEN " + Phone.IS_PRIMARY + "=0 THEN 1 ELSE 0 END";

        final Cursor phonesCursor = context.getContentResolver().query(Phone.CONTENT_URI,
                phonesProjection, phonesSelection, null, phonesSort);

        if (phonesCursor == null) {
            return null;
        }

        final int phonesCount = phonesCursor.getCount();
        if (phonesCount == 0) {
            phonesCursor.close();
            return null;
        }

        for (int i = 0; i < phonesCount; i++) {
            phonesCursor.moveToPosition(i);

            PhoneNumber recipient = new PhoneNumber(context, phonesCursor);

            phoneNumbers.add(recipient);
        }

        phonesCursor.close();

        return phoneNumbers;
    }

    public static PhoneNumber get(Context context, String number) {
        ArrayList<PhoneNumber> phoneNumbers = getPhoneNumbers(context);

        int count = phoneNumbers.size();
        for (int i = 0; i < count; i++) {
            PhoneNumber phoneNumber = phoneNumbers.get(i);
            if (phoneNumber.mNumber == number) {
                return phoneNumber;
            }
        }

        return null;
    }

    public static PhoneNumber from(Context context, Cursor cursor) {
        String number = cursor.getString(PHONE_NUMBER);

        return get(context, number);
    }
}
