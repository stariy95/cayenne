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

package org.apache.cayenne.project.upgrade;

import org.apache.cayenne.resource.Resource;
import org.w3c.dom.Document;

/**
 * @since 4.1
 */
public class UpgradeUnit {

    private Resource resource;

    private Document document;

    UpgradeUnit(Resource resource, Document document) {
        this.resource = resource;
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

    public Resource getResource() {
        return resource;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
