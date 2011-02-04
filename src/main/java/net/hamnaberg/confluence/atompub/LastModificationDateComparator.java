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
