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

import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/10/11
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServicesTest extends AbderaResourceAbstractTest {
    @Test
    public void serviceDocumentRendered() throws IOException {
        Services s = new Services();
        UriInfo info = createURIInfo();
        Response response = s.service(info);
        Service service = toAbderaObject(response);
        Workspace workspace = service.getWorkspace("spaces");
        Assert.assertNotNull(workspace);
        Assert.assertEquals(1, workspace.getCollections().size());
        Assert.assertNotNull(workspace.getCollection("spaces"));
    }
}
