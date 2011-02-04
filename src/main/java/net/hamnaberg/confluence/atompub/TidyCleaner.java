package net.hamnaberg.confluence.atompub;

import org.w3c.tidy.Tidy;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/22/11
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class TidyCleaner {
    private final Tidy tidy = new Tidy();

    public TidyCleaner() {
        tidy.setQuiet(true);
        tidy.setFixComments(false);
        tidy.setHideComments(true);
        tidy.setXHTML(true);
        tidy.setPrintBodyOnly(false);
        //tidy.setWriteback(false);
        tidy.setOnlyErrors(false);
        tidy.setErrout(new PrintWriter(new StringWriter()));
    }

    String clean(String html) {
        StringWriter writer = new StringWriter();
        tidy.parse(new StringReader(html), writer);
        return writer.toString();
    }
}
