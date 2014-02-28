package com.wandoujia.flashbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by xudong on 2/27/14.
 */
public class Config {
    public synchronized static Set<String> getApks(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> value = preferences.getStringSet("apks", new HashSet<String>());


        Set<String> result = new HashSet<String>();
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            //Only display the non-system app info
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (value.contains(packageInfo.packageName)) {
                    result.add(packageInfo.packageName);
                }
            }
        }
        return result;
    }

    public synchronized static void saveApks(Context context, Collection<String> apks) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putStringSet("apks", new HashSet<String>(apks));
        edit.commit();
    }
}
