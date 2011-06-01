package net.hamnaberg.confluence.atompub;


import com.atlassian.bonnie.Searchable;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.search.service.ContentTypeEnum;
import com.atlassian.confluence.search.v2.*;
import com.atlassian.confluence.search.v2.filter.SubsetResultFilter;
import com.atlassian.confluence.search.v2.query.*;
import com.atlassian.confluence.search.v2.sort.ModifiedSort;
import com.atlassian.confluence.search.v2.sort.RelevanceSort;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.user.User;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.abdera.Abdera;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.ext.opensearch.model.OpenSearchDescription;
import org.apache.abdera.ext.opensearch.model.Query;
import org.apache.abdera.ext.opensearch.model.Url;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;

@Path("/search")
@AnonymousAllowed
public class Search {
    private ConfluenceServices services;
    private static final int PAGE_SIZE = 50;

    public Search(ConfluenceServices services) {
        this.services = services;
    }

    @GET
    @Produces(OpenSearchConstants.OPENSEARCH_DESCRIPTION_CONTENT_TYPE)
    @Path("description")
    public Response getDescription(@Context UriInfo info) {
        Factory factory = Abdera.getNewFactory();
        OpenSearchDescription description = new OpenSearchDescription(factory);
        description.setDescription("Search for pages and news in all spaces");
        description.setShortName("confluence-all");
        Url url = new Url(factory);
        url.setTemplate(info.getBaseUriBuilder().segment("search").build().toString() + "?q={query}");
        description.addUrls(url);
        Query query = new Query(factory);
        query.setRole(Query.Role.EXAMPLE);
        query.setSearchTerms("confluence");
        description.addQueries(query);
        CacheControl cc = new CacheControl();
        cc.setMaxAge(24 * 3600 * 365);
        return Response.ok(new AbderaResponseOutput(description)).cacheControl(cc).build();
    }

    @GET
    @Produces(OpenSearchConstants.OPENSEARCH_DESCRIPTION_CONTENT_TYPE)
    @Path("space/{space}/description")
    public Response getDescription(@PathParam("space") String space, @Context UriInfo info) {
        Factory factory = Abdera.getNewFactory();
        OpenSearchDescription description = new OpenSearchDescription(factory);
        description.setDescription(String.format("Search for pages and news in space %s", space));
        description.setShortName("confluence-space-search");
        Url url = new Url(factory);
        url.setTemplate(info.getBaseUriBuilder().segment("search", "space", space).build().toString() + "?q={query}");
        description.addUrls(url);
        Query query = new Query(factory);
        query.setRole(Query.Role.EXAMPLE);
        query.setSearchTerms("confluence");
        description.addQueries(query);
        CacheControl cc = new CacheControl();
        cc.setMaxAge(24 * 3600 * 365);
        return Response.ok(new AbderaResponseOutput(description)).cacheControl(cc).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_ATOM_XML)
    public Response search(@QueryParam("q") String query, @QueryParam("pw") int pageNo, @Context final UriInfo info) {
        return doSearch(query, pageNo, null, info);
    }

    @GET
    @Produces(MediaType.APPLICATION_ATOM_XML)
    @Path("space/{space}")
    public Response searchSpace(@PathParam("space") String space, @QueryParam("q") String query, @QueryParam("pw") int pageNo, @Context final UriInfo info) {
        return doSearch(query, pageNo, space, info);
    }

    private Response doSearch(String query, int pageNo, String space, UriInfo info) {
        final User user = AuthenticatedUserThreadLocal.getUser();
        final Factory factory = Abdera.getInstance().getFactory();
        Feed feed = factory.newFeed();
        feed.setId(UUID.randomUUID().toString());
        feed.setUpdated(new Date());
        Query actualQuery = new Query(factory);
        actualQuery.setInputEncoding("UTF-8");
        actualQuery.setOutputEncoding("UTF-8");
        actualQuery.setStartPage(1);
        actualQuery.setRole(Query.Role.REQUEST);
        actualQuery.setSearchTerms(query);
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (StringUtils.isBlank(query)) {
            feed.setTitle("No results");
            feed.addExtension(actualQuery);
        } else {
            if (space != null) {
                feed.setTitle(String.format("Searching for '%s' in %s", query, space));
            }
            else {
                feed.setTitle(String.format("Searching for '%s'", query));
            }
            feed.addExtension(actualQuery);
            try {
                HashSet<SearchQuery> must = Sets.<SearchQuery>newHashSet(new ContentTypeQuery(Arrays.<ContentTypeEnum>asList(
                        ContentTypeEnum.BLOG,
                        ContentTypeEnum.PAGE
                )));
                if (space != null) {
                    must.add(new InSpaceQuery(space));
                }
                BooleanQuery booleanQuery = new BooleanQuery(
                        must,
                        Collections.singleton(new MultiTextFieldQuery(query, "title", "contentBody")),
                        Collections.<SearchQuery>emptySet()
                );

                ContentSearch contentSearch = new ContentSearch(booleanQuery, new RelevanceSort(), null, new SubsetResultFilter(pageNo, PAGE_SIZE));
                SearchResults results = services.getSearchManager().search(contentSearch);
                List<Searchable> searchables = services.getSearchManager().convertToEntities(results, true);

                Collection<Searchable> filtered = Collections2.filter(searchables, new Predicate<Searchable>() {
                    public boolean apply(Searchable input) {
                        return services.getPermissionManager().hasPermission(user, Permission.VIEW, input);
                    }
                });
                Collection<Entry> entries = Collections2.transform(filtered, new Searchable2Entry(factory, info.getBaseUriBuilder()));
                PagedResult result = new PagedResult(results.getUnfilteredResultsCount(), pageNo, PAGE_SIZE, info.getRequestUriBuilder());
                result.populate(feed);
                for (Entry entry : entries) {
                    feed.addEntry(entry);
                }
            } catch (InvalidSearchException e) {
                return Response.serverError().type("text/plain").entity(e.getMessage()).build();
            }
        }
        CacheControl cc = new CacheControl();
        cc.setMaxAge(5);
        return Response.ok(new AbderaResponseOutput(feed)).cacheControl(cc).build();
    }

    private class Searchable2Entry implements Function<Searchable, Entry> {
        private final Factory factory;
        private final UriBuilder baseUriBuilder;

        public Searchable2Entry(Factory factory, UriBuilder baseUriBuilder) {
            this.factory = factory;
            this.baseUriBuilder = baseUriBuilder;
        }

        public Entry apply(Searchable from) {
            Entry entry = factory.newEntry();
            if (from instanceof AbstractPage) {
                AbstractPage page = (AbstractPage) from;
                String type = null;
                if (page instanceof Page) {
                    type = "pages";
                    entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.PAGE_TERM));
                }
                else if (page instanceof BlogPost) {
                    entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.NEWS_TERM));
                    type = "news";
                }
                entry.addLink(baseUriBuilder.clone().segment("spaces", page.getSpaceKey(), type, page.getIdAsString()).build().toString(), Link.REL_SELF);
                entry.addLink(baseUriBuilder.clone().segment("spaces", page.getSpaceKey(), type, page.getIdAsString(), "edit").build().toString(), Link.REL_EDIT);
                entry.setSummaryAsXhtml(services.getWikiStyleRenderer().convertWikiToXHtml(page.toPageContext(), page.getExcerpt()));
                entry.setEdited(page.getLastModificationDate());
                entry.setUpdated(page.getLastModificationDate());
                entry.setPublished(page.getCreationDate());
                String creatorName = page.getCreatorName();
                if (creatorName != null) {
                    entry.addAuthor(creatorName);
                    if (!creatorName.equals(page.getLastModifierName())) {
                        entry.addAuthor(page.getLastModifierName());
                    }
                }
                else {
                    entry.addAuthor("Confluence");
                }
                entry.setTitle(page.getDisplayTitle());
            }
            return entry;
        }
    }
}
