/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.init;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.List;
import java.util.HashMap;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.chrome.browser.ContentSettingsType;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.website.WebsitePreferenceBridge;
import org.chromium.chrome.browser.preferences.website.ContentSetting;
import org.chromium.chrome.browser.preferences.website.ContentSettingException;
import org.chromium.chrome.browser.tabmodel.TabModelSelectorTabObserver;

@JNINamespace("net::blockers")
public class ShieldsConfig {

    private static final String PREF_AD_BLOCK = "ad_block";
    private static final String PREF_TRACKERS_BLOCKED_COUNT = "trackers_blocked_count";
    private static final String PREF_ADS_BLOCKED_COUNT = "ads_blocked_count";
    private static final String PREF_HTTPS_UPGRADES_COUNT = "https_upgrades_count";
    private static final String PREF_HTTPSE = "httpse";
    private static final String PREF_TRACKING_PROTECTION = "tracking_protection";
    private static final String PREF_FINGERPRINTING_PROTECTION = "fingerprinting_protection";
    private static final String TAG = "ShieldsConfig";
    private static final String SHIELDS_CONFIG_LOCALFILENAME = "shields_config.dat";
    private static final String UPDATE_ADBLOCKER = "update_adblocker";
    // The format is (<top shields switch>,<ads and tracking switch>,<HTTPSE switch>,<JavaScript switch>,<3rd party cookies switch><Fingerprinting switch>)
    // We handle JavaScript blocking by internal implementation of Chromium, but save the state here also
    private static final String ALL_SHIELDS_DEFAULT_MASK = "1,1,1,0,1,0";
    private HashMap<String, String> mSettings = new HashMap<String, String>();
    private HashMap<String, String> mSettingsIncognito = new HashMap<String, String>();
    private ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();
    private Context mContext = null;
    private TabModelSelectorTabObserver mTabModelSelectorTabObserver;
    private final SharedPreferences mSharedPreferences;


    public ShieldsConfig() {
        mContext = ContextUtils.getApplicationContext();
        mSharedPreferences = ContextUtils.getAppSharedPreferences();
        nativeInit();
        new ReadDataAsyncTask().execute();
    }

    class ReadDataAsyncTask extends AsyncTask<Void,Void,Long> {
        @Override
        protected Long doInBackground(Void... params) {
            // Read file with the settings
            try {
                File dataPath = new File(mContext.getApplicationInfo().dataDir, SHIELDS_CONFIG_LOCALFILENAME);

                byte[] buffer = null;
                if (dataPath.exists()) {
                    buffer = ADBlockUtils.readLocalFile(dataPath);
                }
                if (null != buffer) {
                    String[] array = new String(buffer).split(";");
                    for (int i = 0; i < array.length; i++) {
                        if (array[i].equals("")) {
                            continue;
                        }
                        int splitterIndex = array[i].indexOf(",");
                        String host = "";
                        String settings = "";
                        if (-1 != splitterIndex) {
                            host = array[i].substring(0, splitterIndex);
                            if (array[i].length() > splitterIndex + 1) {
                                settings = array[i].substring(splitterIndex + 1);
                            }
                        }
                        if (0 != host.length() && 0 != settings.length()) {
                            mSettings.put(host, settings);
                        }
                    }
                }
            }
            catch (Exception exc) {
                Log.i(TAG, "Error on reading dat file.");
            }
            return null;
        }
    }

    public void Close() {
        nativeClear();
    }

    private String correctSettings(String settings) {
      // Correct settings if it was something wrong
      if (null == settings || 0 == settings.length()) {
        return ALL_SHIELDS_DEFAULT_MASK;
      }
      if (settings.length() == (ALL_SHIELDS_DEFAULT_MASK).length()) {
        return settings;
      }
      if (settings.length() > (ALL_SHIELDS_DEFAULT_MASK).length()) {
        settings = settings.substring(0, (ALL_SHIELDS_DEFAULT_MASK).length());
      }
      settings += (ALL_SHIELDS_DEFAULT_MASK).substring(settings.length());

      return settings;
    }

    private String composeSettings(boolean isTopShieldsEnabled, boolean blockAdsAndTracking,
    boolean isHTTPSEverywhereEnabled, boolean isJavaScriptBlocked/*aka JS block switch enabled*/,
    boolean block3rdPartyCookies, boolean blockFingerprints) {
      String settings = "";
      if (isTopShieldsEnabled) {
        settings = settings + "1,";
      }
      else {
        settings = settings + "0,";
      }
      if (blockAdsAndTracking) {
        settings = settings + "1,";
      }
      else {
        settings = settings + "0,";
      }
      if (isHTTPSEverywhereEnabled) {
        settings = settings + "1,";
      }
      else {
        settings = settings + "0,";
      }
      if (isJavaScriptBlocked) {
        settings = settings + "1,";
      }
      else {
        settings = settings + "0,";
      }
      if (block3rdPartyCookies) {
        settings = settings + "1,";
      }
      else {
        settings = settings + "0,";
      }
      if (blockFingerprints) {
        settings = settings + "1";
      }
      else {
        settings = settings + "0";
      }
      return settings;
    }

    public void setTopHost(boolean incognitoTab, String host, boolean enabled) {
        host = CutWwwPrefix(host);
        try {
            mLock.writeLock().lock();
            String settings = getHostSettings(incognitoTab, host);
            if (settings.length() > 1) {
                if (!enabled) {
                    settings = "0" + settings.substring(1);
                } else {
                    settings = "1" + settings.substring(1);
                }
            } else {
                settings = composeSettings(enabled, blockAdsAndTracking(incognitoTab, host),
                isHTTPSEverywhereEnabled(incognitoTab, host), isJavaScriptBlocked(incognitoTab, host),
                block3rdPartyCookies(incognitoTab, host), blockFingerprints(incognitoTab, host));
            }
            settings = correctSettings(settings);
            if (!incognitoTab) {
                mSettings.put(host, settings);
            } else {
                mSettingsIncognito.put(host, settings);
            }
        }
        finally {
            mLock.writeLock().unlock();
        }
        if (!incognitoTab) {
            new SaveDataAsyncTask().execute();
        }
    }

    public void setAdsAndTracking(boolean incognitoTab, String host, boolean enabled) {
        host = CutWwwPrefix(host);
        try {
            mLock.writeLock().lock();
            String settings = getHostSettings(incognitoTab, host);
            if (settings.length() > 3) {
                if (!enabled) {
                    settings = settings.substring(0, 2) + "0" + settings.substring(3);
                } else {
                    settings = settings.substring(0, 2) + "1" + settings.substring(3);
                }
            } else {
                settings = composeSettings(isTopShieldsEnabled(incognitoTab, host), enabled,
                isHTTPSEverywhereEnabled(incognitoTab, host), isJavaScriptBlocked(incognitoTab, host),
                block3rdPartyCookies(incognitoTab, host), blockFingerprints(incognitoTab, host));
            }
            settings = correctSettings(settings);
            if (incognitoTab) {
                mSettingsIncognito.put(host, settings);
            } else {
                mSettings.put(host, settings);
            }
        }
        finally {
            mLock.writeLock().unlock();
        }
        if (!incognitoTab) {
            new SaveDataAsyncTask().execute();
        }
    }

    public void setHTTPSEverywhere(boolean incognitoTab, String host, boolean enabled) {
        host = CutWwwPrefix(host);
        try {
            mLock.writeLock().lock();
            String settings = getHostSettings(incognitoTab, host);
            if (settings.length() > 5) {
                if (!enabled) {
                    settings = settings.substring(0, 4) + "0" + settings.substring(5);
                } else {
                    settings = settings.substring(0, 4) + "1" + settings.substring(5);
                }
            } else {
                settings = composeSettings(isTopShieldsEnabled(incognitoTab, host), blockAdsAndTracking(incognitoTab, host),
                enabled, isJavaScriptBlocked(incognitoTab, host),
                block3rdPartyCookies(incognitoTab, host), blockFingerprints(incognitoTab, host));
            }
            settings = correctSettings(settings);
            if (incognitoTab) {
                mSettingsIncognito.put(host, settings);
            } else {
                mSettings.put(host, settings);
            }
        }
        finally {
            mLock.writeLock().unlock();
        }
        if (!incognitoTab) {
            new SaveDataAsyncTask().execute();
        }
    }

    public void setJavaScriptBlock(boolean incognitoTab, String host, boolean block, boolean fromTopShields) {
        String hostForAccessMap = CutWwwPrefix(host);

        ContentSetting setting = ContentSetting.ALLOW;
        if (block && !fromTopShields) {
            setting = ContentSetting.BLOCK;
        }

        if (fromTopShields) {
          //when change comes from Top Shields:
          // in anyway do not touch hostSettings string
          // block is false => we should allow js
          // block is true => we should switch js back according to hostSettings string
          if (!block) {
            setting = ContentSetting.ALLOW;
          } else {
            boolean allowJsFromBraveHostSettings = true;//safe dafault
            String settings = getHostSettings(incognitoTab, hostForAccessMap);

            if (null == settings
              || 0 == settings.length()
              || (settings.length() > 7 && '1' == settings.charAt(6))) {
                //'1' means Block Scripts switch is on
                allowJsFromBraveHostSettings = false;
            }
            setting = allowJsFromBraveHostSettings ? ContentSetting.ALLOW : ContentSetting.BLOCK;
          }
        }

        if (incognitoTab) {
            PrefServiceBridge.getInstance().nativeSetContentSettingForPatternIncognito(
                      ContentSettingsType.CONTENT_SETTINGS_TYPE_JAVASCRIPT, host,
                      setting.toInt());
        } else {
            PrefServiceBridge.getInstance().nativeSetContentSettingForPattern(
                      ContentSettingsType.CONTENT_SETTINGS_TYPE_JAVASCRIPT, host,
                      setting.toInt());
        }

        if (!fromTopShields) {
            try {
                mLock.writeLock().lock();
                String settings = getHostSettings(incognitoTab, hostForAccessMap);
                if (settings.length() > 7) {
                    if (!block) {
                        settings = settings.substring(0, 6) + "0" + settings.substring(7);
                    } else {
                        settings = settings.substring(0, 6) + "1" + settings.substring(7);
                    }
                } else {
                    settings = composeSettings(isTopShieldsEnabled(incognitoTab, host), blockAdsAndTracking(incognitoTab, host),
                    isHTTPSEverywhereEnabled(incognitoTab, host), block,
                    block3rdPartyCookies(incognitoTab, host), blockFingerprints(incognitoTab, host));
                }
                settings = correctSettings(settings);
                if (incognitoTab) {
                    mSettingsIncognito.put(hostForAccessMap, settings);
                } else {
                    mSettings.put(hostForAccessMap, settings);
                }
            }
            finally {
                mLock.writeLock().unlock();
            }
            if (!incognitoTab) {
                new SaveDataAsyncTask().execute();
            }
        }
    }

    public void setBlock3rdPartyCookies(boolean incognitoTab, String host, boolean enabled) {
        host = CutWwwPrefix(host);
        try {
            mLock.writeLock().lock();
            String settings = getHostSettings(incognitoTab, host);
            if (settings.length() > 7) {
                if (!enabled) {
                    settings = settings.substring(0, 8) + "0" + settings.substring(9);
                } else {
                    settings = settings.substring(0, 8) + "1" + settings.substring(9);
                }
            } else {
                settings = composeSettings(isTopShieldsEnabled(incognitoTab, host), blockAdsAndTracking(incognitoTab, host),
                isHTTPSEverywhereEnabled(incognitoTab, host), isJavaScriptBlocked(incognitoTab, host),
                enabled, blockFingerprints(incognitoTab, host));
            }
            settings = correctSettings(settings);
            if (incognitoTab) {
                mSettingsIncognito.put(host, settings);
            } else {
                mSettings.put(host, settings);
            }
        }
        finally {
            mLock.writeLock().unlock();
        }
        if (!incognitoTab) {
            new SaveDataAsyncTask().execute();
        }
    }

    public boolean blockAdsAndTracking(boolean incognitoTab, String host) {
        String settings = getHostSettings(incognitoTab, host);
        if (null == settings || settings.length() <= 2) {
            boolean prefAdBlockDefault = true;
            boolean prefAdBlock = mSharedPreferences.getBoolean(
                    PREF_AD_BLOCK, prefAdBlockDefault);
            boolean prefTPDefault = true;
            boolean prefTP = mSharedPreferences.getBoolean(
                    PREF_TRACKING_PROTECTION, prefTPDefault);

            return prefAdBlock || prefTP;
        }
        if ('0' == settings.charAt(2)) {
            return false;
        }

        return true;
    }

    public boolean block3rdPartyCookies(boolean incognitoTab, String host) {
        String settings = getHostSettings(incognitoTab, host);
        if (null == settings || settings.length() <= 8) {
            return PrefServiceBridge.getInstance().isBlockThirdPartyCookiesEnabled();
        }
        if ('0' == settings.charAt(8)) {
            return false;
        }

        return true;
    }

    public boolean isHTTPSEverywhereEnabled(boolean incognitoTab, String host) {
        String settings = getHostSettings(incognitoTab, host);
        if (null == settings || settings.length() <= 5) {
            boolean prefHTTPSEDefault = true;
            boolean prefHTTPSE = mSharedPreferences.getBoolean(
                    PREF_HTTPSE, prefHTTPSEDefault);

            return prefHTTPSE;
        }

        if ('0' == settings.charAt(4)) {
            return false;
        }

        return true;
    }

    public boolean isJavaScriptBlocked(boolean incognitoTab, String host) {
      return !isJavaScriptEnabled(incognitoTab, host);
    }

    public boolean isJavaScriptEnabled(boolean incognitoTab, String host) {
        host = CutWwwPrefix(host);
        List<ContentSettingException> exceptions = (incognitoTab)?
            WebsitePreferenceBridge.getContentSettingsExceptionsIncognito(ContentSettingsType.CONTENT_SETTINGS_TYPE_JAVASCRIPT):
            WebsitePreferenceBridge.getContentSettingsExceptions(ContentSettingsType.CONTENT_SETTINGS_TYPE_JAVASCRIPT);

        for (ContentSettingException exception : exceptions) {
            String pattern = exception.getPattern();

            pattern = CutWwwPrefix(pattern);
            if (!pattern.equals(host)) {
                continue;
            }

            if (ContentSetting.ALLOW == exception.getContentSetting()) {
                return true;
            } else {
                return false;
            }
        }

        if (incognitoTab) {
          //for incognito tab inherit settings from normal tab
          return isJavaScriptEnabled(false, host);
        }

        if (!PrefServiceBridge.getInstance().javaScriptEnabled()) {
            return false;
        }

        return true;
    }

    public boolean isTopShieldsEnabled(boolean incognitoTab, String host) {
        String settings = getHostSettings(incognitoTab, host);
        if (null != settings && 0 != settings.length() && '0' == settings.charAt(0)) {
            return false;
        }

        return true;
    }

    public boolean blockFingerprints(boolean incognitoTab, String host) {
        String settings = getHostSettings(incognitoTab, host);
        if (null == settings || settings.length() <= 10) {
            boolean prefFingerprintsDefault = false;
            boolean prefFingerprints = mSharedPreferences.getBoolean(
                    PREF_FINGERPRINTING_PROTECTION, prefFingerprintsDefault);

            return prefFingerprints;
        }
        if ('0' == settings.charAt(10)) {
            return false;
        }

        return true;
    }

    public void setBlockFingerprints(boolean incognitoTab, String host, boolean block) {
        host = CutWwwPrefix(host);
        try {
            mLock.writeLock().lock();
            String settings = getHostSettings(incognitoTab, host);
            if (settings.length() > 10) {
                if (!block) {
                    settings = settings.substring(0, 10) + "0";
                } else {
                    settings = settings.substring(0, 10) + "1";
                }
            } else {
              settings = composeSettings(isTopShieldsEnabled(incognitoTab, host), blockAdsAndTracking(incognitoTab, host),
              isHTTPSEverywhereEnabled(incognitoTab, host), isJavaScriptBlocked(incognitoTab, host),
              block3rdPartyCookies(incognitoTab, host), block);
            }
            settings = correctSettings(settings);
            if (incognitoTab) {
                mSettingsIncognito.put(host, settings);
            } else {
                mSettings.put(host, settings);
            }
        }
        finally {
            mLock.writeLock().unlock();
        }
        if (!incognitoTab) {
            new SaveDataAsyncTask().execute();
        }
    }

    class SaveDataAsyncTask extends AsyncTask<Void,Void,Long> {
        @Override
        protected Long doInBackground(Void... params) {
            saveSettings();

            return null;
        }
    }

    private void saveSettings() {
      File dataPath = new File(mContext.getApplicationInfo().dataDir, SHIELDS_CONFIG_LOCALFILENAME);
        try {
            FileOutputStream outputStream = new FileOutputStream(dataPath);
            boolean firstIteration = true;
            mLock.writeLock().lock();
            for (HashMap.Entry<String, String> entry : mSettings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!firstIteration) {
                    outputStream.write(";".getBytes(), 0, ";".length());
                }
                outputStream.write(key.getBytes(), 0, key.length());
                outputStream.write(",".getBytes(), 0, ",".length());
                outputStream.write(value.getBytes(), 0, value.length());
                firstIteration = false;
            }
            outputStream.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            mLock.writeLock().unlock();
        }
    }

    private static String CutWwwPrefix(String host) {
      if (null != host && host.startsWith("www.")) {
          host = host.substring("www.".length());
      }
      return host;
    }

    public void setTabModelSelectorTabObserver(TabModelSelectorTabObserver tabModelSelectorTabObserver) {
        mTabModelSelectorTabObserver = tabModelSelectorTabObserver;
    }

    @CalledByNative
    public String getHostSettings(boolean incognitoTab, String host) {
        host = CutWwwPrefix(host);
        try {
            mLock.readLock().lock();

            String settings = incognitoTab ? mSettingsIncognito.get(host) : mSettings.get(host);
            if (incognitoTab && null == settings) {
                settings = mSettings.get(host);
            }
            if (null != settings) {
                return settings;
            }
        }
        finally {
            mLock.readLock().unlock();
        }

        return "1";
    }

    @CalledByNative
    public void setBlockedCountInfo(String url, int trackersBlocked, int adsBlocked, int httpsUpgrades,
          int scriptsBlocked, int fingerprintingBlocked) {
      updateBraveStats(trackersBlocked, adsBlocked, httpsUpgrades);
      if (null != mTabModelSelectorTabObserver) {
          mTabModelSelectorTabObserver.onBraveShieldsCountUpdate(url, trackersBlocked + adsBlocked, httpsUpgrades,
              scriptsBlocked, fingerprintingBlocked);
      }
    }

    @CalledByNative
    public boolean needUpdateAdBlocker() {
      return ContextUtils.getAppSharedPreferences().getBoolean(UPDATE_ADBLOCKER, false);
    }

    @CalledByNative
    public void resetUpdateAdBlockerFlag() {
      ContextUtils.getAppSharedPreferences().edit()
        .putBoolean(UPDATE_ADBLOCKER, false)
        .apply();
    }

    private void updateBraveStats(int trackersBlocked, int adsBlocked, int httpsUpgrades) {
        long trackersBlockedCount = mSharedPreferences.getLong(PREF_TRACKERS_BLOCKED_COUNT, 0) + trackersBlocked;
        long adsBlockedCount = mSharedPreferences.getLong(PREF_ADS_BLOCKED_COUNT, 0) + adsBlocked;
        long httpsUpgradesCount = mSharedPreferences.getLong(PREF_HTTPS_UPGRADES_COUNT, 0) + httpsUpgrades;
        SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putLong(PREF_TRACKERS_BLOCKED_COUNT, trackersBlockedCount);
        sharedPreferencesEditor.putLong(PREF_ADS_BLOCKED_COUNT, adsBlockedCount);
        sharedPreferencesEditor.putLong(PREF_HTTPS_UPGRADES_COUNT, httpsUpgradesCount);
        sharedPreferencesEditor.apply();
    }

    private native void nativeInit();
    private native void nativeClear();
}
