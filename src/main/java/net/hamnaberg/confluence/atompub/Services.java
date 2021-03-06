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

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static net.hamnaberg.confluence.atompub.ConfluenceUtil.*;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/22/11
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/")
@AnonymousAllowed
@Produces("application/atomsvc+xml")
public class Services {

    protected static final CacheControl CACHE_CONTROL = new CacheControl();
    static {
        CACHE_CONTROL.setNoTransform(true);
        CACHE_CONTROL.setMaxAge(24 * 3600);
    }
    private Abdera abdera;

    public Services() {
        abdera = Abdera.getInstance();
    }

    @GET
    public Response service(@Context UriInfo info) {
        Service service = abdera.newService();
        Workspace spaces = service.addWorkspace("spaces");
        Collection collection = spaces.addCollection("spaces", info.getBaseUriBuilder().path(SpaceFeed.class).build().toString());
        collection.setAcceptsNothing();
        Categories categories = abdera.newCategories();
        categories.setFixed(true);
        categories.addCategory(createCategory(PAGE_TERM));
        categories.addCategory(createCategory(SPACE_TERM));
        categories.addCategory(createCategory(NEWS_TERM));
        categories.addCategory(createCategory(COMMENT_TERM));
        collection.addCategories(categories);
        return Response.ok(new AbderaResponseOutput(service)).cacheControl(CACHE_CONTROL).build();
    }
}
