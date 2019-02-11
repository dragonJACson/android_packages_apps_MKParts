/**
 * Copyright (C) 2018 The OmniROM Project
 *               2019 The MoKee Open Source Project
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

package org.mokee.mkparts.gestures;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import com.android.settingslib.widget.FooterPreference;

import org.mokee.mkparts.R;
import org.mokee.mkparts.SettingsPreferenceFragment;
import org.mokee.mkparts.widget.IntervalSeekBarPreference;

import mokee.providers.MKSettings;

public class BottomGestureNavigationSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "BottomGestureNavigationSettings";

    private static final String KEY_BOTTOM_GESTURE_NAVIGATION_SWIPE_LENGTH = "bottom_gesture_navigation_swipe_length";
    private static final String KEY_BOTTOM_GESTURE_NAVIGATION_SWIPE_TIMEOUT = "bottom_gesture_navigation_swipe_timeout";
    private static final String KEY_FOOTER_PREF = "footer_preference";

    private IntervalSeekBarPreference mBottomGestureNavigationSwipeTriggerLength;
    private IntervalSeekBarPreference mBottomGestureNavigationSwipeTriggerTimeout;
    private FooterPreference mBottomGestureNavigationFooterPreference;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bottom_gesture_navigation_settings);

        mBottomGestureNavigationFooterPreference = (FooterPreference) findPreference(KEY_FOOTER_PREF);
        // Only visible on devices that does not have a navigation bar already
        boolean hasNavigationBar = true;
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            hasNavigationBar = windowManager.hasNavigationBar();
            if (!hasNavigationBar) {
                mBottomGestureNavigationFooterPreference.setTitle(R.string.bottom_gesture_navigation_settings_navkeys_help_text);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        mBottomGestureNavigationSwipeTriggerLength = (IntervalSeekBarPreference) findPreference(KEY_BOTTOM_GESTURE_NAVIGATION_SWIPE_LENGTH);
        int value = MKSettings.System.getInt(getContentResolver(),
                MKSettings.System.BOTTOM_GESTURE_NAVIGATION_SWIPE_LIMIT,
                getSwipeLengthInPixel(getResources().getInteger(com.android.internal.R.integer.nav_gesture_swipe_min_length)));

        mBottomGestureNavigationSwipeTriggerLength.setMinValue(getSwipeLengthInPixel(40));
        mBottomGestureNavigationSwipeTriggerLength.setMaxValue(getSwipeLengthInPixel(80));
        mBottomGestureNavigationSwipeTriggerLength.setValue(value);
        mBottomGestureNavigationSwipeTriggerLength.setOnPreferenceChangeListener(this);

        mBottomGestureNavigationSwipeTriggerTimeout = (IntervalSeekBarPreference) findPreference(KEY_BOTTOM_GESTURE_NAVIGATION_SWIPE_TIMEOUT);
        value = MKSettings.System.getInt(getContentResolver(),
                MKSettings.System.BOTTOM_GESTURE_NAVIGATION_TRIGGER_TIMEOUT,
                getResources().getInteger(com.android.internal.R.integer.nav_gesture_swipe_timout));
        mBottomGestureNavigationSwipeTriggerTimeout.setValue(value);
        mBottomGestureNavigationSwipeTriggerTimeout.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mBottomGestureNavigationSwipeTriggerLength) {
            MKSettings.System.putInt(getContentResolver(),
                    MKSettings.System.BOTTOM_GESTURE_NAVIGATION_SWIPE_LIMIT, (Integer) objValue);
        } else if (preference == mBottomGestureNavigationSwipeTriggerTimeout) {
            MKSettings.System.putInt(getContentResolver(),
                    MKSettings.System.BOTTOM_GESTURE_NAVIGATION_TRIGGER_TIMEOUT, (Integer) objValue);
        } else {
            return false;
        }
        return true;
    }

    private int getSwipeLengthInPixel(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}