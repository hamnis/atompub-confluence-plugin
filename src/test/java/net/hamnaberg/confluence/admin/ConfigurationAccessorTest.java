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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/10/11
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurationAccessorTest {
    @Test
    public void nothingIsStoredEmptyConfigIsUsed() {
        TransactionTemplate template = new TransactionTemplate() {
            public <T> T execute(TransactionCallback<T> tTransactionCallback) {
                return tTransactionCallback.doInTransaction();
            }
        };

        PluginSettingsFactory settingsFactory = mock(PluginSettingsFactory.class);
        when(settingsFactory.createGlobalSettings()).thenReturn(new FakePluginSettings());
        ConfigurationAccessor accessor = new ConfigurationAccessor(template, settingsFactory);
        Config config = accessor.getConfig();
        Assert.assertEquals(new Config(), config);
    }

    @Test
    public void wrongContentStoredForPageShouldBeDefaultConfig() {
        TransactionTemplate template = new TransactionTemplate() {
            public <T> T execute(TransactionCallback<T> tTransactionCallback) {
                return tTransactionCallback.doInTransaction();
            }
        };

        PluginSettingsFactory settingsFactory = mock(PluginSettingsFactory.class);
        when(settingsFactory.createGlobalSettings()).thenReturn(new FakePluginSettings(Collections.<String, Object>singletonMap(Config.class.getName() + ".page", "value")));
        ConfigurationAccessor accessor = new ConfigurationAccessor(template, settingsFactory);
        Config config = accessor.getConfig();
        Assert.assertEquals(new Config(), config);
    }

    private static class FakePluginSettings implements PluginSettings {
        private final Map<String, Object> map = new HashMap<String, Object>();

        private FakePluginSettings(Map<String, Object> map) {
            this.map.putAll(map);
        }

        private FakePluginSettings() {
        }

        public Object get(String key) {
            return map.get(key);
        }

        public Object put(String key, Object value) {
            return map.put(key, value);
        }

        public Object remove(String key) {
            return map.remove(key);
        }
    }
}
