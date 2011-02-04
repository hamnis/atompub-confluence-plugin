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
    }

    public CacheControlConfig getPage() {
        return page;
    }

    public void setPage(CacheControlConfig page) {
        this.page = page;
    }

    public CacheControlConfig getPageFeed() {
        return pageFeed;
    }

    public void setPageFeed(CacheControlConfig pageFeed) {
        this.pageFeed = pageFeed;
    }

    public CacheControlConfig getNewsFeed() {
        return newsFeed;
    }

    public void setNewsFeed(CacheControlConfig newsFeed) {
        this.newsFeed = newsFeed;
    }

    public CacheControlConfig getNews() {
        return news;
    }

    public void setNews(CacheControlConfig news) {
        this.news = news;
    }
}
