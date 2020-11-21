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
import android.os.AsyncTask;
import android.os.ServiceManager;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;

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
        // In case UI is not ready.
        if (preference == null) return;

        /* status query and fix, if needed */
        boolean flagG1 = getOverlayState(GOOGLE_OV_A);
        boolean flagS1 = getOverlayState(OPSUSE_OV_A);
        boolean flagU1 = getOverlayState(UBUNTU_OV_A);
        boolean flagG2 = getOverlayState(GOOGLE_OV_N);
        boolean flagS2 = getOverlayState(OPSUSE_OV_N);
        boolean flagU2 = getOverlayState(UBUNTU_OV_N);

        if ((flagS1 && flagS2) && !(flagG1 || flagG2 || flagU1 || flagU2)) {
          updateCaptivePortalURLSummary(preference, OPENSUSE_204);
        } else if ((flagU1 && flagU2) && !(flagG1 || flagG2 || flagS1 || flagS2)) {
          updateCaptivePortalURLSummary(preference, UBUNTU_204);
        } else if ((flagG1 && flagG2) && !(flagU1 || flagU2 || flagS1 || flagS2)) {
          updateCaptivePortalURLSummary(preference, GOOGLE_204);
        } else if (!(flagG1 || flagG2 || flagU1 || flagU2 || flagS1 || flagS2)) {
          updateCaptivePortalURLSummary(preference, GRAPHENE_204);
        } else {
          // Inconsistent state of Overlays => RESET!
          updateCaptivePortalURLSummary(preference, GRAPHENE_204);
          setOverlayStates(false, false, false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String provider = (String) newValue;
        switch (provider) {
            case GOOGLE_204:
                setOverlayStates(true,  /*Google*/
                                 false, /*openSUSE*/
                                 false  /*Ubuntu*/ );
                break;

            case OPENSUSE_204:
                setOverlayStates(false, /*Google*/
                                 true,  /*openSUSE*/
                                 false  /*Ubuntu*/ );
                break;

            case UBUNTU_204:
                setOverlayStates(false, /*Google*/
                                 false, /*openSUSE*/
                                 true   /*Ubuntu*/ );
                break;

            default:
                provider = GRAPHENE_204;
                setOverlayStates(false, /*Google*/
                                 false, /*openSUSE*/
                                 false  /*Ubuntu*/ );
                break;
        }
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
        ListPreference listpref = (ListPreference) preference;
        if (listpref.getValue() != provider) listpref.setValue(provider);
        listpref.setSummary(mContext.getString(R.string.captive_portal_url_summary, provider));
    }

    private void setOverlayStates(boolean flagGoogle, boolean flagSUSE, boolean flagUbuntu) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mOverlayManager.setEnabled(GOOGLE_OV_A, flagGoogle, USER_CURRENT);
                    mOverlayManager.setEnabled(GOOGLE_OV_N, flagGoogle, USER_CURRENT);
                    mOverlayManager.setEnabled(OPSUSE_OV_A, flagSUSE, USER_CURRENT);
                    mOverlayManager.setEnabled(OPSUSE_OV_N, flagSUSE, USER_CURRENT);
                    mOverlayManager.setEnabled(UBUNTU_OV_A, flagUbuntu, USER_CURRENT);
                    mOverlayManager.setEnabled(UBUNTU_OV_N, flagUbuntu, USER_CURRENT);
                    return true;
                } catch (RemoteException re) {
                    Log.w(TAG, "Error setting Captive portal RROs", re);
                    return false;
                }

            }

            @Override
            protected void onPostExecute(Boolean success) {
                int toastMessage;
                if (success)
                    toastMessage = R.string.captive_portal_toast_success;
                else
                    toastMessage = R.string.overlay_toast_failed_to_apply;
                Toast.makeText(mContext, toastMessage, Toast.LENGTH_LONG).show();
            }
        }.execute();
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
