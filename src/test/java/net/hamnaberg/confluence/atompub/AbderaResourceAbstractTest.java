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

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 2/10/11
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbderaResourceAbstractTest {

    protected final <T extends Element> T toAbderaObject(Response response) throws IOException {
        AbderaResponseOutput entity = (AbderaResponseOutput) response.getEntity();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        entity.write(stream);
        Document<T> parsedService = Abdera.getInstance().getParser().parse(new ByteArrayInputStream(stream.toByteArray()));
        return parsedService.getRoot();
    }

    protected UriInfo createURIInfo() {
        UriInfo info = mock(UriInfo.class);
        UriBuilder baseURIBuilder = UriBuilder.fromUri("http://example.com/rest/atompub/latest");
        when(info.getBaseUriBuilder()).thenReturn(baseURIBuilder);
        return info;
    }
    
}
