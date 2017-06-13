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

package org.apache.cayenne.configuration.xml;

import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Base class for handlers that can delegate execution of unknown tags to
 * handlers produced by factory.
 *
 * @since 4.1
 */
abstract public class NamespaceAwareNestedTagHandler extends SAXNestedTagHandler {

    String targetNamespace;

    HandlerFactory factory;

    public NamespaceAwareNestedTagHandler(SAXNestedTagHandler parentHandler, String targetNamespace, HandlerFactory factory) {
        super(parentHandler);
        this.factory = factory;
        this.targetNamespace = targetNamespace;
    }

    public NamespaceAwareNestedTagHandler(NamespaceAwareNestedTagHandler parentHandler, String targetNamespace) {
        this(parentHandler, targetNamespace, parentHandler.factory);
    }

    public NamespaceAwareNestedTagHandler(NamespaceAwareNestedTagHandler parentHandler) {
        this(parentHandler, parentHandler.targetNamespace, parentHandler.factory);
    }

    abstract protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException;

    @Override
    public final void startElement(String namespaceURI, String localName,
                                   String qName, Attributes attributes) throws SAXException {

        // push child handler to the stack...
        ContentHandler childHandler = createChildTagHandler(namespaceURI, localName, qName, attributes);

        if(!namespaceURI.equals(targetNamespace) || !processElement(namespaceURI, localName, attributes)) {
            // recursively pass element down into child handlers
            childHandler.startElement(namespaceURI, localName, qName, attributes);
        }

        parser.setContentHandler(childHandler);
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {
        // try to pass unknown tags to someone else
        return factory.createHandler(namespaceURI, localName, this);
    }

}
