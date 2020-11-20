/*
 * Copyright (C) 2020 The LineageOS Project
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
package com.android.settings.network;

import static android.os.UserHandle.USER_CURRENT;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class CaptivePortalURLPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "CaptivePortalURLPreferenceController";
    private static final String CAPTIVE_PORTAL_URL_KEY = "captive_portal_url";

    private static final String GOOGLE_204   = "Google";
    private static final String GRAPHENE_204 = "GrapheneOS";
    private static final String OPENSUSE_204 = "openSUSE";
    private static final String UBUNTU_204   = "Ubuntu";
    private static final String GOOGLE_OV_A  = "com.android.generate_204.google";
    private static final String GOOGLE_OV_N  = "com.networkstack.generate_204.google";
    private static final String OPSUSE_OV_A  = "com.android.generate_204.opensuse";
    private static final String OPSUSE_OV_N  = "com.networkstack.generate_204.opensuse";
    private static final String UBUNTU_OV_A  = "com.android.generate_204.ubuntu";
    private static final String UBUNTU_OV_N  = "com.networkstack.generate_204.ubuntu";

    private final IOverlayManager mOverlayManager;
    private final UserManager mUm;
    private Context mContext;


    public CaptivePortalURLPreferenceController(Context context) {
        super(context);
        mContext = context;
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        mUm = UserManager.get(context);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            // In case UI is not ready.
            return;
        }

        /* simplified status query and fix, if needed */
        boolean flagGoogle = getOverlayState(GOOGLE_OV_A);
        boolean flagSUSE = getOverlayState(OPSUSE_OV_A);
        boolean flagUbuntu = getOverlayState(UBUNTU_OV_A);

        if (flagSUSE) {
          updateCaptivePortalURLSummary(preference, OPENSUSE_204);
          setOverlayStates(false, true, false);
        } else if (flagUbuntu) {
          updateCaptivePortalURLSummary(preference, UBUNTU_204);
          setOverlayStates(false, false, true);
        } else if (flagGoogle) {
          updateCaptivePortalURLSummary(preference, GOOGLE_204);
          setOverlayStates(true, false, false);
        } else {
          updateCaptivePortalURLSummary(preference, GRAPHENE_204);
          setOverlayStates(false, false, false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean flagGoogle;
        boolean flagSUSE;
        boolean flagUbuntu;
        String provider = (String) newValue;

        switch (provider) {
            case GOOGLE_204:
                flagGoogle = true;
                flagSUSE   = false;
                flagUbuntu = false;
                break;

            case OPENSUSE_204:
                flagGoogle = false;
                flagSUSE   = true;
                flagUbuntu = false;
                break;

            case UBUNTU_204:
                flagGoogle = false;
                flagSUSE   = false;
                flagUbuntu = true;
                break;

            default:
                flagGoogle = false;
                flagSUSE   = false;
                flagUbuntu = false;
                provider   = GRAPHENE_204;
                break;
        }

        setOverlayStates(flagGoogle, flagSUSE, flagUbuntu);
        updateCaptivePortalURLSummary(preference, provider);
        return true;
    }

    @Override
    public boolean isAvailable() {
        if (mUm.isAdminUser()) {
            return validateOverlays();
        } else {
            return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return CAPTIVE_PORTAL_URL_KEY;
    }

    // Called from ResetNetworkConfirm
    public static void ResetCaptivePortalRROs() {
        IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
       try {
           overlayManager.setEnabled(GOOGLE_OV_A, false, USER_CURRENT);
           overlayManager.setEnabled(GOOGLE_OV_N, false, USER_CURRENT);
           overlayManager.setEnabled(OPSUSE_OV_A, false, USER_CURRENT);
           overlayManager.setEnabled(OPSUSE_OV_N, false, USER_CURRENT);
           overlayManager.setEnabled(UBUNTU_OV_A, false, USER_CURRENT);
           overlayManager.setEnabled(UBUNTU_OV_N, false, USER_CURRENT);
           } catch (RemoteException e) { /* do nothing */ }
    }

    // Just in case somebody has purged the CaptivePortal RROs
    private boolean validateOverlays() {
        return ( overlayPresent(GOOGLE_OV_A) && overlayPresent(GOOGLE_OV_N) &&
                 overlayPresent(OPSUSE_OV_A) && overlayPresent(OPSUSE_OV_N) &&
                 overlayPresent(UBUNTU_OV_A) && overlayPresent(UBUNTU_OV_N));
    }

    private boolean overlayPresent(String packageName) {
        OverlayInfo info = null;
        try {
                info = mOverlayManager.getOverlayInfo(packageName, USER_CURRENT);
            } catch (RemoteException e) {
                return false;
            }
        return true;
    }

    private void updateCaptivePortalURLSummary(Preference preference, String provider) {
        int valueIndex;
        String prefSummary;
        ListPreference listpref = (ListPreference) preference;

        valueIndex = listpref.findIndexOfValue(provider);
        prefSummary = mContext.getString(R.string.captive_portal_url_summary, provider);
        listpref.setSummary(prefSummary);
        listpref.setValueIndex(valueIndex);
    }

    private void setOverlayStates(boolean flagGoogle, boolean flagSUSE, boolean flagUbuntu) {
       try {
           mOverlayManager.setEnabled(GOOGLE_OV_A, flagGoogle, USER_CURRENT);
           mOverlayManager.setEnabled(GOOGLE_OV_N, flagGoogle, USER_CURRENT);
           mOverlayManager.setEnabled(OPSUSE_OV_A, flagSUSE, USER_CURRENT);
           mOverlayManager.setEnabled(OPSUSE_OV_N, flagSUSE, USER_CURRENT);
           mOverlayManager.setEnabled(UBUNTU_OV_A, flagUbuntu, USER_CURRENT);
           mOverlayManager.setEnabled(UBUNTU_OV_N, flagUbuntu, USER_CURRENT);
           } catch (RemoteException e) { /* do nothing */ }
    }

    private boolean getOverlayState(String packageName) {
        OverlayInfo info = null;
        boolean ret = false;
        try {
                info = mOverlayManager.getOverlayInfo(packageName, USER_CURRENT);
                ret = (info != null && info.isEnabled());
            } catch (RemoteException e) { /* do nothing */ }
        return ret;
    }
}
