// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.bookmarks;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.ObserverList;
import org.chromium.base.VisibleForTesting;
import org.chromium.chrome.browser.BraveSyncWorker;
import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.components.bookmarks.BookmarkType;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that encapsulates {@link BookmarkBridge} and provides extra features such as undo, large
 * icon fetching, reader mode url redirecting, etc. This class should serve as the single class for
 * the UI to acquire data from the backend.
 */
public class BookmarkModel extends BookmarkBridge {
    /**
     * Observer that listens to delete event. This interface is used by undo controllers to know
     * which bookmarks were deleted. Note this observer only listens to events that go through
     * bookmark model.
     */
    public interface BookmarkDeleteObserver {

        /**
         * Callback being triggered immediately before bookmarks are deleted.
         * @param titles All titles of the bookmarks to be deleted.
         * @param isUndoable Whether the deletion is undoable.
         */
        void onDeleteBookmarks(String[] titles, List<BookmarkItem> bookmarks, boolean isUndoable);
    }

    private ObserverList<BookmarkDeleteObserver> mDeleteObservers = new ObserverList<>();

    /**
     * Initialize bookmark model for last used non-incognito profile.
     */
    public BookmarkModel() {
        this(Profile.getLastUsedProfile().getOriginalProfile());
    }

    @VisibleForTesting
    public BookmarkModel(Profile profile) {
        super(profile);
    }

    /**
     * Clean up all the bridges. This must be called after done using this class.
     */
    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public boolean isBookmarkModelLoaded() {
        return super.isBookmarkModelLoaded();
    }

    /**
     * Add an observer that listens to delete events that go through the bookmark model.
     * @param observer The observer to add.
     */
    void addDeleteObserver(BookmarkDeleteObserver observer) {
        mDeleteObservers.addObserver(observer);
    }

    /**
     * Remove the observer from listening to bookmark deleting events.
     * @param observer The observer to remove.
     */
    void removeDeleteObserver(BookmarkDeleteObserver observer) {
        mDeleteObservers.removeObserver(observer);
    }

    private List<BookmarkItem> GetChildren(BookmarkItem parent) {
        List<BookmarkItem> res = new ArrayList<BookmarkItem>();
        if (!parent.isFolder()) {
            return res;
        }
        res = getBookmarksForFolder(parent.getId());
        List<BookmarkItem> newList = new ArrayList<BookmarkItem>();
        for (BookmarkItem item : res) {
            if (!item.isFolder()) {
                continue;
            }
            newList.addAll(getBookmarksForFolder(item.getId()));
        }
        res.addAll(newList);

        return res;
    }

    /**
     * Delete one or multiple bookmarks from model. If more than one bookmarks are passed here, this
     * method will group these delete operations into one undo bundle so that later if the user
     * clicks undo, all bookmarks deleted here will be restored.
     * @param bookmarks Bookmarks to delete. Note this array should not contain a folder and its
     *                  children, because deleting folder will also remove all its children, and
     *                  deleting children once more will cause errors.
     */
    void deleteBookmarks(BookmarkId... bookmarks) {
        assert bookmarks != null && bookmarks.length > 0;
        // Store all titles of bookmarks.
        List<BookmarkItem> bookmarksItems = new ArrayList<BookmarkItem>();
        List<String> titles = new ArrayList<>();
        boolean isUndoable = true;

        startGroupingUndos();
        for (BookmarkId bookmarkId : bookmarks) {
            BookmarkItem bookmarkItem = getBookmarkById(bookmarkId);
            if (bookmarkItem == null) continue;
            isUndoable &= (bookmarkId.getType() == BookmarkType.NORMAL);
            titles.add(bookmarkItem.getTitle());
            bookmarksItems.add(bookmarkItem);
            bookmarksItems.addAll(GetChildren(bookmarkItem));
            deleteBookmark(bookmarkId);
        }
        endGroupingUndos();

        for (BookmarkDeleteObserver observer : mDeleteObservers) {
            observer.onDeleteBookmarks(titles.toArray(new String[titles.size()]), bookmarksItems,  isUndoable);
        }
    }

    public void deleteBookmarkSilently(BookmarkId bookmark) {
        assert null != bookmark;
        deleteBookmark(bookmark);
    }

    /**
     * Calls {@link BookmarkBridge#moveBookmark(BookmarkId, BookmarkId, int)} for the given
     * bookmark list. The bookmarks are appended at the end.
     */
    void moveBookmarks(List<BookmarkId> bookmarkIds, BookmarkId newParentId) {
        int appenedIndex = getChildCount(newParentId);
        BookmarkItem[] bookmarksToMove = new BookmarkItem[bookmarkIds.size()];
        for (int i = 0; i < bookmarkIds.size(); ++i) {
            moveBookmark(bookmarkIds.get(i), newParentId, appenedIndex + i);
            bookmarksToMove[i] = getBookmarkById(bookmarkIds.get(i));
        }
        ChromeApplication app = (ChromeApplication)ContextUtils.getApplicationContext();
        if (null != app && null != app.mBraveSyncWorker && null != newParentId) {
            app.mBraveSyncWorker.CreateUpdateDeleteBookmarks(BraveSyncWorker.UPDATE_RECORD, bookmarksToMove, true, false);
        }
    }

    /**
     * Calls {@link BookmarkBridge#moveBookmark(BookmarkId, BookmarkId, int)} for the given
     * bookmark. The bookmark is appended at the end. Call that method from Brave's sync only
     */
    public void moveBookmark(BookmarkId bookmarkId, BookmarkId newParentId) {
        int appendedIndex = getChildCount(newParentId);
        moveBookmark(bookmarkId, newParentId, appendedIndex);
    }

    /**
     * @see org.chromium.chrome.browser.bookmarks.BookmarkBridge.BookmarkItem#getTitle()
     */
    String getBookmarkTitle(BookmarkId bookmarkId) {
        BookmarkItem bookmarkItem = getBookmarkById(bookmarkId);
        if (bookmarkItem == null) return "";
        return bookmarkItem.getTitle();
    }

    /**
     * @return The id of the default folder to add bookmarks/folders to.
     */
    public BookmarkId getDefaultFolder() {
        return getMobileFolderId();
    }
}
