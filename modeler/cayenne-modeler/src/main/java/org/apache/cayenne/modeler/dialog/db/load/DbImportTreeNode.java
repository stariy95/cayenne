/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.db.load;

import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

import javax.swing.tree.DefaultMutableTreeNode;


/**
 * @since 4.1
 */
public class DbImportTreeNode extends DefaultMutableTreeNode {

    DbImportTreeNode() {
        this(null);
    }

    private DbImportTreeNode(Object userObject, boolean allowsChildren) {
        super();
        this.userObject = userObject;
        this.allowsChildren = allowsChildren;
        parent = null;
    }

    DbImportTreeNode(Object userObject) {
        this(userObject, true);
    }

    private String getFormattedName(String className, String nodeName) {
        if (nodeName == null) {
            return className;
        } else {
            return String.format("%s: %s", className, nodeName);
        }
    }

    private String getNodeName() {
        if (userObject instanceof FilterContainer) {
            return getFormattedName(userObject.getClass().getSimpleName(), ((FilterContainer) userObject).getName());
        } else if (userObject instanceof PatternParam) {
            return getFormattedName(userObject.getClass().getSimpleName(), ((PatternParam) userObject).getPattern());
        }
        return "";
    }

    String getSimpleNodeName() {
        if (userObject instanceof FilterContainer) {
            return ((FilterContainer) userObject).getName();
        } else if (userObject instanceof PatternParam) {
            return ((PatternParam) userObject).getPattern();
        }
        return "";
    }

    public String toString() {
        if (userObject == null) {
            return "";
        } else if (userObject instanceof ReverseEngineering) {
            return "Reverse Engineering Configuration:";
        } else {
            return getNodeName();
        }
    }
}
