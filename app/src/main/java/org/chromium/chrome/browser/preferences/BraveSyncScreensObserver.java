/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.preferences;

/**
 * Allows monitoring of JavaScript results.
 */
public interface BraveSyncScreensObserver {
    /**
     * Informs when the words code provided is incorrect
     */
    public void onWordsCodeWrong();

    /**
     * Informs when the words code provided is correct
     */
    public void onSeedReceived(String seed);
}
