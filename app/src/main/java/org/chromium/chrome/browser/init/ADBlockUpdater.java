/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.init;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.privacy.PrivacyPreferencesManager;

import java.util.concurrent.Semaphore;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ADBlockUpdater {
    private static final String TAG = "ADBLOCK";
    private static Semaphore mAvailable = new Semaphore(1);
    private static List<String> mWhitelistedRegionalLocales = Arrays.asList("ru", "uk", "be", "hi");
    private static boolean mReceivedAnUpdate = false;
    private static final String UPDATE_ADBLOCKER = "update_adblocker";

    public static void UpdateADBlock(Context context, boolean initShieldsConfig) {
        try {
            mAvailable.acquire();
            if (initShieldsConfig) {
              ChromeApplication app = (ChromeApplication)ContextUtils.getApplicationContext();
              if (null != app) {
                  app.initShieldsConfig();
              }
            }
            try {
              mReceivedAnUpdate = false;
              DownloadTrackingProtectionData(context);
              DownloadAdBlockData(context);
              DownloadAdBlockRegionalData(context);
              DownloadHTTPSData(context);
              if (!initShieldsConfig && mReceivedAnUpdate) {
                // Don't set an update flag on initial download
                ContextUtils.getAppSharedPreferences().edit()
                  .putBoolean(UPDATE_ADBLOCKER, true)
                  .apply();
              }
            } finally {
                mAvailable.release();
            }
        } catch (InterruptedException exc) {
        }
    }

    // Tracking protection data download
    private static void DownloadTrackingProtectionData(Context context) {
        String verNumber = ADBlockUtils.getDataVerNumber(
            ADBlockUtils.TRACKING_PROTECTION_URL, false);
        if (ADBlockUtils.readData(context,
            ADBlockUtils.TRACKING_PROTECTION_LOCALFILENAME,
            ADBlockUtils.TRACKING_PROTECTION_URL,
            ADBlockUtils.ETAG_PREPEND_TP, verNumber,
            ADBlockUtils.TRACKING_PROTECTION_LOCALFILENAME_DOWNLOADED, false)) {
          ADBlockUtils.CreateDownloadedFile(context, ADBlockUtils.TRACKING_PROTECTION_LOCALFILENAME,
              verNumber, ADBlockUtils.TRACKING_PROTECTION_LOCALFILENAME_DOWNLOADED, false);
          mReceivedAnUpdate = true;
        }
    }

    // Adblock data download
    private static void DownloadAdBlockData(Context context) {
        String verNumber = ADBlockUtils.getDataVerNumber(
            ADBlockUtils.ADBLOCK_URL, false);
        if (ADBlockUtils.readData(context,
            ADBlockUtils.ADBLOCK_LOCALFILENAME,
            ADBlockUtils.ADBLOCK_URL,
            ADBlockUtils.ETAG_PREPEND_ADBLOCK, verNumber,
            ADBlockUtils.ADBLOCK_LOCALFILENAME_DOWNLOADED, false)) {
          ADBlockUtils.CreateDownloadedFile(context, ADBlockUtils.ADBLOCK_LOCALFILENAME,
              verNumber, ADBlockUtils.ADBLOCK_LOCALFILENAME_DOWNLOADED, false);
          mReceivedAnUpdate = true;
        }
    }

    // Adblock regional data download
    private static void DownloadAdBlockRegionalData(Context context) {
        String verNumber = ADBlockUtils.getDataVerNumber(
            ADBlockUtils.ADBLOCK_REGIONAL_URL, true);
        final String deviceLanguage = Locale.getDefault().getLanguage();
        ADBlockUtils.RegionalADBlockersSt filesSt = ADBlockUtils.readRegionalABData(context,
            ADBlockUtils.ETAG_PREPEND_REGIONAL_ADBLOCK, verNumber, deviceLanguage);
        if (null != filesSt && null != filesSt.uuid && filesSt.readData) {
            List<String> files = filesSt.uuid;
            boolean changePreference = true;
            for (int i = 0; i < files.size(); i ++) {
                mReceivedAnUpdate = true;
                if (!ADBlockUtils.CreateDownloadedFile(context, files.get(i) + ".dat",
                    verNumber, ADBlockUtils.ADBLOCK_REGIONAL_LOCALFILENAME_DOWNLOADED, i != 0) && 0 == i) {
                        changePreference = false;
                        break;
                    }
            }
            if (changePreference) {
                final boolean enableRegionalAdBlock = (0 != files.size());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        /*if (enableRegionalAdBlock && !mWhitelistedRegionalLocales.contains(deviceLanguage)) {
                            PrivacyPreferencesManager.getInstance().setRegionalAdBlock(false, false);
                            PrefServiceBridge.getInstance().setAdBlockRegionalEnabled(false);
                        } else {*/
                            PrivacyPreferencesManager.getInstance().setRegionalAdBlock(enableRegionalAdBlock, true);
                            PrefServiceBridge.getInstance().setAdBlockRegionalEnabled(enableRegionalAdBlock);
                        //}
                    }
                });
            }
        }
    }

    // HTTPS data download
    private static void DownloadHTTPSData(Context context) {
        // Remove old sqlite files. We use leveldb now, which much faster
        ADBlockUtils.removeOldVersionFiles(context, ADBlockUtils.HTTPS_LOCALFILENAME);
        ADBlockUtils.removeOldVersionFiles(context, ADBlockUtils.HTTPS_LOCALFILENAME_DOWNLOADED);
        //

        String verNumber = ADBlockUtils.getDataVerNumber(
            ADBlockUtils.HTTPS_URL_NEW, false);
        if (ADBlockUtils.readData(context,
              ADBlockUtils.HTTPS_LOCALFILENAME_NEW,
              ADBlockUtils.HTTPS_URL_NEW,
              ADBlockUtils.ETAG_PREPEND_HTTPS, verNumber,
              ADBlockUtils.HTTPS_LOCALFILENAME_DOWNLOADED_NEW, true)) {
            // Make temporary several attempts because it fails on unzipping sometimes
            mReceivedAnUpdate = true;
            boolean unzipped = false;
            for (int i = 0; i < 5; i++) {
                unzipped = ADBlockUtils.UnzipFile(ADBlockUtils.HTTPS_LOCALFILENAME_NEW, verNumber, true);
                if (unzipped) {
                    break;
                }
            }
            //

            if (unzipped) {
                ADBlockUtils.CreateDownloadedFile(context, ADBlockUtils.HTTPS_LEVELDB_FOLDER,
                    verNumber, ADBlockUtils.HTTPS_LOCALFILENAME_DOWNLOADED_NEW, false);
            } else {
                ADBlockUtils.removeOldVersionFiles(context, ADBlockUtils.HTTPS_LOCALFILENAME_NEW);
                ADBlockUtils.removeOldVersionFiles(context, ADBlockUtils.HTTPS_LOCALFILENAME_DOWNLOADED_NEW);
            }
        }
    }
}
