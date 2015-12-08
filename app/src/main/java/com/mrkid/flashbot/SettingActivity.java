package com.mrkid.flashbot;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SettingActivity extends ActionBarActivity {

    private Handler handler = new Handler();
    private ListView listView;

    private Set<String> selection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);


        listView = (ListView) findViewById(R.id.list);

        final PackageManager packageManager = getPackageManager();

        selection = Config.getApks(this);
        final ArrayAdapter<PackageInfo> adapter = new ArrayAdapter<PackageInfo>(this, R.layout.package_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View vi = convertView;
                ItemHolder holder = null;
                if (convertView == null) {
                    vi = getLayoutInflater().inflate(R.layout.package_item, null);
                    holder = new ItemHolder();
                    holder.icon = (ImageView) vi.findViewById(R.id.packageIcon);
                    holder.title = (TextView) vi.findViewById(R.id.packageTitle);

                    vi.setTag(holder);
                } else {
                    holder = (ItemHolder) vi.getTag();
                }

                PackageInfo item = this.getItem(position);

                holder.icon.setImageDrawable(item.applicationInfo.loadIcon(packageManager));
                holder.title.setText(item.applicationInfo.loadLabel(packageManager));

                if (selection.contains(item.packageName)) {
                    vi.setBackgroundColor(getResources().getColor(R.color.light_green));
                } else {
                    vi.setBackgroundColor(getResources().getColor(R.color.light_gray));
                }

                return vi;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PackageInfo item = (PackageInfo) listView.getAdapter().getItem(position);

                if (selection.contains(item.packageName)) {
                    selection.remove(item.packageName);
                    view.setBackgroundColor(getResources().getColor(R.color.light_gray));
                } else {
                    selection.add(item.packageName);
                    view.setBackgroundColor(getResources().getColor(R.color.light_green));
                }
            }
        });
        new Thread() {
            @Override
            public void run() {
                Set<String> selectedPackageNames = Config.getApks(SettingActivity.this);
                final List<PackageInfo> selected = new ArrayList<PackageInfo>();
                final List<PackageInfo> unselected = new ArrayList<PackageInfo>();


                List<PackageInfo> packages = packageManager.getInstalledPackages(0);
                for (int i = 0; i < packages.size(); i++) {
                    PackageInfo packageInfo = packages.get(i);
                    //Only display the non-system app info
                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        if (selectedPackageNames.contains(packageInfo.packageName)) {
                            selected.add(packageInfo);
                        } else {
                            unselected.add(packageInfo);
                        }
                    }
                }


                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.addAll(selected);
                        adapter.addAll(unselected);
                    }
                });


            }
        }.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save_settings) {
            Config.saveApks(this, selection);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    static class ItemHolder {
        ImageView icon;
        TextView title;
    }

}
