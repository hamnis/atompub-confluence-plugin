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
import org.apache.commons.lang.math.NumberUtils;

import javax.ws.rs.core.CacheControl;
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
    @XmlElement(name = "ttl") private int ttl;
    @XmlElement(name = "transform") private boolean transform;
    @XmlElement(name = "revalidate") private boolean revalidate;

    public CacheControlConfig(int ttl, boolean transform, boolean revalidate) {
        this.ttl = ttl;
        this.transform = transform;
        this.revalidate = revalidate;
    }

    public CacheControlConfig() {
        this(300, false, false);
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

    public boolean isRevalidate() {
        return revalidate;
    }

    public void setRevalidate(boolean revalidate) {
        this.revalidate = revalidate;
    }

    public CacheControl toCacheControl() {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(ttl);
        cc.setMustRevalidate(revalidate);
        cc.setNoTransform(transform);
        return cc;
    }

    public Map<String, String> toMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("ttl", String.valueOf(ttl));
        map.put("transform", String.valueOf(transform));
        map.put("revalidate", String.valueOf(revalidate));
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheControlConfig that = (CacheControlConfig) o;

        if (revalidate != that.revalidate) return false;
        if (transform != that.transform) return false;
        if (ttl != that.ttl) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ttl;
        result = 31 * result + (transform ? 1 : 0);
        result = 31 * result + (revalidate ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static CacheControlConfig fromMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new CacheControlConfig(
                NumberUtils.toInt(map.get("ttl"), 300),
                Boolean.parseBoolean(map.get("transform")),
                Boolean.parseBoolean(map.get("revalidate"))
                );
    }
}
