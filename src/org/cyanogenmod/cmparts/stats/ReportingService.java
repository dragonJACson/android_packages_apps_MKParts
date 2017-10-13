/*
 * Copyright (C) 2014-2016 The MoKee Open Source Project
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

package org.cyanogenmod.cmparts.stats;

import android.annotation.Nullable;
import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

import com.mokee.os.Build;

public class ReportingService extends IntentService {

    public ReportingService() {
        super(ReportingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        String deviceId = Build.getUniqueID(getApplicationContext());
        String deviceName = Build.PRODUCT;
        String deviceVersion = Build.VERSION;
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());

        final int jobId = Utilities.getNextJobId(getApplicationContext());
        Log.d(Utilities.TAG, "scheduling jobs id: " + jobId);

        PersistableBundle reportBundle = new PersistableBundle();
        reportBundle.putString(StatsUploadJobService.KEY_UNIQUE_ID, deviceId);
        reportBundle.putString(StatsUploadJobService.KEY_DEVICE_NAME, deviceName);
        reportBundle.putString(StatsUploadJobService.KEY_VERSION, deviceVersion);
        reportBundle.putString(StatsUploadJobService.KEY_COUNTRY, deviceCountry);
        reportBundle.putString(StatsUploadJobService.KEY_CARRIER, deviceCarrier);
        reportBundle.putString(StatsUploadJobService.KEY_CARRIER_ID, deviceCarrierId);

        // set job types
        reportBundle.putInt(StatsUploadJobService.KEY_JOB_TYPE,
                StatsUploadJobService.JOB_TYPE_REPORT);

        // schedule stats upload
        js.schedule(new JobInfo.Builder(jobId, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(reportBundle)
                .setPersisted(true)
                .build());
    }
}
