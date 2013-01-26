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

package com.android.mms.layout;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.widget.RemoteViews;

import com.android.mms.R;

import java.util.ArrayList;

public class MmsNotificationStyle {

    private ArrayList<Action> mActions = new ArrayList<Action>();
    private final Context mContext;
    private final Notification.Builder mBuilder;

    private static class Action {
        int drawableRes;
        CharSequence title;
        PendingIntent intent;
    }

    public MmsNotificationStyle(Context context, Notification.Builder builder) {
        mContext = context;
        mBuilder = builder;
    }

    public void addAction(int drawable, CharSequence title, PendingIntent intent) {
        mBuilder.addAction(drawable, title, intent);
        Action toAdd = new Action();
        toAdd.drawableRes = drawable;
        toAdd.title = title;
        toAdd.intent = intent;
        mActions.add(toAdd);
    }

    public void restyleNotificationActionButtons(RemoteViews bigView, int buttonLayout) {
        bigView.removeAllViews(com.android.internal.R.id.actions);
        for (Action a : mActions) {
            RemoteViews button = new RemoteViews(mContext.getPackageName(), buttonLayout);
            if (a.intent != null) {
                button.setOnClickPendingIntent(R.id.action0, a.intent);
            }
            button.setImageViewResource(R.id.action0, a.drawableRes);
            button.setContentDescription(R.id.action0, a.title);
            bigView.addView(com.android.internal.R.id.actions, button);
        }
    }

}
