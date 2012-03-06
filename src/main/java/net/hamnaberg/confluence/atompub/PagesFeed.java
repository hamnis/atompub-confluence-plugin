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

import bucket.core.comparators.*;
import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.core.ListBuilder;
import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.labels.Namespace;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.core.bean.EntityObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.user.User;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.*;
import org.apache.abdera.model.Collection;
import org.apache.abdera.parser.ParserOptions;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

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
    private static final String PAGES_SEGMENT = "pages";

    private TidyCleaner tidyCleaner;
    private Abdera abdera  = Abdera.getInstance();
    private final ConfluenceServices services;

    public PagesFeed(ConfluenceServices services) {
        this.services = services;
        tidyCleaner = new TidyCleaner();
    }

    @GET
    public Response pages(@PathParam("key") String spaceKey,
                          @Context UriInfo info,
                          @QueryParam("pw") int pageNo,
                          @QueryParam("sort-enabled") @DefaultValue("false") boolean sortEnabled,
                          @QueryParam("page-size") @DefaultValue("50") int pageSize) {
        User user = AuthenticatedUserThreadLocal.getUser();
        URI path = info.getBaseUriBuilder().replacePath("").build();
        Space space = services.getSpaceManager().getSpace(spaceKey);
        if (space == null) {
            throw new IllegalArgumentException(String.format("No space called %s found", spaceKey));
        }
        if (pageNo < 1) {
            pageNo = 1;
        }
        ListBuilder<Page> topLevelPagesBuilder = services.getPageManager().getTopLevelPagesBuilder(space);
        int availableSize = topLevelPagesBuilder.getAvailableSize(); //todo: this may be incorrect
        PagedResult result = new PagedResult(availableSize, pageNo, pageSize, info.getBaseUriBuilder());
        List<Page> pages = filter(user, topLevelPagesBuilder.getPage(result.getCurrentIndex(), pageSize), sortEnabled);
        Feed feed = makeFeed(space, pages, info.getBaseUriBuilder());
        result.populate(feed);
        CacheControl cc = services.getConfigurationAccessor().getConfig().getPageFeed().toCacheControl();
        return Response.ok(new AbderaResponseOutput(feed)).cacheControl(cc).build();
    }

    @POST
    public Response create(@PathParam("key") String key, @Context UriInfo info, InputStream stream) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Space space = services.getSpaceManager().getSpace(key);
        if (space == null) {
            throw new IllegalArgumentException(String.format("No space called %s found", key));
        }

        if (services.getPermissionManager().hasCreatePermission(user, space, Page.class)) {
            ParserOptions options = abdera.getParser().getDefaultParserOptions();
            options.setAutodetectCharset(false);
            options.setCharset("UTF-8");
            Document<Entry> document = abdera.getParser().parse(stream, options);
            Entry entry = document.getRoot();
            ConfluenceUtil.validateCategories(entry, ConfluenceUtil.createCategory(ConfluenceUtil.PAGE_TERM));
            Page page = new Page();
            page.setTitle(entry.getTitle());
            page.setSpace(space);
            page.setCreatorName(user.getName());
            page.setCreationDate(new Date());
            page.setParentPage(space.getHomePage());
            Content content = entry.getContentElement();
            if (content != null && content.getContentType() == Content.Type.TEXT) {
                page.setContent(content.getValue());

            } else {
                throw new IllegalArgumentException("Error in content. Expected text");
            }
            DefaultSaveContext context = new DefaultSaveContext();
            context.setUpdateLastModifier(true);
            services.getPageManager().saveContentEntity(page, context);

            for (Category cat : entry.getCategories(ConfluenceUtil.CONFLUENCE_LABEL_SCHEME)) {
                services.getLabelManager().addLabel(page, ConfluenceUtil.createLabel(cat));
            }

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
        return getPage(key, id, info, false);
    }

    @Path("{id}/edit")
    @GET
    public Response editablePage(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        return getPage(key, id, info, true);
    }

    private Response getPage(String key, long id, UriInfo info, boolean edit) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Page page = services.getPageManager().getPage(id);

        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (services.getPermissionManager().hasPermission(user, edit ? Permission.EDIT : Permission.VIEW, page)) {
            UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
            CacheControl cc = services.getConfigurationAccessor().getConfig().getPage().toCacheControl();
            return Response.ok(new AbderaResponseOutput(createEntryFromPage(resourceURIBuilder, page, edit))).
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
    public Response updatePage(@PathParam("key") String key, @PathParam("id") long id, @HeaderParam("If-Unmodified-Since") Date lastMod, @Context UriInfo info, @Context Request request, InputStream stream) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Page page = services.getPageManager().getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (lastMod == null) {
            return Response.status(428).type("text/plain").entity("Precondition required: You need to include a If-Unmodified-Since header").build();
        }
        if (services.getPermissionManager().hasPermission(user, Permission.EDIT, page)) {
            Response.ResponseBuilder preconditions = request.evaluatePreconditions(page.getLastModificationDate());
            if (preconditions != null) {
                return preconditions.build();
            }
            ParserOptions options = abdera.getParser().getDefaultParserOptions();
            options.setAutodetectCharset(false);
            options.setCharset("UTF-8");
            Document<Entry> doc = abdera.getParser().parse(stream, options).complete();
            Entry entry = doc.getRoot();
            update(key, entry, page, info);
            return Response.noContent().build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @Path("{id}/children")
    @GET
    public Response children(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info, @QueryParam("sort-enabled") @DefaultValue("false") boolean sortEnabled) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Page page = services.getPageManager().getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (services.getPermissionManager().hasPermission(user, Permission.VIEW, page)) {
            List<Page> children = filter(user, page.getChildren(), sortEnabled);
            Feed feed = makeFeed(page, children, info.getBaseUriBuilder());
            CacheControl cc = services.getConfigurationAccessor().getConfig().getPageFeed().toCacheControl();
            return Response.ok(new AbderaResponseOutput(feed)).cacheControl(cc).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @Path("{id}/attachments")
    @GET
    public Response attachments(@PathParam("key") String key, @PathParam("id") long id, @Context UriInfo info) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Page page = services.getPageManager().getPage(id);
        if (page == null) {
            throw new IllegalArgumentException(String.format("No page with id %s found", id));
        }
        if (!page.getSpaceKey().equals(key)) {
            throw new IllegalArgumentException("Trying to get a page which does not belong in the space");
        }
        if (services.getPermissionManager().hasPermission(user, Permission.VIEW, page)) {
            Feed feed = new AttachmentFeed(services).buildFeed(page.getAttachments(), info);
            CacheControl cc = services.getConfigurationAccessor().getConfig().getPageFeed().toCacheControl();
            return Response.ok(new AbderaResponseOutput(feed)).cacheControl(cc).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    private void update(String key, Entry entry, Page page, UriInfo info) {
        UriBuilder resourceURIBuilder = getResourceURIBuilder(info.getBaseUriBuilder()).segment(key);
        Entry entryFromPage = createEntryFromPage(resourceURIBuilder, page, false);
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
        if (entryFromPage.getLink("up") != null && !entryFromPage.getLink("up").equals(up)) {
            throw new IllegalArgumentException(String.format("You are not allowed to move stuff around! (Yet) Parent must not change. Expected %s", entryFromPage.getLink("up")));
        }

        for (Category cat : entry.getCategories(ConfluenceUtil.CONFLUENCE_LABEL_SCHEME)) {
            services.getLabelManager().addLabel(page, ConfluenceUtil.createLabel(cat));
        }
        //TODO: Consider adding support for moving stuff around... Setting the parent etc.
        DefaultSaveContext context = new DefaultSaveContext();
        context.setUpdateLastModifier(true);
        services.getPageManager().saveContentEntity(page, context);
    }

    private Feed makeFeed(EntityObject parent, List<Page> pages, UriBuilder baseURIBuilder) {
        Feed feed = abdera.newFeed();
        URI self;
        UriBuilder spaceURIBuilder;
        if (parent instanceof Page) {
            Page page = (Page) parent;
            spaceURIBuilder = getResourceURIBuilder(baseURIBuilder).clone().segment(page.getSpaceKey());
            self = getResourceURIBuilder(baseURIBuilder).clone().segment(page.getSpaceKey()).segment(PAGES_SEGMENT).segment(page.getIdAsString(), "children").build();
            feed.setTitle("Children of " + ((Page) parent).getDisplayTitle());
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
            feed.addEntry(createEntryFromPage(spaceURIBuilder, page, false));
        }
        return feed;
    }

    private Entry createEntryFromPage(UriBuilder spaceURIBuilder, Page page, boolean edit) {
        Entry entry = abdera.newEntry();
        UriBuilder builder = spaceURIBuilder.clone().segment(PAGES_SEGMENT).segment(page.getIdAsString());
        Link link = entry.addLink(UriBuilder.fromUri(services.getSettingsManager().getGlobalSettings().getBaseUrl()).path(page.getUrlPath()).build().toString(), Link.REL_ALTERNATE);
        link.setMimeType("text/html");
        entry.addLink(builder.build().toString(), Link.REL_SELF);
        entry.addLink(spaceURIBuilder.clone().build().toString(), "collection");
        entry.addLink(builder.clone().segment("edit").build().toString(), Link.REL_EDIT);
        entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.PAGE_TERM));
        entry.setTitle(page.getDisplayTitle());

        String name = page.getCreatorName();
        if (name == null) {
            name = "Confluence";
        }

        entry.addAuthor(name);
        entry.setId("urn:confluence:page:id:" + page.getIdAsString());
        entry.setEdited(page.getLastModificationDate());
        entry.setUpdated(page.getLastModificationDate());
        entry.setPublished(page.getCreationDate());
        entry.setContentElement(getContent(page, edit));

        List<Label> labels = page.getLabels();
        for (Label label : labels) {
            if (label.getNamespace().getPrefix().equals(Namespace.GLOBAL.getPrefix())) {
                entry.addCategory(ConfluenceUtil.createCategoryLabel(label));
            }
        }

        if (page.hasChildren()) {
            //http://tools.ietf.org/html/rfc4685 Atom threading
            Collection collection = abdera.getFactory().newCollection();
            collection.acceptsNothing();
            collection.setHref(builder.clone().segment("children").build().toString());
            collection.setTitle("children");
            entry.addExtension(collection);
        }
        entry.addLink(builder.clone().segment("attachments").build().toString(), Link.REL_RELATED);

        List<Attachment> attachments = page.getAttachments();
        for (Attachment attachment : attachments) {
            AttachmentFeed.createEnclosureLink(attachment, entry, services);
        }

        return entry;
    }

    private Content getContent(Page page, boolean edit) {
        Content content = abdera.getFactory().newContent();
        String baseUrl = services.getSettingsManager().getGlobalSettings().getBaseUrl();

        if (edit) {
            content.setContentType(Content.Type.TEXT);
            content.setValue(page.getContent());
        } else {
            PageContext context = page.toPageContext();
            String origType = context.getOutputType();
            context.setOutputType(RenderContextOutputType.FEED);
            context.setSiteRoot(baseUrl);
            context.setImagePath(baseUrl);
            context.setBaseUrl(baseUrl);
            String value = services.getWikiStyleRenderer().convertWikiToXHtml(context, page.getContent());
            context.setOutputType(origType);
            content.setContentType(Content.Type.XHTML);
            content.setValue(tidyCleaner.clean(value));
        }

        return content;
    }

    private UriBuilder getResourceURIBuilder(UriBuilder baseUriBuilder) {
        return baseUriBuilder.clone().path(SpaceFeed.class);
    }

    private List<Page> filter(User user, List<Page> page, boolean sort) {
        List<Page> pages = new ArrayList<Page>();
        for (Page p : page) {
            if (services.getPermissionManager().hasPermission(user, Permission.VIEW, p)) {
                pages.add(p);
            }
        }
        if (sort) {
            Collections.sort(pages, Collections.reverseOrder(new LastModificationDateComparator()));
        }
        return pages;
    }
}
