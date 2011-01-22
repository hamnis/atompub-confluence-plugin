package net.hamnaberg.confluence;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static net.hamnaberg.confluence.ConfluenceUtil.*;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/22/11
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("service")
@AnonymousAllowed
@Produces("application/atomsvc+xml")
public class Services {

    private Abdera abdera;

    public Services() {
        abdera = Abdera.getInstance();
    }

    @GET
    public Response service(@Context UriInfo info) {
        Service service = abdera.newService();
        Workspace spaces = service.addWorkspace("spaces");
        Collection collection = spaces.addCollection("spaces", info.getBaseUriBuilder().path(SpaceFeed.class).build().toString());
        Categories categories = abdera.newCategories();
        categories.setFixed(true);
        categories.addCategory(createCategory(PAGE_TERM));
        categories.addCategory(createCategory(SPACE_TERM));
        categories.addCategory(createCategory(COMMENT_TERM));
        collection.addCategories(categories);
        return Response.ok(new AbderaResponseOutput(service)).build();
    }
}
