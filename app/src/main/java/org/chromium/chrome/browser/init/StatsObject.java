/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.init;

public class StatsObject {
    public long mMilliSeconds;
    public long mMilliSecondsForWeeklyStat;
    public int mMonth;
    public int mYear;

    public StatsObject() {
        mMilliSeconds = 0;
        mMilliSecondsForWeeklyStat = 0;
        mMonth = 0;
        mYear = 0;
    }
}
