package com.wubydax.toolboxsettings;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/*      Created by Roberto Mariani and Anna Berkovitch, 2015
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

public class ToolboxSettings extends ListActivity {
    private static final String LOG_TAG = ToolboxSettings.class.getName();
    private ListView appList;
    private BaseAdapter adapter;
    private ProgressBar pb;
    private ContentResolver cr;
    private List<String> appInfoList;
    private boolean[] mAppChecked;

    private int cbCounter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbox_settings);
        cr = getContentResolver();
        appList = getListView();
        pb = (ProgressBar) findViewById(R.id.progressBar);
        appInfoList = new ArrayList<>();

        createList();
        appList.setFastScrollEnabled(true);
        appList.setFadingEdgeLength(1);
        appList.setDivider(null);
        appList.setDividerHeight(0);
        appList.setScrollingCacheEnabled(false);
        cbCounter = 0;

    }

    /*
    Creates list of AppInfo objects for all apps installed and visible in launcher
    the list is being passed on tot he adapter to be used as one of 2 lists (second)
     */


    private List<AppInfo> getAppList() {
        PackageManager pm = getPackageManager();
        List<AppInfo> list = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            AppInfo appInfo = new AppInfo();
            appInfo.appIcon = resolveInfo.activityInfo.loadIcon(pm);
            appInfo.appLabel = resolveInfo.loadLabel(pm).toString();
            appInfo.appPackage = resolveInfo.activityInfo.packageName;
            appInfo.activityName = resolveInfo.activityInfo.name;
            list.add(appInfo);
        }
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_toolbox_settings, menu);
        //Handles the ActionBar switch action in regard of turning the toolbox service on/off
        Switch s = (Switch) menu.findItem(R.id.myswitch).getActionView().findViewById(R.id.switchForActionBar);
        int dbOnOff = Settings.System.getInt(cr, "toolbox_onoff", 0);
        boolean isOn = dbOnOff != 0;
        if (isOn) {
            s.setChecked(true);
        } else {
            s.setChecked(false);
        }
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int i = (isChecked) ? 1 : 0;
                Settings.System.putInt(cr, "toolbox_onoff", i);
            }
        });
        /*
        Content Observer for the "toolbox_onoff" key in settings db,
        so that if the toolbox service is turned off from systemui,
        it turns off the switch in the app as well
         */
        SettingsObserver sb = new SettingsObserver(new Handler(), s);
        cr.registerContentObserver(android.provider.Settings.System.getUriFor("toolbox_onoff"), true, sb);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.save) {
            if (appInfoList.size() > 0) {
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < appInfoList.size(); i++) {
                    sb.append(appInfoList.get(i));
                }
                Settings.System.putString(cr, "toolbox_apps", sb.toString());
                Log.d(LOG_TAG, "onOptionsItemSelected " + sb.toString());
            } else {
                Settings.System.putString(cr, "toolbox_apps", null);
            }
        } else if (id == R.id.sort) {
            startActivity(new Intent(this, SortActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    //Returns boolean fo whether the number of chosen apps exceeded 12
    //For stock dpi more than 12 apps create toolbox out of screen bounds when opened
    //Stock number of apps is 5. You need to apply modifications to framework.jar
    //in order to increase the mas size to 12 apps. If you don't, the toolbox will be cut midst 6th item
    private boolean isMax(int counter) {
        boolean isExceeded = false;
        if (counter >= 12) {
            isExceeded = true;
        }
        return isExceeded;
    }

    //Creating the list on the background thread, so not to block ui thread
    private void createList() {
        new AsyncTask<Void, Void, Void>() {
            List<AppInfo> appInfoList;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pb.setVisibility(View.VISIBLE);
                pb.refreshDrawableState();
            }

            @Override
            protected Void doInBackground(Void... params) {
                appInfoList = getAppList();
                Collections.sort(appInfoList, new Comparator<AppInfo>() {

                    @Override
                    public int compare(AppInfo lhs, AppInfo rhs) {
                        return String.CASE_INSENSITIVE_ORDER.compare(lhs.appLabel, rhs.appLabel);
                    }
                });
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                pb.setVisibility(View.GONE);
                adapter = new AppListAdapter(appInfoList);
                appList.setAdapter(adapter);
                for (boolean aMAppChecked : mAppChecked) {
                    if (aMAppChecked) {
                        cbCounter++;
                    }
                }
                showToast(cbCounter);
            }
        }.execute();
    }

    //Shows toast informing the user of the number of apps selected
    //When selected checkboxes number reaches 12, shows toast informing the user that no more apps may be added
    private void showToast(int counter) {
        LayoutInflater inf = getLayoutInflater();
        @SuppressLint("InflateParams") View toast = inf.inflate(R.layout.toast, null);
        TextView tv = (TextView) toast.findViewById(R.id.numberApp);
        if (counter <= 12) {
            tv.setText(String.format("%s/12", String.valueOf(counter)));
        } else {
            tv.setText(R.string.max_reached_toast);
        }
        Toast countToast = new Toast(this);
        countToast.setView(toast);
        countToast.setDuration(Toast.LENGTH_SHORT);
        countToast.show();
    }

    //Basic list adapter with section indexer to display the main toolbox apps choosing content
    private class AppListAdapter extends BaseAdapter implements SectionIndexer {

        List<AppInfo> mAppList;
        List<DefaultItem> mDefaultsList;
        Context c;
        private HashMap<String, Integer> alphaIndexer;
        private String[] sections;

        public AppListAdapter(List<AppInfo> appList) {
            c = ToolboxSettings.this;
            this.mAppList = appList;
            mDefaultsList = defaultsList(); //makes the list for samsung default toolbox items, such as torch, magnifier and so on
            mAppChecked = new boolean[appList.size() + mDefaultsList.size()];

            /*
            Retrieves string with toolbox active items info from settings db
            further, we split the string and retrieve data necessary for applying to checkbox state
             */
            String dbData = Settings.System.getString(cr, "toolbox_apps");
            if (dbData != null && !dbData.equals("")) {

                String[] stringsArray = dbData.split(";");
                for (String singleApp : stringsArray) {
                    String substring = singleApp.substring(0, singleApp.lastIndexOf("/"));
                    appInfoList.add(singleApp + ";");


                    if (substring.equals("S Finder")) {
                        mAppChecked[0] = true;
                    }
                    if (substring.equals("Quick connect")) {
                        mAppChecked[1] = true;
                    }
                    if (substring.equals("Torch")) {
                        mAppChecked[2] = true;
                    }
                    if (substring.equals("Screen write")) {
                        mAppChecked[3] = true;
                    }
                    if (substring.equals("Magnifier")) {
                        mAppChecked[4] = true;
                    }


                    for (int k = 0; k < appList.size(); k++) {
                        if (substring.equals(appList.get(k).appPackage)) {
                            mAppChecked[k + 5] = true;
                        }
                    }
                }

            }
            //adding Indexer to display the first letter of an app while using fast scroll
            alphaIndexer = new HashMap<>();
            for (int i = 0; i < mAppList.size(); i++) {
                String s = mAppList.get(i).appLabel;
                String s1 = s.substring(0, 1).toUpperCase();
                if (!alphaIndexer.containsKey(s1))
                    alphaIndexer.put(s1, i);
            }

            Set<String> sectionLetters = alphaIndexer.keySet();
            ArrayList<String> sectionList = new ArrayList<>(sectionLetters);
            Collections.sort(sectionList);
            sections = new String[sectionList.size()];
            for (int i = 0; i < sectionList.size(); i++)
                sections[i] = sectionList.get(i);

        }

        @Override
        public Object[] getSections() {
            return sections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return alphaIndexer.get(sections[sectionIndex]);
        }

        @Override
        public int getSectionForPosition(int position) {
            for (int i = sections.length - 1; i >= 0; i--) {
                if (position >= alphaIndexer.get(sections[i])) {
                    return i;
                }
            }
            return 0;
        }

        /*
        Makes the list for the 5 default samsung items, available in stock toolbox for the first 5 actions
        We create a list of DefaultItem class objects and set icon and text fields for each variable
         */
        public List<DefaultItem> defaultsList() {
            List<DefaultItem> defaultsList = new ArrayList<>();
            String[] titles = c.getResources().getStringArray(R.array.default_titles);
            String[] summaries = c.getResources().getStringArray(R.array.default_summaries);
            int[] icons = {R.drawable.toolbox_s_finder,
                    R.drawable.toolbox_quick_connect,
                    R.drawable.toolbox_torch_light,
                    R.drawable.toolbox_screen_write,
                    R.drawable.toolbox_magnifier};
            for (int i = 0; i < titles.length; i++) {
                DefaultItem current = new DefaultItem();
                current.setItemName(titles[i]);
                current.setItemDescription(summaries[i]);
                current.setDrawable(icons[i]);
                defaultsList.add(current);
            }
            return defaultsList;
        }

        @Override
        public int getCount() {
            if (mAppList != null) {
                return mAppList.size() + mDefaultsList.size();
            }
            return 0;
        }

        @Override
        public AppInfo getItem(int position) {
            if (mAppList != null) {
                return mAppList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.app_item_view, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.mAppNames = (TextView) convertView.findViewById(R.id.appName);
                viewHolder.mAppPackage = (TextView) convertView.findViewById(R.id.appPackage);
                viewHolder.mAppIcon = (ImageView) convertView.findViewById(R.id.appIcon);
                viewHolder.mAppSelected = (CheckBox) convertView.findViewById(R.id.appCheckbox);
                convertView.setTag(viewHolder);
            }
            final ViewHolder holder = (ViewHolder) convertView.getTag();
            DefaultItem defaultItem = null;
            if (position < 5) {
                defaultItem = mDefaultsList.get(position);
            }

            if (position >= 0 && position < 5) {
                //Will handle the adapter for the default samsung items (first 5 positions)
                assert defaultItem != null;
                holder.mAppNames.setText(defaultItem.getName());
                holder.mAppPackage.setText(defaultItem.getDescription());
                holder.mAppIcon.setImageResource(defaultItem.getIcon());
                holder.mAppSelected.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String fullEntryForApp = "";

                        switch (position) {
                            case 0:
                                fullEntryForApp = "S Finder/index0;";
                                break;
                            case 1:
                                fullEntryForApp = "Quick connect/index1;";
                                break;
                            case 2:
                                fullEntryForApp = "Torch/index2;";
                                break;
                            case 3:
                                fullEntryForApp = "Screen write/index3;";
                                break;
                            case 4:
                                fullEntryForApp = "Magnifier/index4;";
                                break;
                        }

                        if (((CheckBox) v).isChecked()) {
                            if (!isMax(cbCounter)) {
                                cbCounter++;
                                mAppChecked[position] = true;
                                appInfoList.add(fullEntryForApp);
                                showToast(cbCounter);
                            } else {
                                ((CheckBox) v).setChecked(false);
                                showToast(13);
                            }

                        } else {
                            mAppChecked[position] = false;
                            appInfoList.remove(fullEntryForApp);
                            cbCounter--;
                            showToast(cbCounter);


                        }

                    }
                });
                holder.mAppSelected.setChecked(mAppChecked[position]);


            } else {
                //Will handle the adapter for the application info list (positions 5+ in the list view)
                final AppInfo applicationInfo = mAppList.get(position - 5);


                holder.mAppNames.setText(applicationInfo.appLabel);
                holder.mAppPackage.setText(applicationInfo.appPackage);
                holder.mAppIcon.setImageDrawable(applicationInfo.appIcon);
                holder.mAppSelected.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.mAppSelected = (CheckBox) v;
                        AppInfo applicationInfo = null;
                        if (position > 4) {
                            applicationInfo = mAppList.get(position - 5);
                        }

                        assert applicationInfo != null;
                        String activityName = applicationInfo.activityName;
                        String fullEntryForApp = applicationInfo.appPackage + "/" + activityName + ";";


                        if (((CheckBox) v).isChecked()) {
                            if (!isMax(cbCounter)) {
                                cbCounter++;
                                mAppChecked[position] = true;
                                appInfoList.add(fullEntryForApp);
                                showToast(cbCounter);
                            } else {
                                ((CheckBox) v).setChecked(false);
                                showToast(13);
                            }

                        } else {
                            mAppChecked[position] = false;
                            appInfoList.remove(fullEntryForApp);
                            cbCounter--;
                            showToast(cbCounter);

                        }


                    }

                });
                holder.mAppSelected.setChecked(mAppChecked[position]);


//            if(holder.mAppSelected.isSelected()){
//                holder.mAppSelected.setChecked(true);
//            }else{
//                holder.mAppSelected.setChecked(false);
//            }
            }

            return convertView;
        }

        private class ViewHolder {
            public TextView mAppNames;
            public TextView mAppPackage;
            public ImageView mAppIcon;
            public CheckBox mAppSelected;
        }
    }

    //Settings Observer class to coordinate the Actionbar switch with the externally applied changes to settings db
    //F.e. if the toolbox is switched off from systemui toggle, it will switch off the switch on the Actionbar
    private class SettingsObserver extends ContentObserver {

        Switch toolboxSwitch;
        Context c;


        public SettingsObserver(Handler handler, Switch sw) {
            super(handler);
            c = ToolboxSettings.this;
            toolboxSwitch = sw;

        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            int isOnOff = Settings.System.getInt(cr, "toolbox_onoff", 0);
            boolean isOn = (isOnOff == 1);
            if (isOn) {
                toolboxSwitch.setChecked(true);
            } else {
                toolboxSwitch.setChecked(false);
            }
        }

    }


}
