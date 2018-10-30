package org.chromium.chrome.browser.preferences;

import android.os.Bundle;
import org.chromium.chrome.R;

/**
* A preference fragment for selecting a default standard search engine.
*/
public class StandardSearchEnginePreference extends SearchEnginePreference {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.prefs_standard_search_engine);
        mSearchEngineAdapter = new SearchEngineAdapter(getActivity(), false);
    }
}
