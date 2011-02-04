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
