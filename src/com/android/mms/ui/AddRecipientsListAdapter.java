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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import com.android.mms.R;
import com.android.mms.data.Group;
import com.android.mms.data.PhoneNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AddRecipientsListAdapter extends ArrayAdapter<AddRecipientsListItem>
        implements SectionIndexer {
    private final LayoutInflater mFactory;
    private HashMap<String, Integer> mAlphaIndexer;
    private String[] mSections;

    public AddRecipientsListAdapter(Context context, List<AddRecipientsListItem> items) {
        super(context, R.layout.add_recipients_list_item, items);
        mFactory = LayoutInflater.from(context);

        mAlphaIndexer = new HashMap<String, Integer>();
        for (int i = 0; i < items.size(); i++) {
            AddRecipientsListItem item = items.get(i);
            if (!item.isGroup()) {
                String name = item.getPhoneNumber().getName();
                String s = name.substring(0, 1).toUpperCase();

                if (!mAlphaIndexer.containsKey(s)) {
                    mAlphaIndexer.put(s, i);
                }
            }
        }

        Set<String> sectionLetters = mAlphaIndexer.keySet();
        ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);
        Collections.sort(sectionList);

        mSections = sectionList.toArray(new String[sectionList.size()]);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        AddRecipientsListItem view;

        if (convertView == null) {
            view = (AddRecipientsListItem) mFactory.inflate(
                    R.layout.add_recipients_list_item, viewGroup, false);
        } else {
            if (convertView instanceof AddRecipientsListItem) {
                view = (AddRecipientsListItem) convertView;
            } else {
                return convertView;
            }
        }

        bindView(position, view);
        return view;
    }

    private void bindView(int position, AddRecipientsListItem view) {
        final AddRecipientsListItem item = getItem(position);

        if (!item.isGroup()) {
            PhoneNumber phoneNumber = item.getPhoneNumber();
            PhoneNumber lastNumber = position != 0
                    ? getItem(position - 1).getPhoneNumber() : null;
            PhoneNumber nextNumber = position != getCount() - 1
                    ? getItem(position + 1).getPhoneNumber() : null;
            long contactId = phoneNumber.getContactId();
            long lastContactId = lastNumber != null ? lastNumber.getContactId() : -1;
            long nextContactId = nextNumber != null ? nextNumber.getContactId() : -1;

            boolean showHeader = mAlphaIndexer.containsValue(position);
            boolean showFooter = contactId != nextContactId;
            boolean isFirst = contactId != lastContactId;

            view.bind(getContext(), phoneNumber, showHeader, showFooter, isFirst);
        } else {
            view.bind(getContext(), item.getGroup(), position == 0);
        }
    }

    @Override
    public int getPositionForSection(int section) {
        return mAlphaIndexer.get(mSections[section]);
    }

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    @Override
    public Object[] getSections() {
        return mSections;
    }
}
