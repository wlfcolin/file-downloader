package org.wlf.filedownloader;

import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DownloadStatus Configuration
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadStatusConfiguration {

    /**
     * Configuration Builder
     */
    public static class Builder {

        /**
         * all listen urls, default is null which means listen all
         */
        private Set<String> mListenUrls;
        /**
         * whether auto release the listener when the listen url downloads finished, default is false
         */
        private boolean mAutoRelease;

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
         * config whether auto release the listener when the listen url downloads finished
         *
         * @param autoRelease true means will auto release the listener the listen url downloads finished, default is
         *                    false
         * @return the Builder
         */
        public Builder configAutoRelease(boolean autoRelease) {
            this.mAutoRelease = autoRelease;
            return this;
        }

        /**
         * build DownloadStatusConfiguration
         *
         * @return the DownloadStatusConfiguration
         */
        public DownloadStatusConfiguration build() {
            return new DownloadStatusConfiguration(this);
        }
    }

    // builder
    private Builder mBuilder;

    private DownloadStatusConfiguration(Builder builder) {
        mBuilder = builder;
    }

    /**
     * get listen urls
     *
     * @return listen urls
     */
    public Set<String> getListenUrls() {
        if (mBuilder == null) {
            return null;
        }
        return mBuilder.mListenUrls;
    }

    /**
     * is auto release the listener when the listen url downloads finished
     *
     * @return true means auto release, default is false
     */
    public boolean isAutoRelease() {
        if (mBuilder == null) {
            return false;
        }
        return mBuilder.mAutoRelease;
    }
}
