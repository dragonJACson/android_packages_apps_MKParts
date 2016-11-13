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
import org.mokee.mkparts.R;

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

    private static final int REQUEST_TIMEOUT = 60000;

    public static final String KEY_UNIQUE_ID = "device_hash";
    public static final String KEY_DEVICE_NAME = "device_name";
    public static final String KEY_VERSION = "device_version";
    public static final String KEY_COUNTRY = "device_country";
    public static final String KEY_CARRIER = "device_carrier";
    public static final String KEY_CARRIER_ID = "device_carrier_id";
    public static final String KEY_FLASH_TIME = "device_flash_time";

    private final Map<JobParameters, StatsReportTask> mReportJobs
            = Collections.synchronizedMap(new ArrayMap<JobParameters, StatsReportTask>());
    private final Map<JobParameters, StatsUpdateTask> mUpdateJobs
            = Collections.synchronizedMap(new ArrayMap<JobParameters, StatsUpdateTask>());

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        int jobType = jobParameters.getExtras().getInt(KEY_JOB_TYPE);
        Log.d(Utilities.TAG, "onStartJob() called with " + "job type = [" + jobType + "]");
        switch (jobType) {
            case JOB_TYPE_REPORT:
                final StatsReportTask reportTask = new StatsReportTask(jobParameters);
                mReportJobs.put(jobParameters, reportTask);
                reportTask.execute((Void) null);
                break;
            case JOB_TYPE_UPDATE:
                final StatsUpdateTask updateTask = new StatsUpdateTask(jobParameters);
                mUpdateJobs.put(jobParameters, updateTask);
                updateTask.execute((Void) null);
                break;
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(Utilities.TAG, "onStopJob() called with " + "jobParameters = [" + jobParameters + "]");
        switch (jobParameters.getExtras().getInt(KEY_JOB_TYPE)) {
            case JOB_TYPE_REPORT:
                final StatsReportTask cancelledReportJob;
                cancelledReportJob = mReportJobs.remove(jobParameters);

                if (cancelledReportJob != null) {
                    // cancel the ongoing background task
                    cancelledReportJob.cancel(true);
                    return true; // reschedule
                }
                break;
            case JOB_TYPE_UPDATE:
                final StatsUpdateTask cancelledUpdateJob;
                cancelledUpdateJob = mUpdateJobs.remove(jobParameters);

                if (cancelledUpdateJob != null) {
                    // cancel the ongoing background task
                    cancelledUpdateJob.cancel(true);
                    return true; // reschedule
                }
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

            StringBuffer params = new StringBuffer();
            params.append(KEY_UNIQUE_ID + "=" + deviceId + "&")
                    .append(KEY_DEVICE_NAME + "=" + deviceName + "&")
                    .append(KEY_VERSION + "=" + deviceVersion + "&")
                    .append(KEY_COUNTRY + "=" + deviceCountry + "&")
                    .append(KEY_CARRIER + "=" + deviceCarrier + "&")
                    .append(KEY_CARRIER_ID + "=" + deviceCarrierId);

            URL url = new URL(getString(R.string.stats_report_url));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setInstanceFollowRedirects(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setConnectTimeout(REQUEST_TIMEOUT);
                urlConnection.setReadTimeout(REQUEST_TIMEOUT);
                urlConnection.setDoInput(true);
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("Accept-Charset", "utf-8");
                byte[] bytes = params.toString().getBytes();
                urlConnection.getOutputStream().write(bytes);
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
            if (result) {
                String currentVersion = Utilities.getVersion();
                final SharedPreferences prefs = getSharedPreferences(ReportingServiceManager.ANONYMOUS_PREF, 0);
                long device_flash_time = 0;
                try {
                    device_flash_time = Long.valueOf(convertStreamToJSONObject(responseStream).getString("device_flash_time"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                prefs.edit().putLong(ReportingServiceManager.ANONYMOUS_FLASH_TIME,
                        device_flash_time).putString(ReportingServiceManager.ANONYMOUS_VERSION, currentVersion).apply();

                // reschedule
                Utilities.updateLastSynced(getApplicationContext());
                ReportingServiceManager.setAlarm(getApplicationContext());
            }

            mReportJobs.remove(mJobParams);
            jobFinished(mJobParams, !result);
        }
    }

    private class StatsUpdateTask extends AsyncTask<Void, Void, Boolean> {

        private JobParameters mJobParams;

        public StatsUpdateTask(JobParameters jobParams) {
            this.mJobParams = jobParams;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            PersistableBundle extras = mJobParams.getExtras();

            String deviceId = extras.getString(KEY_UNIQUE_ID);
            String deviceVersion = extras.getString(KEY_VERSION);
            String deviceFlashTime = extras.getString(KEY_FLASH_TIME);

            boolean success = false;
            if (!isCancelled()) {
                try {
                    success = updateToServer(deviceId, deviceVersion, deviceFlashTime);
                } catch (IOException e) {
                    Log.e(Utilities.TAG, "Could not upload stats update to commnity server", e);
                    success = false;
                }
            }

            Log.d(Utilities.TAG, "job id " + mJobParams.getJobId() + ", has finished with success="
                    + success);
            return success;
        }

        private boolean updateToServer(String deviceId, String deviceVersion, String deviceFlashTime) throws IOException {

            StringBuffer params = new StringBuffer();
            params.append(KEY_UNIQUE_ID + "=" + deviceId + "&")
                    .append(KEY_VERSION + "=" + deviceVersion + "&")
                    .append(KEY_FLASH_TIME + "=" + deviceFlashTime);

            URL url = new URL(getString(R.string.stats_update_url));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setInstanceFollowRedirects(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setConnectTimeout(REQUEST_TIMEOUT);
                urlConnection.setReadTimeout(REQUEST_TIMEOUT);
                urlConnection.setDoInput(true);
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("Accept-Charset", "utf-8");
                byte[] bytes = params.toString().getBytes();
                urlConnection.getOutputStream().write(bytes);
                urlConnection.connect();

                final int responseCode = urlConnection.getResponseCode();

                Log.d(Utilities.TAG, "mokee server response code=" + responseCode);
                final boolean success = responseCode == HttpURLConnection.HTTP_OK;
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
            if (result) {
                String currentVersion = Utilities.getVersion();
                final SharedPreferences prefs = getSharedPreferences(ReportingServiceManager.ANONYMOUS_PREF, 0);
                prefs.edit().putString(ReportingServiceManager.ANONYMOUS_VERSION, currentVersion).apply();

                // reschedule
                Utilities.updateLastSynced(getApplicationContext());
                ReportingServiceManager.setAlarm(getApplicationContext());
            }
            mUpdateJobs.remove(mJobParams);
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
