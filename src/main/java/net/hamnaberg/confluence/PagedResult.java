package net.hamnaberg.confluence;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Link;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/22/11
 * Time: 5:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagedResult<T> {
    private List<T> list;
    private Abdera abdera = Abdera.getInstance();
    private final int pageNo;
    private final int totalPages;
    private final int size;
    private final int pageSize;
    private final UriBuilder baseURIBuilder;

    public PagedResult(List<T> list, int pageNo, int pageSize, UriBuilder baseURIBuilder) {
        this.baseURIBuilder = baseURIBuilder.clone();
        this.pageSize = pageSize;
        this.size = list.size();
        this.totalPages = getTotalPages(pageSize);
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageNo > totalPages) {
            pageNo = totalPages;
        }
        this.pageNo = pageNo;
        this.list = list.subList(getCurrentIndex(), getNextIndex());
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
        link.setHref(baseURIBuilder.clone().queryParam("pw", pageNo + 1).build().toString());
        return link;
    }

    public Link getPrevious() {
        if (!hasPrevious()) {
            return null;
        }
        Link link = abdera.getFactory().newLink();
        link.setRel(Link.REL_PREVIOUS);
        link.setHref(baseURIBuilder.clone().queryParam("pw", pageNo - 1).build().toString());
        return link;
    }

    public Link getFirst() {
        if (totalPages == 0) {
            return null;
        }
        Link link = abdera.getFactory().newLink();
        link.setRel(Link.REL_FIRST);
        link.setHref(baseURIBuilder.clone().queryParam("pw", 1).build().toString());
        return link;
    }

    public Link getLast() {
        if (totalPages == 0) {
            return null;
        }
        Link link = abdera.getFactory().newLink();
        link.setRel(Link.REL_LAST);
        link.setHref(baseURIBuilder.clone().queryParam("pw", totalPages).build().toString());
        return link;
    }

    public List<T> getList() {
        return list;
    }
}
