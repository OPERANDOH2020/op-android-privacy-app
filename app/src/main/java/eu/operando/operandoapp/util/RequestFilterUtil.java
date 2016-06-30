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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.RequiresPermission;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.operando.operandoapp.MainContext;
import eu.operando.operandoapp.R;
import eu.operando.operandoapp.util.LocationHelper.GPSTracker;

/**
 * Created by nikos on 23/5/2016.
 */
public class RequestFilterUtil {
    Context context;
    TelephonyManager telephonyManager;
    GPSTracker gpsTracker;

    String[] contactsInfo;
    String IMEI, phoneNumber, subscriberID, carrierName, androidID;
    String[] macAddresses;

    public RequestFilterUtil(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        this.gpsTracker = new GPSTracker(context);
        this.contactsInfo = genContactsInfo();
        this.IMEI = genIMEI();
        this.phoneNumber = genPhoneNumber();
        this.subscriberID = genSubscriberID();
        this.carrierName = genCarrierName();
        this.androidID = genAndroidID();
        this.macAddresses = genMacAddresses();
    }

    //region Generate Phone Info

    public String genIMEI(){
        return telephonyManager.getDeviceId();
    }

    public String genPhoneNumber(){
        return telephonyManager.getLine1Number();
    }

    public String genSubscriberID() {
        return telephonyManager.getSubscriberId();
    }

    public String genCarrierName(){
        return telephonyManager.getNetworkOperatorName();
    }

    public String genAndroidID(){
        return android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    }

    public String[] genContactsInfo() {
        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        Set<String> ret = new HashSet<>();
        while (phones.moveToNext()) {
            //String name=phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
            String normalizedNumber2 = PhoneNumberUtils.stripSeparators(phoneNumber);
            //ret.add(name);
            ret.add(phoneNumber);
            ret.add(normalizedNumber);
            ret.add(normalizedNumber2);
        }
        phones.close();
        return ret.toArray(new String[ret.size()]);
    }

    public String[] genMacAddresses(){
        List<String> result = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("/proc/net/arp")));
            //--omit first line--
            String firstLine = br.readLine();
            //-------------------
            String line;
            while((line = br.readLine()) != null) {
                result.add(line.split("\\s+")[3]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toArray(new String[result.size()]);
    }

    //endregion

    //region Getters

    public String[] getContactsInfo() {
        return contactsInfo;
    }

    public String getIMEI() {
        return this.IMEI;
    }

    public String getPhoneNumber() {
        if (this.phoneNumber.equals("")){
            return ReadFile(new File(context.getFilesDir(), "phonenumber.conf"));
        }
        return this.phoneNumber;
    }

    public String ReadFile(File f){
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
        }
        catch (IOException e) {
            Log.d("ERROR", e.getMessage());
        }
        return text.toString();
    }

    public String getSubscriberID() {
        return this.subscriberID;
    }

    public String getCarrierName() {
        return this.carrierName;
    }

    public String getAndroidID() {
        return this.androidID;
    }

    public String[] getLocationInfo() {
        List<String> ret = new ArrayList<>();
        for (String loc : gpsTracker.getLocations()) {
            if (loc.length() >= 10)
                loc = loc.substring(0, loc.length() - 4);
            ret.add(loc);
        }
        return ret.toArray(new String[ret.size()]);
    }

    public String[] getMacAddresses(){
        return this.macAddresses;
    }

    //endregion

    //region Filter Enumerator

    public enum FilterType {
        CONTACTS,
        IMEI,
        PHONENUMBER,
        IMSI,
        CARRIERNAME,
        ANDROIDID,
        LOCATION,
        MACADRESSES
    }

    //endregion

    //region Filter Description

    public static String getDescriptionForFilterType(FilterType filterType) {
        switch (filterType) {
            case CONTACTS:
                return "Contacts Data";
            case IMEI:
                return "IMEI";
            case PHONENUMBER:
                return "Phone Number";
            case IMSI:
                return "Device Id";
            case CARRIERNAME:
                return "Carrier Name";
            case LOCATION:
                return "Location Information";
            case ANDROIDID:
                return "Android Id";
            case MACADRESSES:
                return "Mac Addresses";
            default:
                return "Undefined";
        }
    }

    //endregion

    public static String messageForMatchedFilters(Set<FilterType> exfiltrated) {
        StringBuilder message = new StringBuilder();
        for (RequestFilterUtil.FilterType filterType : exfiltrated) {
            if (message.length() > 0) {
                message.append(", ");
            }
            message.append(RequestFilterUtil.getDescriptionForFilterType(filterType));
        }
        return message.toString();
    }

    @Deprecated
    public static String[] genDummyForArray(String[] arr) {
        String[] dummy = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            String str = arr[i];
            dummy[i] = str.replaceAll("\\d", "0");
        }
        return dummy;
    }
}
