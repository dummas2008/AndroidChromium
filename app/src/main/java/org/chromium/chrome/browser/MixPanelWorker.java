/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser;

import android.content.SharedPreferences;
//import com.mixpanel.android.mpmetrics.MixpanelAPI;
//import com.mixpanel.android.mpmetrics.MPConfig;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.json.JSONException;
import org.json.JSONObject;
import 	java.util.Calendar;

public class MixPanelWorker {
    //private static final String PREF_SEND_METRICS = "send_metrics";
    //private static final String PREF_MIXPANEL_DISTINCT_ID_GENERATED_TIME = "mixpanel_distinct_id_generated_time";

    public static void GetMixpanelInstance(ChromeApplication app) {
        /*if ((null != app) && (null == app.mMixpanelInstance) && !ConfigAPIs.MIXPANEL_TOKEN.isEmpty()
        && ContextUtils.getAppSharedPreferences().getBoolean(PREF_SEND_METRICS, true)) {
            app.mMixpanelInstance = MixpanelAPI.getInstance(ContextUtils.getApplicationContext(), ConfigAPIs.MIXPANEL_TOKEN);
            long distinctIdGeneratedTime = ContextUtils.getAppSharedPreferences().getLong(PREF_MIXPANEL_DISTINCT_ID_GENERATED_TIME, 0);
            Calendar cal = Calendar.getInstance();
            long currentTime = cal.getTimeInMillis();
            // Keep in mind that indexing is started from 0
            int currentMonth = cal.get(Calendar.MONTH) + 1;
            cal.setTimeInMillis(distinctIdGeneratedTime);
            int genMonth = cal.get(Calendar.MONTH) + 1;
            // We regenerate distinct id each odd month
            boolean isOddMonth = (currentMonth % 2) == 1;
            boolean setNewDistictIdGeneratedTime = (distinctIdGeneratedTime == 0);
            if ((distinctIdGeneratedTime > 0) && isOddMonth && currentMonth > genMonth) {
                // It is time to regenerate distinct id
                app.mMixpanelInstance.reset();
                setNewDistictIdGeneratedTime = true;
            }
            if (setNewDistictIdGeneratedTime) {
                SharedPreferences.Editor sharedPreferencesEditor = ContextUtils.getAppSharedPreferences().edit();
                sharedPreferencesEditor.putLong(PREF_MIXPANEL_DISTINCT_ID_GENERATED_TIME, currentTime);
                sharedPreferencesEditor.apply();
            }
        }*/
    }

    // Send event with no options
    public static void SendEvent(String eventName) {
        /*if (!ContextUtils.getAppSharedPreferences().getBoolean(PREF_SEND_METRICS, true)) {
            return;
        }
        ChromeApplication app = (ChromeApplication)ContextUtils.getApplicationContext();
        GetMixpanelInstance(app);
        if (null != app && null != app.mMixpanelInstance) {
            app.mMixpanelInstance.track(eventName);
        }*/
    }

    // Send event with option
    public static void SendEvent(String eventName, String propertyName, Object propertyValue) {
        /*if (!ContextUtils.getAppSharedPreferences().getBoolean(PREF_SEND_METRICS, true)) {
            return;
        }
        ChromeApplication app = (ChromeApplication)ContextUtils.getApplicationContext();
        GetMixpanelInstance(app);
        if (null != app && null != app.mMixpanelInstance) {
            try {
                JSONObject obj = new JSONObject();
                obj.put(propertyName, propertyValue);
                app.mMixpanelInstance.track(eventName, obj);
            } catch (JSONException e) {
            }
        }*/
    }

    // Send event for Brave app start
    public static void SendBraveAppStartEvent() {
        /*if (!ContextUtils.getAppSharedPreferences().getBoolean(PREF_SEND_METRICS, true)) {
            return;
        }
        ChromeApplication app = (ChromeApplication)ContextUtils.getApplicationContext();
        GetMixpanelInstance(app);
        if (null != app && null != app.mMixpanelInstance) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("HTTPS Everywhere", PrefServiceBridge.getInstance().isHTTPSEEnabled());
                obj.put("Tracking Protection Mode", PrefServiceBridge.getInstance().isTrackingProtectionEnabled());
                obj.put("Ad Block", PrefServiceBridge.getInstance().isAdBlockEnabled());
                obj.put("Regional Ad Block", PrefServiceBridge.getInstance().isAdBlockRegionalEnabled());
                obj.put("Fingerprinting Protection", PrefServiceBridge.getInstance().isFingerprintingProtectionEnabled());
                obj.put("JavaScript", PrefServiceBridge.getInstance().javaScriptEnabled());
                app.mMixpanelInstance.track("Brave App Start", obj);
            } catch (JSONException e) {
            }
        }*/
    }
}
