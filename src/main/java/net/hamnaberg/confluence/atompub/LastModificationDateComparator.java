package net.hamnaberg.confluence.atompub;

import com.atlassian.core.bean.EntityObject;

import java.util.Comparator;

/**
* Created by IntelliJ IDEA.
* User: maedhros
* Date: 1/25/11
* Time: 7:14 PM
* To change this template use File | Settings | File Templates.
*/
public class LastModificationDateComparator implements Comparator<EntityObject> {
    public int compare(EntityObject o1, EntityObject o2) {
        return o1.getLastModificationDate().compareTo(o2.getLastModificationDate());
    }
}
