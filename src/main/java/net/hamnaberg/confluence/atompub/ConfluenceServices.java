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

package net.hamnaberg.confluence.atompub;

import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.search.v2.SearchManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import net.hamnaberg.confluence.admin.ConfigurationAccessor;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/8/11
 * Time: 8:04 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ConfluenceServices {
    private final PageManager pageManager;
    private final SpaceManager spaceManager;
    private final WikiStyleRenderer wikiStyleRenderer;
    private final SearchManager searchManager;
    private final PermissionManager permissionManager;
    private SettingsManager settingsManager;
    private final ConfigurationAccessor configurationAccessor;
    private final LabelManager labelManager;

    public ConfluenceServices(PageManager pageManager, SpaceManager spaceManager, WikiStyleRenderer wikiStyleRenderer, SearchManager searchManager, PermissionManager permissionManager, TransactionTemplate transactionTemplate, PluginSettingsFactory pluginSettingsFactory, LabelManager labelMananger, SettingsManager settingsManager) {
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.wikiStyleRenderer = wikiStyleRenderer;
        this.searchManager = searchManager;
        this.permissionManager = permissionManager;
        this.settingsManager = settingsManager;
        this.configurationAccessor = new ConfigurationAccessor(transactionTemplate, pluginSettingsFactory);
        this.labelManager = labelMananger;
    }

    public PageManager getPageManager() {
        return pageManager;
    }

    public SpaceManager getSpaceManager() {
        return spaceManager;
    }

    public WikiStyleRenderer getWikiStyleRenderer() {
        return wikiStyleRenderer;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public ConfigurationAccessor getConfigurationAccessor() {
        return configurationAccessor;
    }

    public LabelManager getLabelManager() {
        return labelManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
