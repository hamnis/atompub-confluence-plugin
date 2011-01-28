package net.hamnaberg.confluence;

import com.atlassian.bonnie.Searchable;
import com.atlassian.confluence.core.ListBuilder;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.search.service.ContentTypeEnum;
import com.atlassian.confluence.search.v2.ContentSearch;
import com.atlassian.confluence.search.v2.InvalidSearchException;
import com.atlassian.confluence.search.v2.SearchManager;
import com.atlassian.confluence.search.v2.SearchSort;
import com.atlassian.confluence.search.v2.filter.SubsetResultFilter;
import com.atlassian.confluence.search.v2.query.ContentTypeQuery;
import com.atlassian.confluence.search.v2.searchfilter.SpacePermissionsSearchFilter;
import com.atlassian.confluence.search.v2.sort.ModifiedSort;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.core.bean.EntityObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.renderer.WikiStyleRenderer;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
' * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/25/11
 * Time: 6:45 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("spaces/{key}/pages")
@Produces("application/atom+xml")
@Consumes("application/atom+xml")
@AnonymousAllowed
public class PagesFeed {
    private static final int PAGE_SIZE = 10;
    private static final String PAGES_SEGMENT = "pages";

    private TidyCleaner tidyCleaner;
    private final PageManager pageManager;
    private final SpaceManager spaceManager;
    private final WikiStyleRenderer wikiStyleRenderer;
    private final SearchManager searchManager;
    private Abdera abdera  = Abdera.getInstance();

    public PagesFeed(PageManager pageManager, SpaceManager spaceManager, WikiStyleRenderer wikiStyleRenderer, SearchManager searchManager) {
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.wikiStyleRenderer = wikiStyleRenderer;
        this.searchManager = searchManager;
        tidyCleaner = new TidyCleaner();

    }

    @GET
    public Response pages(@PathParam("key") String key, @Context UriInfo info, @QueryParam("pw") int pageNo) {
        URI uri = info.getBaseUri();
        System.out.println("uri = " + uri);
        Space space = spaceManager.getSpace(key);
        if (space == null) {
            throw new IllegalArgumentException(String.format("No space called %s found", key));
        }
        if (pageNo < 1) {
            pageNo = 1;
        }
        try {
            ListBuilder<Page> topLevelPagesBuilder = pageManager.getTopLevelPagesBuilder(space);
            int availableSize = topLevelPagesBuilder.getAvailableSize();
            PagedResult result = new PagedResult(availableSize, pageNo, PAGE_SIZE, info.getBaseUriBuilder());
            List<Searchable> searchables = searchManager.searchEntities(
                    new ContentSearch(
                            new ContentTypeQuery(ContentTypeEnum.PAGE),
                            new ModifiedSort(SearchSort.Order.DESCENDING),
                            SpacePermissionsSearchFilter.getInstance(),
                            new SubsetResultFilter(pageNo - 1, PAGE_SIZE)
                    )
            );
            List<Page> pages = new ArrayList<Page>();
            for (Searchable res : searchables) {
                Page page = (Page) res;
                if (page.getParent() == null) {
                    pages.add(page);
                }
            }
            Feed feed = generate(space, pages, info.getBaseUriBuilder());
            result.populate(feed);
            return Response.ok(new AbderaResponseOutput(feed)).build();
        } catch (InvalidSearchException e) {
            throw new RuntimeException(e);
        }
    }

    @Path("{id}")
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

    //TODO: Decide if the <content> should have a source, then this is useful.
    @Path("{id}/content")
    @GET
    @Produces("text/plain")
    public Response pageContent(@PathParam("key") String key, @PathParam("id") long id) {
        Page page = pageManager.getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        return Response.ok(page.getContent()).build();
    }

    @Path("{id}/children")
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
        URI self;
        UriBuilder spaceURIBuilder;
        if (parent instanceof Page) {
            Page page = (Page) parent;
            spaceURIBuilder = getResourceURIBuilder(baseURIBuilder).clone().segment(page.getSpaceKey());
            self = getResourceURIBuilder(baseURIBuilder).clone().segment(page.getSpaceKey()).segment(PAGES_SEGMENT).segment(page.getIdAsString()).build();
            feed.setTitle("Children of " + ((Page) parent).getTitle());
            feed.setId("urn:confluence:page:id:" + parent.getId());
        } else if (parent instanceof Space) {
            feed.setTitle(((Space) parent).getName());
            spaceURIBuilder = getResourceURIBuilder(baseURIBuilder).clone().segment(((Space) parent).getKey());
            self = spaceURIBuilder.build();
            feed.setId("urn:confluence:space:id:" + parent.getId());
        } else {
            throw new IllegalArgumentException("Unkown parent");
        }
        feed.setUpdated(parent.getLastModificationDate());
        feed.addLink(self.toString(), Link.REL_SELF);
        feed.addLink(getResourceURIBuilder(baseURIBuilder).build().toString(), "up");
        for (Page page : pages) {
            feed.addEntry(createEntryFromPage(spaceURIBuilder, page));
        }
        return feed;
    }

    private Entry createEntryFromPage(UriBuilder spaceURIBuilder, Page page) {
        Entry entry = abdera.newEntry();
        UriBuilder builder = spaceURIBuilder.clone().segment(PAGES_SEGMENT).segment(page.getIdAsString());
        if (page.hasChildren()) {
            //http://tools.ietf.org/html/rfc4685 Atom threading
            Link link = entry.addLink(builder.clone().segment("children").build().toString(), "replies");
            link.setAttributeValue(new QName("http://purl.org/syndication/thread/1.0", "count", "thr"), String.valueOf(page.getChildren().size()));
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
        PageContext context = page.toPageContext();
        String origType = context.getOutputType();
        context.setOutputType(RenderContextOutputType.HTML_EXPORT);
        String value = wikiStyleRenderer.convertWikiToXHtml(context, page.getContent());
        context.setOutputType(origType);
        entry.setContentAsXhtml(tidyCleaner.clean(value));
        entry.setEdited(page.getLastModificationDate());
        entry.setUpdated(page.getLastModificationDate());
        entry.setPublished(page.getCreationDate());
        //page.isDeleted() add a tombstone here.
        return entry;
    }

    private UriBuilder getResourceURIBuilder(UriBuilder baseUriBuilder) {
        return baseUriBuilder.clone().path(SpaceFeed.class);
    }

}
