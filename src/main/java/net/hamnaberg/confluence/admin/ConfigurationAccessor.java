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

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/4/11
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurationAccessor {
    private final TransactionTemplate transactionTemplate;
    private final PluginSettingsFactory pluginSettingsFactory;

    public ConfigurationAccessor(TransactionTemplate transactionTemplate, PluginSettingsFactory pluginSettingsFactory) {
        this.transactionTemplate = transactionTemplate;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }


    public Config getConfig() {
        return transactionTemplate.execute(new TransactionCallback<Config>() {
            public Config doInTransaction() {
                PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
                Config config = new Config();
                config.setPage(CacheControlConfig.fromMap((Map<String, String>) settings.get(name("page"))));
                config.setPageFeed(CacheControlConfig.fromMap((Map<String, String>) settings.get(name("page_feed"))));
                config.setNewsFeed(CacheControlConfig.fromMap((Map<String, String>) settings.get(name("news_feed"))));
                config.setNews(CacheControlConfig.fromMap((Map<String, String>) settings.get(name("news"))));
                return config;
            }
        });
    }

    public void setConfig(final Config config) {
        transactionTemplate.execute(new TransactionCallback<Void>() {
            public Void doInTransaction() {
                PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
                settings.put(name("page"), config.getPage().toMap());
                settings.put(name("page_feed"), config.getPageFeed().toMap());
                settings.put(name("news"), config.getNews().toMap());
                settings.put(name("news_feed"), config.getNewsFeed().toMap());
                return null;
            }
        });
    }


    private String name(String key) {
        return Config.class.getName() + "." + key;
    }
}
