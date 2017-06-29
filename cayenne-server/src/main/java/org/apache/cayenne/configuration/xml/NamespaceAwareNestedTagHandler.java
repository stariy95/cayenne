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

import java.util.Objects;

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

    protected String targetNamespace;

    private StringBuilder charactersBuffer = new StringBuilder();

    public NamespaceAwareNestedTagHandler(LoaderContext loaderContext) {
        super(loaderContext);
    }

    public NamespaceAwareNestedTagHandler(SAXNestedTagHandler parentHandler, String targetNamespace) {
        super(parentHandler);
        this.targetNamespace = Objects.requireNonNull(targetNamespace);
    }

    public NamespaceAwareNestedTagHandler(NamespaceAwareNestedTagHandler parentHandler) {
        this(parentHandler, parentHandler.targetNamespace);
    }

    abstract protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException;

    protected void processCharData(String localName, String data) {
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        charactersBuffer.append(ch, start, length);
    }

    @Override
    public final void startElement(String namespaceURI, String localName,
                                   String qName, Attributes attributes) throws SAXException {

        // push child handler to the stack...
        ContentHandler childHandler = createChildTagHandler(namespaceURI, localName, qName, attributes);

        if(!namespaceURI.equals(targetNamespace) || !processElement(namespaceURI, localName, attributes)) {
            // recursively pass element down into child handlers
            childHandler.startElement(namespaceURI, localName, qName, attributes);
        }

        loaderContext.getXmlReader().setContentHandler(childHandler);
        charactersBuffer.delete(0, charactersBuffer.length());
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        super.endElement(namespaceURI, localName, qName);
        if(namespaceURI.equals(targetNamespace) && parentHandler instanceof NamespaceAwareNestedTagHandler) {
            ((NamespaceAwareNestedTagHandler)parentHandler).processCharData(localName, charactersBuffer.toString());
        }
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {
        // try to pass unknown tags to someone else
        return loaderContext.getFactory().createHandler(namespaceURI, localName, this);
    }

    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }
}
