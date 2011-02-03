package net.hamnaberg.confluence;

import com.atlassian.bonnie.Searchable;
import com.atlassian.confluence.core.DefaultSaveContext;
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
import com.atlassian.confluence.search.v2.searchfilter.InSpaceSearchFilter;
import com.atlassian.confluence.search.v2.sort.ModifiedSort;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.core.bean.EntityObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.user.User;
import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

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
    private final PermissionManager permissionManager;
    private Abdera abdera  = Abdera.getInstance();

    public PagesFeed(PageManager pageManager, SpaceManager spaceManager, WikiStyleRenderer wikiStyleRenderer, SearchManager searchManager, PermissionManager permissionManager) {
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.wikiStyleRenderer = wikiStyleRenderer;
        this.searchManager = searchManager;
        this.permissionManager = permissionManager;
        tidyCleaner = new TidyCleaner();

    }

    @GET
    public Response pages(@PathParam("key") String key, @Context UriInfo info, @QueryParam("pw") int pageNo) {
        User user = AuthenticatedUserThreadLocal.getUser();
        URI path = info.getBaseUriBuilder().replacePath("").build();
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
                            new InSpaceSearchFilter(new TreeSet<String>(Arrays.asList(key))),
                            new SubsetResultFilter(pageNo - 1, PAGE_SIZE)
                    )
            );
            List<Page> pages = new ArrayList<Page>();
            for (Searchable res : searchables) {
                Page page = (Page) res;

                if (page.getParent() == null && permissionManager.hasPermission(user, Permission.VIEW, page)) {
                    pages.add(page);
                }
            }
            Feed feed = generate(space, pages, info.getBaseUriBuilder(), path);
            result.populate(feed);
            return Response.ok(new AbderaResponseOutput(feed)).build();
        } catch (InvalidSearchException e) {
            throw new RuntimeException(e);
        }
    }

    @Path("{id}")
    @GET
    public Response page(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        User user = AuthenticatedUserThreadLocal.getUser();
        URI path = info.getBaseUriBuilder().replacePath("").build();
        Page page = pageManager.getPage(id);

        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (permissionManager.hasPermission(user, Permission.VIEW, page)) {
            UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
            return Response.ok(new AbderaResponseOutput(createEntryFromPage(resourceURIBuilder, page, path))).build();
        }
        else {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

    }
    
    @Path("{id}")
    @PUT
    public Response updatePage(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info, InputStream stream) {
        User user = AuthenticatedUserThreadLocal.getUser();
        
        Page page = pageManager.getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (permissionManager.hasPermission(user, Permission.EDIT, page)) {
            Document<Entry> doc = abdera.getParser().parse(stream);
            Entry entry = doc.getRoot();
            update(key, entry, page, info);
            return Response.noContent().build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    private void update(String key, Entry entry, Page page, UriInfo info) {
        URI path = info.getBaseUriBuilder().replacePath("").build();
        UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
        Entry entryFromPage = createEntryFromPage(resourceURIBuilder, page, path);
        if (!entryFromPage.getId().equals(entry.getId())) {
            throw new IllegalArgumentException("Wrong ID specified");
        }
        Content content = entry.getContentElement();
        if (content == null) {
            throw new IllegalArgumentException("No Content Specified");
        }
        if (content.getSrc() == null) {
            if (content.getContentType() == Content.Type.TEXT) {
                //Update content of the page.
                page.setContent(content.getText());
            }
            else {
                throw new IllegalArgumentException("We only support to update the Confluence format which is served in text/plain.");
            }
        }
        else if (!entryFromPage.getContentElement().getSrc().equals(content.getSrc())) {
            throw new IllegalArgumentException(String.format("The Content URI was changed; Expected '%s' got '%s'",entryFromPage.getContentElement().getSrc(), content.getSrc()));
        }
        //updating metadata.
        Link up = entry.getLink("up");
        if (!entryFromPage.getLink("up").equals(up)) {
            throw new IllegalArgumentException(String.format("You are not allowed to move stuff around! (Yet) Parent must not change. Expected %s", entryFromPage.getLink("up")));
        }
        //TODO: Consider adding support for moving stuff around... Setting the parent etc.
        DefaultSaveContext context = new DefaultSaveContext();
        context.setUpdateLastModifier(true);
        pageManager.saveContentEntity(page, context);
    }

    @Path("{id}/children")
    @GET
    public Response children(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        URI path = info.getBaseUriBuilder().replacePath("").build();
        Page page = pageManager.getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
        Feed feed = generate(page, page.getChildren(), resourceURIBuilder, path);
        return Response.ok(new AbderaResponseOutput(feed)).build();
    }

    private Feed generate(EntityObject parent, List<Page> pages, UriBuilder baseURIBuilder, URI hostAndPort) {
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
            feed.addEntry(createEntryFromPage(spaceURIBuilder, page, hostAndPort));
        }
        return feed;
    }

    @Path("{id}/content")
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public Response pageContentTextPlain(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        return getContent(key, id, info, false);
    }

    @Path("{id}/content")
    @GET
    @Produces({MediaType.APPLICATION_XHTML_XML})
    public Response pageContentXHTML(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        return getContent(key, id, info, true);
    }

    private Response getContent(String key, long id, UriInfo info, boolean xhtml) {
        URI path = info.getBaseUriBuilder().replacePath("").build();
        Page page = pageManager.getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (xhtml) {
            PageContext context = page.toPageContext();
            String origType = context.getOutputType();
            context.setOutputType(RenderContextOutputType.HTML_EXPORT);
            context.setSiteRoot(path.toString());
            String value = wikiStyleRenderer.convertWikiToXHtml(context, page.getContent());
            context.setOutputType(origType);
            CacheControl cc = new CacheControl();
            cc.setMustRevalidate(true);
            cc.setMaxAge(10);
            return Response.ok(tidyCleaner.clean(value)).type(MediaType.APPLICATION_XHTML_XML_TYPE).cacheControl(cc).build();

        }

        return Response.ok(page.getContent()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    private Entry createEntryFromPage(UriBuilder spaceURIBuilder, Page page, URI hostAndPort) {
        User user = AuthenticatedUserThreadLocal.getUser();

        Entry entry = abdera.newEntry();
        UriBuilder builder = spaceURIBuilder.clone().segment(PAGES_SEGMENT).segment(page.getIdAsString());
        if (page.hasChildren()) {
            //http://tools.ietf.org/html/rfc4685 Atom threading
            Link link = entry.addLink(builder.clone().segment("children").build().toString(), "replies");
            link.setAttributeValue(new QName("http://purl.org/syndication/thread/1.0", "count", "thr"), String.valueOf(page.getChildren().size()));
            //Add rel="feed" to entry
        }
        Link link = entry.addLink(UriBuilder.fromUri(hostAndPort).path(page.getUrlPath()).build().toString(), Link.REL_ALTERNATE);
        link.setMimeType("text/html");
        entry.addLink(builder.build().toString(), Link.REL_SELF);
        if (permissionManager.hasPermission(user, Permission.EDIT, page)) {
            entry.addLink(builder.build().toString(), Link.REL_EDIT);
        }
        entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.PAGE_TERM));
        entry.setTitle(page.getTitle());

        String name = page.getCreatorName();
        if (name == null) {
            name = "Confluence";
        }
        entry.addAuthor(name);
        entry.setId("urn:confluence:page:id:" + page.getIdAsString());
        entry.setEdited(page.getLastModificationDate());
        entry.setUpdated(page.getLastModificationDate());
        entry.setPublished(page.getCreationDate());
        entry.setContent(new IRI(builder.clone().segment("content").build()), MediaType.APPLICATION_XHTML_XML);
        //page.isDeleted() add a tombstone here.
        return entry;
    }

    private UriBuilder getResourceURIBuilder(UriBuilder baseUriBuilder) {
        return baseUriBuilder.clone().path(SpaceFeed.class);
    }

}
