package org.wlf.filedownloader;

import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DownloadFileChange Configuration
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileChangeConfiguration {

    /**
     * Configuration Builder
     */
    public static class Builder {

        /**
         * all listen urls, default is null which means listen all
         */
        private Set<String> mListenUrls;
        /**
         * whether the callback method is in a single thread, true means the callback method is in a single thread,
         * otherwise in main thread, default is false
         */
        private boolean mIsThreadCallback = false;

        /**
         * add the url for listening
         *
         * @param url file url
         * @return the Builder
         */
        public Builder addListenUrl(String url) {
            if (UrlUtil.isUrl(url)) {
                if (mListenUrls == null) {
                    mListenUrls = new HashSet<String>();
                }
                mListenUrls.add(url);
            }
            return this;
        }

        /**
         * add the urls for listening
         *
         * @param urls file url
         * @return the Builder
         */
        public Builder addListenUrls(List<String> urls) {

            List<String> needAdd = new ArrayList<String>();

            for (String url : urls) {
                if (!UrlUtil.isUrl(url)) {
                    continue;
                }
                needAdd.add(url);
            }

            if (!CollectionUtil.isEmpty(needAdd)) {
                if (mListenUrls == null) {
                    mListenUrls = new HashSet<String>();
                }
                mListenUrls.addAll(needAdd);
            }
            return this;
        }

        /**
         * config whether the caller hope the callback method is sync
         *
         * @param isThreadCallback true means the callback method is in a single thread, otherwise in main thread,
         *                         default is false
         * @return the Builder
         */
        public Builder configTreadCallback(boolean isThreadCallback) {
            this.mIsThreadCallback = isThreadCallback;
            return this;
        }

        /**
         * build DownloadFileChangeConfiguration
         *
         * @return the DownloadFileChangeConfiguration
         */
        public DownloadFileChangeConfiguration build() {
            return new DownloadFileChangeConfiguration(this);
        }

    }

    // builder
    private Builder mBuilder;

    private DownloadFileChangeConfiguration(Builder builder) {
        mBuilder = builder;
    }

    /**
     * get listen urls
     *
     * @return listen urls, default is null which means listen all
     */
    public Set<String> getListenUrls() {
        if (mBuilder == null) {
            return null;
        }
        return mBuilder.mListenUrls;
    }

    /**
     * whether the callback method is sync
     *
     * @return true means the callback method is in a single thread, otherwise in main thread, default is false
     */
    public boolean isTreadCallback() {
        if (mBuilder == null) {
            return false;
        }
        return mBuilder.mIsThreadCallback;
    }
}
