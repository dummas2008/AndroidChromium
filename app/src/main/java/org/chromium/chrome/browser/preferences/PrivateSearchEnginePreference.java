package org.chromium.chrome.browser.preferences;

import android.os.Bundle;
import org.chromium.chrome.R;

/**
* A preference fragment for selecting a default private search engine.
*/
public class PrivateSearchEnginePreference extends SearchEnginePreference {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.prefs_private_search_engine);
        mSearchEngineAdapter = new SearchEngineAdapter(getActivity(), true);
    }
}
