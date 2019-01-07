/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser;

import org.chromium.base.Log;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.tab.Tab;

import java.net.URL;
import java.net.MalformedURLException;

public class DisableVideoPauseOnBackground {
    private static String TAG = "PLAYBG";
    public static void Execute(Tab tab) {
        final boolean videoInBackgroundEnabled = PrefServiceBridge.getInstance().playVideoInBackgroundEnabled();
        if (videoInBackgroundEnabled && NeedToDisable(tab)) {
          tab.getWebContents().evaluateJavaScript(SCRIPT, null);
        }
    }

    private static boolean IsYTWatchUrl(String sUrl) {
        if (sUrl == null || sUrl.isEmpty()) {
          return false;
        }

        try {
          URL url = new URL(sUrl);
          if ("/watch".equalsIgnoreCase(url.getPath()) ) {
            String sHost = url.getHost();
            if ("www.youtube.com".equalsIgnoreCase(sHost) ||
                "youtube.com".equalsIgnoreCase(sHost) ||
                "m.youtube.com".equalsIgnoreCase(sHost)) {
                return true;
            }
          }
        } catch(MalformedURLException e) {
          Log.w(TAG, "MalformedURLException "+ e.getMessage());
        }

        return false;
    }

    private static boolean NeedToDisable(Tab tab) {
        boolean bNeedToDisablePause = (tab != null) && IsYTWatchUrl(tab.getUrl());
        return bNeedToDisablePause;
    }

    private static final String SCRIPT = ""
+"(function() {"
+"    if (document._addEventListener === undefined) {"
+"        document._addEventListener = document.addEventListener;"
+"        document.addEventListener = function(a,b,c) {"
+"            if(a != 'visibilitychange') {"
+"                document._addEventListener(a,b,c);"
+"            }"
+"        };"
+"    }"
+"}());"
;
}
