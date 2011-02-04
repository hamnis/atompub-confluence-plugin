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
