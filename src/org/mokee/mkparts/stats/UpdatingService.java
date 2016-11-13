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

package org.mokee.mkparts.stats;

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

public class UpdatingService extends IntentService {

    public UpdatingService() {
        super(UpdatingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        String deviceId = Build.getUniqueID(getApplicationContext());
        String deviceVersion = Build.VERSION;
        String deviceFlashTime = String.valueOf(getSharedPreferences(ReportingServiceManager.ANONYMOUS_PREF, 0).getLong(ReportingServiceManager.ANONYMOUS_FLASH_TIME, 0));

        final int jobId = Utilities.getNextJobId(getApplicationContext());
        Log.d(Utilities.TAG, "scheduling jobs id: " + jobId);

        PersistableBundle updateBundle = new PersistableBundle();
        updateBundle.putString(StatsUploadJobService.KEY_UNIQUE_ID, deviceId);
        updateBundle.putString(StatsUploadJobService.KEY_VERSION, deviceVersion);
        updateBundle.putString(StatsUploadJobService.KEY_FLASH_TIME, deviceFlashTime);

        // set job types
        updateBundle.putInt(StatsUploadJobService.KEY_JOB_TYPE,
                StatsUploadJobService.JOB_TYPE_UPDATE);

        // schedule stats upload
        js.schedule(new JobInfo.Builder(jobId, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(updateBundle)
                .setPersisted(true)
                .build());
    }
}
