package net.hamnaberg.confluence;

import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.core.bean.EntityObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.WikiStyleRenderer;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.*;
import org.apache.abdera.model.Collection;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;

@Path("spaces")
@AnonymousAllowed
@Produces("application/atom+xml")
@Consumes("application/atom+xml")
public class SpaceFeed {

    // We just have to define the variables and the setters, then Spring injects the correct objects for us to use. Simple and efficient.
    // You just need to know *what* you want to inject and use.

    private final PageManager pageManager;
    private final SpaceManager spaceManager;
    private final WikiStyleRenderer wikiStyleRenderer;
    private final Abdera abdera = Abdera.getInstance();
    private Comparator<EntityObject> reverseLastModifiedComporator;

    public SpaceFeed(PageManager pageManager, SpaceManager spaceManager, WikiStyleRenderer wikiStyleRenderer) {
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.wikiStyleRenderer = wikiStyleRenderer;
        reverseLastModifiedComporator = Collections.reverseOrder(new LastModificationDateComparator());
    }

    @GET
    public Response spaces(@Context UriInfo info) {
        Feed feed = abdera.newFeed();
        feed.setId(info.getRequestUri().toString());
        feed.setTitle("Confluence Space feed");
        feed.setUpdated(new Date());
        List<Space> spaces = new ArrayList<Space>(spaceManager.getAllSpaces());
        Collections.sort(spaces, reverseLastModifiedComporator);
        UriBuilder uriBuilder = info.getRequestUriBuilder();
        for (Space space : spaces) {
            Entry entry = abdera.newEntry();
            entry.setId("urn:confluence:space:id:" + space.getId());
            entry.setTitle(space.getName());
            entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.SPACE_TERM));
            entry.addAuthor(space.getCreatorName());
            entry.setUpdated(space.getLastModificationDate());
            entry.setSummary(wikiStyleRenderer.convertWikiToXHtml(new RenderContext(), space.getDescription().getContent()));
            entry.addLink(space.getHomePage().getUrlPath(), Link.REL_ALTERNATE);
            feed.addExtension(createCollection(uriBuilder, space, "pages"));
            feed.addExtension(createCollection(uriBuilder, space, "news"));
            //entry.addLink(entry.addLink(uriBuilder.clone().segment(space.getKey()).build().toString(), "feed"));
            feed.addEntry(entry);
        }
        System.out.println("SpaceFeed.spaces");
        return Response.ok(new AbderaResponseOutput(feed)).build();
    }

/*    @Path("{key}/pages")
    public PagesFeed pages() {
        return new PagesFeed(pageManager, spaceManager, wikiStyleRenderer);
    }
*/
    private Collection createCollection(UriBuilder uriBuilder, Space space, String name) {
        Collection pageCollection = abdera.getFactory().newCollection();
        pageCollection.acceptsNothing();
        pageCollection.setTitle(name);
        pageCollection.setHref(uriBuilder.clone().segment(space.getKey()).segment(name).build().toString());
        return pageCollection;
    }

}
