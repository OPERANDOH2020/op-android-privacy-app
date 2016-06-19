package eu.operando.operandoapp.database.model;

/**
 * Created by periklismaravelias on 04/06/16.
 */
public class PendingNotification {
    public String app_info, permission, app_name;
    public int id;

    public PendingNotification(String app_info, String permission, int id){
        //whole info
        this.app_info = app_info;
        //only permission
        this.permission = permission;
        //only app name
        this.app_name = app_info.replaceAll("\\(.+?\\)", "") ;
        //notification id
        this.id = id;
    }
}
