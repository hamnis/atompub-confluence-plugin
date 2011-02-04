package net.hamnaberg.confluence.admin;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/3/11
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/")
public class ConfigurationResource {

    private final UserManager userManager;
    private final ConfigurationAccessor accessor;

    public ConfigurationResource(UserManager userManager, PluginSettingsFactory pluginSettingsFactory, TransactionTemplate transactionTemplate) {
        this.userManager = userManager;
        accessor = new ConfigurationAccessor(transactionTemplate, pluginSettingsFactory);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request) {
        String username = userManager.getRemoteUsername(request);
        if (username != null && !userManager.isSystemAdmin(username)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.ok(accessor.getConfig()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@Context HttpServletRequest request, Config config) {
        String username = userManager.getRemoteUsername(request);
        if (username != null && !userManager.isSystemAdmin(username)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        accessor.setConfig(config);

        return Response.noContent().build();
    }

}
