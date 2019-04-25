/*
 * Copyright (C) 2014-2019 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mokee.mkparts.stats;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

public class ReportingServiceManager extends BroadcastReceiver {

    public static final String ACTION_LAUNCH_SERVICE =
            "org.mokee.mkparts.action.TRIGGER_REPORT_METRICS";

    protected static final String ANONYMOUS_PREF = "mokee_stats";

    protected static final String ANONYMOUS_FLASH_TIME = "pref_anonymous_flash_time";

    protected static final String ANONYMOUS_LAST_CHECKED = "pref_anonymous_checked_in";

    protected static final String ANONYMOUS_VERSION = "pref_anonymous_version";

    protected static final String ANONYMOUS_UNIQUE_ID = "pref_anonymous_unique_id";

    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;
    private static final long MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR;
    private static final long UPDATE_INTERVAL = 1L * MILLIS_PER_DAY;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            setAlarm(ctx);
        } else if (intent.getAction().equals(ACTION_LAUNCH_SERVICE)) {
            launchService(ctx);
        }
    }

    public static void setAlarm(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ANONYMOUS_PREF, Context.MODE_PRIVATE);

        long lastSynced = prefs.getLong(ANONYMOUS_LAST_CHECKED, 0);
        String currentVersion = Utilities.getVersion();
        String prefVersion = prefs.getString(ANONYMOUS_VERSION, currentVersion);

        if (lastSynced == 0 || !currentVersion.equals(prefVersion)) {
            launchService(context);
            return;
        }
        long millisFromNow = (lastSynced + UPDATE_INTERVAL) - System.currentTimeMillis();

        Intent intent = new Intent(ACTION_LAUNCH_SERVICE);
        intent.setClass(context, ReportingServiceManager.class);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millisFromNow,
            PendingIntent.getBroadcast(context, 0, intent, 0));
        Log.d(Utilities.TAG, "Next sync attempt in : " + millisFromNow / MILLIS_PER_HOUR + " hours");
    }

    public static void launchService(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ANONYMOUS_PREF, Context.MODE_PRIVATE);
        long lastSynced = prefs.getLong(ANONYMOUS_LAST_CHECKED, 0);
        String currentVersion = Utilities.getVersion();
        String prefVersion = prefs.getString(ANONYMOUS_VERSION, currentVersion);

        boolean shouldSync = false;
        if (lastSynced == 0 || !currentVersion.equals(prefVersion)
                || System.currentTimeMillis() - lastSynced >= UPDATE_INTERVAL) {
            shouldSync = true;
        }
        if (shouldSync && Utilities.isWifiOnly(context) ||
                shouldSync && !Utilities.isWifiOnly(context) && !TextUtils.isEmpty(SystemProperties.get("gsm.version.baseband"))) {
            Intent intent = new Intent();
            intent.setClass(context, ReportingService.class);
            context.startServiceAsUser(intent, UserHandle.OWNER);
        } else {
            setAlarm(context);
        }
    }
}
