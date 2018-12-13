// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.init;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.StrictMode;

import com.squareup.leakcanary.LeakCanary;

import org.chromium.base.ActivityState;
import org.chromium.base.ApplicationStatus;
import org.chromium.base.ApplicationStatus.ActivityStateListener;
import org.chromium.base.AsyncTask;
import org.chromium.base.CommandLine;
import org.chromium.base.ContentUriUtils;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.PathUtils;
import org.chromium.base.SysUtils;
import org.chromium.base.ThreadUtils;
import org.chromium.base.TraceEvent;
import org.chromium.base.annotations.RemovableInRelease;
import org.chromium.base.library_loader.LibraryLoader;
import org.chromium.base.library_loader.LibraryProcessType;
import org.chromium.base.library_loader.ProcessInitException;
import org.chromium.base.memory.MemoryPressureUma;
import org.chromium.chrome.browser.AppHooks;
import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.ChromeStrictMode;
import org.chromium.chrome.browser.ChromeSwitches;
import org.chromium.chrome.browser.FileProviderHelper;
import org.chromium.chrome.browser.crash.LogcatExtractionRunnable;
import org.chromium.chrome.browser.download.DownloadManagerService;
import org.chromium.chrome.browser.init.InstallationSourceInformer;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.privacy.PrivacyPreferencesManager;
import org.chromium.chrome.browser.services.GoogleServicesManager;
import org.chromium.chrome.browser.tabmodel.document.DocumentTabModelImpl;
import org.chromium.chrome.browser.util.PackageUtils;
import org.chromium.chrome.browser.webapps.ActivityAssigner;
import org.chromium.chrome.browser.webapps.ChromeWebApkHost;
import org.chromium.components.crash.browser.CrashDumpManager;
import org.chromium.content_public.browser.BrowserStartupController;
import org.chromium.content_public.browser.DeviceUtils;
import org.chromium.content_public.browser.SpeechRecognition;
import org.chromium.net.NetworkChangeNotifier;
import org.chromium.policy.CombinedPolicyProvider;
import org.chromium.ui.resources.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Application level delegate that handles start up tasks.
 * {@link AsyncInitializationActivity} classes should override the {@link BrowserParts}
 * interface for any additional initialization tasks for the initialization to work as intended.
 */
public class ChromeBrowserInitializer {
    private static final String TAG = "BrowserInitializer";
    private static final String PREF_ALLOW_3RD_PARTY_COOKIES_SWITCHED = "allow_3rd_party_cookies_switched";
    private static ChromeBrowserInitializer sChromeBrowserInitializer;
    private static BrowserStartupController sBrowserStartupController;
    private final ChromeApplication mApplication;
    private final Locale mInitialLocale = Locale.getDefault();

    private boolean mPreInflationStartupComplete;
    private boolean mPostInflationStartupComplete;
    private boolean mNativeInitializationComplete;
    private boolean mNetworkChangeNotifierInitializationComplete;

    private boolean mAdBlockInitCalled = false;
    private boolean mUpdateStatsCalled = false;
    private boolean mInstallationSourceChecked = false;
    private boolean mSearchSuggestSwitched = false;

    // Public to allow use in ChromeBackupAgent
    public static final String PRIVATE_DATA_DIRECTORY_SUFFIX = "chrome";

    /**
     * A callback to be executed when there is a new version available in Play Store.
     */
    public interface OnNewVersionAvailableCallback extends Runnable {
        /**
         * Set the update url to get the new version available.
         * @param updateUrl The url to be used.
         */
        void setUpdateUrl(String updateUrl);
    }

    /**
     * This class is an application specific object that orchestrates the app initialization.
     * @return The singleton instance of {@link ChromeBrowserInitializer}.
     */
    public static ChromeBrowserInitializer getInstance() {
        if (sChromeBrowserInitializer == null) {
            sChromeBrowserInitializer = new ChromeBrowserInitializer();
        }
        return sChromeBrowserInitializer;
    }

    /**
     * This class is an application specific object that orchestrates the app initialization.
     * @deprecated Use getInstance with no arguments instead.
     * @param context The context to get the application context from.
     * @return The singleton instance of {@link ChromeBrowserInitializer}.
     */
    public static ChromeBrowserInitializer getInstance(Context context) {
        return getInstance();
    }

    private ChromeBrowserInitializer() {
        mApplication = (ChromeApplication) ContextUtils.getApplicationContext();
        initLeakCanary();
    }

    private void InitAdBlock() {
      Log.i(TAG, "Init AdBlock");
      if (mAdBlockInitCalled) {
          Log.i(TAG, "Init AdBlock was already called");

          return;
      }
      mAdBlockInitCalled = true;
      // Download tracking protection, adblock annd HTTPSE files lists
      PathUtils.setPrivateDataDirectorySuffix(ADBlockUtils.PRIVATE_DATA_DIRECTORY_SUFFIX);
      new UpdateADBlockAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      boolean alreadySwitched = ContextUtils.getAppSharedPreferences().getBoolean(PREF_ALLOW_3RD_PARTY_COOKIES_SWITCHED, false);
      if (!alreadySwitched) {
        PrefServiceBridge.getInstance().setBlockThirdPartyCookiesEnabled(true);
        ContextUtils.getAppSharedPreferences().edit()
          .putBoolean(PREF_ALLOW_3RD_PARTY_COOKIES_SWITCHED, true)
          .apply();
      }
      Log.i(TAG, "Started AdBlock async tasks");
    }

    private void UpdateStats() {
      if (mUpdateStatsCalled) {
          return;
      }
      mUpdateStatsCalled = true;
      new Thread(new Runnable() {
          @Override
          public void run () {
              try {
                  StatsUpdater.UpdateStats(ContextUtils.getApplicationContext());
              }
              catch(Exception exc) {
                  // Just ignore it if we cannot update
              }
          }
      }).start();
    }

    private void CheckInstallationSource() {
      if (mInstallationSourceChecked) {
        return;
      }

      mInstallationSourceChecked = true;
      new CheckInstallationSourceAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void SwitchSearchSuggestEnabled() {
      if (mSearchSuggestSwitched) {
        return;
      }

      mSearchSuggestSwitched = true;
      new SwitchSearchSuggestAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // ADBlock update
    class UpdateADBlockAsyncTask extends AsyncTask<Long> {
        @Override
        protected Long doInBackground() {
            try {
                ADBlockUpdater.UpdateADBlock(mApplication.getApplicationContext(), true);
            }
            catch(Exception exc) {
                // Just ignore it if we cannot update
            }

            return null;
        }
    }

    class CheckInstallationSourceAsyncTask extends AsyncTask<Long> {
       @Override
       protected Long doInBackground() {
           try {
             Context context = mApplication.getApplicationContext();
             // A list with valid installers package name
             List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));

             // The package name of the app that has installed your app
             final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
             Log.i(TAG, "Installation source detection, installer=" + installer);

             // true if your app has been downloaded from Play Store
             boolean fromPlayStore = installer != null && validInstallers.contains(installer);
             Log.i(TAG, "Installation source detection, fromPlayStore="+fromPlayStore);
             if (!fromPlayStore) {
               InstallationSourceInformer.InformFromOther();
             }
           }
           catch(Exception exc) {
               // not critical
               Log.i(TAG, "Installation source detection: ex " + exc);
           }

           return null;
       }
    }

    // Search suggestions are turned off by default in Brave since 1.0.38
    // The users who had updated from 1.0.37 and earlier versions and had not
    // touched this setting, should have it still turned on.
    // For the first installation we switch it to false to simulate 'default false'.
    class SwitchSearchSuggestAsyncTask extends AsyncTask<Long>
    {
        private static final String PREF_SEARCH_SUGGESTIONS_SWITCHED_DEFAULT_FALSE = "search_suggestions_switched_to_default_false";

        @Override
        protected Long doInBackground() {
            try {
                boolean alreadySwitched = ContextUtils.getAppSharedPreferences().getBoolean(PREF_SEARCH_SUGGESTIONS_SWITCHED_DEFAULT_FALSE, false);
                if (!alreadySwitched) {
                    if (PackageUtils.isFirstInstall(mApplication.getApplicationContext())) {
                        ThreadUtils.postOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                PrefServiceBridge.getInstance().setSearchSuggestEnabled(false);
                            }
                        });
                    }

                    ContextUtils.getAppSharedPreferences().edit()
                      .putBoolean(PREF_SEARCH_SUGGESTIONS_SWITCHED_DEFAULT_FALSE, true)
                      .apply();
                }
            }
            catch(Exception exc) {
                // not critical
            }

            return null;
        }
    }

    @RemovableInRelease
    private void initLeakCanary() {
        // Watch that Activity objects are not retained after their onDestroy() has been called.
        // This is a no-op in release builds.
        LeakCanary.install(mApplication);
    }

    /**
     * @return whether native initialization is complete.
     */
    public boolean hasNativeInitializationCompleted() {
        return mNativeInitializationComplete;
    }

    /**
     * Initializes the Chrome browser process synchronously.
     *
     * @throws ProcessInitException if there is a problem with the native library.
     */
    public void handleSynchronousStartup() throws ProcessInitException {
        handleSynchronousStartupInternal(false);
    }

    /**
     * Initializes the Chrome browser process synchronously with GPU process warmup.
     */
    public void handleSynchronousStartupWithGpuWarmUp() throws ProcessInitException {
        handleSynchronousStartupInternal(true);
    }

    private void handleSynchronousStartupInternal(final boolean startGpuProcess)
            throws ProcessInitException {
        ThreadUtils.checkUiThread();

        BrowserParts parts = new EmptyBrowserParts() {
            @Override
            public boolean shouldStartGpuProcess() {
                return startGpuProcess;
            }
        };
        handlePreNativeStartup(parts);
        handlePostNativeStartup(false, parts);
    }

    /**
     * Execute startup tasks that can be done without native libraries. See {@link BrowserParts} for
     * a list of calls to be implemented.
     * @param parts The delegate for the {@link ChromeBrowserInitializer} to communicate
     *              initialization tasks.
     */
    public void handlePreNativeStartup(final BrowserParts parts) {
        ThreadUtils.checkUiThread();

        ProcessInitializationHandler.getInstance().initializePreNative();
        preInflationStartup();
        parts.preInflationStartup();
        if (parts.isActivityFinishing()) return;

        preInflationStartupDone();
        parts.setContentViewAndLoadLibrary(() -> this.onInflationComplete(parts));
    }

    /**
     * This is called after the layout inflation has been completed (in the callback sent to {@link
     * BrowserParts#setContentViewAndLoadLibrary}). This continues the post-inflation pre-native
     * startup tasks. Namely {@link BrowserParts#postInflationStartup()}.
     * @param parts The {@link BrowserParts} that has finished layout inflation
     */
    private void onInflationComplete(final BrowserParts parts) {
        if (parts.isActivityFinishing()) return;
        postInflationStartup();
        parts.postInflationStartup();
    }

    /**
     * This is needed for device class manager which depends on commandline args that are
     * initialized in preInflationStartup()
     */
    private void preInflationStartupDone() {
        // Domain reliability uses significant enough memory that we should disable it on low memory
        // devices for now.
        // TODO(zbowling): remove this after domain reliability is refactored. (crbug.com/495342)
        if (SysUtils.isLowEndDevice()) {
            CommandLine.getInstance().appendSwitch(ChromeSwitches.DISABLE_DOMAIN_RELIABILITY);
        }
    }

    /**
     * Pre-load shared prefs to avoid being blocked on the disk access async task in the future.
     * Running in an AsyncTask as pre-loading itself may cause I/O.
     */
    private void warmUpSharedPrefs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            new AsyncTask<Void>() {
                @Override
                protected Void doInBackground() {
                    ContextUtils.getAppSharedPreferences();
                    DocumentTabModelImpl.warmUpSharedPrefs(mApplication);
                    ActivityAssigner.warmUpSharedPrefs(mApplication);
                    DownloadManagerService.warmUpSharedPrefs();
                    return null;
                }
            }
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            ContextUtils.getAppSharedPreferences();
            DocumentTabModelImpl.warmUpSharedPrefs(mApplication);
            ActivityAssigner.warmUpSharedPrefs(mApplication);
            DownloadManagerService.warmUpSharedPrefs();
        }
    }

    private void preInflationStartup() {
        ThreadUtils.assertOnUiThread();
        if (mPreInflationStartupComplete) return;
        PathUtils.setPrivateDataDirectorySuffix(PRIVATE_DATA_DIRECTORY_SUFFIX);

        // Ensure critical files are available, so they aren't blocked on the file-system
        // behind long-running accesses in next phase.
        // Don't do any large file access here!
        ChromeStrictMode.configureStrictMode();
        ChromeWebApkHost.init();

        warmUpSharedPrefs();

        DeviceUtils.addDeviceSpecificUserAgentSwitch();
        ApplicationStatus.registerStateListenerForAllActivities(
                createActivityStateListener());

        mPreInflationStartupComplete = true;
    }

    private void postInflationStartup() {
        ThreadUtils.assertOnUiThread();
        if (mPostInflationStartupComplete) return;

        // Check to see if we need to extract any new resources from the APK. This could
        // be on first run when we need to extract all the .pak files we need, or after
        // the user has switched locale, in which case we want new locale resources.
        ResourceExtractor.get().startExtractingResources();

        mPostInflationStartupComplete = true;
    }

    /**
     * Execute startup tasks that require native libraries to be loaded. See {@link BrowserParts}
     * for a list of calls to be implemented.
     * @param isAsync Whether this call should synchronously wait for the browser process to be
     *                fully initialized before returning to the caller.
     * @param delegate The delegate for the {@link ChromeBrowserInitializer} to communicate
     *                 initialization tasks.
     */
    public void handlePostNativeStartup(final boolean isAsync, final BrowserParts delegate)
            throws ProcessInitException {
        assert ThreadUtils.runningOnUiThread() : "Tried to start the browser on the wrong thread";
        if (!mPostInflationStartupComplete) {
            throw new IllegalStateException(
                    "ChromeBrowserInitializer.handlePostNativeStartup called before "
                    + "ChromeBrowserInitializer.postInflationStartup has been run.");
        }
        final ChainedTasks tasks = new ChainedTasks();
        // If full browser process is not going to be launched, it is up to individual service to
        // launch its required components.
        if (!delegate.startServiceManagerOnly()
                && !ProcessInitializationHandler.getInstance().postNativeInitializationComplete()) {
            tasks.add(new Runnable() {
                @Override
                public void run() {
                    ProcessInitializationHandler.getInstance().initializePostNative();
                }
            });
        }

        if (!mNetworkChangeNotifierInitializationComplete) {
            tasks.add(new Runnable() {
                @Override
                public void run() {
                    initNetworkChangeNotifier();
                }
            });
        }

        tasks.add(new Runnable() {
            @Override
            public void run() {
                // This is not broken down as a separate task, since this:
                // 1. Should happen as early as possible
                // 2. Only submits asynchronous work
                // 3. Is thus very cheap (profiled at 0.18ms on a Nexus 5 with Lollipop)
                // It should also be in a separate task (and after) initNetworkChangeNotifier, as
                // this posts a task to the UI thread that would interfere with preconneciton
                // otherwise. By preconnecting afterwards, we make sure that this task has run.
                delegate.maybePreconnect();

                onStartNativeInitialization();
            }
        });

        tasks.add(new Runnable() {
            @Override
            public void run() {
                if (delegate.isActivityDestroyed()) return;
                delegate.initializeCompositor();
            }
        });

        tasks.add(new Runnable() {
            @Override
            public void run() {
                if (delegate.isActivityDestroyed()) return;
                delegate.initializeState();
            }
        });

        if (!mNativeInitializationComplete) {
            tasks.add(new Runnable() {
                @Override
                public void run() {
                    onFinishNativeInitialization();
                }
            });
        }

        tasks.add(new Runnable() {
            @Override
            public void run() {
                if (delegate.isActivityDestroyed()) return;
                delegate.finishNativeInitialization();
            }
        });

        if (isAsync) {
            // We want to start this queue once the C++ startup tasks have run; allow the
            // C++ startup to run asynchonously, and set it up to start the Java queue once
            // it has finished.
            startChromeBrowserProcessesAsync(delegate.shouldStartGpuProcess(),
                    delegate.startServiceManagerOnly(),
                    new BrowserStartupController.StartupCallback() {
                        @Override
                        public void onFailure() {
                            delegate.onStartupFailure();
                        }

                        @Override
                        public void onSuccess() {
                            tasks.start(false);
                        }
                    });
        } else {
            startChromeBrowserProcessesSync();
            tasks.start(true);
        }
    }

    private void startChromeBrowserProcessesAsync(boolean startGpuProcess,
            boolean startServiceManagerOnly, BrowserStartupController.StartupCallback callback)
            throws ProcessInitException {
        try {
            TraceEvent.begin("ChromeBrowserInitializer.startChromeBrowserProcessesAsync");
            getBrowserStartupController().startBrowserProcessesAsync(
                    startGpuProcess, startServiceManagerOnly, callback);
        } finally {
            TraceEvent.end("ChromeBrowserInitializer.startChromeBrowserProcessesAsync");
        }
    }

    private void startChromeBrowserProcessesSync() throws ProcessInitException {
        try {
            TraceEvent.begin("ChromeBrowserInitializer.startChromeBrowserProcessesSync");
            ThreadUtils.assertOnUiThread();
            StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
            LibraryLoader.getInstance().ensureInitialized(LibraryProcessType.PROCESS_BROWSER);
            StrictMode.setThreadPolicy(oldPolicy);
            LibraryLoader.getInstance().asyncPrefetchLibrariesToMemory();
            getBrowserStartupController().startBrowserProcessesSync(false);
            GoogleServicesManager.get(mApplication);
        } finally {
            TraceEvent.end("ChromeBrowserInitializer.startChromeBrowserProcessesSync");
        }
    }

    private BrowserStartupController getBrowserStartupController() {
        if (sBrowserStartupController == null) {
            sBrowserStartupController =
                    BrowserStartupController.get(LibraryProcessType.PROCESS_BROWSER);
        }
        return sBrowserStartupController;
    }

    public void initNetworkChangeNotifier() {
        if (mNetworkChangeNotifierInitializationComplete) return;
        mNetworkChangeNotifierInitializationComplete = true;

        ThreadUtils.assertOnUiThread();
        TraceEvent.begin("NetworkChangeNotifier.init");
        // Enable auto-detection of network connectivity state changes.
        NetworkChangeNotifier.init();
        NetworkChangeNotifier.setAutoDetectConnectivityState(true);
        TraceEvent.end("NetworkChangeNotifier.init");
    }

    private void onStartNativeInitialization() {
        ThreadUtils.assertOnUiThread();
        if (mNativeInitializationComplete) return;
        // The policies are used by browser startup, so we need to register the policy providers
        // before starting the browser process.
        AppHooks.get().registerPolicyProviders(CombinedPolicyProvider.get());

        SpeechRecognition.initialize(mApplication);
    }

    private void onFinishNativeInitialization() {
        if (mNativeInitializationComplete) return;

        mNativeInitializationComplete = true;
        ContentUriUtils.setFileProviderUtil(new FileProviderHelper());

        // When a minidump is detected, extract and append a logcat to it, then upload it to the
        // crash server. Note that the logcat extraction might fail. This is ok; in that case, the
        // minidump will be found and uploaded upon the next browser launch.
        CrashDumpManager.registerUploadCallback(new CrashDumpManager.UploadMinidumpCallback() {
            @Override
            public void tryToUploadMinidump(File minidump) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new LogcatExtractionRunnable(minidump));
            }
        });

        MemoryPressureUma.initializeForBrowser();

        InitAdBlock();
        UpdateStats();
        CheckInstallationSource();
        SwitchSearchSuggestEnabled();
    }

    private ActivityStateListener createActivityStateListener() {
        return new ActivityStateListener() {
            @Override
            public void onActivityStateChange(Activity activity, int newState) {
                if (newState == ActivityState.CREATED || newState == ActivityState.DESTROYED) {
                    // Android destroys Activities at some point after a locale change, but doesn't
                    // kill the process.  This can lead to a bug where Chrome is halfway RTL, where
                    // stale natively-loaded resources are not reloaded (http://crbug.com/552618).
                    if (!mInitialLocale.equals(Locale.getDefault())) {
                        Log.e(TAG, "Killing process because of locale change.");
                        Process.killProcess(Process.myPid());
                    }
                }
            }
        };
    }

    /**
     * For unit testing of clients.
     * @param initializer The (dummy or mocked) initializer to use.
     */
    public static void setForTesting(ChromeBrowserInitializer initializer) {
        sChromeBrowserInitializer = initializer;
    }

    /**
     * Set {@link BrowserStartupController) to use for unit testing.
     * @param controller The (dummy or mocked) {@link BrowserStartupController) instance.
     */
    public static void setBrowserStartupControllerForTesting(BrowserStartupController controller) {
        sBrowserStartupController = controller;
    }
}
