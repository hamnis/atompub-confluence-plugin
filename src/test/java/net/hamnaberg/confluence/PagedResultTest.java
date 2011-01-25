package net.hamnaberg.confluence;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Link;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        PagedResult<String> res = new PagedResult<String>(Arrays.asList("123", "123", "1234", "1244"), 1, 2, UriBuilder.fromPath("/"));
        List<String> list = res.getList();
        Assert.assertEquals(Arrays.asList("123", "123"), list);
        Assert.assertNotNull(res.getNext());
        Assert.assertEquals(String.format(LINK_TEMPLATE, Link.REL_NEXT, 2), res.getNext().toString());
        Assert.assertNull(res.getPrevious());
        Assert.assertEquals(String.format(LINK_TEMPLATE, Link.REL_FIRST, 1), res.getFirst().toString());
        Assert.assertEquals(String.format(LINK_TEMPLATE, Link.REL_LAST, 2), res.getLast().toString());
    }

    @Test
    public void secondPageWithFourElements() {
        PagedResult<String> res = new PagedResult<String>(Arrays.asList("123", "123", "1234", "1244"), 2, 2, UriBuilder.fromPath("/"));
        List<String> list = res.getList();
        Assert.assertEquals(4, res.getNextIndex());
        Assert.assertEquals(2, res.getCurrentIndex());
        Assert.assertEquals(Arrays.asList("1234", "1244"), list);
    }

    @Test
    public void listWith5ElementsShouldHaveIndex4OnLastElement() {
        PagedResult<String> res = new PagedResult<String>(Arrays.asList("123", "123", "1234", "1244", "11243"), 3, 2, UriBuilder.fromPath("/"));
        List<String> list = res.getList();
        Assert.assertEquals(5, res.getNextIndex());
        Assert.assertEquals(4, res.getCurrentIndex());
        Assert.assertEquals(Arrays.asList("11243"), list);
    }

    @Test
    public void emptyList() {
        PagedResult<String> res = new PagedResult<String>(Collections.<String>emptyList(), 1, 2, UriBuilder.fromPath("/"));
        List<String> list = res.getList();
        Assert.assertEquals(0, res.getNextIndex());
        Assert.assertEquals(0, res.getCurrentIndex());
        Assert.assertEquals(Collections.<String>emptyList(), list);
    }
}
