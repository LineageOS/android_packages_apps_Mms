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
import java.util.HashMap;
import java.util.Map;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.android.mms.ui.AddRecipientsListItem;

public class RecipientsListLoader extends AsyncTaskLoader<ArrayList<AddRecipientsListItem>> {

    private ArrayList<PhoneNumber> mPhoneNumbers;
    private ArrayList<Group> mGroups;
    private ArrayList<GroupMembership> mGroupMemberships;
    private Context mContext;
    private ArrayList<AddRecipientsListItem> mRecipientsList;

    public RecipientsListLoader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public ArrayList<AddRecipientsListItem> loadInBackground() {
        mPhoneNumbers = PhoneNumber.getPhoneNumbers(mContext);
        if (mPhoneNumbers == null) {
            return null;
        }

        // Get things ready
        mGroups = Group.getGroups(mContext);
        mGroupMemberships = GroupMembership.getGroupMemberships(mContext);
        Map<Long, ArrayList<Long>> groupIdWithContactsId = new HashMap<Long, ArrayList<Long>>();
        ArrayList<AddRecipientsListItem> recipientsList = new ArrayList<AddRecipientsListItem>();

        // Store GID with all its CIDs
        if (mGroups != null && mGroupMemberships != null) {
            for (GroupMembership membership : mGroupMemberships) {
                Long gid = membership.getGroupId();
                Long uid = membership.getContactId();

                if (!groupIdWithContactsId.containsKey(gid)) {
                    groupIdWithContactsId.put(gid, new ArrayList<Long>());
                }

                if (!groupIdWithContactsId.get(gid).contains(uid)) {
                    groupIdWithContactsId.get(gid).add(uid);
                }
            }

            // For each PhoneNumber, find its GID, and add it to correct Group
            for (PhoneNumber phoneNumber : mPhoneNumbers) {
                long cid = phoneNumber.getContactId();

                for (Map.Entry<Long, ArrayList<Long>> entry : groupIdWithContactsId.entrySet()) {
                    if (!entry.getValue().contains(cid)) {
                        continue;
                    }
                    for (Group group : mGroups) {
                        if (group.getId() == entry.getKey()) {
                            group.addPhoneNumber(phoneNumber);
                            phoneNumber.addGroup(group);
                        }
                    }
                }
            }

            // Add the groups to the list first
            for (Group group : mGroups) {
                recipientsList.add(new AddRecipientsListItem(mContext, group));
            }
        }

        // Add phone numbers to the list
        for (PhoneNumber phoneNumber : mPhoneNumbers) {
            recipientsList.add(new AddRecipientsListItem(mContext, phoneNumber));
        }

        // We are done
        return recipientsList;
    }

    // Called when there is new data to deliver to the client.  The
    // super class will take care of delivering it; the implementation
    // here just adds a little more logic.
    @Override
    public void deliverResult(ArrayList<AddRecipientsListItem> recipientsList) {
        mRecipientsList = recipientsList;

        if (isStarted()) {
            // If the Loader is started, immediately deliver its results.
            super.deliverResult(recipientsList);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mRecipientsList != null) {
            // If we currently have a result available, deliver it immediately.
            deliverResult(mRecipientsList);
        }

        if (takeContentChanged() || mRecipientsList == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(ArrayList<AddRecipientsListItem> recipientsList) {
        super.onCanceled(recipientsList);
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated if needed.
        if (mRecipientsList != null) {
            mRecipientsList = null;
        }
    }
}
