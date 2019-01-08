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

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class PushingUtils {

    public static final String KEY_TITLE = "title";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_TYPE = "type";
    public static final String KEY_URL = "url";
    public static final String KEY_CLIPBOARD = "clipboard";

    public static boolean verifyPush(String str1, String str2) {
        String[] list = str1.split(",");
        for (String value : list) {
            if (TextUtils.equals(str2.toLowerCase(Locale.ENGLISH), value.toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        return false;
    }

    public static String getStringFromJson(String key, JSONObject json) {
        return getStringFromJson(key, json, null);
    }

    public static String getStringFromJson(String key, JSONObject json, String def) {
        if (!json.isNull(key)) {
            try {
                return json.getString(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return def;
    }
}
