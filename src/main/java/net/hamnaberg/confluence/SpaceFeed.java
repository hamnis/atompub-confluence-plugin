package net.hamnaberg.confluence;

import com.atlassian.confluence.pages.Page;
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
    protected static final String PAGES_SEGMENT = "pages";

    // We just have to define the variables and the setters, then Spring injects the correct objects for us to use. Simple and efficient.
    // You just need to know *what* you want to inject and use.

    private final PageManager pageManager;
    private final SpaceManager spaceManager;
    private final WikiStyleRenderer wikiStyleRenderer;
    private final Abdera abdera = Abdera.getInstance();
    private static final int PAGE_SIZE = 10;
    private TidyCleaner tidyCleaner;
    private Comparator<EntityObject> reverseLastModifiedComporator;

    public SpaceFeed(PageManager pageManager, SpaceManager spaceManager, WikiStyleRenderer wikiStyleRenderer) {
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.wikiStyleRenderer = wikiStyleRenderer;
        tidyCleaner = new TidyCleaner();
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
        return Response.ok(new AbderaResponseOutput(feed)).build();
    }

    private Collection createCollection(UriBuilder uriBuilder, Space space, String name) {
        Collection pageCollection = abdera.getFactory().newCollection();
        pageCollection.acceptsNothing();
        pageCollection.setTitle(name);
        pageCollection.setHref(uriBuilder.clone().segment(space.getKey()).segment(name).build().toString());
        return pageCollection;
    }

    @Path("{key}/pages")
    @GET
    public Response pages(@PathParam("key") String key, @Context UriInfo info, @QueryParam("pw") int pageNo) {
        Space space = spaceManager.getSpace(key);
        if (space == null) {
            throw new IllegalArgumentException(String.format("No space called %s found", key));
        }
        List<Page> pages = new ArrayList<Page>(pageManager.getPages(space, true));
        addPagination(pageNo, pages);
        Collections.sort(pages, reverseLastModifiedComporator);
        return Response.ok(new AbderaResponseOutput(generate(space, pages, info.getBaseUriBuilder()))).build();
    }

    private List<Page> addPagination(int start, List<Page> pages) {
        int index = start > 0 ? start : 0;
        int total = pages.size();
        int endIndex = index + PAGE_SIZE;
        if (endIndex > total) {
            endIndex = total;
        }
        return pages.subList(index, endIndex);
    }

    @Path("{key}/pages/{id}")
    @GET
    public Response page(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        Page page = pageManager.getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
        return Response.ok(new AbderaResponseOutput(createEntryFromPage(resourceURIBuilder, page))).build();
    }
    
    @Path("{key}/pages/{id}/children")
    @GET
    public Response children(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        Page page = pageManager.getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
        Feed feed = generate(page, page.getChildren(), resourceURIBuilder);
        return Response.ok(new AbderaResponseOutput(feed)).build();
    }

    private Feed generate(EntityObject parent, List<Page> pages, UriBuilder baseURIBuilder) {
        Feed feed = abdera.newFeed();
        UriBuilder builder;
        if (parent instanceof Page) {
            Page page = (Page) parent;
            builder = getResourceURIBuilder(baseURIBuilder).segment(page.getSpaceKey()).segment(PAGES_SEGMENT).segment(page.getIdAsString());
            feed.setTitle("Children of " + ((Page) parent).getTitle());
            feed.setId("urn:confluence:page:id:" + parent.getId());
        }
        else if (parent instanceof Space) {
            feed.setTitle(((Space) parent).getName());
            builder = getResourceURIBuilder(baseURIBuilder).segment(((Space) parent).getKey());
            feed.setId("urn:confluence:space:id:" + parent.getId());
        }
        else {
            throw new IllegalArgumentException("Unkown parent");
        }
        feed.setUpdated(parent.getLastModificationDate());
        feed.addLink(builder.build().toString(), Link.REL_SELF);
        feed.addLink(getResourceURIBuilder(baseURIBuilder).build().toString(), "up");
        for (Page page : pages) {
            feed.addEntry(createEntryFromPage(builder, page));
        }
        return feed;
    }

    private Entry createEntryFromPage(UriBuilder resourceURIBuilder, Page page) {
        Entry entry = abdera.newEntry();
        UriBuilder builder = resourceURIBuilder.segment(PAGES_SEGMENT).segment(page.getIdAsString());
        if (page.hasChildren()) {
            entry.addLink(builder.clone().segment("children").build().toString(), "feed");
            //Add rel="feed" to entry
        }
        entry.addLink(page.getUrlPath(), Link.REL_ALTERNATE);
        entry.addLink(builder.build().toString(), Link.REL_SELF);
        entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.PAGE_TERM));
        entry.setTitle(page.getTitle());

        String name = page.getCreatorName();
        if (name == null) {
            name = "Confluence";
        }
        entry.addAuthor(name);
        entry.setId("urn:confluence:page:id:" + page.getIdAsString());
        String value = wikiStyleRenderer.convertWikiToXHtml(page.toPageContext(), page.getContent());
        entry.setContentAsXhtml(tidyCleaner.clean(value));
        entry.setEdited(page.getLastModificationDate());
        entry.setUpdated(page.getLastModificationDate());
        entry.setPublished(page.getCreationDate());
        //page.isDeleted() add a tombstone here.
        return entry;
    }

    private UriBuilder getResourceURIBuilder(UriBuilder baseUriBuilder) {
        return baseUriBuilder.clone().path(getClass());
    }

    private static class LastModificationDateComparator implements Comparator<EntityObject> {
        public int compare(EntityObject o1, EntityObject o2) {
            return o1.getLastModificationDate().compareTo(o2.getLastModificationDate());
        }
    }
}
