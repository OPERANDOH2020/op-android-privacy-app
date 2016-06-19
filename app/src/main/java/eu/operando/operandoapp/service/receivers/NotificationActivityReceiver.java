package eu.operando.operandoapp.service.receivers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import eu.operando.operandoapp.database.DatabaseHelper;
import eu.operando.operandoapp.database.model.AllowedDomain;
import eu.operando.operandoapp.database.model.BlockedDomain;
import eu.operando.operandoapp.database.model.PendingNotification;
import eu.operando.operandoapp.util.RequestFilterUtil;

/**
 * Created by periklismaravelias on 31/05/16.
 */
public class NotificationActivityReceiver extends BroadcastReceiver {

    private String addAllowedSuccessful = "Added to Allowed Domains Successfully",
                   addAllowedUnsuccessfull = "This domain already exists in your allowed domain list",
                   addBlockedSuccessfull = "Added to Blocked Domains Successfully",
                   addBlockedUnsuccessfull = "This domain already exists in your blocked domain list";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            Bundle choiceBundle = intent.getExtras();
            String choice = intent.getAction();
            if (choice != null){
                DatabaseHelper db = new DatabaseHelper(context);
                if (choice.equals("allow")){
                    boolean add = true;
                    for (String s : choiceBundle.getStringArray("exfiltrated")) {
                        add = add && db.addAllowedDomain(choiceBundle.getString("appInfo"), s);
                    }
                    Toast.makeText(context, add ? addAllowedSuccessful : addAllowedUnsuccessfull, Toast.LENGTH_SHORT).show();
                    NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                    int id = choiceBundle.getInt("notificationId");
                    nm.cancel(id);
                    db.removePendingNotification(id);
                    db.sendSettingsToServer(new RequestFilterUtil(context).getIMEI());
                } else if (choice.equals("block")){
                    boolean add = true;
                    for (String s : choiceBundle.getStringArray("exfiltrated")) {
                        add = add && db.addBlockedDomain(choiceBundle.getString("appInfo"), s);
                    }
                    Toast.makeText(context, add ? addBlockedSuccessfull : addBlockedUnsuccessfull, Toast.LENGTH_SHORT).show();
                    NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                    int id = choiceBundle.getInt("notificationId");
                    nm.cancel(id);
                    db.removePendingNotification(id);
                    db.sendSettingsToServer(new RequestFilterUtil(context).getIMEI());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
