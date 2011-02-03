package net.hamnaberg.confluence;

import com.atlassian.bonnie.Searchable;
import com.atlassian.confluence.pages.BlogPost;
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
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.user.User;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
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
@Path("spaces/{key}/news")
@Produces("application/atom+xml")
@Consumes("application/atom+xml")
@AnonymousAllowed
public class NewsFeed {
    private static final int PAGE_SIZE = 10;
    private static final String NEWS_SEGMENT = "news";

    private TidyCleaner tidyCleaner;
    private final PageManager pageManager;
    private final SpaceManager spaceManager;
    private final WikiStyleRenderer wikiStyleRenderer;
    private final SearchManager searchManager;
    private final PermissionManager permissionManager;
    private Abdera abdera  = Abdera.getInstance();

    public NewsFeed(PageManager pageManager, SpaceManager spaceManager, WikiStyleRenderer wikiStyleRenderer, SearchManager searchManager, PermissionManager permissionManager) {
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.wikiStyleRenderer = wikiStyleRenderer;
        this.searchManager = searchManager;
        this.permissionManager = permissionManager;
        tidyCleaner = new TidyCleaner();
    }

    @GET
    public Response news(@PathParam("key") String key, @Context UriInfo info, @QueryParam("pw") int pageNo) {
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
            int availableSize = spaceManager.getNumberOfBlogPosts(space);
            PagedResult result = new PagedResult(availableSize, pageNo, PAGE_SIZE, info.getBaseUriBuilder());
            List<Searchable> searchables = searchManager.searchEntities(
                    new ContentSearch(
                            new ContentTypeQuery(ContentTypeEnum.BLOG),
                            new ModifiedSort(SearchSort.Order.DESCENDING),
                            new InSpaceSearchFilter(new TreeSet<String>(Arrays.asList(key))),
                            new SubsetResultFilter(pageNo - 1, PAGE_SIZE)
                    )
            );
            List<BlogPost> pages = new ArrayList<BlogPost>();
            for (Searchable res : searchables) {
                if (permissionManager.hasPermission(user, Permission.VIEW, res)) {
                    pages.add((BlogPost) res);
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
    public Response item(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        User user = AuthenticatedUserThreadLocal.getUser();

        URI path = info.getBaseUriBuilder().replacePath("").build();
        BlogPost post = pageManager.getBlogPost(id);
        if (post == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!post.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (permissionManager.hasPermission(user, Permission.VIEW, post)) {
            UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
            return Response.ok(new AbderaResponseOutput(createEntryFromPage(resourceURIBuilder, post, path))).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }
    
    private Feed generate(Space parent, List<BlogPost> pages, UriBuilder baseURIBuilder, URI hostAndPort) {
        Feed feed = abdera.newFeed();
        feed.setTitle(parent.getName());
        UriBuilder spaceURIBuilder = getResourceURIBuilder(baseURIBuilder).clone().segment(parent.getKey());
        feed.setId("urn:confluence:space:id:" + parent.getId());
        feed.setUpdated(parent.getLastModificationDate());
        feed.addLink(spaceURIBuilder.build().toString(), Link.REL_SELF);
        feed.addLink(getResourceURIBuilder(baseURIBuilder).build().toString(), "up");
        for (BlogPost page : pages) {
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
            return Response.ok(tidyCleaner.clean(value)).type(MediaType.APPLICATION_XHTML_XML_TYPE).build();

        }

        return Response.ok(page.getContent()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    private Entry createEntryFromPage(UriBuilder spaceURIBuilder, BlogPost post, URI hostAndPort) {
        Entry entry = abdera.newEntry();
        UriBuilder builder = spaceURIBuilder.clone().segment(NEWS_SEGMENT).segment(post.getIdAsString());
        Link link = entry.addLink(UriBuilder.fromUri(hostAndPort).path(post.getUrlPath()).build().toString(), Link.REL_ALTERNATE);
        link.setMimeType("text/html");
        entry.addLink(builder.build().toString(), Link.REL_SELF);
        entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.NEWS_TERM));
        entry.setTitle(post.getTitle());

        String name = post.getCreatorName();
        if (name == null) {
            name = "Confluence";
        }
        entry.addAuthor(name);
        entry.setId("urn:confluence:blogentry:id:" + post.getIdAsString());
        entry.setEdited(post.getLastModificationDate());
        entry.setUpdated(post.getLastModificationDate());
        entry.setPublished(post.getCreationDate());
        PageContext context = post.toPageContext();
        String origType = context.getOutputType();
        context.setOutputType(RenderContextOutputType.HTML_EXPORT);
        context.setSiteRoot(hostAndPort.toString());
        String value = wikiStyleRenderer.convertWikiToXHtml(context, post.getContent());
        context.setOutputType(origType);
        entry.setContentAsXhtml(tidyCleaner.clean(value));
        //page.isDeleted() add a tombstone here.
        return entry;
    }

    private UriBuilder getResourceURIBuilder(UriBuilder baseUriBuilder) {
        return baseUriBuilder.clone().path(SpaceFeed.class);
    }

}
