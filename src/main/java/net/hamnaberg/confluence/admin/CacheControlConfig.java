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

import org.apache.commons.lang.math.NumberUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/3/11
 * Time: 12:56 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "config")
public class CacheControlConfig {
    @XmlElement(name = "ttl") private int ttl = 60;
    @XmlElement(name = "transform") private boolean transform = false;
    @XmlElement(name = "revalidate") private boolean must_revalidate = true;

    public CacheControlConfig(int ttl, boolean transform, boolean must_revalidate) {
        this.ttl = ttl;
        this.transform = transform;
        this.must_revalidate = must_revalidate;
    }

    public CacheControlConfig() {
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public boolean isTransform() {
        return transform;
    }

    public void setTransform(boolean transform) {
        this.transform = transform;
    }

    public boolean isMust_revalidate() {
        return must_revalidate;
    }

    public void setMust_revalidate(boolean must_revalidate) {
        this.must_revalidate = must_revalidate;
    }

    public Map<String, String> toMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("ttl", String.valueOf(ttl));
        map.put("transform", String.valueOf(transform));
        map.put("must_revalidate", String.valueOf(must_revalidate));
        return map;
    }

    public static CacheControlConfig fromMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        return new CacheControlConfig(
                NumberUtils.toInt(map.get("ttl"), 0),
                Boolean.parseBoolean(map.get("transform")),
                Boolean.parseBoolean(map.get("must_revalidate"))
                );
    }
}
