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

package eu.operando.operandoapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;

import org.apache.commons.codec.digest.DigestUtils;
import org.xmlpull.v1.XmlSerializer;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import eu.operando.operandoapp.MainContext;
import eu.operando.operandoapp.database.model.AllowedDomain;
import eu.operando.operandoapp.database.model.BlockedDomain;
import eu.operando.operandoapp.database.model.DomainFilter;
import eu.operando.operandoapp.database.model.FilterFile;
import eu.operando.operandoapp.database.model.PendingNotification;
import eu.operando.operandoapp.database.model.ResponseFilter;
import eu.operando.operandoapp.database.model.TrustedAccessPoint;
import eu.operando.operandoapp.util.RequestFilterUtil;

/**
 * Created by nikos on 11/5/2016.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    //region Variable Declaration

    // Logcat tag
    private static final String LOG = "DatabaseHelper";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "operando.db";

    // Table Names
    private static final String TABLE_RESPONSE_FILTERS = "response_filters";
    private static final String TABLE_DOMAIN_FILTERS = "domain_filters";
    private static final String TABLE_ALLOWED_DOMAINS = "allowed_domains";
    private static final String TABLE_BLOCKED_DOMAINS = "blocked_domains";
    private static final String TABLE_PENDING_NOTIFICATIONS = "pending_notifications";
    private static final String TABLE_TRUSTED_ACCESS_POINTS = "trusted_access_points";
    private static final String TABLE_STATISTICS = "statistics";

    //column names
    private static final String KEY_ID = "id";
    private static final String KEY_MODIFIED = "modified";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_COUNT = "filtercount";
    private static final String KEY_WILDCARD = "iswildcard";
    private static final String KEY_APP_INFO = "app_info";
    private static final String KEY_APP_NAME = "app_name";
    private static final String KEY_NOTIFICATION_ID = "notification_id";
    private static final String KEY_PERMISSIONS = "permissions";
    private static final String KEY_SSID = "ssid";
    private static final String KEY_BSSID = "bssid";
    private static final String KEY_PHONENUMBER = "phonenumber";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMSI = "imsi";
    private static final String KEY_CARRIERNAME = "carriername";
    private static final String KEY_ANDROIDID = "androidid";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_CONTACTSINFO = "contactsinfo";
    private static final String KEY_MACADDRESSES = "macaddresses";

    //server url
    public static final String serverUrl = "http://snf-713867.vm.okeanos.grnet.gr:5000/";

    //endregion

    //region Table Creation

    private static final String CREATE_TABLE_RESPONSE_FILTERS = "CREATE TABLE "
            + TABLE_RESPONSE_FILTERS + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_CONTENT
            + " TEXT," + KEY_SOURCE + " TEXT," + KEY_MODIFIED
            + " DATETIME" + ")";
    private static final String CREATE_TABLE_DOMAIN_FILTERS = "CREATE TABLE "
            + TABLE_DOMAIN_FILTERS + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_CONTENT
            + " TEXT," + KEY_SOURCE + " TEXT," + KEY_WILDCARD + " INTEGER," + KEY_MODIFIED
            + " DATETIME" + ")";
    private static final String CREATE_TABLE_ALLOWED_DOMAINS = "CREATE TABLE "
            + TABLE_ALLOWED_DOMAINS + "(" + KEY_APP_INFO
            + " TEXT," + KEY_PERMISSIONS + " TEXT, PRIMARY KEY (" + KEY_APP_INFO + ", " + KEY_PERMISSIONS + "))";
    private static final String CREATE_TABLE_BLOCKED_DOMAINS = "CREATE TABLE "
            + TABLE_BLOCKED_DOMAINS + "(" + KEY_APP_INFO
            + " TEXT," + KEY_PERMISSIONS + " TEXT, PRIMARY KEY (" + KEY_APP_INFO + ", " + KEY_PERMISSIONS + "))";
    private static final String CREATE_TABLE_PENDING_NOTIFICATIONS = "CREATE TABLE "
            + TABLE_PENDING_NOTIFICATIONS + "(" + KEY_APP_INFO + " TEXT PRIMARY KEY," + KEY_APP_NAME
            + " TEXT," + KEY_PERMISSIONS + " TEXT," + KEY_NOTIFICATION_ID + " INTEGER)";
    private static final String CREATE_TABLE_TRUSTED_ACCESS_POINTS = "CREATE TABLE "
            + TABLE_TRUSTED_ACCESS_POINTS + "(" + KEY_SSID + " TEXT PRIMARY KEY," + KEY_BSSID + " TEXT" + ")";
    private static final String CREATE_TABLE_STATISTICS = "CREATE TABLE "
            + TABLE_STATISTICS + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_PHONENUMBER + " INTEGER,"
            + KEY_IMEI + " INTEGER," + KEY_IMSI + " INTEGER," + KEY_CARRIERNAME + " INTEGER,"
            + KEY_ANDROIDID + " INTEGER," + KEY_LOCATION + " INTEGER," + KEY_CONTACTSINFO + " INTEGER,"
            + KEY_MACADDRESSES + " INTEGER)";
    private int LIMIT = 500;

    //endregion

    //region DatabaseHelper Special Functions

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        try {
            // creating required tables
            db.execSQL(CREATE_TABLE_RESPONSE_FILTERS);
            db.execSQL(CREATE_TABLE_DOMAIN_FILTERS);
            db.execSQL(CREATE_TABLE_ALLOWED_DOMAINS);
            db.execSQL(CREATE_TABLE_BLOCKED_DOMAINS);
            db.execSQL(CREATE_TABLE_PENDING_NOTIFICATIONS);
            db.execSQL(CREATE_TABLE_TRUSTED_ACCESS_POINTS);
            db.execSQL(CREATE_TABLE_STATISTICS);
            db.execSQL("INSERT INTO " + TABLE_STATISTICS + " VALUES (null, 0, 0, 0, 0, 0, 0, 0, 0)");
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSE_FILTERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOMAIN_FILTERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALLOWED_DOMAINS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKED_DOMAINS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING_NOTIFICATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + CREATE_TABLE_TRUSTED_ACCESS_POINTS);
        db.execSQL("DROP TABLE IF EXISTS " + CREATE_TABLE_STATISTICS);

        // create new tables
        onCreate(db);
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    //endregion

    //region Response Filters

    public int createResponseFilter(ResponseFilter responseFilter) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CONTENT, responseFilter.getContent().trim());
        values.put(KEY_SOURCE, responseFilter.getSource());
        values.put(KEY_MODIFIED, getDateTime());

        // insert row
        int id = (int) db.insert(TABLE_RESPONSE_FILTERS, null, values);

        return id;
    }

    public ResponseFilter getResponseFilter(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_RESPONSE_FILTERS + " WHERE "
                + KEY_ID + " = " + id;


        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        ResponseFilter responseFilter = new ResponseFilter();
        responseFilter.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        responseFilter.setContent((c.getString(c.getColumnIndex(KEY_CONTENT))));
        responseFilter.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
        responseFilter.setModified(c.getString(c.getColumnIndex(KEY_MODIFIED)));

        return responseFilter;
    }

    public List<ResponseFilter> getAllResponseFilters() {
        List<ResponseFilter> responseFilters = new ArrayList<ResponseFilter>();
        String selectQuery = "SELECT  * FROM " + TABLE_RESPONSE_FILTERS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                ResponseFilter responseFilter = new ResponseFilter();
                responseFilter.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                responseFilter.setContent((c.getString(c.getColumnIndex(KEY_CONTENT))));
                responseFilter.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
                responseFilter.setModified(c.getString(c.getColumnIndex(KEY_MODIFIED)));

                responseFilters.add(responseFilter);
            } while (c.moveToNext());
        }

        return responseFilters;
    }

    public List<ResponseFilter> getAllUserResponseFilters() {
        List<ResponseFilter> responseFilters = new ArrayList<ResponseFilter>();
        String selectQuery = "SELECT  * FROM " + TABLE_RESPONSE_FILTERS + " WHERE " + KEY_SOURCE + " IS NULL ";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                ResponseFilter responseFilter = new ResponseFilter();
                responseFilter.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                responseFilter.setContent((c.getString(c.getColumnIndex(KEY_CONTENT))));
                responseFilter.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
                responseFilter.setModified(c.getString(c.getColumnIndex(KEY_MODIFIED)));

                responseFilters.add(responseFilter);
            } while (c.moveToNext());
        }

        return responseFilters;
    }

    public List<FilterFile> getAllResponseFilterFiles() {
        List<FilterFile> filterFiles = new ArrayList<>();
        String selectQuery = "SELECT " + KEY_SOURCE + ", COUNT(*) AS " + KEY_COUNT + " FROM " + TABLE_RESPONSE_FILTERS
                + " WHERE " + KEY_SOURCE + " IS NOT NULL GROUP BY " + KEY_SOURCE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        Log.e("tag", selectQuery);
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {

                FilterFile filterFile = new FilterFile();
                filterFile.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
                filterFile.setFilterCount(c.getInt(c.getColumnIndex(KEY_COUNT)));
                Log.e("tag", filterFile.toString());
                Log.e("tag", filterFile.getTitle());
                filterFiles.add(filterFile);
            } while (c.moveToNext());
        }

        return filterFiles;
    }

    public List<String> getAllResponseFiltersForSource(String source) {
        List<String> filters = new ArrayList<>();
        String selectQuery = "SELECT " + KEY_CONTENT + " FROM " + TABLE_RESPONSE_FILTERS
                + " WHERE " + KEY_SOURCE + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, new String[]{source});

        int count = 0;

        if (c.moveToFirst()) {
            do {
                filters.add(c.getString(c.getColumnIndex(KEY_CONTENT)));
                count++;
            } while (c.moveToNext() && count < LIMIT);
        }

        if (count == LIMIT) {
            filters.add("--- Omitted " + (c.getCount() - LIMIT) + " entries ---");
        }

        return filters;
    }

    public int getResponseFilterCount() {
        String countQuery = "SELECT  * FROM " + TABLE_RESPONSE_FILTERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();

        // return count
        return count;
    }

    public int updateResponseFilter(ResponseFilter responseFilter) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CONTENT, responseFilter.getContent().trim());
        values.put(KEY_SOURCE, responseFilter.getSource());
        values.put(KEY_MODIFIED, getDateTime());

        // updating row
        return db.update(TABLE_RESPONSE_FILTERS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(responseFilter.getId())});
    }

    public int deleteResponseFilter(ResponseFilter responseFilter) {
        return deleteResponseFilter(responseFilter.getId());
    }

    public int deleteResponseFilter(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_RESPONSE_FILTERS, KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public int deleteResponseFilterFile(FilterFile filterFile) {
        return deleteResponseFilterFile(filterFile.getSource());
    }

    public int deleteResponseFilterFile(String source) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_RESPONSE_FILTERS, KEY_SOURCE + " = ?",
                new String[]{source});
    }

    //endregion

    //region Domain Filters

    public int createDomainFilter(DomainFilter domainFilter) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CONTENT, domainFilter.getContent().trim());
        values.put(KEY_SOURCE, domainFilter.getSource());
        values.put(KEY_WILDCARD, domainFilter.getWildcard());
        values.put(KEY_MODIFIED, getDateTime());

        // insert row
        int id = (int) db.insert(TABLE_DOMAIN_FILTERS, null, values);

        return id;
    }

    public DomainFilter getDomainFilter(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT  * FROM " + TABLE_DOMAIN_FILTERS + " WHERE "
                + KEY_ID + " = " + id;

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        DomainFilter domainFilter = new DomainFilter();
        domainFilter.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        domainFilter.setContent((c.getString(c.getColumnIndex(KEY_CONTENT))));
        domainFilter.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
        domainFilter.setModified(c.getString(c.getColumnIndex(KEY_MODIFIED)));
        domainFilter.setWildcard((c.getInt(c.getColumnIndex(KEY_WILDCARD))));

        return domainFilter;
    }

    public List<DomainFilter> getAllDomainFilters() {
        List<DomainFilter> domainFilters = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_DOMAIN_FILTERS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                DomainFilter domainFilter = new DomainFilter();
                domainFilter.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                domainFilter.setContent((c.getString(c.getColumnIndex(KEY_CONTENT))));
                domainFilter.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
                domainFilter.setModified(c.getString(c.getColumnIndex(KEY_MODIFIED)));
                domainFilter.setWildcard((c.getInt(c.getColumnIndex(KEY_WILDCARD))));
                domainFilters.add(domainFilter);
            } while (c.moveToNext());
        }

        return domainFilters;
    }

    public List<DomainFilter> getAllUserDomainFilters() {
        List<DomainFilter> domainFilters = new ArrayList<DomainFilter>();
        String selectQuery = "SELECT  * FROM " + TABLE_DOMAIN_FILTERS + " WHERE " + KEY_SOURCE + " IS NULL ";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                DomainFilter domainFilter = new DomainFilter();
                domainFilter.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                domainFilter.setContent((c.getString(c.getColumnIndex(KEY_CONTENT))));
                domainFilter.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
                domainFilter.setModified(c.getString(c.getColumnIndex(KEY_MODIFIED)));
                domainFilter.setWildcard((c.getInt(c.getColumnIndex(KEY_WILDCARD))));
                domainFilters.add(domainFilter);
            } while (c.moveToNext());
        }

        return domainFilters;
    }

    public List<FilterFile> getAllDomainFilterFiles() {
        List<FilterFile> filterFiles = new ArrayList<>();
        String selectQuery = "SELECT " + KEY_SOURCE + ", COUNT(*) AS " + KEY_COUNT + " FROM " + TABLE_DOMAIN_FILTERS
                + " WHERE " + KEY_SOURCE + " IS NOT NULL GROUP BY " + KEY_SOURCE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        if (c.moveToFirst()) {
            do {

                FilterFile filterFile = new FilterFile();
                filterFile.setSource((c.getString(c.getColumnIndex(KEY_SOURCE))));
                filterFile.setFilterCount(c.getInt(c.getColumnIndex(KEY_COUNT)));
                filterFiles.add(filterFile);
            } while (c.moveToNext());
        }

        return filterFiles;
    }

    public List<String> getAllDomainFiltersForSource(String source) {
        List<String> filters = new ArrayList<>();
        String selectQuery = "SELECT " + KEY_CONTENT + " FROM " + TABLE_DOMAIN_FILTERS
                + " WHERE " + KEY_SOURCE + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, new String[]{source});

        int count = 0;

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                filters.add(c.getString(c.getColumnIndex(KEY_CONTENT)));
                count++;
            } while (c.moveToNext() && count < LIMIT);
        }

        if (count == LIMIT) {
            filters.add("--- Omitted " + (c.getCount() - LIMIT) + " entries ---");
        }
        return filters;
    }

    //https://stackoverflow.com/questions/5451285/sqlite-select-query-with-like-condition-in-reverse
    public boolean isDomainBlocked(String domain){
        /*String selectQuery = "SELECT * FROM " + TABLE_DOMAIN_FILTERS
                + " WHERE ((" + KEY_WILDCARD + " = 0 AND " + KEY_CONTENT + " LIKE '%" + domain + "') OR"
                        + "(" + KEY_WILDCARD + " = 1 AND ? LIKE '%' || " + KEY_CONTENT + " || '%'))";*/
        String selectQuery = "SELECT * FROM " + TABLE_DOMAIN_FILTERS
                + " WHERE ? LIKE '%' || " + KEY_CONTENT + " || '%'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery(selectQuery, new String[]{domain});
        }catch(Exception e){
            e.getMessage();
        }
        int count = c.getCount();
        c.close();
        return (count > 0);
    }

    public int getDomainFilterCount() {
        String countQuery = "SELECT  * FROM " + TABLE_DOMAIN_FILTERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();

        // return count
        return count;
    }

    public int updateDomainFilter(DomainFilter domainFilter) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CONTENT, domainFilter.getContent().trim());
        values.put(KEY_SOURCE, domainFilter.getSource());
        values.put(KEY_WILDCARD, domainFilter.getWildcard());
        values.put(KEY_MODIFIED, getDateTime());

        // updating row
        return db.update(TABLE_DOMAIN_FILTERS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(domainFilter.getId())});
    }

    public int deleteDomainFilter(DomainFilter domainFilter) {
        return deleteDomainFilter(domainFilter.getId());
    }

    public int deleteDomainFilter(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_DOMAIN_FILTERS, KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public int deleteDomainFilterFile(FilterFile filterFile) {
        return deleteDomainFilterFile(filterFile.getSource());
    }

    public int deleteDomainFilterFile(String source) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_DOMAIN_FILTERS, KEY_SOURCE + " = ?",
                new String[]{source});
    }

    //endregion

    //region Pending Notifications

    /*
    ------------------------------------------------------------------
    Pending Notification
    ------------------------------------------------------------------
     */
    public List<PendingNotification> getAllPendingNotifications(){
        List<PendingNotification> pendingNotifications = new ArrayList<>();
        try{
            String selectQuery = "SELECT * FROM " + TABLE_PENDING_NOTIFICATIONS;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                do {
                    PendingNotification pendingNotification = new PendingNotification(
                            c.getString(c.getColumnIndex(KEY_APP_INFO)),
                            c.getString(c.getColumnIndex(KEY_PERMISSIONS)),
                            c.getInt(c.getColumnIndex(KEY_NOTIFICATION_ID))
                    );
                    pendingNotifications.add(pendingNotification);
                } while (c.moveToNext());
            }
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }
        return pendingNotifications;
    }

    public boolean addPendingNotification(PendingNotification pendingNotification){
        String appInfo = pendingNotification.app_info;
        String permissions = pendingNotification.permission;
        String appName = pendingNotification.app_name;
        int notificationId = pendingNotification.id;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_APP_INFO, appInfo);
        values.put(KEY_APP_NAME, appName);
        values.put(KEY_PERMISSIONS, permissions);
        values.put(KEY_NOTIFICATION_ID, notificationId);
        int id = (int) db.insert(TABLE_PENDING_NOTIFICATIONS, null, values);
        return id >= 0;
    }

    public boolean removePendingNotification(int notificationId){
        SQLiteDatabase db = this.getWritableDatabase();
        int id = (int) db.delete(TABLE_PENDING_NOTIFICATIONS, KEY_NOTIFICATION_ID + "='" + notificationId + "'", null);
        return id >= 0;
    }

    //endregion

    //region Allowed Domains & Apps

    /*
    ------------------------------------------------------------------
    Allowed Domain
    ------------------------------------------------------------------
     */

    public List<AllowedDomain> getAllAllowedDomains() {
        List<AllowedDomain> allowedDomains = new ArrayList<>();
        try {
            String selectQuery = "SELECT * FROM " + TABLE_ALLOWED_DOMAINS;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                do {
                    AllowedDomain allowedDomain = new AllowedDomain(c.getString(c.getColumnIndex(KEY_APP_INFO)), c.getString(c.getColumnIndex(KEY_PERMISSIONS)));
                    allowedDomains.add(allowedDomain);
                } while (c.moveToNext());
            }
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }
        return allowedDomains;
    }

    public List<String[]> getAllPermissionsPerAllowedDomain() {
        List<String[]> result = new ArrayList<>();
        try {
            String KEY_CONCAT_PERMISSIONS = "concat_permissions";
            String selectQuery =
                    "SELECT " + KEY_APP_INFO + ", GROUP_CONCAT(" + KEY_PERMISSIONS + ") AS " + KEY_CONCAT_PERMISSIONS +
                    " FROM (" +
                        "SELECT " + KEY_APP_INFO + ", " + KEY_PERMISSIONS +
                        " FROM " + TABLE_ALLOWED_DOMAINS +
                        " ORDER BY " + KEY_APP_INFO + ", " + KEY_PERMISSIONS +
                    ") " +
                    "GROUP BY " + KEY_APP_INFO;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                do {
                    result.add(new String[] {c.getString(c.getColumnIndex(KEY_APP_INFO)), c.getString(c.getColumnIndex(KEY_CONCAT_PERMISSIONS))});
                } while (c.moveToNext());
            }
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }
        return result;
    }

    public boolean addAllowedDomain(String domain, String exfiltrated) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_APP_INFO, domain.trim());
        values.put(KEY_PERMISSIONS, exfiltrated);
        int id = (int) db.insert(TABLE_ALLOWED_DOMAINS, null, values);
        return id >= 0;
    }

    public boolean removeAllowedDomain(String domain, String exfiltrated){
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int id = (int) db.delete(TABLE_ALLOWED_DOMAINS, KEY_APP_INFO + " LIKE '%" + domain + "%' AND " + KEY_PERMISSIONS + "='" + exfiltrated + "'", null);
            return true;
        } catch (Exception e){
            e.getMessage();
            return false;
        }
    }

    //endregion

    //region Blocked Domains & Apps

    /*
    ------------------------------------------------------------------
    Blocked Domain
    ------------------------------------------------------------------
     */

    public List<BlockedDomain> getAllBlockedDomains(){
        List<BlockedDomain> blockedDomains = new ArrayList<>();
        try {
            String selectQuery = "SELECT * FROM " + TABLE_BLOCKED_DOMAINS;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                do {
                    BlockedDomain blockedDomain = new BlockedDomain(c.getString(c.getColumnIndex(KEY_APP_INFO)), c.getString(c.getColumnIndex(KEY_PERMISSIONS)));
                    blockedDomains.add(blockedDomain);
                } while (c.moveToNext());
            }
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }
        return blockedDomains;
    }

    public List<String[]> getAllPermissionsPerBlockedDomain(){
        List<String[]> result = new ArrayList<>();
        try {
            String KEY_CONCAT_PERMISSIONS = "concat_permissions";
            String selectQuery =
                    "SELECT " + KEY_APP_INFO + ", GROUP_CONCAT(" + KEY_PERMISSIONS + ") AS " + KEY_CONCAT_PERMISSIONS +
                            " FROM (" +
                            "SELECT " + KEY_APP_INFO + ", " + KEY_PERMISSIONS +
                            " FROM " + TABLE_BLOCKED_DOMAINS +
                            " ORDER BY " + KEY_APP_INFO + ", " + KEY_PERMISSIONS +
                            ") " +
                            "GROUP BY " + KEY_APP_INFO;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                do {
                    result.add(new String[] {c.getString(c.getColumnIndex(KEY_APP_INFO)), c.getString(c.getColumnIndex(KEY_CONCAT_PERMISSIONS))});
                } while (c.moveToNext());
            }
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }
        return result;
    }

    public boolean addBlockedDomain(String domain, String exfiltrated) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_APP_INFO, domain.trim());
        values.put(KEY_PERMISSIONS, exfiltrated);
        int id = (int) db.insert(TABLE_BLOCKED_DOMAINS, null, values);
        return id >= 0;
    }

    public boolean removeBlockedDomain(String domain, String exfiltrated){
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int id = (int) db.delete(TABLE_BLOCKED_DOMAINS, KEY_APP_INFO + " LIKE '%" + domain + "%' AND " + KEY_PERMISSIONS + "='" + exfiltrated + "'", null);
            return id > 0;
        } catch (Exception e){
            return false;
        }
    }

    //endregion

    //region Trusted Access Points

    public List<TrustedAccessPoint> getAllTrustedAccessPoints(){
        List<TrustedAccessPoint> trustedAccessPoints = new ArrayList<>();
        try {
            String selectQuery = "SELECT * FROM " + TABLE_TRUSTED_ACCESS_POINTS;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                do {
                    TrustedAccessPoint trustedAccessPoint = new TrustedAccessPoint(c.getString(c.getColumnIndex(KEY_SSID)), c.getString(c.getColumnIndex(KEY_BSSID)));
                    trustedAccessPoints.add(trustedAccessPoint);
                } while (c.moveToNext());
            }
        } catch (Exception e){
            Log.d("ERROR", e.getMessage());
        }
        return trustedAccessPoints;
    }

    public boolean addTrustedAccessPoint(TrustedAccessPoint tap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SSID, tap.ssid);
        values.put(KEY_BSSID, tap.bssid);
        int id = (int) db.insert(TABLE_TRUSTED_ACCESS_POINTS, null, values);
        return id >= 0;
    }

    public boolean removeTrustedAccessPoint(TrustedAccessPoint tap){
        SQLiteDatabase db = this.getWritableDatabase();
        int id = (int) db.delete(TABLE_TRUSTED_ACCESS_POINTS, KEY_SSID + "='" + tap.ssid + "' AND " + KEY_BSSID + "='" + tap.bssid + "'", null);
        return id > 0;
    }

    //endregion

    //region Send Settings To Server

    public void sendSettingsToServer(final String imei){
        new Thread(new Runnable(){
            @Override
            public void run() {
                String xml_settings = exportAllPermissionsPerDomain();
                if (xml_settings != null){
                    try {
                        String urlParameters = "userxml=" + xml_settings + "&userID=" + Base64.encode(DigestUtils.sha1(imei), Base64.DEFAULT);
                        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                        URL url = new URL(serverUrl + "/prefs");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(15000);
                        conn.setConnectTimeout(15000);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                            wr.write( postData );
                        }
                        int responseCode=conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK){
                            //Do something on HTTP_OK ?????
                        }
                        else{
                            throw new Exception();
                        }
                        Log.d("SENT", Integer.toString(responseCode));
                } catch (Exception e){
                        //create a file as database to resend later
                        Writer writer = null;
                        try {
                            File file = new File(MainContext.INSTANCE.getContext().getFilesDir(), "resend.inf");
                            if (!file.exists()){
                                writer = new BufferedWriter(new FileWriter(file));
                                writer.write("1"); //currently putting 1 for "true" to resend
                            }
                        }catch(Exception ex){
                            ex.getMessage();
                        }finally{
                            try{
                                writer.close();
                            }catch(Exception ex){
                                ex.getMessage();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public String exportAllPermissionsPerDomain(){
        List<String[]> result1 = getAllPermissionsPerAllowedDomain();
        List<String[]> result2 = getAllPermissionsPerBlockedDomain();
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter sw = new StringWriter();
        try{
            serializer.setOutput(sw);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "settings");
            //allowed
            serializer.startTag("", "allowed");
            for (String[] s : result1){
                serializer.startTag("", "app");
                serializer.attribute("", "app_name", s[0].replaceAll("\\(.*?\\)", ""));
                serializer.startTag("", "app_info");
                serializer.text(s[0]);
                serializer.endTag("", "app_info");
                serializer.startTag("", "permissions");
                serializer.text(s[1]);
                serializer.endTag("", "permissions");
                serializer.endTag("", "app");
            }
            serializer.endTag("", "allowed");
            //blocked
            serializer.startTag("", "blocked");
            for (String[] s : result2){
                serializer.startTag("", "app");
                serializer.attribute("", "app_name", s[0].replaceAll("\\(.*?\\)", ""));
                serializer.startTag("", "app_info");
                serializer.text(s[0]);
                serializer.endTag("", "app_info");
                serializer.startTag("", "permissions");
                serializer.text(s[1]);
                serializer.endTag("", "permissions");
                serializer.endTag("", "app");
            }
            serializer.endTag("", "blocked");
            serializer.endTag("", "settings");
            serializer.endDocument();
            serializer.flush();
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        return sw.toString();
    }

    //endregion

    //region Statistics

    public List<String> getStatistics(){
        SQLiteDatabase db = DatabaseHelper.this.getWritableDatabase();
        List<String> result = new ArrayList();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_STATISTICS + " WHERE " + KEY_ID + " = 1", null);
        if (c.moveToFirst()) {
            for (int i = 1; i < c.getColumnCount(); i++) { //omit column 0
                result.add(filterName(c.getColumnName(i)) + ": " + c.getInt(i) + (c.getInt(i) == 1 ? " time." : " times."));
            }
        }
        return result;
    }

    private String filterName(String filter){
        String result = "";
        switch(filter){
            case KEY_CONTACTSINFO:
                result = "Contacts Data";
                break;
            case KEY_IMEI:
                result = "IMEI";
                break;
            case KEY_PHONENUMBER:
                result = "Phone Number";
                break;
            case KEY_IMSI:
                result = "Device Id";
                break;
            case KEY_CARRIERNAME:
                result = "Carrier Name";
                break;
            case KEY_LOCATION:
                result = "Location Information";
                break;
            case KEY_ANDROIDID:
                result = "Android Id";
                break;
            case KEY_MACADDRESSES:
                result = "Mac Addresses";
                break;
        }
        return result;
    }

    public void updateStatistics(final Set<RequestFilterUtil.FilterType> exfiltrated){
        new Thread(new Runnable(){
            @Override
            public void run() {
                for (RequestFilterUtil.FilterType filter : exfiltrated){
                    String column = "";
                    switch(RequestFilterUtil.getDescriptionForFilterType(filter)){
                        case "Contacts Data":
                            column = KEY_CONTACTSINFO;
                            break;
                        case "IMEI":
                            column = KEY_IMEI;
                            break;
                        case "Phone Number":
                            column = KEY_PHONENUMBER;
                            break;
                        case "Device Id":
                            column = KEY_IMSI;
                            break;
                        case "Carrier Name":
                            column = KEY_CARRIERNAME;
                            break;
                        case "Location Information":
                            column = KEY_LOCATION;
                            break;
                        case "Android Id":
                            column = KEY_ANDROIDID;
                            break;
                        case "Mac Addresses":
                            column = KEY_MACADDRESSES;
                            break;
                    }
                    if (!column.equals("")){
                        SQLiteDatabase db = DatabaseHelper.this.getWritableDatabase();
                        try {
                            db.execSQL("UPDATE " + TABLE_STATISTICS + " SET " + column + " = " + column + "+1 WHERE " + KEY_ID + "=1");
                        }catch(SQLException sqle){
                            sqle.getMessage();
                        }
                    }
                }
            }
        }).start();
    }

    //endregion

}