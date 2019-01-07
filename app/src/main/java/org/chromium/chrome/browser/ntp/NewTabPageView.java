// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.ntp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.base.TraceEvent;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.ContextUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.compositor.layouts.content.InvalidationAwareThumbnailProvider;
import org.chromium.chrome.browser.ntp.NewTabPage.FakeboxDelegate;
import org.chromium.chrome.browser.ntp.cards.NewTabPageAdapter;
import org.chromium.chrome.browser.ntp.cards.NewTabPageRecyclerView;
import org.chromium.chrome.browser.offlinepages.OfflinePageBridge;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.suggestions.SuggestionsConfig;
import org.chromium.chrome.browser.suggestions.SuggestionsDependencyFactory;
import org.chromium.chrome.browser.suggestions.SuggestionsUiDelegate;
import org.chromium.chrome.browser.suggestions.TileGroup;
import org.chromium.chrome.browser.tab.EmptyTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.util.FeatureUtilities;
import org.chromium.chrome.browser.util.ViewUtils;
import org.chromium.chrome.browser.widget.displaystyle.UiConfig;

/**
 * The native new tab page, represented by some basic data such as title and url, and an Android
 * View that displays the page.
 */
public class NewTabPageView extends FrameLayout {
    private static final String TAG = "NewTabPageView";

    /*
    * For Brave stats
    */
    private static final String PREF_TRACKERS_BLOCKED_COUNT = "trackers_blocked_count";
    private static final String PREF_ADS_BLOCKED_COUNT = "ads_blocked_count";
    private static final String PREF_HTTPS_UPGRADES_COUNT = "https_upgrades_count";
    private static final short MILLISECONDS_PER_ITEM = 50;

    private NewTabPageRecyclerView mRecyclerView;

    private NewTabPageLayout mNewTabPageLayout;
    private ViewGroup mBraveStatsView;
    private ImageView mBraveStatsShadow;

    private NewTabPageManager mManager;
    private Tab mTab;
    private SnapScrollHelper mSnapScrollHelper;
    private UiConfig mUiConfig;
    private Runnable mUpdateSearchBoxOnScrollRunnable;

    private boolean mNewTabPageRecyclerViewChanged;
    private int mSnapshotWidth;
    private int mSnapshotHeight;
    private int mSnapshotScrollY;
    private ContextMenuManager mContextMenuManager;
    private SharedPreferences mSharedPreferences;

    /**
     * Manages the view interaction with the rest of the system.
     */
    public interface NewTabPageManager extends SuggestionsUiDelegate {
        /** @return Whether the location bar is shown in the NTP. */
        boolean isLocationBarShownInNTP();

        /** @return Whether voice search is enabled and the microphone should be shown. */
        boolean isVoiceSearchEnabled();

        /**
         * Animates the search box up into the omnibox and bring up the keyboard.
         * @param beginVoiceSearch Whether to begin a voice search.
         * @param pastedText Text to paste in the omnibox after it's been focused. May be null.
         */
        void focusSearchBox(boolean beginVoiceSearch, String pastedText);

        /**
         * @return whether the {@link NewTabPage} associated with this manager is the current page
         * displayed to the user.
         */
        boolean isCurrentPage();

        /**
         * Called when the NTP has completely finished loading (all views will be inflated
         * and any dependent resources will have been loaded).
         */
        void onLoadingComplete();
    }

    /**
     * Default constructor required for XML inflation.
     */
    public NewTabPageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRecyclerView = new NewTabPageRecyclerView(getContext());

        // Don't attach now, the recyclerView itself will determine when to do it.
        mNewTabPageLayout = (NewTabPageLayout) LayoutInflater.from(getContext())
                                    .inflate(R.layout.new_tab_page_layout, mRecyclerView, false);
    }

    /**
     * Initializes the NTP. This must be called immediately after inflation, before this object is
     * used in any other way.
     *
     * @param manager NewTabPageManager used to perform various actions when the user interacts
     *                with the page.
     * @param tab The Tab that is showing this new tab page.
     * @param searchProviderHasLogo Whether the search provider has a logo.
     * @param searchProviderIsGoogle Whether the search provider is Google.
     * @param scrollPosition The adapter scroll position to initialize to.
     */
    public void initialize(NewTabPageManager manager, Tab tab, TileGroup.Delegate tileGroupDelegate,
            boolean searchProviderHasLogo, boolean searchProviderIsGoogle, int scrollPosition) {
        TraceEvent.begin(TAG + ".initialize()");
        mTab = tab;
        mSharedPreferences = ContextUtils.getAppSharedPreferences();
        mManager = manager;
        mUiConfig = new UiConfig(this);

        assert manager.getSuggestionsSource() != null;

        // Don't store a direct reference to the activity, because it might change later if the tab
        // is reparented.
        Runnable closeContextMenuCallback = () -> mTab.getActivity().closeContextMenu();
        mContextMenuManager = new ContextMenuManager(mManager.getNavigationDelegate(),
                mRecyclerView::setTouchEnabled, closeContextMenuCallback, false);
        mTab.getWindowAndroid().addContextMenuCloseListener(mContextMenuManager);

        mNewTabPageLayout.initialize(manager, tab, tileGroupDelegate, searchProviderHasLogo,
                searchProviderIsGoogle, mRecyclerView, mContextMenuManager, mUiConfig);

        mSnapScrollHelper = new SnapScrollHelper(mManager, mNewTabPageLayout, mRecyclerView);
        mRecyclerView.setSnapScrollHelper(mSnapScrollHelper);
        addView(mRecyclerView);

        mRecyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean animateMove(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
                // If |mNewTabPageLayout| is animated by the RecyclerView because an item below it
                // was dismissed, avoid also manipulating its vertical offset in our scroll handling
                // at the same time. The onScrolled() method is called when an item is dismissed and
                // the item at the top of the viewport is repositioned.
                if (holder.itemView == mNewTabPageLayout) mNewTabPageLayout.setIsViewMoving(true);

                // Cancel any pending scroll update handling, a new one will be scheduled in
                // onAnimationFinished().
                mRecyclerView.removeCallbacks(mUpdateSearchBoxOnScrollRunnable);

                return super.animateMove(holder, fromX, fromY, toX, toY);
            }

            @Override
            public void onAnimationFinished(ViewHolder viewHolder) {
                super.onAnimationFinished(viewHolder);

                // When an item is dismissed, the items at the top of the viewport might not move,
                // and onScrolled() might not be called. We can get in the situation where the
                // toolbar buttons disappear, so schedule an update for it. This can be cancelled
                // from animateMove() in case |mNewTabPageLayout| will be moved. We don't know that
                // from here, as the RecyclerView will animate multiple items when one is dismissed,
                // and some will "finish" synchronously if they are already in the correct place,
                // before other moves have even been scheduled.
                if (viewHolder.itemView == mNewTabPageLayout) {
                    mNewTabPageLayout.setIsViewMoving(false);
                }
                mRecyclerView.removeCallbacks(mUpdateSearchBoxOnScrollRunnable);
                mRecyclerView.post(mUpdateSearchBoxOnScrollRunnable);
            }
        });

        Profile profile = Profile.getLastUsedProfile();
        OfflinePageBridge offlinePageBridge =
                SuggestionsDependencyFactory.getInstance().getOfflinePageBridge(profile);

        mBraveStatsView = (ViewGroup)mNewTabPageLayout.findViewById(R.id.brave_stats);
        mBraveStatsShadow = (ImageView)mNewTabPageLayout.findViewById(R.id.brave_stats_shadow);

        mUpdateSearchBoxOnScrollRunnable = mNewTabPageLayout::updateSearchBoxOnScroll;

        initializeLayoutChangeListener();
        mNewTabPageLayout.setSearchProviderInfo(searchProviderHasLogo, searchProviderIsGoogle);

        mRecyclerView.init(mUiConfig, mContextMenuManager);

        // Set up snippets
        NewTabPageAdapter newTabPageAdapter = new NewTabPageAdapter(
                mManager, mNewTabPageLayout, mUiConfig, offlinePageBridge, mContextMenuManager);
        newTabPageAdapter.refreshSuggestions();
        mRecyclerView.setAdapter(newTabPageAdapter);
        mRecyclerView.getLinearLayoutManager().scrollToPosition(scrollPosition);

        setupScrollHandling();

        // When the NewTabPageAdapter's data changes we need to invalidate any previous
        // screen captures of the NewTabPageView.
        newTabPageAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onChanged() {
                mNewTabPageRecyclerViewChanged = true;
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                onChanged();
            }
        });

        manager.addDestructionObserver(NewTabPageView.this::onDestroy);

        TraceEvent.end(TAG + ".initialize()");
    }

    /**
     * @return The {@link NewTabPageLayout} displayed in this NewTabPageView.
     */
    NewTabPageLayout getNewTabPageLayout() {
        return mNewTabPageLayout;
    }

    /**
     * Sets the {@link FakeboxDelegate} associated with the new tab page.
     * @param fakeboxDelegate The {@link FakeboxDelegate} used to determine whether the URL bar
     *                        has focus.
     */
    public void setFakeboxDelegate(FakeboxDelegate fakeboxDelegate) {
        mRecyclerView.setFakeboxDelegate(fakeboxDelegate);
    }

    private void initializeLayoutChangeListener() {
        TraceEvent.begin(TAG + ".initializeLayoutChangeListener()");

        // Listen for layout changes on the NewTabPageView itself to catch changes in scroll
        // position that are due to layout changes after e.g. device rotation. This contrasts with
        // regular scrolling, which is observed through an OnScrollListener.
        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight,
                                          oldBottom) -> { mSnapScrollHelper.handleScroll(); });
        TraceEvent.end(TAG + ".initializeLayoutChangeListener()");
    }

    /**
     * Sets up Brave stats.
     */
    private void updateBraveStats() {
        TraceEvent.begin(TAG + ".updateBraveStats()");
        long trackersBlockedCount = mSharedPreferences.getLong(PREF_TRACKERS_BLOCKED_COUNT, 0);
        long adsBlockedCount = mSharedPreferences.getLong(PREF_ADS_BLOCKED_COUNT, 0);
        long httpsUpgradesCount = mSharedPreferences.getLong(PREF_HTTPS_UPGRADES_COUNT, 0);
        long estimatedMillisecondsSaved = (trackersBlockedCount + adsBlockedCount) * MILLISECONDS_PER_ITEM;
        TextView trackersBlockedCountTextView = (TextView) mBraveStatsView.findViewById(R.id.brave_stats_text_trackers_count);
        TextView adsBlockedCountTextView = (TextView) mBraveStatsView.findViewById(R.id.brave_stats_text_ads_count);
        TextView httpsUpgradesCountTextView = (TextView) mBraveStatsView.findViewById(R.id.brave_stats_text_https_count);
        TextView estTimeSavedTextView = (TextView) mBraveStatsView.findViewById(R.id.brave_stats_text_time_count);
        trackersBlockedCountTextView.setText(getBraveStatsStringFormNumber(trackersBlockedCount));
        adsBlockedCountTextView.setText(getBraveStatsStringFormNumber(adsBlockedCount));
        httpsUpgradesCountTextView.setText(getBraveStatsStringFormNumber(httpsUpgradesCount));
        estTimeSavedTextView.setText(getBraveStatsStringFromTime(estimatedMillisecondsSaved / 1000));
        TraceEvent.end(TAG + ".updateBraveStats()");
    }

    /*
    * Gets string view of specific number for Brave stats
    */
    private String getBraveStatsStringFormNumber(long number) {
        String result = "";
        String suffix = "";
        if (number >= 1000 * 1000 * 1000) {
            result = result + (number / (1000 * 1000 * 1000));
            number = number % (1000 * 1000 * 1000);
            result = result + "." + (number / (10 * 1000 * 1000));
            suffix = "B";
        }
        else if (number >= (10 * 1000 * 1000) && number < (1000 * 1000 * 1000)) {
            result = result + (number / (1000 * 1000));
            suffix = "M";
        }
        else if (number >= (1000 * 1000) && number < (10 * 1000 * 1000)) {
            result = result + (number / (1000 * 1000));
            number = number % (1000 * 1000);
            result = result + "." + (number / (100 * 1000));
            suffix = "M";
        }
        else if (number >= (10 * 1000) && number < (1000 * 1000)) {
            result = result + (number / 1000);
            suffix = "K";
        }
        else if (number >= 1000 && number < (10* 1000)) {
            result = result + (number / 1000);
            number = number % 1000;
            result = result + "." + (number / 100);
            suffix = "K";
        }
        else {
            result = result + number;
        }
        result = result + suffix;
        return result;
    }

    /*
    * Gets string view of specific time in seconds for Brave stats
    */
    private String getBraveStatsStringFromTime(long seconds) {
        String result = "";
        if (seconds > 24 * 60 * 60) {
            result = result + (seconds / (24 * 60 * 60)) + "d";
        }
        else if (seconds > 60 * 60) {
            result = result + (seconds / (60 * 60)) + "h";
        }
        else if (seconds > 60) {
            result = result + (seconds / 60) + "m";
        }
        else {
            result = result + seconds + "s";
        }
        return result;
    }

    @VisibleForTesting
    public NewTabPageRecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * Adds listeners to scrolling to take care of snap scrolling and updating the search box on
     * scroll.
     */
    private void setupScrollHandling() {
        TraceEvent.begin(TAG + ".setupScrollHandling()");
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mSnapScrollHelper.handleScroll();
            }
        });

        TraceEvent.end(TAG + ".setupScrollHandling()");
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Trigger a scroll update when reattaching the window to signal the toolbar that
        // it needs to reset the NTP state. Note that this is handled here rather than
        // NewTabPageLayout#onAttachedToWindow() because NewTabPageLayout may not be
        // immediately attached to the window if the RecyclerView is scrolled when the NTP
        // is refocused.
        if (mManager.isLocationBarShownInNTP()) mNewTabPageLayout.updateSearchBoxOnScroll();

        if (FeatureUtilities.isBottomToolbarEnabled()) {
            ((MarginLayoutParams) getLayoutParams()).bottomMargin =
                    getResources().getDimensionPixelSize(R.dimen.bottom_toolbar_height);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            updateBraveStats();
        }
    }

    /**
     * @see InvalidationAwareThumbnailProvider#shouldCaptureThumbnail()
     */
    boolean shouldCaptureThumbnail() {
        if (getWidth() == 0 || getHeight() == 0) return false;

        return mNewTabPageRecyclerViewChanged || mNewTabPageLayout.shouldCaptureThumbnail()
                || getWidth() != mSnapshotWidth || getHeight() != mSnapshotHeight
                || mRecyclerView.computeVerticalScrollOffset() != mSnapshotScrollY;
    }

    /**
     * @see InvalidationAwareThumbnailProvider#captureThumbnail(Canvas)
     */
    void captureThumbnail(Canvas canvas) {
        mNewTabPageLayout.onPreCaptureThumbnail();
        ViewUtils.captureBitmap(this, canvas);
        mSnapshotWidth = getWidth();
        mSnapshotHeight = getHeight();
        mSnapshotScrollY = mRecyclerView.computeVerticalScrollOffset();
        mNewTabPageRecyclerViewChanged = false;
    }

    /**
     * Scrolls to the top of content suggestions header if one exists. If not, scrolls to the top
     * of the first article suggestion. Uses scrollToPositionWithOffset to position the suggestions
     * below the toolbar and not below the status bar.
     */
    void scrollToSuggestions() {
        int scrollPosition = getSuggestionsScrollPosition();
        // Nothing to scroll to; return early.
        if (scrollPosition == RecyclerView.NO_POSITION) return;

        // Scrolling doesn't occur if it's called too soon i.e. the ntp hasn't finished loading.
        if (mTab.isLoading()) {
            mTab.addObserver(new EmptyTabObserver() {
                @Override
                public void onPageLoadFinished(Tab tab) {
                    mRecyclerView.getLinearLayoutManager().scrollToPositionWithOffset(
                            scrollPosition, getScrollToSuggestionsOffset());
                    mTab.removeObserver(this);
                }
            });
            return;
        }

        mRecyclerView.getLinearLayoutManager().scrollToPositionWithOffset(
                scrollPosition, getScrollToSuggestionsOffset());
    }

    /**
     * Retrieves the position of articles or of their header in the NTP adapter to scroll to.
     * @return The header's position if a header is present. Otherwise, the first
     *         suggestion card's position.
     */
    private int getSuggestionsScrollPosition() {
        // Header always exists.
        if (ChromeFeatureList.isEnabled(
                    ChromeFeatureList.NTP_ARTICLE_SUGGESTIONS_EXPANDABLE_HEADER)) {
            return mRecyclerView.getNewTabPageAdapter().getArticleHeaderPosition();
        }

        // Only articles are visible. Headers are not present.
        if (ChromeFeatureList.isEnabled(ChromeFeatureList.SIMPLIFIED_NTP)) {
            return mRecyclerView.getNewTabPageAdapter().getFirstSnippetPosition();
        }

        // With Simplified NTP not enabled, bookmarks/downloads and their headers are added to the
        // NTP if they're not empty.
        int scrollPosition = mRecyclerView.getNewTabPageAdapter().getArticleHeaderPosition();
        return scrollPosition == RecyclerView.NO_POSITION
                ? mRecyclerView.getNewTabPageAdapter().getFirstSnippetPosition()
                : scrollPosition;
    }

    private int getScrollToSuggestionsOffset() {
        int offset = getResources().getDimensionPixelSize(R.dimen.toolbar_height_no_shadow);

        if (needsExtraOffset()) {
            offset += getResources().getDimensionPixelSize(
                              R.dimen.content_suggestions_card_modern_margin)
                    / 2;
        }
        return offset;
    }

    /**
     * Checks if extra offset needs to be added for aesthetic reasons.
     * @return True if modern is enabled (and space exists between each suggestion card) and no
     *         header is showing.
     */
    private boolean needsExtraOffset() {
        return SuggestionsConfig.useModernLayout()
                && !ChromeFeatureList.isEnabled(
                           ChromeFeatureList.NTP_ARTICLE_SUGGESTIONS_EXPANDABLE_HEADER)
                && mRecyclerView.getNewTabPageAdapter().getArticleHeaderPosition()
                == RecyclerView.NO_POSITION;
    }

    /**
     * @return The adapter position the user has scrolled to.
     */
    public int getScrollPosition() {
        return mRecyclerView.getScrollPosition();
    }

    private void onDestroy() {
        mTab.getWindowAndroid().removeContextMenuCloseListener(mContextMenuManager);
    }

    @VisibleForTesting
    public SnapScrollHelper getSnapScrollHelper() {
        return mSnapScrollHelper;
    }
}
