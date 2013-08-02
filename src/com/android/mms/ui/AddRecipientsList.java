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
import java.util.HashSet;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.mms.R;
import com.android.mms.data.Group;
import com.android.mms.data.PhoneNumber;
import com.android.mms.data.RecipientsListLoader;

public class AddRecipientsList extends ListActivity
        implements LoaderManager.LoaderCallbacks<ArrayList<AddRecipientsListItem>> {
    private static final String TAG = "AddRecipientsList";

    private static final int MENU_DONE = 0;
    private static final int MENU_MOBILE = 1;

    public static final String MOBILE_NUMBERS_ONLY = "pref_key_mobile_numbers_only";

    private AddRecipientsListAdapter mListAdapter;
    private int mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
    private int mSavedFirstItemOffset;
    private HashSet<PhoneNumber> mCheckedPhoneNumbers;
    private Menu mMenu;
    private boolean mMobileOnly = true;
    private LinearLayout mProgressSpinner;

    // Keys for extras and icicles
    private final static String LAST_LIST_POS = "last_list_pos";
    private final static String LAST_LIST_OFFSET = "last_list_offset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_recipients_list_screen);

        // Progress spinner
        mProgressSpinner = (LinearLayout) findViewById(R.id.progress_spinner);

        // List view
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setFastScrollEnabled(true);
        listView.setFastScrollAlwaysVisible(true);
        listView.setDivider(null);
        listView.setDividerHeight(0);
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

        // Get things ready
        mCheckedPhoneNumbers = new HashSet<PhoneNumber>();
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState != null) {
            mSavedFirstVisiblePosition = savedInstanceState.getInt(LAST_LIST_POS,
                    AdapterView.INVALID_POSITION);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Load the required preference values
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mMobileOnly = sharedPreferences.getBoolean(MOBILE_NUMBERS_ONLY, true);

        mMenu = menu;
        mMenu.add(0, MENU_DONE, 0, R.string.menu_done)
             .setIcon(R.drawable.ic_menu_done_holo_light)
             .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT)
             .setVisible(false);

        mMenu.add(0, MENU_MOBILE, 0, R.string.menu_mobile)
             .setCheckable(true)
             .setChecked(mMobileOnly)
             .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return super.onCreateOptionsMenu(mMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DONE:
                ArrayList<String> numbers = new ArrayList<String>();
                for (PhoneNumber phoneNumber : mCheckedPhoneNumbers) {
                    if (phoneNumber.isChecked()) {
                        numbers.add(phoneNumber.getNumber());
                    }
                }

                // Pass the resulting set of numbers back
                Intent intent = new Intent();
                intent.putExtra("recipients", numbers);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            case MENU_MOBILE:
                // If it was checked before it should be unchecked now and vice versa
                boolean checked = mMobileOnly ? false : true;
                item.setChecked(checked);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                sharedPreferences.edit().putBoolean(MOBILE_NUMBERS_ONLY, checked).commit();

                // Restart the loader to reflect the change
                getLoaderManager().restartLoader(0, null, this);
                mMobileOnly = checked;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkPhoneNumber(PhoneNumber phoneNumber, boolean check) {
        phoneNumber.setChecked(check);

        if (check) {
            mCheckedPhoneNumbers.add(phoneNumber);
        } else {
            mCheckedPhoneNumbers.remove(phoneNumber);
            ArrayList<Group> phoneGroups = phoneNumber.getGroups();
            if (phoneGroups != null) {
                for (Group group : phoneGroups) {
                    if (group.isChecked()) {
                        group.setChecked(false);
                    }
                }
            }
        }
    }

    private void checkGroup(Group group, boolean check) {
        group.setChecked(check);
        ArrayList<PhoneNumber> phoneNumbers = group.getPhoneNumbers();

        if (phoneNumbers != null) {
            for (PhoneNumber phoneNumber : phoneNumbers) {
                if (phoneNumber.isDefault() || phoneNumber.isFirst()) {
                    checkPhoneNumber(phoneNumber, check);
                }
            }
        }
    }

    @Override
    public Loader<ArrayList<AddRecipientsListItem>> onCreateLoader(int arg0, Bundle arg1) {
        // Show the progress indicator
        mProgressSpinner.setVisibility(View.VISIBLE);
        return new RecipientsListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AddRecipientsListItem>> loader,
            ArrayList<AddRecipientsListItem> data) {
        // We have an old list, get rid of it before we start again
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetInvalidated();
        }

        // Hide the progress indicator
        mProgressSpinner.setVisibility(View.GONE);

        // Create and set the list adapter
        mListAdapter = new AddRecipientsListAdapter(this, data);
        if (mListAdapter == null) {
            // We have no numbers to show, indicate it
            ListView listView = getListView();
            ((TextView)(listView.getEmptyView())).setText(
                    mMobileOnly ? R.string.no_recipients_mobile_only
                                : R.string.no_recipients);
        } else {
            setListAdapter(mListAdapter);
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<AddRecipientsListItem>> data) {
        mListAdapter.notifyDataSetInvalidated();
    }
}
