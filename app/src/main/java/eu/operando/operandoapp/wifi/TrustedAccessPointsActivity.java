package eu.operando.operandoapp.wifi;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.Toast;

import eu.operando.operandoapp.R;
import eu.operando.operandoapp.database.DatabaseHelper;
import eu.operando.operandoapp.database.model.PendingNotification;
import eu.operando.operandoapp.database.model.TrustedAccessPoint;

public class TrustedAccessPointsActivity extends AppCompatActivity {

    ScrollView TrustedAccessPointScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trusted_access_points_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            setTitle("Trusted Access Points");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TrustedAccessPointScrollView = (ScrollView) findViewById(R.id.TrustedAccessPointsScrollView);

        String ssid = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();
        String bssid = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getBSSID();
        final TrustedAccessPoint currentAccessPoint = new TrustedAccessPoint(ssid, bssid);
        try{
            for (final TrustedAccessPoint trustedAccessPoint : new DatabaseHelper(this).getAllTrustedAccessPoints()){
                String tap_info = trustedAccessPoint.ssid + " || " + trustedAccessPoint.bssid;
                Button b = new Button(this);
                b.setText(tap_info);
                b.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
                ((TableLayout)TrustedAccessPointScrollView.getChildAt(0)).addView(b);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (trustedAccessPoint.isEqual(currentAccessPoint)){
                            Toast.makeText(TrustedAccessPointsActivity.this, "The currently used Access Point cannot be removed from your Trusted Access Point list", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                new AlertDialog.Builder(TrustedAccessPointsActivity.this)
                                        .setIcon(R.drawable.logo_bevel)
                                        .setTitle("Remove Trusted Access Point")
                                        .setMessage("Do you really want to remove this specific Access Point? This action cannot be undone and must be redone manually!")
                                        .setPositiveButton("REMOVE", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                try {
                                                    DatabaseHelper db = new DatabaseHelper(TrustedAccessPointsActivity.this);
                                                    boolean remove = db.removeTrustedAccessPoint(trustedAccessPoint);
                                                    Toast.makeText(TrustedAccessPointsActivity.this, remove ? "Successfully remove Trusted Access Point" : "Could not remove Trusted Access Point", Toast.LENGTH_SHORT).show();
                                                } catch (Exception e) {
                                                    Toast.makeText(TrustedAccessPointsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                                }
                                                Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                                finish();
                                                startActivity(intent);
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
                    }
                });
            }
        }catch(Exception e){
            Log.d("ERROR", e.getMessage());
        }
    }

}
