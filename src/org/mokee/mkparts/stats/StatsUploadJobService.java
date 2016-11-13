/*
 * Copyright (C) 2016 The MoKee Open Source Project
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

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.ArrayMap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class StatsUploadJobService extends JobService {

    public static final String KEY_JOB_TYPE = "job_type";
    public static final int JOB_TYPE_REPORT = 1;
    public static final int JOB_TYPE_UPDATE = 2;

    private static final int REQUEST_TIMEOUT = 10000;

    public static final String KEY_UNIQUE_ID = "device_hash";
    public static final String KEY_DEVICE_NAME = "device_name";
    public static final String KEY_VERSION = "device_version";
    public static final String KEY_COUNTRY = "device_country";
    public static final String KEY_CARRIER = "device_carrier";
    public static final String KEY_CARRIER_ID = "device_carrier_id";

    private final Map<JobParameters, StatsReportTask> mReportJobs
            = Collections.synchronizedMap(new ArrayMap<JobParameters, StatsReportTask>());

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(Utilities.TAG, "onStartJob() called with " + "jobParameters = [" + jobParameters + "]");
        switch (jobParameters.getExtras().getInt(KEY_JOB_TYPE)) {
            case JOB_TYPE_REPORT:
                final StatsReportTask uploadTask = new StatsReportTask(jobParameters);
                mReportJobs.put(jobParameters, uploadTask);
                uploadTask.execute((Void) null);
                break;
            case JOB_TYPE_UPDATE:
                break;
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(Utilities.TAG, "onStopJob() called with " + "jobParameters = [" + jobParameters + "]");
        switch (jobParameters.getExtras().getInt(KEY_JOB_TYPE)) {
            case JOB_TYPE_REPORT:
                final StatsReportTask cancelledJob;
                cancelledJob = mReportJobs.remove(jobParameters);

                if (cancelledJob != null) {
                    // cancel the ongoing background task
                    cancelledJob.cancel(true);
                    return true; // reschedule
                }
                break;
            case JOB_TYPE_UPDATE:
                break;
        }

        return false;
    }

    private class StatsReportTask extends AsyncTask<Void, Void, Boolean> {

        private JobParameters mJobParams;

        InputStream responseStream;

        public StatsReportTask(JobParameters jobParams) {
            this.mJobParams = jobParams;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            PersistableBundle extras = mJobParams.getExtras();

            String deviceId = extras.getString(KEY_UNIQUE_ID);
            String deviceName = extras.getString(KEY_DEVICE_NAME);
            String deviceVersion = extras.getString(KEY_VERSION);
            String deviceCountry = extras.getString(KEY_COUNTRY);
            String deviceCarrier = extras.getString(KEY_CARRIER);
            String deviceCarrierId = extras.getString(KEY_CARRIER_ID);

            boolean success = false;
            if (!isCancelled()) {
                try {
                    success = reportToServer(deviceId, deviceName, deviceVersion, deviceCountry,
                            deviceCarrier, deviceCarrierId);
                } catch (IOException e) {
                    Log.e(Utilities.TAG, "Could not upload stats checkin to commnity server", e);
                    success = false;
                }
            }

            Log.d(Utilities.TAG, "job id " + mJobParams.getJobId() + ", has finished with success="
                    + success);
            return success;
        }

        private boolean reportToServer(String deviceId, String deviceName, String deviceVersion,
                String deviceCountry, String deviceCarrier, String deviceCarrierId) throws IOException {

            final Uri uri = Uri.parse(getString(R.string.stats_report_url)).buildUpon()
                    .appendQueryParameter(KEY_UNIQUE_ID, deviceId)
                    .appendQueryParameter(KEY_DEVICE_NAME, deviceName)
                    .appendQueryParameter(KEY_VERSION, deviceVersion)
                    .appendQueryParameter(KEY_COUNTRY, deviceCountry)
                    .appendQueryParameter(KEY_CARRIER, deviceCarrier)
                    .appendQueryParameter(KEY_CARRIER_ID, deviceCarrierId).build();
            URL url = new URL(uri.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setInstanceFollowRedirects(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setConnectTimeout(REQUEST_TIMEOUT);
                urlConnection.setReadTimeout(REQUEST_TIMEOUT);
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("Accept-Charset", "utf-8");
                urlConnection.connect();

                final int responseCode = urlConnection.getResponseCode();

                Log.d(Utilities.TAG, "mokee server response code=" + responseCode);
                final boolean success = responseCode == HttpURLConnection.HTTP_OK;
                responseStream = new BufferedInputStream(!success
                        ? urlConnection.getErrorStream()
                        : urlConnection.getInputStream());
                if (!success) {
                    Log.w(Utilities.TAG, "failed sending, server returned: " + getResponse(urlConnection,
                            !success));
                }
                return success;
            } finally {
                urlConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            long interval;

            if (result) {
                String versionCode = Utilities.getVersionCode();
                final SharedPreferences prefs = getSharedPreferences(ReportingServiceManager.ANONYMOUS_PREF, 0);
                long device_flash_time = 0;
                try {
                    device_flash_time = Long.valueOf(convertStreamToJSONObject(responseStream).getString("device_flash_time"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                prefs.edit().putLong(ReportingServiceManager.ANONYMOUS_FLASH_TIME,
                        device_flash_time).putString(ReportingServiceManager.ANONYMOUS_VERSION_CODE, versionCode).apply();
                // use set interval
                interval = 0;
            } else {
                // error, try again in 3 hours
                interval = 3L * 60L * 60L * 1000L;
            }
            Utilities.updateLastSynced(getApplicationContext());
            ReportingServiceManager.setAlarm(getApplicationContext(), interval);
            mReportJobs.remove(mJobParams);
            jobFinished(mJobParams, !result);
        }
    }

    private JSONObject convertStreamToJSONObject(InputStream is) throws IOException, JSONException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        for (String str = reader.readLine(); str != null; str = reader.readLine()) {
            builder.append(str);
        }
        return new JSONObject(builder.toString());
    }

    private String getResponse(HttpURLConnection httpUrlConnection, boolean errorStream)
            throws IOException {
        InputStream responseStream = new BufferedInputStream(errorStream
                ? httpUrlConnection.getErrorStream()
                : httpUrlConnection.getInputStream());

        BufferedReader responseStreamReader = new BufferedReader(
                new InputStreamReader(responseStream));
        String line = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();
        responseStream.close();

        return stringBuilder.toString();
    }
}
