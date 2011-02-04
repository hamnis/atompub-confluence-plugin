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

import org.apache.abdera.model.Element;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
* Created by IntelliJ IDEA.
* User: maedhros
* Date: 1/22/11
* Time: 3:09 PM
* To change this template use File | Settings | File Templates.
*/
class AbderaResponseOutput implements StreamingOutput {
    private final Element element;

    public AbderaResponseOutput(Element element) {
        this.element = element;
    }

    public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        element.writeTo(outputStream);
    }
}
