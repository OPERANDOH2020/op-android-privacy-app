package eu.operando.operandoapp.filters.domain;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.Toast;

import eu.operando.operandoapp.R;
import eu.operando.operandoapp.database.DatabaseHelper;
import eu.operando.operandoapp.database.model.AllowedDomain;
import eu.operando.operandoapp.database.model.BlockedDomain;

public class DomainManagerActivity extends AppCompatActivity {

    ScrollView AllowedDomainScrollView, BlockedDomainScrollView;
    TabHost tabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.domain_manager_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            setTitle("Domain Manager");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        AllowedDomainScrollView = (ScrollView) findViewById(R.id.AllowedDomainScrollView);
        BlockedDomainScrollView = (ScrollView) findViewById(R.id.BlockedDomainScrollView);

        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("allowed");
        tabSpec.setContent(R.id.AllowedDomainScrollView);
        tabSpec.setIndicator("Allowed");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("blocked");
        tabSpec.setContent(R.id.BlockedDomainScrollView);
        tabSpec.setIndicator("Blocked");
        tabHost.addTab(tabSpec);

        try {
            //Load All Allowed Domains
            for (AllowedDomain a_dmn : new DatabaseHelper(this).getAllAllowedDomains()) {
                final String info = a_dmn.info.trim();
                final String exfiltrated = a_dmn.exfiltrated.trim();
                Button b = new Button(this);
                b.setText(info + " || " + exfiltrated);
                b.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
                ((TableLayout)AllowedDomainScrollView.getChildAt(0)).addView(b);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            new AlertDialog.Builder(DomainManagerActivity.this)
                                    .setIcon(R.drawable.logo_bevel)
                                    .setTitle("Manage Application Permission")
                                    .setMessage("What do you want to do with this specific application permission?")
                                    .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                DatabaseHelper db = new DatabaseHelper(DomainManagerActivity.this);
                                                boolean remove = db.removeAllowedDomain(info, exfiltrated);
                                                Toast.makeText(DomainManagerActivity.this, remove ? "Deleted from Allowed Domains" : "Could not be deleted from Allowed Domains", Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Toast.makeText(DomainManagerActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            }
                                            Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                            finish();
                                            startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton("MOVE TO BLOCKED", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                DatabaseHelper db = new DatabaseHelper(DomainManagerActivity.this);
                                                boolean add = db.addBlockedDomain(info, exfiltrated);
                                                boolean remove = db.removeAllowedDomain(info, exfiltrated);
                                                Toast.makeText(DomainManagerActivity.this, add && remove ? "Moved to Blocked Domains" : "Could not be moved to Blocked Domains", Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Toast.makeText(DomainManagerActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
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
                });
            }
            //Load All Blocked Domains
            for (BlockedDomain b_dmn : new DatabaseHelper(this).getAllBlockedDomains()) {
                final String info = b_dmn.info.trim();
                final String exfiltrated = b_dmn.exfiltrated.trim();
                Button b = new Button(this);
                b.setText(info + " || " + exfiltrated);
                b.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
                ((TableLayout)BlockedDomainScrollView.getChildAt(0)).addView(b);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            new AlertDialog.Builder(DomainManagerActivity.this)
                                    .setIcon(R.drawable.logo_bevel)
                                    .setTitle("Manage Application Permission")
                                    .setMessage("What do you want to do with this specific application permission?")
                                    .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                DatabaseHelper db = new DatabaseHelper(DomainManagerActivity.this);
                                                boolean remove = db.removeBlockedDomain(info, exfiltrated);
                                                Toast.makeText(DomainManagerActivity.this, remove ? "Deleted from Blocked Domains" : "Could not be deleted from Blocked Domains", Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Toast.makeText(DomainManagerActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            }
                                            Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                            finish();
                                            startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton("MOVE TO ALLOWED", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                DatabaseHelper db = new DatabaseHelper(DomainManagerActivity.this);
                                                boolean add = db.addAllowedDomain(info, exfiltrated);
                                                boolean remove = db.removeBlockedDomain(info, exfiltrated);
                                                Toast.makeText(DomainManagerActivity.this, add && remove ? "Moved to Allowed Domains" : "Could not be moved to Allowed Domains", Toast.LENGTH_SHORT).show();
                                            } catch (Exception e) {
                                                Toast.makeText(DomainManagerActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
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
                });
            }
        }
        catch(Exception e) {
            Log.d("ERROR", e.getMessage());
        }
    }

}
