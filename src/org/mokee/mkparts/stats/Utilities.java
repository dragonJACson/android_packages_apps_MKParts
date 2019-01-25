/*
 * Copyright (C) 2014-2017 The MoKee Open Source Project
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

import com.mokee.os.Build;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class Utilities {

    /* package */ static final String KEY_LAST_JOB_ID = "last_job_id";
    /* package */ static final int QUEUE_MAX_THRESHOLD = 1000;

    public static final String TAG = "ReportingServiceManager";

    public static String getCarrier(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String carrier = tm.getNetworkOperatorName();
        return TextUtils.isEmpty(carrier) ? "Unknown" : carrier;
    }

    public static String getCarrierId(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String carrierId = tm.getNetworkOperator();
        return TextUtils.isEmpty(carrierId) ? "0" : carrierId;
    }

    public static String getCountryCode(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = tm.getNetworkCountryIso();
        return TextUtils.isEmpty(countryCode) ? "Unknown" : countryCode;
    }

    public static String getVersion() {
        return Build.VERSION.startsWith("MK") ? Build.VERSION : "Unknown";
    }

    public static void updateLastSynced(Context context) {
        context.getSharedPreferences(ReportingServiceManager.ANONYMOUS_PREF, 0).edit()
                .putLong(ReportingServiceManager.ANONYMOUS_LAST_CHECKED, System.currentTimeMillis())
                .commit();
    }

    public static int getLastJobId(Context context) {
        return context.getSharedPreferences(ReportingServiceManager.ANONYMOUS_PREF, 0).getInt(KEY_LAST_JOB_ID, 0);
    }

    private static void setLastJobId(Context context, int id) {
        context.getSharedPreferences(ReportingServiceManager.ANONYMOUS_PREF, 0).edit()
                .putInt(KEY_LAST_JOB_ID, id).commit();
    }

    public static int getNextJobId(Context context) {
        int lastId = getLastJobId(context);
        if (lastId >= QUEUE_MAX_THRESHOLD) {
            lastId = 1;
        } else {
            lastId += 1;
        }
        setLastJobId(context, lastId);
        return lastId;
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

}
