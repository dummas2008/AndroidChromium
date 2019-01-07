/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.init;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import org.chromium.base.Log;

import org.chromium.chrome.browser.init.InstallationSourceInformer;

public class InstallReferrerReceiver extends BroadcastReceiver {
    private static final String TAG = "STAT";

    @Override
    public void onReceive(final Context context, Intent intent) {
      String referrer = intent.getStringExtra("referrer");

      if (referrer == null) {
        InstallationSourceInformer.InformFromPlayMarket();
        return;
      }

      Uri uri = Uri.parse("http://www.stub.co/?"+referrer);
      String utm_medium_value = uri.getQueryParameter("utm_medium");
      if (utm_medium_value != null && !utm_medium_value.isEmpty() && !utm_medium_value.equals("organic")) {
        InstallationSourceInformer.InformFromPromo();
      } else {
        InstallationSourceInformer.InformFromPlayMarket();
      }

      // In any way update stats with promo name
      String utm_campaign_value = uri.getQueryParameter("utm_campaign");
      InstallationSourceInformer.InformStatsPromo(utm_campaign_value);

      // Get and save user referal program code
      String urpc = uri.getQueryParameter("urpc");
      InstallationSourceInformer.InformUserReferralProgramCode(urpc);
    }
}
