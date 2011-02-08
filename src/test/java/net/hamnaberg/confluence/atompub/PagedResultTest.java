package net.hamnaberg.confluence.atompub;

import org.apache.abdera.model.Link;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/22/11
 * Time: 5:26 PM
 * To change this template use File | Settings | File Templates.
 */
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
