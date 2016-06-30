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

package eu.operando.operandoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.chainsaw.Main;
import org.bouncycastle.operator.OperatorCreationException;
import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.BouncyCastleSslEngineSource;
import org.littleshoot.proxy.mitm.RootCertificateException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import be.shouldit.proxy.lib.APL;
import eu.operando.operandoapp.about.AboutActivity;
import eu.operando.operandoapp.database.DatabaseHelper;
import eu.operando.operandoapp.database.model.DomainFilter;
import eu.operando.operandoapp.database.model.PendingNotification;
import eu.operando.operandoapp.filters.DownloadTask;
import eu.operando.operandoapp.filters.domain.DomainFiltersActivity;
import eu.operando.operandoapp.filters.domain.DomainManagerActivity;
import eu.operando.operandoapp.filters.domain.PermissionsPerDomainActivity;
import eu.operando.operandoapp.filters.response.ResponseFiltersActivity;
import eu.operando.operandoapp.service.ProxyService;
import eu.operando.operandoapp.service.StatisticsActivity;
import eu.operando.operandoapp.settings.SettingActivity;
import eu.operando.operandoapp.settings.Settings;
import eu.operando.operandoapp.settings.ThemeStyle;
import eu.operando.operandoapp.util.CertificateUtil;
import eu.operando.operandoapp.util.Logger;
import eu.operando.operandoapp.util.MainUtil;
import eu.operando.operandoapp.util.RequestFilterUtil;
import eu.operando.operandoapp.wifi.AccessPointsActivity;
import eu.operando.operandoapp.wifi.TrustedAccessPointsActivity;

//proxy status: active, paused, stopped. (ean den exw certs, to isProxyRunning einai false).
//link to proxy: established, non-established
enum OperandoProxyStatus {
    ACTIVE,
    PAUSED,
    STOPPED
}

enum OperandoProxyLink {
    VALID,
    INVALID
}

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    private MainContext mainContext = MainContext.INSTANCE;
    private FloatingActionButton fab = null;
    private WebView webView = null;

    private ThemeStyle currentThemeStyle;

    //Buttons
    private Button WiFiAPButton = null;
    private Button responseFiltersButton = null;
    private Button domainFiltersButton = null;
    private Button domainManagerButton = null;
    private Button permissionsPerDomainButton = null;
    private Button trustedAccessPointsButton = null;
    private Button updateButton = null;
    private Button statisticsButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MainUtil.initializeMainContext(getApplicationContext());
        Settings settings = mainContext.getSettings();
        settings.initializeDefaultValues();
        setCurrentThemeStyle(settings.getThemeStyle());
        setTheme(getCurrentThemeStyle().themeAppCompatStyle());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        settings.registerOnSharedPreferenceChangeListener(this);

        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);

        //region Floating Action Button

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainUtil.isServiceRunning(mainContext.getContext(), ProxyService.class) && !MainUtil.isProxyPaused(mainContext)) {
                    //Update Preferences to BypassProxy
                    MainUtil.setProxyPaused(mainContext, true);
                    fab.setImageResource(android.R.drawable.ic_media_play);
                    //Toast.makeText(mainContext.getContext(), "-- bypass (disable) proxy --", Toast.LENGTH_SHORT).show();
                } else if (MainUtil.isServiceRunning(mainContext.getContext(), ProxyService.class) && MainUtil.isProxyPaused(mainContext)) {

                    MainUtil.setProxyPaused(mainContext, false);
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                    //Toast.makeText(mainContext.getContext(), "-- re-enable proxy --", Toast.LENGTH_SHORT).show();
                } else if (!mainContext.getAuthority().aliasFile(BouncyCastleSslEngineSource.KEY_STORE_FILE_EXTENSION).exists()) {
                    try {
                        installCert();
                    } catch (RootCertificateException | GeneralSecurityException | OperatorCreationException | IOException ex) {
                        Logger.error(this, ex.getMessage(), ex.getCause());
                    }
                }
            }
        });

        //endregion

        //region TabHost

        final TabHost tabHost = (TabHost) findViewById(R.id.tabHost2);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("wifi_ap");
        tabSpec.setContent(R.id.WifiAndAccessPointsScrollView);
        tabSpec.setIndicator("", getResources().getDrawable(R.drawable.ic_home));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("response_domain_filters");
        tabSpec.setContent(R.id.ResponseAndDomainFiltersScrollView);
        tabSpec.setIndicator("", getResources().getDrawable(R.drawable.ic_filter));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("pending_notifications");
        tabSpec.setContent(R.id.PendingNotificationsScrollView);
        tabSpec.setIndicator("", getResources().getDrawable(R.drawable.ic_pending_notification));
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("logs");
        tabSpec.setContent(R.id.LogsScrollView);
        tabSpec.setIndicator("", getResources().getDrawable(R.drawable.ic_report));
        tabHost.addTab(tabSpec);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                switch (tabId){
                    case "pending_notifications":
                        //region Load Tab3
                        ((TableLayout) ((LinearLayout) ((ScrollView) findViewById(R.id.PendingNotificationsScrollView)).getChildAt(0)).getChildAt(1)).removeAllViews();
                        LoadPendingNotificationsTab();
                        //endregion
                        break;
                    case "logs":
                        //region Load Tab 4
                        //because it is a heavy task it is being loaded asynchronously
                        ((TableLayout) ((LinearLayout) ((ScrollView) findViewById(R.id.LogsScrollView)).getChildAt(0)).getChildAt(1)).removeAllViews();
                        new AsyncTask() {
                            private ProgressDialog mProgress;
                            private List<String[]> apps;
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                mProgress = new ProgressDialog(MainActivity.this);
                                mProgress.setCancelable(false);
                                mProgress.setCanceledOnTouchOutside(false);
                                mProgress.setTitle("Fetching Application Data Logs");
                                mProgress.show();
                            }

                            @Override
                            protected Object doInBackground(Object[] params) {
                                apps = new ArrayList();
                                for (String[] app : getInstalledApps(false)){
                                    apps.add(new String[] {app[0], GetDataForApp(Integer.parseInt(app[1]))});
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Object o) {
                                super.onPostExecute(o);
                                mProgress.dismiss();
                                for (String[] app : apps) {
                                    if (app[0].contains(".")) {
                                        continue;
                                    }
                                    TextView tv = new TextView(MainActivity.this);
                                    tv.setTextSize(18);
                                    tv.setText(app[0] + " || " + app[1]);
                                    ((TableLayout) ((LinearLayout) ((ScrollView) findViewById(R.id.LogsScrollView)).getChildAt(0)).getChildAt(1)).addView(tv);

                                    View separator = new View(MainActivity.this);
                                    separator.setBackgroundColor(Color.BLACK);
                                    separator.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 5));
                                    ((TableLayout) ((LinearLayout) ((ScrollView) findViewById(R.id.LogsScrollView)).getChildAt(0)).getChildAt(1)).addView(separator);
                                }
                            }
                        }.execute();
                        //endregion
                        break;
                }
            }
        });

        //endregion

        //region Buttons

        WiFiAPButton = (Button) findViewById(R.id.WiFiAPButton);
        WiFiAPButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent i = new Intent(mainContext.getContext(), AccessPointsActivity.class);
                startActivity(i);
            }
        });

        responseFiltersButton = (Button) findViewById(R.id.responseFiltersButton);
        responseFiltersButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(mainContext.getContext(), ResponseFiltersActivity.class);
                startActivity(i);
            }
        });

        domainFiltersButton = (Button) findViewById(R.id.domainFiltersButton);
        domainFiltersButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(mainContext.getContext(), DomainFiltersActivity.class);
                startActivity(i);
            }
        });

        domainManagerButton = (Button) findViewById(R.id.domainManagerButton);
        domainManagerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(mainContext.getContext(), DomainManagerActivity.class);
                startActivity(i);
            }
        });

        permissionsPerDomainButton = (Button) findViewById(R.id.permissionsPerDomainButton);
        permissionsPerDomainButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(mainContext.getContext(), PermissionsPerDomainActivity.class);
                startActivity(i);
            }
        });

        trustedAccessPointsButton = (Button) findViewById(R.id.trustedAccessPointsButton);
        trustedAccessPointsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(mainContext.getContext(), TrustedAccessPointsActivity.class);
                startActivity(i);
            }
        });

        updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mark first time has not runned and update like it's initial .
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("firstTime", true);
                editor.commit();
                DownloadInitialSettings();
            }
        });

        statisticsButton = (Button) findViewById(R.id.statisticsButton);
        statisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mainContext.getContext(), StatisticsActivity.class);
                startActivity(i);
            }
        });

        //endregion

        //region Action Bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            setTitle(R.string.app_name);
        }

        //endregion

        //region Send Cached Settings
        //send cached settings if exist...
        BufferedReader br = null;
        try {
            File file = new File(MainContext.INSTANCE.getContext().getFilesDir(), "resend.inf");
            StringBuilder content = new StringBuilder();
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            if (content.toString().equals("1")){
                File f = new File(file.getCanonicalPath());
                f.delete();
                new DatabaseHelper(MainActivity.this).sendSettingsToServer(new RequestFilterUtil(MainActivity.this).getIMEI());
            }
        }catch(Exception ex){
            ex.getMessage();
        }finally{
            try{br.close();}catch(Exception ex){ex.getMessage();}
        }
        //endregion

        initializeProxyService();
    }

    //region Overrides

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                startProxyService();
            } else {
                //super.onActivityResult(requestCode, resultCode, data);
                if (mainContext.getAuthority().aliasFile(BouncyCastleSslEngineSource.KEY_STORE_FILE_EXTENSION).exists()) {
                    mainContext.getAuthority().aliasFile(BouncyCastleSslEngineSource.KEY_STORE_FILE_EXTENSION).delete();
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (shouldReload()) {
            reloadActivity();
        } else {
            mainContext.getScanner().update();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainContext.getBUS().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainContext.getBUS().register(this);
        updateStatusView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_menu:
                Intent settingsIntent = new Intent(mainContext.getContext(), SettingActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.apn_menu: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.action_apn);
                builder.setPositiveButton(android.R.string.cancel, null);
                builder.setNegativeButton("Open APN Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent apnIntent = new Intent(android.provider.Settings.ACTION_APN_SETTINGS);
                        apnIntent.putExtra("sub_id", 1); //SubscriptionManager.NAME_SOURCE_SIM_SOURCE
                        startActivity(apnIntent);
                    }
                });
                String message = "In order to enable OperandoApp proxy while using wireless networks (e.g. 3G), you will need to modify the corresponding Access Point configuration for your provider. Please set the following values:\n\nProxy: 127.0.0.1\nPort: 8899";
                builder.setMessage(message);
                builder.create().show();

                return true;
            }
            case R.id.help_menu:
                Toast.makeText(this, "You have selected HELP (To be added).", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.about_menu:
                Intent aboutIntent = new Intent(mainContext.getContext(), AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //endregion

    @Subscribe
    public void onOperandoStatusEvent(OperandoStatusEvent event) {
        updateStatusView();
    }

    private void startProxyService() {
        if (mainContext.getSharedPreferences().getBoolean("proxyPaused", false))
            fab.setImageResource(android.R.drawable.ic_media_play);
        else
            fab.setImageResource(android.R.drawable.ic_media_pause);
        MainUtil.startProxyService(mainContext);
        //when proxy starts, check for phone number
        CheckPhoneNumber();
    }

    private void installCert() throws RootCertificateException, GeneralSecurityException, OperatorCreationException, IOException {

        new AsyncTask<Void, Void, Certificate>() {
            Exception error;
            ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = ProgressDialog.show(MainActivity.this, null,
                        "Generating SSL certificate...");
                dialog.setCancelable(false);
            }

            @Override
            protected Certificate doInBackground(Void... params) {
                try {
                    Certificate cert = BouncyCastleSslEngineSource.initializeKeyStoreStatic(mainContext.getAuthority());
                    return cert;
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Certificate certificate) {
                dialog.dismiss();
                if (certificate != null) {
                    Intent intent = KeyChain.createInstallIntent();
                    try {
                        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, certificate.getEncoded());
                    } catch (CertificateEncodingException e) {
                        e.printStackTrace();
                    }
                    intent.putExtra(KeyChain.EXTRA_NAME, mainContext.getAuthority().commonName());
                    startActivityForResult(intent, 1);
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "Failed to load certificates, exiting: "
                                    + error.getMessage(), Toast.LENGTH_LONG)
                            .show();
                    finish();
                }
            }
        }.execute();

    }

    private void initializeProxyService() {
        Authority authority = mainContext.getAuthority();
        try {
            if (CertificateUtil.isCACertificateInstalled(authority.aliasFile(BouncyCastleSslEngineSource.KEY_STORE_FILE_EXTENSION),
                    BouncyCastleSslEngineSource.KEY_STORE_TYPE,
                    authority.password())) {
                startProxyService();
            } else {
                installCert();
            }
        } catch (RootCertificateException | GeneralSecurityException | OperatorCreationException | IOException ex) {
            Logger.error(this, ex.getMessage(), ex.getCause());
        }
    }

    protected boolean shouldReload() {
        Settings settings = mainContext.getSettings();
        ThemeStyle settingThemeStyle = settings.getThemeStyle();
        boolean result = !getCurrentThemeStyle().equals(settingThemeStyle);
        if (result) {
            setCurrentThemeStyle(settingThemeStyle);
        }
        return result;
    }

    private void reloadActivity() {
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected ThemeStyle getCurrentThemeStyle() {
        return currentThemeStyle;
    }

    protected void setCurrentThemeStyle(ThemeStyle currentThemeStyle) {
        this.currentThemeStyle = currentThemeStyle;
    }

    private void updateStatusView() {
        OperandoProxyStatus proxyStatus = OperandoProxyStatus.STOPPED;
        OperandoProxyLink proxyLink = OperandoProxyLink.INVALID;
        boolean isProxyRunning = MainUtil.isServiceRunning(mainContext.getContext(), ProxyService.class);
        boolean isProxyPaused = MainUtil.isProxyPaused(mainContext);
        if (isProxyRunning) {
            if (isProxyPaused) {
                proxyStatus = OperandoProxyStatus.PAUSED;
            } else {
                proxyStatus = OperandoProxyStatus.ACTIVE;
            }
        }
        try {
            Proxy proxy = APL.getCurrentHttpProxyConfiguration();
            InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
            if (proxyAddress != null) {
                //TODO: THIS SHOULD BE DYNAMIC
                String proxyHost = proxyAddress.getHostName();
                int proxyPort = proxyAddress.getPort();

                if (proxyHost.equals("127.0.0.1") && proxyPort == 8899) {
                    proxyLink = OperandoProxyLink.VALID;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        String info = "";
        try {
            InputStream is = getResources().openRawResource(R.raw.info_template);
            info = IOUtils.toString(is);
            IOUtils.closeQuietly(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        info = info.replace("@@status@@", proxyStatus.name());
        info = info.replace("@@link@@", proxyLink.name());
        webView.loadDataWithBaseURL("", info, "text/html", "UTF-8", "");
        webView.setBackgroundColor(Color.TRANSPARENT); //TRANSPARENT
    }

    private void CheckPhoneNumber(){
        if (new RequestFilterUtil(MainActivity.this).getPhoneNumber().equals("")){
            try {
                final EditText phoneInput = new EditText(MainActivity.this);
                phoneInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                final File phoneFile = new File(getFilesDir(), "phonenumber.conf");
                if (!phoneFile.exists()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setIcon(R.drawable.logo_bevel)
                            .setTitle("Input phone number")
                            .setMessage("The phone number could not be fetched automatically. Please input it below for automated exfiltration checks.")
                            .setView(phoneInput)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        FileOutputStream stream = null;
                                        try {
                                            stream = new FileOutputStream(phoneFile);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        stream.write(phoneInput.getText().toString().getBytes());
                                        stream.close();
                                        Toast.makeText(getApplicationContext(), "Phone number saved successfully", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                                    }
                                    //after phone parsing, load download initial settings
                                    DownloadInitialSettings();
                                }
                            })
                            .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //nothing
                                }
                            })
                            .show();
                }
            }catch (Exception e){
                Log.d("ERROR", e.getMessage());
            }
        }
    }

    private List<String[]> getInstalledApps(boolean getSysPackages) {
        List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
        Collections.sort(
                packs, new Comparator<PackageInfo>() {
                    @Override
                    public int compare(PackageInfo arg0, PackageInfo arg1) {
                        return (arg0.applicationInfo.loadLabel(getPackageManager()).toString()).compareTo(arg1.applicationInfo.loadLabel(getPackageManager()).toString());
                    }
                }
        );
        List<String[]> res = new ArrayList<>();
        for(int i=0;i<packs.size();i++) {
            PackageInfo p = packs.get(i);
            if ((!getSysPackages) && (p.versionName == null)) {
                continue ;
            }
            res.add(new String[] {p.applicationInfo.loadLabel(getPackageManager()).toString(), p.applicationInfo.uid + ""});
        }
        return res;
    }

    private String GetDataForApp(int uid){
        String total;
        try{
            //round to 2 decimal places
            total = Math.round((TrafficStats.getUidTxBytes(uid) / (1024.0 * 1024.0)) * 100.0) / 100.0 + " MB / " + Math.round((TrafficStats.getUidRxBytes(uid) / (1024.0 * 1024.0)) * 100.0) / 100.0 + " MB";
        } catch (Exception e){
            total = "0 MB / 0 MB";
        }
        return total;
    }

    private void LoadPendingNotificationsTab(){
        final String addAllowedSuccessful = "Added to Allowed Domains Successfully",
                addAllowedUnsuccessfull = "This domain already exists in your allowed domain list",
                addBlockedSuccessfull = "Added to Blocked Domains Successfully",
                addBlockedUnsuccessfull = "This domain already exists in your blocked domain list";

        try{
            for (final PendingNotification pending_notification : new DatabaseHelper(this).getAllPendingNotifications()){
                String info = pending_notification.app_info + " || " + pending_notification.permission + " || " + pending_notification.id;
                Button b = new Button(this);
                b.setText(info);
                b.setLayoutParams(new LinearLayout.LayoutParams(android.app.ActionBar.LayoutParams.MATCH_PARENT, android.app.ActionBar.LayoutParams.WRAP_CONTENT));
                ((TableLayout) ((LinearLayout) ((ScrollView) findViewById(R.id.PendingNotificationsScrollView)).getChildAt(0)).getChildAt(1)).addView(b);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            final int notificationId = Integer.parseInt(((Button) v).getText().toString().split(" \\|\\| ")[2]);
                            new AlertDialog.Builder(MainActivity.this)
                                    .setIcon(R.drawable.logo_bevel)
                                    .setTitle("Manage Application Permission")
                                    .setMessage("What do you want to do with this specific application permission?")
                                    .setPositiveButton("ALLOW", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                DatabaseHelper db = new DatabaseHelper(MainActivity.this);
                                                boolean add = true;
                                                for (String permission : pending_notification.permission.split("\\, ")) {
                                                    add = add && db.addAllowedDomain(pending_notification.app_info, permission);
                                                }
                                                boolean remove = db.removePendingNotification(notificationId);
                                                ((NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
                                                Toast.makeText(MainActivity.this, add && remove ? addAllowedSuccessful : addAllowedUnsuccessfull, Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            }
                                            ((TableLayout) ((LinearLayout) ((ScrollView) findViewById(R.id.PendingNotificationsScrollView)).getChildAt(0)).getChildAt(1)).removeAllViews();
                                            LoadPendingNotificationsTab();
                                            new DatabaseHelper(MainActivity.this).sendSettingsToServer(new RequestFilterUtil(MainActivity.this).getIMEI());
                                        }
                                    })
                                    .setNegativeButton("BLOCK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                DatabaseHelper db = new DatabaseHelper(MainActivity.this);
                                                boolean add = true;
                                                for (String permission : pending_notification.permission.split("\\, ")) {
                                                    add = add && db.addBlockedDomain(pending_notification.app_info, permission);
                                                }
                                                boolean remove = db.removePendingNotification(notificationId);
                                                ((NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
                                                Toast.makeText(MainActivity.this, add && remove ? addBlockedSuccessfull : addBlockedUnsuccessfull, Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            }
                                            ((TableLayout) ((LinearLayout) ((ScrollView) findViewById(R.id.PendingNotificationsScrollView)).getChildAt(0)).getChildAt(1)).removeAllViews();
                                            LoadPendingNotificationsTab();
                                            new DatabaseHelper(MainActivity.this).sendSettingsToServer(new RequestFilterUtil(MainActivity.this).getIMEI());
                                        }
                                    })
                                    .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //nothing
                                        }
                                    })
                                    .show();
                        } catch (Exception e) {
                            Log.d("ERROR", e.getMessage());
                        }
                    }
                });
            }
        }catch(Exception e){
            Log.d("ERROR", e.getMessage());
        }
    }

    private void DownloadInitialSettings(){

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("firstTime", true) && haveNetworkConnection()) {
            // run one time code here
            final File tmp = new File(getFilesDir(), "domainfilters_" + System.currentTimeMillis());
            try {
                new DownloadTask(MainActivity.this, new URL(DatabaseHelper.serverUrl + "/blocked_urls"), tmp, new DownloadTask.Listener() {
                    @Override
                    public void onCompleted() {
                        new AsyncTask<Void, Void, Integer>() {
                            ProgressDialog dialog;

                            @Override
                            protected void onPreExecute() {
                                dialog = ProgressDialog.show(MainActivity.this, null,
                                        "Applying up to date settings for your convenience, to keep you safe.\nThis might take while...");
                                dialog.setCancelable(false);
                            }

                            @Override
                            protected Integer doInBackground(Void... params) {
                                new DatabaseHelper(MainActivity.this).deleteDomainFilterFile(DatabaseHelper.serverUrl);
                                Integer count = 0;
                                BufferedReader br = null;
                                try {
                                    br = new BufferedReader(new FileReader(tmp));
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                        int hash = line.indexOf('#');
                                        if (hash >= 0)
                                            line = line.substring(0, hash);
                                        line = line.trim();
                                        try{
                                            String blockedDomain = line;
                                            if (blockedDomain.equals("local") || StringUtils.containsAny(blockedDomain, "localhost", "127.0.0.1", "broadcasthost"))
                                                continue;
                                            DomainFilter domainFilter = new DomainFilter();
                                            domainFilter.setContent(blockedDomain);
                                            domainFilter.setSource(DatabaseHelper.serverUrl);
                                            domainFilter.setIsWildcard(false);
                                            new DatabaseHelper(MainActivity.this).createDomainFilter(domainFilter);
                                            count++;
                                        } catch (Exception e){
                                            Log.i("Error", "Invalid hosts file line: " + line);
                                        }
                                    }
                                    Log.i("Error", count + " entries read");
                                } catch (IOException ex) {
                                    Log.e("Error", ex.toString() + "\n" + Log.getStackTraceString(ex));
                                } finally {
                                    if (br != null)
                                        try {
                                            br.close();
                                        } catch (IOException exex) {
                                            Log.e("Error", exex.toString() + "\n" + Log.getStackTraceString(exex));
                                        }
                                }
                                return count;
                            }

                            @Override
                            protected void onPostExecute(Integer count) {
                                dialog.dismiss();
                                // mark first time has runned.
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("firstTime", false);
                                editor.commit();
                            }
                        }.execute();
                    }

                    @Override
                    public void onCancelled() {
                        if (tmp.exists())
                            tmp.delete();
                    }

                    @Override
                    public void onException(Throwable ex) {
                        if (tmp.exists())
                            tmp.delete();

                        ex.printStackTrace();
                        Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).execute();
            }catch(MalformedURLException mue){
                mue.getMessage();
            }
        } else if (!haveNetworkConnection()) {
            Toast.makeText(MainActivity.this, "You don't seem to have a working Internet Connection.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

}
