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
import org.apache.abdera.model.*;
import org.apache.commons.lang.StringUtils;

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
            CacheControl cc = new CacheControl();
            cc.setMaxAge(60);
            cc.setNoTransform(true);
            return Response.ok(new AbderaResponseOutput(feed)).cacheControl(cc).build();
        } catch (InvalidSearchException e) {
            throw new RuntimeException(e);
        }
    }


    @POST
    public Response create(@PathParam("key") String key, @Context UriInfo info, InputStream stream) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Space space = spaceManager.getSpace(key);
        if (space == null) {
            throw new IllegalArgumentException(String.format("No space called %s found", key));
        }

        if (permissionManager.hasCreatePermission(user, space, Page.class)) {
            Document<Entry> document = abdera.getParser().parse(stream);
            Entry entry = document.getRoot();
            Page page = new Page();
            page.setTitle(entry.getTitle());
            page.setSpace(space);
            Content content = entry.getContentElement();
            if (content != null && content.getContentType() == Content.Type.TEXT) {
                page.setContent(entry.getContent());

            } else {
                throw new IllegalArgumentException("Error in content. Expected text");
            }
            pageManager.saveContentEntity(page, new DefaultSaveContext());
            if (page.getIdAsString() != null) {
                return Response.created(info.getRequestUriBuilder().path(page.getIdAsString()).build()).build();
            }
            return Response.serverError().build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @Path("{id}")
    @GET
    public Response page(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        return getPage(key, id, info, true);
    }

    @Path("{id}/edit")
    @GET
    public Response editablePage(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        return getPage(key, id, info, false);
    }

    private Response getPage(String key, long id, UriInfo info, boolean edit) {
        User user = AuthenticatedUserThreadLocal.getUser();
        URI path = info.getBaseUriBuilder().replacePath("").build();
        Page page = pageManager.getPage(id);

        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (permissionManager.hasPermission(user, edit ? Permission.EDIT : Permission.VIEW, page)) {
            UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
            CacheControl cc = new CacheControl();
            cc.setMaxAge(60);
            cc.setMustRevalidate(true);
            return Response.ok(new AbderaResponseOutput(createEntryFromPage(resourceURIBuilder, page, path, edit))).
                    cacheControl(cc).
                    lastModified(page.getLastModificationDate()).
                    build();
        }
        else {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @Path("{id}/edit")
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
        Entry entryFromPage = createEntryFromPage(resourceURIBuilder, page, path, false);
        if (!entryFromPage.getId().equals(entry.getId())) {
            throw new IllegalArgumentException("Wrong ID specified");
        }
        page.setTitle(entry.getTitle());
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
        else {
            throw new IllegalArgumentException("We do not allow Content with src on edit");
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
            feed.addEntry(createEntryFromPage(spaceURIBuilder, page, hostAndPort, true));
        }
        return feed;
    }

    private Entry createEntryFromPage(UriBuilder spaceURIBuilder, Page page, URI hostAndPort, boolean edit) {
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
        entry.addLink(builder.clone().segment("edit").build().toString(), Link.REL_EDIT);
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
        entry.setContentElement(getContent(page, hostAndPort, !edit));
        //page.isDeleted() add a tombstone here.
        return entry;
    }

    private Content getContent(Page page, URI baseURI, boolean xhtml) {
        Content content = abdera.getFactory().newContent();

        if (xhtml) {
            PageContext context = page.toPageContext();
            String origType = context.getOutputType();
            context.setOutputType(RenderContextOutputType.HTML_EXPORT);
            context.setSiteRoot(baseURI.toString());
            String value = wikiStyleRenderer.convertWikiToXHtml(context, page.getContent());
            context.setOutputType(origType);
            content.setContentType(Content.Type.XHTML);
            content.setValue(tidyCleaner.clean(value));
        } else {
            content.setContentType(Content.Type.TEXT);
            content.setValue(page.getContent());
        }

        return content;
    }

    private UriBuilder getResourceURIBuilder(UriBuilder baseUriBuilder) {
        return baseUriBuilder.clone().path(SpaceFeed.class);
    }
}
