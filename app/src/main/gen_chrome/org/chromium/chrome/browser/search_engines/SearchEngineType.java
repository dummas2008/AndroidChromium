
// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This file is autogenerated by
//     java_cpp_enum.py
// From
//     ../../components/search_engines/search_engine_type.h

package org.chromium.chrome.browser.search_engines;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
    SearchEngineType.SEARCH_ENGINE_UNKNOWN, SearchEngineType.SEARCH_ENGINE_OTHER,
    SearchEngineType.SEARCH_ENGINE_AOL, SearchEngineType.SEARCH_ENGINE_ASK,
    SearchEngineType.SEARCH_ENGINE_ATLAS, SearchEngineType.SEARCH_ENGINE_AVG,
    SearchEngineType.SEARCH_ENGINE_BAIDU, SearchEngineType.SEARCH_ENGINE_BABYLON,
    SearchEngineType.SEARCH_ENGINE_BING, SearchEngineType.SEARCH_ENGINE_CONDUIT,
    SearchEngineType.SEARCH_ENGINE_DAUM, SearchEngineType.SEARCH_ENGINE_DELFI,
    SearchEngineType.SEARCH_ENGINE_DELTA, SearchEngineType.SEARCH_ENGINE_ECOSIA,
    SearchEngineType.SEARCH_ENGINE_FUNMOODS, SearchEngineType.SEARCH_ENGINE_GOO,
    SearchEngineType.SEARCH_ENGINE_GOOGLE, SearchEngineType.SEARCH_ENGINE_IMINENT,
    SearchEngineType.SEARCH_ENGINE_IMESH, SearchEngineType.SEARCH_ENGINE_IN,
    SearchEngineType.SEARCH_ENGINE_INCREDIBAR, SearchEngineType.SEARCH_ENGINE_KVASIR,
    SearchEngineType.SEARCH_ENGINE_LIBERO, SearchEngineType.SEARCH_ENGINE_MAILRU,
    SearchEngineType.SEARCH_ENGINE_NAJDI, SearchEngineType.SEARCH_ENGINE_NATE,
    SearchEngineType.SEARCH_ENGINE_NAVER, SearchEngineType.SEARCH_ENGINE_NETI,
    SearchEngineType.SEARCH_ENGINE_NIGMA, SearchEngineType.SEARCH_ENGINE_OK,
    SearchEngineType.SEARCH_ENGINE_ONET, SearchEngineType.SEARCH_ENGINE_RAMBLER,
    SearchEngineType.SEARCH_ENGINE_SAPO, SearchEngineType.SEARCH_ENGINE_SEARCHNU,
    SearchEngineType.SEARCH_ENGINE_SEARCH_RESULTS, SearchEngineType.SEARCH_ENGINE_SEARX,
    SearchEngineType.SEARCH_ENGINE_SEZNAM, SearchEngineType.SEARCH_ENGINE_SNAPDO,
    SearchEngineType.SEARCH_ENGINE_SOFTONIC, SearchEngineType.SEARCH_ENGINE_SOGOU,
    SearchEngineType.SEARCH_ENGINE_SOSO, SearchEngineType.SEARCH_ENGINE_SWEETPACKS,
    SearchEngineType.SEARCH_ENGINE_TERRA, SearchEngineType.SEARCH_ENGINE_TUT,
    SearchEngineType.SEARCH_ENGINE_VINDEN, SearchEngineType.SEARCH_ENGINE_VIRGILIO,
    SearchEngineType.SEARCH_ENGINE_WALLA, SearchEngineType.SEARCH_ENGINE_WP,
    SearchEngineType.SEARCH_ENGINE_YAHOO, SearchEngineType.SEARCH_ENGINE_YANDEX,
    SearchEngineType.SEARCH_ENGINE_ZOZNAM, SearchEngineType.SEARCH_ENGINE_360,
    SearchEngineType.SEARCH_ENGINE_MAX, SearchEngineType.SEARCH_ENGINE_DUCKDUCKGO,
    SearchEngineType.SEARCH_ENGINE_AMAZON, SearchEngineType.SEARCH_ENGINE_GITHUB,
    SearchEngineType.SEARCH_ENGINE_STACKOVERFLOW, SearchEngineType.SEARCH_ENGINE_MDN,
    SearchEngineType.SEARCH_ENGINE_TWITTER, SearchEngineType.SEARCH_ENGINE_WIKIPEDIA,
    SearchEngineType.SEARCH_ENGINE_YOUTUBE, SearchEngineType.SEARCH_ENGINE_STARTPAGE,
    SearchEngineType.SEARCH_ENGINE_INFOGALACTIC, SearchEngineType.SEARCH_ENGINE_WOLFRAMALPHA,
    SearchEngineType.SEARCH_ENGINE_SEMANTICSCHOLAR, SearchEngineType.SEARCH_ENGINE_QWANT,
    SearchEngineType.SEARCH_ENGINE_DUCKDUCKGOLIGHT
})
@Retention(RetentionPolicy.SOURCE)
public @interface SearchEngineType {
  /**
   * Prepopulated engines.
   */
  int SEARCH_ENGINE_UNKNOWN = -1;
  int SEARCH_ENGINE_OTHER = 0;
  int SEARCH_ENGINE_AOL = 1;
  int SEARCH_ENGINE_ASK = 2;
  int SEARCH_ENGINE_ATLAS = 3;
  int SEARCH_ENGINE_AVG = 4;
  int SEARCH_ENGINE_BAIDU = 5;
  int SEARCH_ENGINE_BABYLON = 6;
  int SEARCH_ENGINE_BING = 7;
  int SEARCH_ENGINE_CONDUIT = 8;
  int SEARCH_ENGINE_DAUM = 9;
  int SEARCH_ENGINE_DELFI = 10;
  int SEARCH_ENGINE_DELTA = 11;
  int SEARCH_ENGINE_ECOSIA = 12;
  int SEARCH_ENGINE_FUNMOODS = 13;
  int SEARCH_ENGINE_GOO = 14;
  int SEARCH_ENGINE_GOOGLE = 15;
  int SEARCH_ENGINE_IMINENT = 16;
  int SEARCH_ENGINE_IMESH = 17;
  int SEARCH_ENGINE_IN = 18;
  int SEARCH_ENGINE_INCREDIBAR = 19;
  int SEARCH_ENGINE_KVASIR = 20;
  int SEARCH_ENGINE_LIBERO = 21;
  int SEARCH_ENGINE_MAILRU = 22;
  int SEARCH_ENGINE_NAJDI = 23;
  int SEARCH_ENGINE_NATE = 24;
  int SEARCH_ENGINE_NAVER = 25;
  int SEARCH_ENGINE_NETI = 26;
  int SEARCH_ENGINE_NIGMA = 27;
  int SEARCH_ENGINE_OK = 28;
  int SEARCH_ENGINE_ONET = 29;
  int SEARCH_ENGINE_RAMBLER = 30;
  int SEARCH_ENGINE_SAPO = 31;
  int SEARCH_ENGINE_SEARCHNU = 32;
  int SEARCH_ENGINE_SEARCH_RESULTS = 33;
  int SEARCH_ENGINE_SEARX = 34;
  int SEARCH_ENGINE_SEZNAM = 35;
  int SEARCH_ENGINE_SNAPDO = 36;
  int SEARCH_ENGINE_SOFTONIC = 37;
  int SEARCH_ENGINE_SOGOU = 38;
  int SEARCH_ENGINE_SOSO = 39;
  int SEARCH_ENGINE_SWEETPACKS = 40;
  int SEARCH_ENGINE_TERRA = 41;
  int SEARCH_ENGINE_TUT = 42;
  int SEARCH_ENGINE_VINDEN = 43;
  int SEARCH_ENGINE_VIRGILIO = 44;
  int SEARCH_ENGINE_WALLA = 45;
  int SEARCH_ENGINE_WP = 46;
  int SEARCH_ENGINE_YAHOO = 47;
  int SEARCH_ENGINE_YANDEX = 48;
  int SEARCH_ENGINE_ZOZNAM = 49;
  int SEARCH_ENGINE_360 = 50;
  int SEARCH_ENGINE_MAX = 51;
  int SEARCH_ENGINE_DUCKDUCKGO = 52;
  int SEARCH_ENGINE_AMAZON = 53;
  int SEARCH_ENGINE_GITHUB = 54;
  int SEARCH_ENGINE_STACKOVERFLOW = 55;
  int SEARCH_ENGINE_MDN = 56;
  int SEARCH_ENGINE_TWITTER = 57;
  int SEARCH_ENGINE_WIKIPEDIA = 58;
  int SEARCH_ENGINE_YOUTUBE = 59;
  int SEARCH_ENGINE_STARTPAGE = 60;
  int SEARCH_ENGINE_INFOGALACTIC = 61;
  int SEARCH_ENGINE_WOLFRAMALPHA = 62;
  int SEARCH_ENGINE_SEMANTICSCHOLAR = 63;
  int SEARCH_ENGINE_QWANT = 64;
  int SEARCH_ENGINE_DUCKDUCKGOLIGHT = 65;
}
