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

package com.android.mms.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.mms.R;
import com.android.mms.data.Group;
import com.android.mms.data.GroupMembership;
import com.android.mms.data.PhoneNumber;

public class AddRecipientsList extends ListActivity {
    private static final String TAG = "AddRecipientsList";

    public static boolean mIsRunning;

    private AddRecipientsListAdapter mListAdapter;
    private int mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
    private int mSavedFirstItemOffset;
    private ArrayList<PhoneNumber> mPhoneNumbers;
    private ArrayList<Group> mGroups;
    private ArrayList<GroupMembership> mGroupMemberships;
    private ArrayList<PhoneNumber> mCheckedPhoneNumbers;
    private static final int MENU_DONE = 0;
    private Menu mMenu;

    // Keys for extras and icicles
    private final static String LAST_LIST_POS = "last_list_pos";
    private final static String LAST_LIST_OFFSET = "last_list_offset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_recipients_list_screen);

        // List
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setFastScrollEnabled(true);
        listView.setFastScrollAlwaysVisible(true);
        listView.setDivider(null);
        listView.setDividerHeight(0);

        // Tell the list view which view to display when the list is empty
        listView.setEmptyView(findViewById(R.id.empty));
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                AddRecipientsListItem item =
                        (AddRecipientsListItem) adapter.getItemAtPosition(position);

                if (item.isGroup()) {
                    Group group = item.getGroup();
                    checkGroup(group, !group.isChecked());
                } else {
                    PhoneNumber phoneNumber = item.getPhoneNumber();
                    checkPhoneNumber(phoneNumber, !phoneNumber.isChecked());
                }

                if (mMenu != null) {
                    mMenu.findItem(MENU_DONE).setVisible(mCheckedPhoneNumbers.size() > 0);
                }

                mListAdapter.notifyDataSetChanged();
            }
         });

        initListAdapter();

        if (mListAdapter == null) {
            ((TextView)(listView.getEmptyView())).setText(R.string.no_recipients);
        }

        if (savedInstanceState != null) {
            mSavedFirstVisiblePosition =
                    savedInstanceState.getInt(LAST_LIST_POS, AdapterView.INVALID_POSITION);
            mSavedFirstItemOffset = savedInstanceState.getInt(LAST_LIST_OFFSET, 0);
        } else {
            mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
            mSavedFirstItemOffset = 0;
        }

        ActionBar mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LAST_LIST_POS, mSavedFirstVisiblePosition);
        outState.putInt(LAST_LIST_OFFSET, mSavedFirstItemOffset);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Remember where the list is scrolled to so we can restore the scroll position
        // when we come back to this activity and *after* we complete querying for the
        // contacts.
        ListView listView = getListView();
        mSavedFirstVisiblePosition = listView.getFirstVisiblePosition();
        View firstChild = listView.getChildAt(0);
        mSavedFirstItemOffset = (firstChild == null) ? 0 : firstChild.getTop();
        mIsRunning = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        mMenu.add(0, MENU_DONE, 0, R.string.menu_done)
             .setIcon(R.drawable.ic_menu_done_holo_light)
             .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT)
             .setVisible(false);
        return super.onCreateOptionsMenu(mMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case MENU_DONE:
                int count = mCheckedPhoneNumbers.size();
                String[] resultData = new String[count];
                for (int i = 0; i < count; i++) {
                    PhoneNumber phoneNumber = mCheckedPhoneNumbers.get(i);
                    if (phoneNumber.isChecked()) {
                        resultData[i] = phoneNumber.getNumber();
                    }
                }

                Intent intent = new Intent();
                intent.putExtra("com.android.mms.ui.AddRecipients", resultData);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkPhoneNumber(PhoneNumber phoneNumber, boolean check) {
        phoneNumber.setChecked(check);

        if (check) {
            if (!mCheckedPhoneNumbers.contains(phoneNumber)) {
                mCheckedPhoneNumbers.add(phoneNumber);
            }
        } else {
            if (mCheckedPhoneNumbers.contains(phoneNumber)) {
                mCheckedPhoneNumbers.remove(phoneNumber);
            }

            ArrayList<Group> phoneGroups = phoneNumber.getGroups();
            int count = phoneGroups.size();
            for (int i = 0; i < count; i++) {
                Group group = phoneGroups.get(i);
                if (group.isChecked()) {
                    group.setChecked(false);
                }
            }
        }
    }

    private void checkGroup(Group group, boolean check) {
        group.setChecked(check);
        ArrayList<PhoneNumber> phoneNumbers = group.getPhoneNumbers();
        int count = phoneNumbers.size();

        for (int i = 0; i < count; i++) {
            PhoneNumber phoneNumber = phoneNumbers.get(i);
            if (phoneNumber.isDefault() || phoneNumber.isFirst()) {
                checkPhoneNumber(phoneNumber, check);
            }
        }
    }


    private void initListAdapter() {
        mPhoneNumbers = PhoneNumber.getPhoneNumbers(this);

        if (mPhoneNumbers == null) {
            return;
        }

        mCheckedPhoneNumbers = new ArrayList<PhoneNumber>();
        mGroups = Group.getGroups(this);
        mGroupMemberships = GroupMembership.getGroupMemberships(this);

        Map<Long,ArrayList<Long>> groupIdWithContactsId = new HashMap<Long, ArrayList<Long>>();

        // Store GID with all its CIDs
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
        for (PhoneNumber phoneNumber : mPhoneNumbers ) {
            long cid = phoneNumber.getContactId();

            Iterator<Long> iterator = groupIdWithContactsId.keySet().iterator();
            while (iterator.hasNext()) {
                long gid = (Long)iterator.next();
                if (groupIdWithContactsId.get(gid).contains(cid)) {
                    for (Group group : mGroups) {
                        if (group.getId() == gid) {
                            group.addPhoneNumber(phoneNumber);
                            phoneNumber.addGroup(group);
                        }
                    }
                }
            }
        }

        // TODO: fix this
        int phoneNumbersCount = mPhoneNumbers.size();
        int groupsCount = mGroups.size();
        ArrayList<AddRecipientsListItem> items = new ArrayList<AddRecipientsListItem>();
        for (int i = 0; i < groupsCount; i++) {
            Group group = mGroups.get(i);
            items.add(i, new AddRecipientsListItem(this, group));
        }

        for (int i = 0; i < phoneNumbersCount; i++) {
            PhoneNumber phoneNumber = mPhoneNumbers.get(i);
            items.add(i + groupsCount, new AddRecipientsListItem(this, phoneNumber));
        }

        mListAdapter = new AddRecipientsListAdapter(this, items);
        setListAdapter(mListAdapter);
    }
}
