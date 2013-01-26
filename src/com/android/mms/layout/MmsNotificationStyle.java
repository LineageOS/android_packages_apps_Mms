package com.android.mms.layout;

import android.app.PendingIntent;
import android.content.Context;
import android.widget.RemoteViews;

import com.android.mms.R;

import java.util.ArrayList;

public class MmsNotificationStyle {

    private ArrayList<Action> mActions = new ArrayList<Action>();
    private Context mContext;

    private static class Action {
        int drawableRes;
        PendingIntent intent;
    }

    public MmsNotificationStyle(Context context) {
        mContext = context;
    }

    public void addAction(int drawable, PendingIntent intent) {
        Action toAdd = new Action();
        toAdd.drawableRes = drawable;
        toAdd.intent = intent;
        mActions.add(toAdd);
    }

    public void restyleNotificationActionButtons(RemoteViews bigView, int buttonLayout) {
        bigView.removeAllViews(com.android.internal.R.id.actions);
        for (Action a : mActions) {
            RemoteViews button = new RemoteViews(mContext.getPackageName(), buttonLayout);
            button.setOnClickPendingIntent(R.id.action0, a.intent);
            button.setImageViewResource(R.id.action0, a.drawableRes);
            bigView.addView(com.android.internal.R.id.actions, button);
        }
    }

}
