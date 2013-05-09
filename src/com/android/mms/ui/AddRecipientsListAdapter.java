package com.android.mms.ui;

import android.content.Context;
import android.provider.UserDictionary;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.R;
import com.android.mms.data.Group;
import com.android.mms.data.PhoneNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AddRecipientsListAdapter extends ArrayAdapter<AddRecipientsListItem> implements SectionIndexer {
        private static final String TAG = "AddRecipientsListAdapter";
        private final LayoutInflater mFactory;
        private HashMap<String, Integer> alphaIndexer;
        private String[] sections;

        public AddRecipientsListAdapter(Context context, List<AddRecipientsListItem> items) {
            super(context, R.layout.add_recipients_list_item, items);
            mFactory = LayoutInflater.from(context);

            alphaIndexer = new HashMap<String, Integer>();
            for (int i = 0; i < items.size(); i++)
            {
                AddRecipientsListItem item = items.get(i);
                if (!item.isGroup()) {
                    String s = item.getPhoneNumber().getName().substring(0, 1).toUpperCase();
                    if (!alphaIndexer.containsKey(s)) {
                        alphaIndexer.put(s, i);
                    }
                }
            }

            Set<String> sectionLetters = alphaIndexer.keySet();
            ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);
            Collections.sort(sectionList);
            sections = new String[sectionList.size()];
            for (int i = 0; i < sectionList.size(); i++) {
                sections[i] = sectionList.get(i);
            }
        }

        public View getView(int position, View convertView, ViewGroup viewGroup){
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
            final AddRecipientsListItem item = this.getItem(position);

            PhoneNumber phoneNumber = item.getPhoneNumber();
            Group group = item.getGroup();

            boolean showSeparator;

            if (!item.isGroup()) {
                showSeparator = alphaIndexer.containsValue(position);
                view.bind(getContext(), phoneNumber, showSeparator);
            } else {
                showSeparator = (position == 0);
                view.bind(getContext(), group, showSeparator);
            }

        }

        @Override
        public int getPositionForSection(int section) {
            return alphaIndexer.get(sections[section]);
        }

        @Override
        public int getSectionForPosition(int position) {
            return 1;
        }

        @Override
        public Object[] getSections() {
            return sections;
        }
}
