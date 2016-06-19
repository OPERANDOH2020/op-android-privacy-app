/*
 * Copyright (c) 2016 {UPRC}.
 *
 * OperandoApp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OperandoApp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OperandoApp.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *       Nikos Lykousas {UPRC}, Constantinos Patsakis {UPRC}
 * Initially developed in the context of OPERANDO EU project www.operando.eu
 */

package eu.operando.operandoapp.util;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import org.bouncycastle.crypto.prng.RandomGenerator;

import java.util.Date;
import java.util.Random;
import java.util.Set;

import eu.operando.operandoapp.MainContext;
import eu.operando.operandoapp.R;
import eu.operando.operandoapp.database.DatabaseHelper;
import eu.operando.operandoapp.database.model.PendingNotification;
import eu.operando.operandoapp.service.ProxyService;
import eu.operando.operandoapp.service.receivers.NotificationActivityReceiver;

/**
 * Created by nikos on 5/6/16.
 */
public class NotificationUtil {

    public void displayExfiltratedNotification(Context context, String applicationInfo, Set<RequestFilterUtil.FilterType> exfiltrated, int mainNotificationId) {
        try {
            StringBuilder sb = new StringBuilder();
            for (RequestFilterUtil.FilterType f : exfiltrated) {
                sb.append(f + ", ");
            }
            sb.delete(sb.length() - 2, sb.length() - 1);

            RemoteViews smallContentView = new RemoteViews(context.getPackageName(), R.layout.proxy_notification_small);
            smallContentView.setImageViewResource(R.id.image, R.drawable.logo_bevel);
            smallContentView.setTextViewText(R.id.titleTxtView, "Personal Information");
            smallContentView.setTextViewText(R.id.subtitleTxtView, "Expand for more info");

            RemoteViews bigContentView = new RemoteViews(context.getPackageName(), R.layout.proxy_notification_large);
            bigContentView.setImageViewResource(R.id.image, R.drawable.logo_bevel);
            bigContentView.setTextViewText(R.id.titleTxtView, "Personal Information");
            bigContentView.setTextViewText(R.id.subtitleTxtView, applicationInfo.replaceAll("\\s\\(.*?\\)", "") + " requires access to " + sb);
            bigContentView.setTextViewText(R.id.allowBtn, "Allow");
            bigContentView.setTextViewText(R.id.blockBtn, "Block");

            //get exfiltrated info to string array
            String[] exfiltrated_array = new String[exfiltrated.size()];
            int i = 0;
            for (RequestFilterUtil.FilterType filter_type : exfiltrated){
                exfiltrated_array[i] = filter_type.name();
                i++;
            }

            //set listeners for notification buttons

            Intent allowIntent = new Intent(context, NotificationActivityReceiver.class);
            allowIntent.setAction("allow");
            Bundle allowBundle = new Bundle();
            allowBundle.putString("appInfo", applicationInfo);
            allowBundle.putInt("notificationId", mainNotificationId);
            allowBundle.putStringArray("exfiltrated", exfiltrated_array);
            allowIntent.putExtras(allowBundle);
            PendingIntent pendingAllowIntent = PendingIntent.getBroadcast(context, mainNotificationId + 1, allowIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            bigContentView.setOnClickPendingIntent(R.id.allowBtn, pendingAllowIntent);

            Intent blockIntent = new Intent(context, NotificationActivityReceiver.class);
            blockIntent.setAction("block");
            Bundle blockBundle = new Bundle();
            blockBundle.putString("appInfo", applicationInfo);
            blockBundle.putInt("notificationId", mainNotificationId);
            blockBundle.putStringArray("exfiltrated", exfiltrated_array);
            blockIntent.putExtras(blockBundle);
            PendingIntent pendingBlockIntent = PendingIntent.getBroadcast(context, mainNotificationId + 2, blockIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            bigContentView.setOnClickPendingIntent(R.id.blockBtn, pendingBlockIntent);

            Notification.Builder mBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.logo_bevel)
                    .setContent(smallContentView);
            Notification proxyNotification = mBuilder.build();
            proxyNotification.defaults |= Notification.DEFAULT_ALL;
            proxyNotification.bigContentView = bigContentView;

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(mainNotificationId, proxyNotification);
            DatabaseHelper db = new DatabaseHelper(context);
            db.addPendingNotification(new PendingNotification(applicationInfo, TextUtils.join(", ", exfiltrated_array), mainNotificationId));
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }

    }
}
