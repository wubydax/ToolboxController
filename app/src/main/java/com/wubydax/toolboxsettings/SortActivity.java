package com.wubydax.toolboxsettings;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wubydax.toolboxsettings.dragscroll.DragSortController;
import com.wubydax.toolboxsettings.dragscroll.DragSortListView;

import java.util.ArrayList;
import java.util.List;

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

public class SortActivity extends Activity {
    private ContentResolver cr;
    private String[] packageNames;
    private List<String> stringsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort);
        ActionBar ab = getActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        DragSortListView lv = (DragSortListView) findViewById(R.id.listViewSort);
        TextView noAppsText = (TextView) findViewById(R.id.noAppsText);
        cr = getContentResolver();
        //Retrieving the string containing the info for the selected apps
        String dbApps = Settings.System.getString(cr, ToolboxSettings.TOOLBOX_APPS_KEY);
        //If string is null or is "", we show empty screen with text informing the user, that the toolbox is empty
        if (dbApps != null && !dbApps.equals("")) {
            packageNames = dbApps.split(";");
        } else {
            packageNames = null;
            noAppsText.setVisibility(View.VISIBLE);
        }
        stringsList = new ArrayList<>();
        List<SortedItems> list = listItems();
        SortableItemsAdapter adapter = new SortableItemsAdapter(list);
        lv.setDropListener(adapter);
        SectionController sc = new SectionController(lv, adapter);
        lv.setOnTouchListener(sc);
        lv.setFloatViewManager(sc);
        lv.setAdapter(adapter);
        lv.setDivider(null);
        lv.setDividerHeight(0);
    }

    /*
        Creating the list of SortedItems class objects and populating them with string and drawable
        in order to use later on in the adapter to display the list of saved apps/items
        We try to retrieve the ApplicationInfo content from the string, and if an exception si thrown,
        we catch it and assume that then we're talking about the default samsung items which have
        different structure of string in settings db. We then go one by one and set the text and the drawable for
        each one of the default items
         */
    private List<SortedItems> listItems() {
        List<SortedItems> list = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        if (packageNames != null) {
            for (String string : packageNames) {
                stringsList.add(string);
                SortedItems current = new SortedItems();
                String[] dataArray = string.split("/");
                String packageName = dataArray[0];
                String activityName = dataArray[1];
                String label = "";
                Drawable dr = null;
                try {
                    ComponentName componentInfo = new ComponentName(packageName, activityName);
                    Intent intent = new Intent();
                    intent.setComponent(componentInfo);
                    ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, 0);
                    label = resolveInfo.loadLabel(packageManager).toString();
                    dr = resolveInfo.loadIcon(packageManager);
                } catch (NullPointerException e) {
                    string = string.split("/")[0];
                    switch (string) {
                        case "S Finder":
                            label = "S Finder";
                            dr = getDrawable(R.drawable.toolbox_s_finder);
                            break;
                        case "Quick connect":
                            label = "Quick Connect";
                            dr = getDrawable(R.drawable.toolbox_quick_connect);
                            break;
                        case "Torch":
                            label = "Torch";
                            dr = getDrawable(R.drawable.toolbox_torch_light);
                            break;
                        case "Screen write":
                            label = "Screen Write";
                            dr = getDrawable(R.drawable.toolbox_screen_write);
                            break;
                        case "Magnifier":
                            label = "Magnifier";
                            dr = getDrawable(R.drawable.toolbox_magnifier);
                            break;
                    }
                }
                current.setLabel(label);
                current.setIcon(dr);
                list.add(current);

            }
        }
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sort, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.save) {
            if (stringsList.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < stringsList.size(); i++) {
                    sb.append(stringsList.get(i)).append(";");
                }
                Settings.System.putString(cr, ToolboxSettings.TOOLBOX_APPS_KEY, sb.toString());
                finish();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    SectionController class extends the drag scroll controller which
    allows us to handle the floating view while dragging and dropping the list items
     */
    private class SectionController extends DragSortController {

        DragSortListView mDslv;
        private SortableItemsAdapter mAdapter;

        public SectionController(DragSortListView dslv, SortableItemsAdapter adapter) {
            super(dslv);
            setRemoveEnabled(false);
            mDslv = dslv;
            mAdapter = adapter;
        }

        @Override
        public void setDragHandleId(int id) {
            super.setDragHandleId(id);
        }


        @Override
        public View onCreateFloatView(int position) {

            return mAdapter.getView(position, null, mDslv);
        }


        @Override
        public void onDestroyFloatView(View floatView) {
            //do nothing; block super from crashing
        }

    }

    //Class that provides an object for each item on list view
    private class SortedItems {
        String appName;
        Drawable appIcon;

        public String getAppName() {
            return appName;
        }

        public Drawable getIcon() {
            return appIcon;
        }

        public void setIcon(Drawable dr) {
            appIcon = dr;
        }

        public void setLabel(String label) {
            appName = label;
        }
    }

    private class SortableItemsAdapter extends BaseAdapter implements DragSortListView.DropListener {
        Context c;
        List<SortedItems> ls;
        int mDivPos;

        public SortableItemsAdapter(List<SortedItems> itemsList) {
            c = SortActivity.this;
            ls = itemsList;
            mDivPos = ls.size();

        }

        /*
        When an item is "dropped" after being dragged, we remove it from the original position
        in the list and add it to the new position
        we do that for both list of SortedItem objects and list of strings we got after aplitting the string we
        retrieved from settings db. This way upon clicking "save" we already have a list of strings to be unitied into string builder
         */
        @Override
        public void drop(int from, int to) {
            SortedItems data = ls.remove(dataPosition(from));
            String dataString = stringsList.remove(dataPosition(from));
            ls.add(dataPosition(to), data);
            stringsList.add(dataPosition(to), dataString);
            notifyDataSetChanged();

        }

        private int dataPosition(int position) {
            return position > mDivPos ? position - 1 : position;
        }

        @Override
        public int getCount() {
            return ls.size();
        }

        @Override
        public SortedItems getItem(int position) {
            return ls.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inf = getLayoutInflater();
                convertView = inf.inflate(R.layout.sort_apps_item, null);
                ViewHolder vh = new ViewHolder();
                vh.label = (TextView) convertView.findViewById(R.id.appNameSort);
                vh.icon = (ImageView) convertView.findViewById(R.id.appIconSort);
                convertView.setTag(vh);
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            SortedItems si = getItem(position);
            holder.icon.setImageDrawable(si.getIcon());
            holder.label.setText(si.getAppName());
            return convertView;
        }

        private class ViewHolder {
            public TextView label;
            public ImageView icon;
        }
    }
}
