package net.hamnaberg.confluence;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Category;
import org.apache.commons.lang.StringUtils;

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

    private ConfluenceUtil(){}

    public static Category createCategory(String name) {
        Category category = Abdera.getInstance().getFactory().newCategory();
        category.setScheme(CONFLUENCE_CATEGORY_SCHEME);
        category.setTerm(name);
        return category;
    }
}
