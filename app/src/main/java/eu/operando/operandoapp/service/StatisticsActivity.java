package eu.operando.operandoapp.service;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import eu.operando.operandoapp.R;
import eu.operando.operandoapp.database.DatabaseHelper;

public class StatisticsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            setTitle("Statistics");
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        for (String s : new DatabaseHelper(this).getStatistics()){
            TextView textView = new TextView(this);
            textView.setText(s);
            textView.setTextSize(17);
            ((TableLayout) findViewById(R.id.statisticsTableLayout)).addView(textView);

            View separator = new View(this);
            separator.setBackgroundColor(Color.BLACK);
            separator.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 5));
            ((TableLayout) findViewById(R.id.statisticsTableLayout)).addView(separator);
        }
    }
}
