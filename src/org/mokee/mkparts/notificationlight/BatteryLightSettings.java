/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2012 The MoKee Open Source Project
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

package org.mokee.mkparts.notificationlight;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.mokee.mkparts.R;
import org.mokee.mkparts.SettingsPreferenceFragment;

import mokee.preference.MKSystemSettingSwitchPreference;
import mokee.providers.MKSettings;

public class BatteryLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "BatteryLightSettings";

    private static final String LOW_COLOR_PREF = "low_color";
    private static final String MEDIUM_COLOR_PREF = "medium_color";
    private static final String FULL_COLOR_PREF = "full_color";
    private static final String LIGHT_ENABLED_PREF = "battery_light_enabled";
    private static final String PULSE_ENABLED_PREF = "battery_light_pulse";

    private PreferenceGroup mColorPrefs;
    private ApplicationLightPreference mLowColorPref;
    private ApplicationLightPreference mMediumColorPref;
    private ApplicationLightPreference mFullColorPref;
    private MKSystemSettingSwitchPreference mLightEnabledPref;
    private MKSystemSettingSwitchPreference mPulseEnabledPref;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.battery_light_settings);
        getActivity().getActionBar().setTitle(R.string.battery_light_title);

        PreferenceScreen prefSet = getPreferenceScreen();

        PreferenceGroup mGeneralPrefs = (PreferenceGroup) prefSet.findPreference("general_section");

        mLightEnabledPref = (MKSystemSettingSwitchPreference) prefSet.findPreference(LIGHT_ENABLED_PREF);
        mPulseEnabledPref = (MKSystemSettingSwitchPreference) prefSet.findPreference(PULSE_ENABLED_PREF);

        if (!getResources().getBoolean(com.android.internal.R.bool.config_ledCanPulse) ||
                getResources().getBoolean(org.mokee.platform.internal.R.bool.config_useSegmentedBatteryLed)) {
            mGeneralPrefs.removePreference(mPulseEnabledPref);
        }

        // Does the Device support changing battery LED colors?
        if (getResources().getBoolean(com.android.internal.R.bool.config_multiColorBatteryLed)) {
            setHasOptionsMenu(true);

            // Low, Medium and full color preferences
            mLowColorPref = (ApplicationLightPreference) prefSet.findPreference(LOW_COLOR_PREF);
            mLowColorPref.setOnPreferenceChangeListener(this);

            mMediumColorPref = (ApplicationLightPreference) prefSet.findPreference(MEDIUM_COLOR_PREF);
            mMediumColorPref.setOnPreferenceChangeListener(this);

            mFullColorPref = (ApplicationLightPreference) prefSet.findPreference(FULL_COLOR_PREF);
            mFullColorPref.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(prefSet.findPreference("colors_list"));
            resetColors();
        }

        addTrigger(MKSettings.System.getUriFor(MKSettings.System.BATTERY_LIGHT_ENABLED));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDefault();
    }

    private void refreshDefault() {
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        if (mLowColorPref != null) {
            int lowColor = MKSettings.System.getInt(resolver, MKSettings.System.BATTERY_LIGHT_LOW_COLOR,
                    res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB));
            mLowColorPref.setAllValues(lowColor, 0, 0, false);
        }

        if (mMediumColorPref != null) {
            int mediumColor = MKSettings.System.getInt(resolver, MKSettings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                    res.getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
            mMediumColorPref.setAllValues(mediumColor, 0, 0, false);
        }

        if (mFullColorPref != null) {
            int fullColor = MKSettings.System.getInt(resolver, MKSettings.System.BATTERY_LIGHT_FULL_COLOR,
                    res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB));
            mFullColorPref.setAllValues(fullColor, 0, 0, false);
        }
    }

    /**
     * Updates the default or application specific notification settings.
     *
     * @param key of the specific setting to update
     * @param color
     */
    protected void updateValues(String key, Integer color) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (key.equals(LOW_COLOR_PREF)) {
            MKSettings.System.putInt(resolver, MKSettings.System.BATTERY_LIGHT_LOW_COLOR, color);
        } else if (key.equals(MEDIUM_COLOR_PREF)) {
            MKSettings.System.putInt(resolver, MKSettings.System.BATTERY_LIGHT_MEDIUM_COLOR, color);
        } else if (key.equals(FULL_COLOR_PREF)) {
            MKSettings.System.putInt(resolver, MKSettings.System.BATTERY_LIGHT_FULL_COLOR, color);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefaults();
                return true;
        }
        return false;
    }

    protected void resetColors() {
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        // Reset to the framework default colors
        MKSettings.System.putInt(resolver, MKSettings.System.BATTERY_LIGHT_LOW_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB));
        MKSettings.System.putInt(resolver, MKSettings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
        MKSettings.System.putInt(resolver, MKSettings.System.BATTERY_LIGHT_FULL_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB));
        refreshDefault();
    }

    protected void resetToDefaults() {
        final Resources res = getResources();
        final boolean batteryLightEnabled = res.getBoolean(R.bool.def_battery_light_enabled);
        final boolean batteryLightPulseEnabled = res.getBoolean(R.bool.def_battery_light_pulse);

        if (mLightEnabledPref != null) mLightEnabledPref.setChecked(batteryLightEnabled);
        if (mPulseEnabledPref != null) mPulseEnabledPref.setChecked(batteryLightPulseEnabled);

        resetColors();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ApplicationLightPreference lightPref = (ApplicationLightPreference) preference;
        updateValues(lightPref.getKey(), lightPref.getColor());
        return true;
    }

    public static final SummaryProvider SUMMARY_PROVIDER = new SummaryProvider() {
        @Override
        public String getSummary(Context context, String key) {
            if (MKSettings.System.getInt(context.getContentResolver(),
                    MKSettings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
                return context.getString(R.string.enabled);
            }
            return context.getString(R.string.disabled);
        }
    };
}
