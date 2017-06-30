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
package org.apache.cayenne.project.upgrade.v6;

import java.io.InputStream;
import java.net.URL;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.resource.Resource;
import org.xml.sax.InputSource;

/**
 * @since 3.1
 */
@Deprecated
class XMLDataMapLoader_V3_0_0_1 {

	public DataMap load(Resource configurationResource) throws CayenneRuntimeException {

		MapLoader mapLoader = new MapLoader();
		URL url = configurationResource.getURL();

		DataMap map;

		try (InputStream in = url.openStream();) {
			map = mapLoader.loadDataMap(new InputSource(in));
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error loading configuration from %s", e, url);
		}

		return map;
	}
}
