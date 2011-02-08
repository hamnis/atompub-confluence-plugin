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
import com.atlassian.confluence.pages.BlogPost;
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
import org.apache.abdera.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
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

    @POST
    public Response create(@PathParam("key") String key, @Context UriInfo info, InputStream stream) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Space space = spaceManager.getSpace(key);
        if (space == null) {
            throw new IllegalArgumentException(String.format("No space called %s found", key));
        }

        if (permissionManager.hasCreatePermission(user, space, BlogPost.class)) {
            Document<Entry> document = abdera.getParser().parse(stream);
            Entry entry = document.getRoot();
            BlogPost post = new BlogPost();
            post.setTitle(entry.getTitle());
            post.setSpace(space);
            Content content = entry.getContentElement();
            if (content != null && content.getContentType() == Content.Type.TEXT) {
                post.setContent(entry.getContent());

            } else {
                throw new IllegalArgumentException("Error in content. Expected text");
            }
            pageManager.saveContentEntity(post, new DefaultSaveContext());
            if (post.getIdAsString() != null) {
                return Response.created(info.getRequestUriBuilder().path(post.getIdAsString()).build()).build();
            }
            return Response.serverError().build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }
    

    @Path("{id}")
    @GET
    public Response item(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        return getItem(key, id, info, false);
    }

    @Path("{id}/edit")
    @GET
    public Response editableItem(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        return getItem(key, id, info, true);
    }

    private Response getItem(String key, long id, UriInfo info, boolean edit) {
        User user = AuthenticatedUserThreadLocal.getUser();

        URI path = info.getBaseUriBuilder().replacePath("").build();
        BlogPost post = pageManager.getBlogPost(id);
        if (post == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!post.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (permissionManager.hasPermission(user, edit ? Permission.EDIT : Permission.VIEW, post)) {
            UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
            return Response.ok(new AbderaResponseOutput(createEntryFromPage(resourceURIBuilder, post, path, !edit))).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @Path("{id}/edit")
    @PUT
    public Response updateItem(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info, InputStream stream) {
        User user = AuthenticatedUserThreadLocal.getUser();

        BlogPost post = pageManager.getBlogPost(id);
        if (post == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!post.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (permissionManager.hasPermission(user, Permission.EDIT, post)) {
            Document<Entry> doc = abdera.getParser().parse(stream);
            Entry entry = doc.getRoot();
            update(key, entry, post, info);
            return Response.noContent().build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    private void update(String key, Entry entry, BlogPost post, UriInfo info) {
        URI path = info.getBaseUriBuilder().replacePath("").build();
        UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
        Entry entryFromPage = createEntryFromPage(resourceURIBuilder, post, path, false);
        if (!entryFromPage.getId().equals(entry.getId())) {
            throw new IllegalArgumentException("Wrong ID specified");
        }
        post.setTitle(entry.getTitle());
        Content content = entry.getContentElement();
        if (content == null) {
            throw new IllegalArgumentException("No Content Specified");
        }
        if (content.getSrc() == null) {
            if (content.getContentType() == Content.Type.TEXT) {
                //Update content of the page.
                post.setContent(content.getText());
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
        pageManager.saveContentEntity(post, context);
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
            feed.addEntry(createEntryFromPage(spaceURIBuilder, page, hostAndPort, true));
        }
        return feed;
    }

    private Entry createEntryFromPage(UriBuilder spaceURIBuilder, BlogPost post, URI hostAndPort, boolean xhtml) {
        Entry entry = abdera.newEntry();
        UriBuilder builder = spaceURIBuilder.clone().segment(NEWS_SEGMENT).segment(post.getIdAsString());
        Link link = entry.addLink(UriBuilder.fromUri(hostAndPort).path(post.getUrlPath()).build().toString(), Link.REL_ALTERNATE);
        link.setMimeType("text/html");
        entry.addLink(builder.build().toString(), Link.REL_SELF);
        entry.addLink(builder.clone().segment("edit").build().toString(), Link.REL_SELF);
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
        entry.setContentElement(getContent(post, hostAndPort, xhtml));
        //page.isDeleted() add a tombstone here.
        return entry;
    }

    private Content getContent(BlogPost post, URI baseURI, boolean xhtml) {
        Content content = abdera.getFactory().newContent();

        if (xhtml) {
            PageContext context = post.toPageContext();
            String origType = context.getOutputType();
            context.setOutputType(RenderContextOutputType.HTML_EXPORT);
            context.setSiteRoot(baseURI.toString());
            String value = wikiStyleRenderer.convertWikiToXHtml(context, post.getContent());
            context.setOutputType(origType);
            content.setContentType(Content.Type.XHTML);
            content.setValue(tidyCleaner.clean(value));
        } else {
            content.setContentType(Content.Type.TEXT);
            content.setValue(post.getContent());
        }

        return content;
    }

    private UriBuilder getResourceURIBuilder(UriBuilder baseUriBuilder) {
        return baseUriBuilder.clone().path(SpaceFeed.class);
    }

}