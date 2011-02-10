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

import org.apache.abdera.model.Link;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;


public class PagedResultTest {
    protected static final String LINK_TEMPLATE = "<link xmlns=\"http://www.w3.org/2005/Atom\" rel=\"%s\" href=\"/?pw=%s\" />";

    @Test
    public void firstPageWithFourElements() {
        PagedResult res = new PagedResult(4, 1, 2, UriBuilder.fromPath("/"));
        Assert.assertNotNull(res.getNext());
        Assert.assertEquals(String.format(LINK_TEMPLATE, Link.REL_NEXT, 2), res.getNext().toString());
        Assert.assertNull(res.getPrevious());
        Assert.assertEquals(String.format(LINK_TEMPLATE, Link.REL_FIRST, 1), res.getFirst().toString());
        Assert.assertEquals(String.format(LINK_TEMPLATE, Link.REL_LAST, 2), res.getLast().toString());
    }

    @Test
    public void secondPageWithFourElements() {
        PagedResult res = new PagedResult(4, 2, 2, UriBuilder.fromPath("/"));
        Assert.assertEquals(4, res.getNextIndex());
        Assert.assertEquals(2, res.getCurrentIndex());
    }

    @Test
    public void listWith5ElementsShouldHaveIndex4OnLastElement() {
        PagedResult res = new PagedResult(5, 3, 2, UriBuilder.fromPath("/"));
        Assert.assertEquals(5, res.getNextIndex());
        Assert.assertEquals(4, res.getCurrentIndex());
    }

    @Test
    public void emptyList() {
        PagedResult res = new PagedResult(0, 1, 2, UriBuilder.fromPath("/"));
        Assert.assertEquals(0, res.getNextIndex());
        Assert.assertEquals(0, res.getCurrentIndex());
    }
}
