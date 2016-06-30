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

package eu.operando.operandoapp.filters.domain;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import eu.operando.operandoapp.MainContext;
import eu.operando.operandoapp.R;
import eu.operando.operandoapp.database.DatabaseHelper;
import eu.operando.operandoapp.database.model.DomainFilter;
import eu.operando.operandoapp.database.model.FilterFile;
import eu.operando.operandoapp.filters.DownloadTask;


public class DomainFiltersActivity extends AppCompatActivity {

    private static final String TAG = "DomainFiltersActivity";

    private MainContext mainContext = MainContext.INSTANCE;

    @InjectView(R.id.recycler_view_holder)
    public FrameLayout recyclerViewHolder;


    private RecyclerView recyclerView;

    private List<DomainFilter> userFilters;

    private List<FilterFile> externalFilters;

    private UserDomainFiltersAdapter userDomainFiltersAdapter;

    private ExternalDomainFiltersAdapter externalDomainFiltersAdapter;

    private MenuItem deleteAction;

    private Bus BUS = mainContext.getBUS();

    private DatabaseHelper db = mainContext.getDatabaseHelper();

    private int viewSelected = 0; //0: user, 1: external

    private Set<DomainFilter> userDomainFiltersSelected = new HashSet<>();
    private Set<FilterFile> externalFilterFilesSelected = new HashSet<>();

    protected void updateFiltersList() {
        userFilters = db.getAllUserDomainFilters();
        externalFilters = db.getAllDomainFilterFiles();
    }

    protected void inValidateSelections() {
        userDomainFiltersSelected.clear();
        externalFilterFilesSelected.clear();
        if (userDomainFiltersAdapter != null && externalDomainFiltersAdapter != null) {
            userDomainFiltersAdapter.updateEditAction();
            externalDomainFiltersAdapter.updateEditAction();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MainContext.INSTANCE.getSettings().getThemeStyle().themeAppCompatStyle());
        updateFiltersList();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filters_content);
        ButterKnife.inject(this);


        userDomainFiltersAdapter = new UserDomainFiltersAdapter();
        externalDomainFiltersAdapter = new ExternalDomainFiltersAdapter();

        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //recyclerView.setAdapter(userDomainFiltersAdapter);
        //recyclerViewHolder.addView(recyclerView);


        //region Setup Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            View spinnerContainer = LayoutInflater.from(this).inflate(R.layout.toolbar_spinner,
                    toolbar, false);
            ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            toolbar.addView(spinnerContainer, lp);

            DomainFiltersSpinnerAdapter spinnerAdapter = new DomainFiltersSpinnerAdapter();

            Spinner spinner = (Spinner) spinnerContainer.findViewById(R.id.toolbar_spinner);
            spinner.setAdapter(spinnerAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    viewSelected = position;
                    recyclerViewHolder.removeAllViews();
                    //recyclerViewHolder.invalidate();
                    switch (position) {
                        case 0:
                            recyclerView.setAdapter(userDomainFiltersAdapter);
                            break;
                        case 1:
                            recyclerView.setAdapter(externalDomainFiltersAdapter);
                            break;
                        default:
                            recyclerView.setAdapter(userDomainFiltersAdapter);
                            break;
                    }
                    recyclerViewHolder.addView(recyclerView);
                    inValidateSelections();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

        }
        //endregion
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filter_menu, menu);
        deleteAction = menu.getItem(0);
        deleteAction.setEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BUS.register(userDomainFiltersAdapter);
        BUS.register(externalDomainFiltersAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BUS.unregister(userDomainFiltersAdapter);
        BUS.unregister(externalDomainFiltersAdapter);
    }

    @OnClick(R.id.add_filter)
    public void addFilter() {

        if (viewSelected == 0) { //User Filter

            View dialogView = getLayoutInflater().inflate(R.layout.user_domain_filter_dialog, null);
            final EditText input = (EditText) dialogView.findViewById(R.id.filter_content);
            final CheckBox isWildcard = (CheckBox) dialogView.findViewById(R.id.is_wildcard);

            AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("New DomainFilter")
                    .setView(dialogView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            DomainFilter domainFilter = new DomainFilter();
                            domainFilter.setContent(input.getText().toString().toLowerCase());
                            domainFilter.setSource(null);
                            domainFilter.setIsWildcard(isWildcard.isChecked());
                            db.createDomainFilter(domainFilter);
                            updateFiltersList();
                            userDomainFiltersAdapter.notifyItemInserted(userFilters.size() - 1);
                            recyclerView.scrollToPosition(userFilters.size() - 1);
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

            final AlertDialog dialog = builder.create();

            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (Patterns.DOMAIN_NAME.matcher(s).matches()) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    } else dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            });

            dialog.show();

        } else { //Imported filter list
            final EditText input = new EditText(this);
            input.setSingleLine(true);
            //input.setHint("Enter URL");
            input.setText(DatabaseHelper.serverUrl + "/blocked_urls");
            new AlertDialog.Builder(this).setTitle("Import filters from remote file (hosts file format)")
                    .setView(input).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    final String importUrl = input.getText().toString();
                    long start = System.currentTimeMillis();
                    importExternalFilters(importUrl);
                    long end = System.currentTimeMillis();
                    Toast.makeText(DomainFiltersActivity.this, (end-start) + "ms required", Toast.LENGTH_LONG).show();

                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            }).show();
        }
    }

    protected void importExternalFilters(final String importUrl) {
        final File tmp = new File(getFilesDir(), "domainfilters_" + System.currentTimeMillis());
        try {
            new DownloadTask(DomainFiltersActivity.this, new URL(importUrl), tmp, new DownloadTask.Listener() {
                @Override
                public void onCompleted() {
                    //Toast.makeText(DomainFiltersActivity.this, R.string.msg_downloaded, Toast.LENGTH_LONG).show();

                    new AsyncTask<Void, Void, Integer>() {

                        ProgressDialog dialog;

                        @Override
                        protected void onPreExecute() {
                            dialog = ProgressDialog.show(DomainFiltersActivity.this, null,
                                    "Parsing downloaded file...");
                            dialog.setCancelable(false);
                        }

                        @Override
                        protected Integer doInBackground(Void... params) {
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
                                        domainFilter.setSource(importUrl);
                                        domainFilter.setIsWildcard(false);
                                        db.createDomainFilter(domainFilter);
                                        count++;
                                    } catch (Exception e){
                                        Log.i(TAG, "Invalid hosts file line: " + line);
                                    }
                                }
                                Log.i(TAG, count + " entries read");
                            } catch (IOException ex) {
                                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                            } finally {
                                if (br != null)
                                    try {
                                        br.close();
                                    } catch (IOException exex) {
                                        Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex));
                                    }
                            }

                            return count;
                        }

                        @Override
                        protected void onPostExecute(Integer count) {
                            dialog.dismiss();
                            if (count > 0) {
                                updateFiltersList();
                                externalDomainFiltersAdapter.notifyDataSetChanged();
                            }
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
                    Toast.makeText(DomainFiltersActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }).execute();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            Toast.makeText(DomainFiltersActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                break;
            case R.id.action_delete:
                if (viewSelected == 0)
                    userDomainFiltersAdapter.deleteCheckedItems();
                else
                    externalDomainFiltersAdapter.deleteCheckedItems();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private class DomainFiltersSpinnerAdapter extends BaseAdapter {

        final String[] filterSrcArray = new String[]{"User specified", "External"};

        @Override
        public int getCount() {
            return filterSrcArray.length;
        }

        @Override
        public String getItem(int position) {
            return filterSrcArray[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView :
                    getLayoutInflater().inflate(R.layout.toolbar_spinner_item_actionbar, parent, false);

            TextView title = (TextView) view.findViewById(R.id.toolbarTitle);
            title.setText(getResources().getString(R.string.domain_filters));

            TextView subtitle = (TextView) view.findViewById(R.id.toolbarSubtitle);
            subtitle.setTextColor(Color.WHITE);
            subtitle.setText(getItem(position));
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {

            View view = convertView != null ? convertView :
                    getLayoutInflater().inflate(R.layout.toolbar_spinner_item_dropdown, parent, false);

            TextView textView = (TextView) view.findViewById(R.id.toolbarSpinnerCell);
            textView.setText(getItem(position));

            return view;
        }
    }

    /*
    -----------------------------------------
    Domain Filters entered manually by user
    -----------------------------------------
     */
    private class UserDomainFiltersAdapter extends RecyclerView.Adapter<UserDomainFiltersRowHolder> {

        @Override
        public UserDomainFiltersRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new UserDomainFiltersRowHolder(getLayoutInflater().inflate(R.layout.filter_row, parent, false));
        }

        @Override
        public void onBindViewHolder(UserDomainFiltersRowHolder holder, int position) {

            DomainFilter DomainFilter = userFilters.get(position);
            holder.setDomainFilter(DomainFilter);
            holder.setChecked(userDomainFiltersSelected.contains(DomainFilter));
        }

        @Override
        public int getItemCount() {
            return userFilters.size();
        }

        @Subscribe
        public void onUserDomainFilterCheckStateChangedEvent(UserDomainFilterCheckStateChangedEvent event) {
            if (event.isChecked) {
                userDomainFiltersSelected.add(event.DomainFilter);
            } else {
                userDomainFiltersSelected.remove(event.DomainFilter);
            }

            //Enable or disable the delete option when there are userDomainFiltersSelected items
            updateEditAction();
        }

        public void updateEditAction() {
            if (viewSelected == 0 && userDomainFiltersSelected.size() > 0)
                deleteAction.setEnabled(true);
            else deleteAction.setEnabled(false);
        }

        public void deleteCheckedItems() {
            for (DomainFilter DomainFilter : userDomainFiltersSelected) {
                db.deleteDomainFilter(DomainFilter);
                updateFiltersList();
                userDomainFiltersAdapter.notifyDataSetChanged();
            }
            inValidateSelections();
        }
    }

    class UserDomainFiltersRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView contentLabel;
        private CheckBox checkBox;

        private DomainFilter domainFilter;

        public UserDomainFiltersRowHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            contentLabel = ButterKnife.findById(itemView, R.id.filter_content);
            checkBox = ButterKnife.findById(itemView, R.id.checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    BUS.post(new UserDomainFilterCheckStateChangedEvent(domainFilter, isChecked));
                }
            });
        }

        public void setDomainFilter(DomainFilter DomainFilter) {
            this.domainFilter = DomainFilter;
            contentLabel.setText(DomainFilter.getContent());
        }

        public void setChecked(boolean checked) {
            checkBox.setChecked(checked);
        }

        @Override
        public void onClick(View view) {


            View dialogView = getLayoutInflater().inflate(R.layout.user_domain_filter_dialog, null);
            final EditText input = (EditText) dialogView.findViewById(R.id.filter_content);
            input.append(domainFilter.getContent());
            final CheckBox isWildcard = (CheckBox) dialogView.findViewById(R.id.is_wildcard);
            isWildcard.setChecked(domainFilter.isWildcard());
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext()).setTitle("Edit DomainFilter")
                    .setView(dialogView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            domainFilter.setContent(input.getText().toString());
                            domainFilter.setIsWildcard(isWildcard.isChecked());
                            db.updateDomainFilter(domainFilter);
                            updateFiltersList();
                            userDomainFiltersAdapter.notifyDataSetChanged();

                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });

            final AlertDialog dialog = builder.create();


            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (Patterns.DOMAIN_NAME.matcher(s).matches()) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    } else dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            dialog.show();


        }
    }

    public class UserDomainFilterCheckStateChangedEvent {
        public boolean isChecked;
        public DomainFilter DomainFilter;

        public UserDomainFilterCheckStateChangedEvent(DomainFilter DomainFilter, boolean isChecked) {
            this.DomainFilter = DomainFilter;
            this.isChecked = isChecked;
        }
    }

    /*
    -------------------------------------------
    Domain Filters imported from remote file
    -------------------------------------------
     */

    private class ExternalDomainFiltersAdapter extends RecyclerView.Adapter<ExternalDomainFiltersRowHolder> {

        @Override
        public ExternalDomainFiltersRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ExternalDomainFiltersRowHolder(getLayoutInflater().inflate(R.layout.filter_row, parent, false));
        }

        @Override
        public void onBindViewHolder(ExternalDomainFiltersRowHolder holder, int position) {

            Log.e("TAG", externalFilters.toString());

            FilterFile filterFile = externalFilters.get(position);

            Log.e("TAG", "---->" + filterFile);

            holder.setFilterFile(filterFile);
            holder.setChecked(externalFilterFilesSelected.contains(filterFile));
        }

        @Override
        public int getItemCount() {
            return externalFilters.size();
        }

        @Subscribe
        public void onExternalDomainFilterCheckStateChangedEvent(ExternalDomainFilterCheckStateChangedEvent event) {
            if (event.isChecked) {
                externalFilterFilesSelected.add(event.filterFile);
            } else {
                externalFilterFilesSelected.remove(event.filterFile);
            }
            updateEditAction();
        }

        public void updateEditAction() {
            if (viewSelected == 1 && externalFilterFilesSelected.size() > 0)
                deleteAction.setEnabled(true);
            else deleteAction.setEnabled(false);
        }

        public void deleteCheckedItems() {
            for (FilterFile filterFile : externalFilterFilesSelected) {
                db.deleteDomainFilterFile(filterFile);
                updateFiltersList();
                externalDomainFiltersAdapter.notifyDataSetChanged();
            }
            inValidateSelections();
        }
    }

    class ExternalDomainFiltersRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView contentLabel;
        private CheckBox checkBox;

        private FilterFile filterFile;

        public ExternalDomainFiltersRowHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            contentLabel = ButterKnife.findById(itemView, R.id.filter_content);
            checkBox = ButterKnife.findById(itemView, R.id.checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    BUS.post(new ExternalDomainFilterCheckStateChangedEvent(filterFile, isChecked));
                }
            });
        }

        public void setFilterFile(FilterFile filterFile) {
            this.filterFile = filterFile;
            contentLabel.setText(filterFile.getTitle());
        }

        public void setChecked(boolean checked) {
            checkBox.setChecked(checked);
        }

        @Override
        public void onClick(View view) {
            final String importUrl = filterFile.getSource();
            new AlertDialog.Builder(itemView.getContext()).setTitle("FilterFile actions").setMessage("URL: " + importUrl)
                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            db.deleteDomainFilterFile(importUrl);
                            importExternalFilters(importUrl);
                            updateFiltersList();
                            externalDomainFiltersAdapter.notifyDataSetChanged();
                            inValidateSelections();
                        }
                    }).setNeutralButton("Preview", new DialogInterface.OnClickListener() { //Scrollable dialog preview
                public void onClick(DialogInterface dialog, int whichButton) {


                    new AsyncTask<Void, Void, Spanned>() {
                        ProgressDialog dialog;

                        @Override
                        protected void onPreExecute() {
                            dialog = ProgressDialog.show(DomainFiltersActivity.this, null,
                                    "Generating filters list...");
                            dialog.setCancelable(false);
                        }

                        @Override
                        protected Spanned doInBackground(Void... params) {
                            return Html.fromHtml(TextUtils.join("<br />", db.getAllDomainFiltersForSource(importUrl)));
                        }

                        @Override
                        protected void onPostExecute(Spanned filters) {
                            dialog.dismiss();
                            final AlertDialog.Builder prevDialog = new AlertDialog.Builder(itemView.getContext());
                            View rootView = getLayoutInflater().inflate(R.layout.dialog_scrollable_text, null);
                            prevDialog.setView(rootView);
                            final AlertDialog alertDialog = prevDialog.create();
                            ((TextView) rootView.findViewById(R.id.tv_scrollable_text)).setText(filters);
                            ((TextView) rootView.findViewById(R.id.tv_scrollable_text_dialog_title)).setText("Entries");
                            rootView.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    alertDialog.dismiss();
                                }
                            });
                            alertDialog.show();
                        }
                    }.execute();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            }).show();

        }
    }

    public class ExternalDomainFilterCheckStateChangedEvent {
        public boolean isChecked;
        public FilterFile filterFile;

        public ExternalDomainFilterCheckStateChangedEvent(FilterFile filterFile, boolean isChecked) {
            this.filterFile = filterFile;
            this.isChecked = isChecked;
        }
    }
}
