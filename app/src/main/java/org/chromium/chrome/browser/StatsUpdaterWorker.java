/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser;

import android.content.Context;

import org.chromium.base.Log;
import org.chromium.chrome.browser.init.StatsUpdater;

public class StatsUpdaterWorker {

    private static final int INTERVAL_TO_UPDATE = 1000 * 60 * 20;    // Milliseconds

    private UpdateThread mUpdateThread = null;

    private Context mContext;
    private boolean mStopThread = false;

    public StatsUpdaterWorker(Context context) {
        mContext = context;
        mUpdateThread = new UpdateThread();
        if (null != mUpdateThread) {
            mUpdateThread.start();
        }
    }

    public void Stop() {
        mStopThread = true;
        if (mUpdateThread != null) {
            mUpdateThread.interrupt();
            mUpdateThread = null;
        }
    }

    class UpdateThread extends Thread {
        @Override
        public void run() {
          for (;;) {
              try {
                  Thread.sleep(StatsUpdaterWorker.INTERVAL_TO_UPDATE);
                  StatsUpdater.UpdateStats(mContext);
              }
              catch(Exception exc) {
                  // Just ignore it if we cannot update
                  Log.i("TAG", "Update loop exception: " + exc);
              }
              if (mStopThread) {
                  break;
              }
          }
        }
    }
}
