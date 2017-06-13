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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

/**
 * @since 4.1
 */
final class DataChannelHandler extends SAXNestedTagHandler {

    private static Logger logger = LoggerFactory.getLogger(XMLDataChannelDescriptorLoader.class);

    static final String DOMAIN_TAG = "domain";

    private XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader;
    DataChannelDescriptor descriptor;

    DataChannelHandler(XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader, DataChannelDescriptor dataChannelDescriptor, XMLReader parser) {
        super(parser);
        this.xmlDataChannelDescriptorLoader = xmlDataChannelDescriptorLoader;
        this.descriptor = dataChannelDescriptor;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String name, Attributes attributes) {

        if (localName.equals(DOMAIN_TAG)) {
            return new DataChannelChildrenHandler(xmlDataChannelDescriptorLoader, this);
        }

        logger.info(unexpectedTagMessage(localName, DOMAIN_TAG));
        return super.createChildTagHandler(namespaceURI, localName, name, attributes);
    }
}
