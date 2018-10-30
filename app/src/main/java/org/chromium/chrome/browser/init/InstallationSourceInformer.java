/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


package org.chromium.chrome.browser.init;

import android.content.Context;
import android.content.SharedPreferences;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
//import org.chromium.chrome.browser.MixPanelWorker;
import org.chromium.chrome.browser.util.PackageUtils;

public class InstallationSourceInformer {
  //private static final String TAG = "STAT";
  //private static final String PREF_MIXPANEL_INSTALL_SOURCE_INFORMED = "mixpanel_installation_source_informed";

  public static void InformFromOther() {
    Inform("Others");
  }

  public static void InformFromPlayMarket() {
    Inform("Google Play");
  }

  public static void InformFromPromo() {
    Inform("Promo");
  }

  private static synchronized void Inform(String sourceName) {
    /*if (IsAlreadyInformed()) {
      return;
    }

    MixPanelWorker.SendEvent("Installed from " + sourceName);
    SetAlreadyInformed();*/
  }

  /*private static boolean IsAlreadyInformed() {
    boolean installSourceInformed = ContextUtils.getAppSharedPreferences().getBoolean(
      PREF_MIXPANEL_INSTALL_SOURCE_INFORMED, false);
    return installSourceInformed;
  }

  private static void SetAlreadyInformed() {
    SharedPreferences.Editor sharedPreferencesEditor = ContextUtils.getAppSharedPreferences().edit();
    sharedPreferencesEditor.putBoolean(PREF_MIXPANEL_INSTALL_SOURCE_INFORMED, true);
    sharedPreferencesEditor.apply();
}*/

  private static final String STATS_PREF_NAME = "StatsPreferences";
  private static final String PROMO_NAME = "Promo";
  private static final String URPC_NAME = "UserReferalProgramCode";

  public static void InformStatsPromo(String promoName) {
    Context context = ContextUtils.getApplicationContext();
    if (promoName != null && !promoName.isEmpty() && PackageUtils.isFirstInstall(context)) {
      SharedPreferences sharedPref = context.getSharedPreferences(STATS_PREF_NAME, 0);
      SharedPreferences.Editor editor = sharedPref.edit();

      editor.putString(PROMO_NAME, promoName);
      editor.apply();
    }
  }

  public static void InformUserReferralProgramCode(String urpc) {
    Context context = ContextUtils.getApplicationContext();
    if (urpc != null && !urpc.isEmpty() && PackageUtils.isFirstInstall(context)) {
      SharedPreferences sharedPref = context.getSharedPreferences(STATS_PREF_NAME, 0);
      SharedPreferences.Editor editor = sharedPref.edit();

      editor.putString(URPC_NAME, urpc);
      editor.apply();
    }
  }
}
