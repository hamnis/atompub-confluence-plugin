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

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.ext.opensearch.model.IntegerElement;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;

import javax.ws.rs.core.UriBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/22/11
 * Time: 5:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagedResult {
    private Abdera abdera = Abdera.getInstance();
    private final int pageNo;
    private final int totalPages;
    private final int size;
    private final int pageSize;
    private final UriBuilder baseURIBuilder;

    public PagedResult(int size, int pageNo, int pageSize, UriBuilder baseURIBuilder) {
        this.baseURIBuilder = baseURIBuilder.clone();
        this.pageSize = pageSize;
        this.size = size;
        this.totalPages = getTotalPages(pageSize);
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageNo > totalPages) {
            pageNo = totalPages;
        }
        this.pageNo = pageNo;
    }

    int getTotalPages(int pageSize) {
        return size % pageSize == 0 ? size /pageSize : 1 + (size / pageSize);
    }

    int getCurrentIndex() {
        if ((pageNo -1) < 1) {
            return 0;
        }
      return ((pageNo - 1) * pageSize);
    }

    int getNextIndex() {
      return Math.min(pageNo * pageSize, size);
    }

    public boolean hasNext() {
      return pageNo < totalPages;
    }

    public boolean hasPrevious() {
      return pageNo > 1;
    }

    public Link getNext() {
        if (!hasNext()) {
            return null;
        }
        Link link = abdera.getFactory().newLink();
        link.setRel(Link.REL_NEXT);
        link.setHref(baseURIBuilder.clone().replaceQueryParam("pw", pageNo + 1).build().toString());
        return link;
    }

    public Link getPrevious() {
        if (!hasPrevious()) {
            return null;
        }
        Link link = abdera.getFactory().newLink();
        link.setRel(Link.REL_PREVIOUS);
        link.setHref(baseURIBuilder.clone().replaceQueryParam("pw", pageNo - 1).build().toString());
        return link;
    }

    public Link getFirst() {
        if (totalPages == 0) {
            return null;
        }
        Link link = abdera.getFactory().newLink();
        link.setRel(Link.REL_FIRST);
        link.setHref(baseURIBuilder.clone().replaceQueryParam("pw", 1).build().toString());
        return link;
    }

    public Link getLast() {
        if (totalPages == 0) {
            return null;
        }
        Link link = abdera.getFactory().newLink();
        link.setRel(Link.REL_LAST);
        link.setHref(baseURIBuilder.clone().replaceQueryParam("pw", totalPages).build().toString());
        return link;
    }

    public void populate(Feed feed) {
        addLink(getFirst(), feed);
        addLink(getLast(), feed);
        addLink(getNext(), feed);
        addLink(getPrevious(), feed);
        IntegerElement itemsPerPage = abdera.getFactory().newExtensionElement(OpenSearchConstants.ITEMS_PER_PAGE);
        itemsPerPage.setValue(pageSize);
        feed.addExtension(itemsPerPage);
        IntegerElement totalResults = abdera.getFactory().newExtensionElement(OpenSearchConstants.TOTAL_RESULTS);
        totalResults.setValue(size);
        feed.addExtension(totalResults);
    }

    private void addLink(Link link, Feed feed) {
        if (link != null) {
            feed.addLink(link);
        }
    }
}
