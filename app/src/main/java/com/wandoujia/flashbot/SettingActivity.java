package com.wandoujia.flashbot;

import android.app.ListActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SettingActivity extends ListActivity {

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        final PackageManager packageManager = getPackageManager();

        final Set<String> selection = Config.getApks(this);
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
        setListAdapter(adapter);

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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        PackageInfo item = (PackageInfo) getListAdapter().getItem(position);
        Set<String> selected = Config.getApks(this);

        if (selected.contains(item.packageName)) {
            selected.remove(item.packageName);
            v.setBackgroundColor(getResources().getColor(R.color.light_gray));
        } else {
            selected.add(item.packageName);
            v.setBackgroundColor(getResources().getColor(R.color.light_green));
        }

        Config.saveApks(this, selected);


    }

    static class ItemHolder {
        ImageView icon;
        TextView title;
    }

}
