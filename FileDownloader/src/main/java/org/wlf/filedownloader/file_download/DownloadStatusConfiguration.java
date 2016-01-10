package org.wlf.filedownloader.file_download;

import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.UrlUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DownloadStatus Configuration
 * <br/>
 * 下载文件缓存器
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadStatusConfiguration {

    /**
     * Configuration Builder
     */
    public static class Builder {

        private Set<String> mListenUrls = new HashSet<String>();
        private boolean mAutoRelease;// auto release when download finished

        /**
         * add the url for listening
         *
         * @param url file url
         * @return the Builder
         */
        public Builder addListenUrl(String url) {
            if (UrlUtil.isUrl(url)) {
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
                mListenUrls.addAll(needAdd);
            }
            return this;
        }

        /**
         * config whether auto release the listener when download finished
         *
         * @param autoRelease true means will auto release the listener when download finished
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
     * is auto release
     *
     * @return true means auto release
     */
    public boolean isAutoRelease() {
        if (mBuilder == null) {
            return false;
        }
        return mBuilder.mAutoRelease;
    }

}
