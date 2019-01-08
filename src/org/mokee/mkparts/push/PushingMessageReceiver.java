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

package org.mokee.mkparts.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.mokee.utils.MoKeeUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.mokee.os.Build;

import org.json.JSONException;
import org.json.JSONObject;
import org.mokee.mkparts.R;

import cn.jpush.android.api.JPushInterface;
import mokee.providers.MKSettings;

public class PushingMessageReceiver extends BroadcastReceiver {

    public static final String TAG = PushingMessageReceiver.class.getSimpleName();

    public static final String MKPUSH_ALIAS = "pref_alias";
    public static final String MKPUSH_TAGS = "pref_tags";

    public static final int MSG_SET_ALIAS = 1001;
    public static final int MSG_SET_TAGS = 1002;

    private static final String PUSH_NOTIFICATION_CHANNEL = "push_notification_channel";
    private static final String COPY_TO_CLIPBOARD_ACTION = "copy_to_clipboard_action";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Bundle bundle = intent.getExtras();
        Log.d(TAG, "[MyReceiver] onReceive - " + intent.getAction() + ", extras: " + printBundle(bundle));
        if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
            String message = bundle.getString(JPushInterface.EXTRA_MESSAGE);
            String customContentString = bundle.getString(JPushInterface.EXTRA_EXTRA);
            onMessage(ctx, message, customContentString);
            JPushInterface.reportNotificationOpened(ctx, bundle.getString(JPushInterface.EXTRA_MSG_ID));
        } else if (COPY_TO_CLIPBOARD_ACTION.equals(intent.getAction())) {
            ClipboardManager clipboardManager = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, intent.getStringExtra(PushingUtils.KEY_CLIPBOARD)));
            Toast.makeText(ctx, R.string.text_copied, Toast.LENGTH_LONG).show();
        }
    }

    public void onMessage(Context ctx, String message, String customContentString) {
        if (TextUtils.isEmpty(customContentString) || !MoKeeUtils.isSupportLanguage(true)
                || MKSettings.System.getInt(ctx.getContentResolver(), MKSettings.System.RECEIVE_PUSH_NOTIFICATIONS, 1) != 1)
            return;
        try {
            JSONObject customJson = new JSONObject(customContentString);

            String title = PushingUtils.getStringFromJson(PushingUtils.KEY_TITLE, customJson);
            if (TextUtils.isEmpty(title)) return;

            String device = PushingUtils.getStringFromJson(PushingUtils.KEY_DEVICE, customJson, "all");
            String type = PushingUtils.getStringFromJson(PushingUtils.KEY_TYPE, customJson, "all");
            String url = PushingUtils.getStringFromJson(PushingUtils.KEY_URL, customJson);
            String clipboard = PushingUtils.getStringFromJson(PushingUtils.KEY_CLIPBOARD, customJson);

            if (PushingUtils.verifyPush(device, Build.PRODUCT) && PushingUtils.verifyPush(type, Build.RELEASE_TYPE)
                    || device.equals("all") && type.equals("all")
                    || PushingUtils.verifyPush(type, Build.RELEASE_TYPE) && device.equals("all")
                    || PushingUtils.verifyPush(device, Build.PRODUCT) && type.equals("all")) {
                promptUser(ctx, url, title, message, clipboard);
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    private void promptUser(Context context, String url, String title, String message, String clipboard) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(
                PUSH_NOTIFICATION_CHANNEL,
                context.getString(R.string.push_notification),
                NotificationManager.IMPORTANCE_HIGH
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, PUSH_NOTIFICATION_CHANNEL);
        notificationBuilder.setSmallIcon(R.drawable.ic_push_notify);

        PendingIntent pendingIntent;
        if (!TextUtils.isEmpty(url)) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            if (TextUtils.isEmpty(clipboard)) return;
            pendingIntent = copyToClipboardIntent(context, clipboard);
        }
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setTicker(title);
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(message);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setColor(context.getColor(com.android.internal.R.color.system_notification_accent_color));
        NotificationCompat.BigTextStyle notificationStyle = new NotificationCompat.BigTextStyle();
        notificationStyle.bigText(message);
        notificationBuilder.setStyle(notificationStyle);

        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify((int) (System.currentTimeMillis() / 1000), notificationBuilder.build());
    }

    private static PendingIntent copyToClipboardIntent(Context context, String clipboard) {
        Intent intent = new Intent(context, PushingMessageReceiver.class);
        intent.setAction(COPY_TO_CLIPBOARD_ACTION);
        intent.putExtra(PushingUtils.KEY_CLIPBOARD, clipboard);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String printBundle(Bundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (String key : bundle.keySet()) {
            if (key.equals(JPushInterface.EXTRA_NOTIFICATION_ID)) {
                sb.append("\nkey:" + key + ", value:" + bundle.getInt(key));
            } else if (key.equals(JPushInterface.EXTRA_CONNECTION_CHANGE)) {
                sb.append("\nkey:" + key + ", value:" + bundle.getBoolean(key));
            } else {
                sb.append("\nkey:" + key + ", value:" + bundle.getString(key));
            }
        }
        return sb.toString();
    }
}