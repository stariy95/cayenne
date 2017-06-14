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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;

/**
 * @since 3.1
 * @since 4.1 moved to org.apache.cayenne.configuration.xml package
 */
public class XMLDataMapLoader implements DataMapLoader {

    @Inject
    protected HandlerFactory handlerFactory;

    public DataMap load(Resource configurationResource) throws CayenneRuntimeException {

        final RootDataMapHandler rootHandler;

        try(InputStream in = configurationResource.getURL().openStream()) {
            XMLReader parser = Util.createXmlReader();
            rootHandler = new RootDataMapHandler(parser, handlerFactory);

            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            parser.parse(new InputSource(in));
        } catch (Exception e) {
            throw new CayenneRuntimeException("Error loading configuration from %s", e, configurationResource.getURL());
        }

        return rootHandler.getDataMap();
    }
}
