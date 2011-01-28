package net.hamnaberg.confluence;

import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.SpaceType;
import com.atlassian.confluence.spaces.SpacesQuery;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.core.bean.EntityObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.user.User;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.*;

@Path("spaces")
@Produces("application/atom+xml")
@Consumes("application/atom+xml")
@AnonymousAllowed
public class SpaceFeed {

    // We just have to define the variables and the setters, then Spring injects the correct objects for us to use. Simple and efficient.
    // You just need to know *what* you want to inject and use.

    private final SpaceManager spaceManager;
    private final Abdera abdera = Abdera.getInstance();
    private Comparator<EntityObject> reverseLastModifiedComporator;

    public SpaceFeed(SpaceManager spaceManager) {
        this.spaceManager = spaceManager;
        reverseLastModifiedComporator = Collections.reverseOrder(new LastModificationDateComparator());
    }

    @GET
    public Response spaces(@Context UriInfo info) {
        User user = AuthenticatedUserThreadLocal.getUser();
        Feed feed = abdera.newFeed();
        feed.setId(info.getRequestUri().toString());
        feed.setTitle("Confluence Space feed");
        SpacesQuery query = SpacesQuery.newQuery().forUser(user).withSpaceType(SpaceType.GLOBAL).withPermission(SpacePermission.VIEWSPACE_PERMISSION).build();
        List<Space> spaces = new ArrayList<Space>(spaceManager.getAllSpaces(query));
        Collections.sort(spaces, reverseLastModifiedComporator);
        UriBuilder uriBuilder = info.getRequestUriBuilder();
        if (spaces.size() >= 1) {
            feed.setUpdated(spaces.get(0).getLastModificationDate());
        }
        else {
            feed.setUpdated(new Date());
        }
        for (Space space : spaces) {
            Entry entry = abdera.newEntry();
            entry.setId("urn:confluence:space:id:" + space.getId());
            entry.setTitle(space.getName());
            entry.addCategory(ConfluenceUtil.createCategory(ConfluenceUtil.SPACE_TERM));
            entry.addAuthor(space.getCreatorName());
            entry.setUpdated(space.getLastModificationDate());
            entry.setSummary("");
            Link link = entry.addLink(space.getHomePage().getUrlPath(), Link.REL_ALTERNATE);
            link.setMimeType("text/html");
            entry.addExtension(createCollection(uriBuilder, space, "pages"));
            entry.addExtension(createCollection(uriBuilder, space, "news"));
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

}
