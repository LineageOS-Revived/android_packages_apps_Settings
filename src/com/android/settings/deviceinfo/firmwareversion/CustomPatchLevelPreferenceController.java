/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;

public class CustomPatchLevelPreferenceController extends BasePreferenceController {

    private static final String TAG = "CustomPatchCtrl";

    private final PackageManager mPackageManager;
    private final String mCurrentPatch;

    public CustomPatchLevelPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
        mCurrentPatch = DeviceInfoUtils.getCustomPatch();
    }

    @Override
    public int getAvailabilityStatus() {
        return !TextUtils.isEmpty(mCurrentPatch)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mCurrentPatch;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        new AlertDialog.Builder(mContext)
            .setTitle(R.string.custom_patch)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(R.string.custom_patch_info)
            .setNegativeButton(R.string.cancel, null)
            .create().show();

        return true;
    }
}
