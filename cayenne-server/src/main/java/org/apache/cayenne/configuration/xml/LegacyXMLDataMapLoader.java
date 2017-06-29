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

import java.io.InputStream;
import java.net.URL;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.resource.Resource;
import org.xml.sax.InputSource;

/**
 * @deprecated since 4.1 kept only for bakward compatibility in some test and upgrade code
 *             use {@link org.apache.cayenne.configuration.xml.XMLDataMapLoader} instead
 */
@Deprecated
public class LegacyXMLDataMapLoader implements DataMapLoader {

    public DataMap load(Resource configurationResource) throws CayenneRuntimeException {

        MapLoader mapLoader = new MapLoader();
        URL url = configurationResource.getURL();

        try (InputStream in = url.openStream()) {
            return mapLoader.loadDataMap(new InputSource(in));
        } catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error loading configuration from %s",
                    e,
                    url);
        }
    }
}
