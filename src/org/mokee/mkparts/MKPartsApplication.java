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

package org.mokee.mkparts;

import android.app.ActivityThread;
import android.app.Application;
import android.content.SharedPreferences;
import android.mokee.utils.MoKeeUtils;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.mokee.os.Build;
import com.mokee.os.Build.VERSION;

import java.util.HashSet;
import java.util.Set;

import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.TagAliasCallback;

import static org.mokee.mkparts.push.PushingMessageReceiver.MKPUSH_ALIAS;
import static org.mokee.mkparts.push.PushingMessageReceiver.MKPUSH_TAGS;
import static org.mokee.mkparts.push.PushingMessageReceiver.MSG_SET_ALIAS;
import static org.mokee.mkparts.push.PushingMessageReceiver.MSG_SET_TAGS;
import static org.mokee.mkparts.push.PushingMessageReceiver.TAG;

public class MKPartsApplication extends Application {

    public static final String MKPUSH_PREF = "mokee_push";

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!ActivityThread.currentProcessName().equals(getPackageName())) return;
        // MoKeePush Interface
        prefs = getApplicationContext().getSharedPreferences(MKPUSH_PREF, 0);
        JPushInterface.setDebugMode(false);
        JPushInterface.init(this);
        // Set Alias
        String alias = Build.getUniqueID(this);
        String prefAlias = prefs.getString(MKPUSH_ALIAS, null);
        if (!alias.equals(prefAlias) && !TextUtils.isEmpty(alias))
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_ALIAS, alias));
        // Set Tags
        Set<String> tags = new HashSet<>();
        tags.add(Build.PRODUCT);
        tags.add(VERSION.CODENAME);
        tags.add(android.os.Build.USER);
        Set<String> prefTags = prefs.getStringSet(MKPUSH_TAGS, null);
        if (!tags.equals(prefTags) && !tags.isEmpty())
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TAGS, tags));
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SET_ALIAS:
                    Log.d(TAG, "Set alias in handler.");
                    JPushInterface.setAliasAndTags(getApplicationContext(), (String) msg.obj, null,
                            mAliasCallback);
                    break;
                case MSG_SET_TAGS:
                    Log.d(TAG, "Set tags in handler.");
                    JPushInterface.setAliasAndTags(getApplicationContext(), null,
                            (Set<String>) msg.obj, mTagsCallback);
                    break;
                default:
                    Log.i(TAG, "Unhandled msg - " + msg.what);
            }
        }
    };

    private final TagAliasCallback mAliasCallback = new TagAliasCallback() {

        @Override
        public void gotResult(int code, String alias, Set<String> tags) {
            String logs;
            switch (code) {
                case 0:
                    logs = "Set alias success";
                    prefs.edit().putString(MKPUSH_ALIAS, alias).apply();
                    Log.i(TAG, logs);
                    break;
                case 6002:
                    logs = "Failed to set alias due to timeout. Try again after 60s.";
                    Log.i(TAG, logs);
                    if (MoKeeUtils.isOnline(getApplicationContext())) {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_ALIAS, alias),
                                1000 * 60);
                    } else {
                        Log.i(TAG, "No network");
                    }
                    break;
                default:
                    logs = "Failed with errorCode = " + code;
                    Log.e(TAG, logs);
            }
        }
    };

    private final TagAliasCallback mTagsCallback = new TagAliasCallback() {

        @Override
        public void gotResult(int code, String alias, Set<String> tags) {
            String logs;
            switch (code) {
                case 0:
                    logs = "Set tag success";
                    prefs.edit().putStringSet(MKPUSH_TAGS, tags).apply();
                    Log.i(TAG, logs);
                    break;
                case 6002:
                    logs = "Failed to set tags due to timeout. Try again after 60s.";
                    Log.i(TAG, logs);
                    if (MoKeeUtils.isOnline(getApplicationContext())) {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_TAGS, tags),
                                1000 * 60);
                    } else {
                        Log.i(TAG, "No network");
                    }
                    break;
                default:
                    logs = "Failed with errorCode = " + code;
                    Log.e(TAG, logs);
            }
        }
    };

}
