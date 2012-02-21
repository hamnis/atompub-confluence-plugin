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

import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.labels.Namespace;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maedhros
 * Date: 1/22/11
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ConfluenceUtil {
    public static final String PAGE_TERM = "page";
    public static final String NEWS_TERM = "news";
    public static final String SPACE_TERM = "space";
    public static final String COMMENT_TERM = "comment";
    public static final String CONFLUENCE_CATEGORY_SCHEME = "urn:confluence:category";
    public static final String CONFLUENCE_LABEL_SCHEME = "urn:confluence:label";

    private ConfluenceUtil(){}

    public static Category createCategory(String name) {
        Category category = Abdera.getInstance().getFactory().newCategory();
        category.setScheme(CONFLUENCE_CATEGORY_SCHEME);
        category.setTerm(name);
        return category;
    }

    public static Category createCategoryLabel(Label label) {
        Category category = Abdera.getInstance().getFactory().newCategory();
        category.setScheme(CONFLUENCE_LABEL_SCHEME);
        category.setTerm(label.getName());
        category.setLabel(label.getDisplayTitle());
        return category;
    }

    public static Label createLabel(Category cat) {
        return new Label(cat.getTerm(), Namespace.GLOBAL);
    }

    public static void validateCategories(Entry entry, Category expected) {
        List<Category> categories = entry.getCategories(CONFLUENCE_CATEGORY_SCHEME);
        boolean found = false;
        for (Category category : categories) {
            if (category.getScheme().equals(expected.getScheme()) && category.getTerm().equals(expected.getTerm())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException(String.format("No category matching %s found", expected));
        }
    }
}
