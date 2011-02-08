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
        tidy.setPrintBodyOnly(true);
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
