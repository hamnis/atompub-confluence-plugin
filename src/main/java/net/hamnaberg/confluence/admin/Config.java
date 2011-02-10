/*
 * Copyright 2011 Erlend Hamnaberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.hamnaberg.confluence.admin;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/3/11
 * Time: 1:59 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement
public class Config {
    @XmlElement(name = "page") private CacheControlConfig page;
    @XmlElement(name = "page_feed") private CacheControlConfig pageFeed;
    @XmlElement(name = "news_feed") private CacheControlConfig newsFeed;
    @XmlElement(name = "news") private CacheControlConfig news;

    public Config(CacheControlConfig page, CacheControlConfig pageFeed, CacheControlConfig newsFeed, CacheControlConfig news) {
        this.page = page;
        this.pageFeed = pageFeed;
        this.newsFeed = newsFeed;
        this.news = news;
    }

    public Config() {
        this(new CacheControlConfig(), new CacheControlConfig(), new CacheControlConfig(), new CacheControlConfig());
    }

    public CacheControlConfig getPage() {
        return page;
    }

    public void setPage(CacheControlConfig page) {
        this.page = page == null ? new CacheControlConfig() : page;
    }

    public CacheControlConfig getPageFeed() {
        return pageFeed;
    }

    public void setPageFeed(CacheControlConfig pageFeed) {
        this.pageFeed = pageFeed == null ? new CacheControlConfig() : pageFeed;
    }

    public CacheControlConfig getNewsFeed() {
        return newsFeed;
    }

    public void setNewsFeed(CacheControlConfig newsFeed) {
        this.newsFeed = newsFeed == null ? new CacheControlConfig() : newsFeed;
    }

    public CacheControlConfig getNews() {
        return news;
    }

    public void setNews(CacheControlConfig news) {
        this.news = news == null ? new CacheControlConfig() : news;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (news != null ? !news.equals(config.news) : config.news != null) return false;
        if (newsFeed != null ? !newsFeed.equals(config.newsFeed) : config.newsFeed != null) return false;
        if (page != null ? !page.equals(config.page) : config.page != null) return false;
        if (pageFeed != null ? !pageFeed.equals(config.pageFeed) : config.pageFeed != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = page != null ? page.hashCode() : 0;
        result = 31 * result + (pageFeed != null ? pageFeed.hashCode() : 0);
        result = 31 * result + (newsFeed != null ? newsFeed.hashCode() : 0);
        result = 31 * result + (news != null ? news.hashCode() : 0);
        return result;
    }
}
