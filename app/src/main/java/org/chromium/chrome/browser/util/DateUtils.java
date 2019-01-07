/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.util;

import org.chromium.base.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static String getPreviousMondayDate(Calendar time) {
        if (time == null) {
          return null;
        }

        int daysToSubstract = 0;
        switch(time.get(Calendar.DAY_OF_WEEK)) {
          case Calendar.SUNDAY:
                daysToSubstract = 6;
                break;
            case Calendar.MONDAY:
                daysToSubstract = 7;
                break;
            default:
                daysToSubstract = time.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        }

        Calendar mondayOfPrevWeek = time;
        mondayOfPrevWeek.add(Calendar.DAY_OF_MONTH, -1 * daysToSubstract);

        assert mondayOfPrevWeek.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        return dateFormat.format(mondayOfPrevWeek.getTime());
    }

    //Tests
    //private static final String TAG = "DateUtils";
    // private static Calendar DateFromString(String s) {
    //     SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
    //     try {
    //         Date date = format.parse(s);
    //         Calendar calendar = Calendar.getInstance();
    //         calendar.setTime(date);
    //         return calendar;
    //     } catch(Exception e) {}
    //     return null;
    // }
    //
    // public static void testGetPreviousMondayDate() {
    //     Log.i(TAG, "testGetPreviousMondayDatechecking AAAA-AA => null");
    //     String s0 = getPreviousMondayDate(DateFromString("AAAA-AAA"));
    //     assert s0 == null;
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2017-11-26 => 2017-11-20");
    //     String s1 = getPreviousMondayDate(DateFromString("2017-11-26"));
    //     assert s1.equals("2017-11-20");
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2017-11-21 => 2017-11-20");
    //     String s2 = getPreviousMondayDate(DateFromString("2017-11-21"));
    //     assert s2.equals("2017-11-20");
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2017-11-20 => 2017-11-13");
    //     String s3 = getPreviousMondayDate(DateFromString("2017-11-20"));
    //     assert s3.equals("2017-11-13");
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2017-11-24 => 2017-11-20");
    //     String s4 = getPreviousMondayDate(DateFromString("2017-11-24"));
    //     assert s4.equals("2017-11-20");
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2018-01-01 => 2017-12-25");
    //     String s5 = getPreviousMondayDate(DateFromString("2018-01-01"));
    //     assert s5.equals("2017-12-25");
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2017-12-01 => 2017-11-27");
    //     String s6 = getPreviousMondayDate(DateFromString("2017-12-01"));
    //     assert s6.equals("2017-11-27");
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2016-01-01 => 2015-12-28");
    //     String s7 = getPreviousMondayDate(DateFromString("2016-01-01"));
    //     assert s7.equals("2015-12-28");
    //
    //     Log.i(TAG, "testGetPreviousMondayDatechecking 2017-01-01 => 2016-12-26");
    //     String s8 = getPreviousMondayDate(DateFromString("2017-01-01"));
    //     assert s8.equals("2016-12-26");
    // }
}
