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
package org.apache.cayenne.project.upgrade.v10;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.BaseUpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.v9.ProjectUpgrader_V9;
import org.apache.cayenne.resource.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UpgradeHandler_V10 extends BaseUpgradeHandler {

    static final String PREVIOUS_VERSION = "9";
    static final String TO_VERSION = "10";

    @Inject
    protected Injector injector;

    @Inject
    private ProjectSaver projectSaver;

    public UpgradeHandler_V10(Resource source) {
        super(source);
    }

    @Override
    protected Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException {
        if (compareVersions(metaData.getProjectVersion(), PREVIOUS_VERSION) == -1) {
            ProjectUpgrader_V9 upgraderV9 = new ProjectUpgrader_V9();
            injector.injectMembers(upgraderV9);
            UpgradeHandler handlerV9 = upgraderV9.getUpgradeHandler(projectSource);
            projectSource = handlerV9.performUpgrade();
        }

        Document document = readDOMDocument(projectSource);
        processProjectFile(document);
        processAdditionalDatamapFiles(document);

        return projectSource;
    }

    private void processProjectFile(Document document) {
        // Obtain the root element
        Element domain = document.getDocumentElement();
        domain.setAttribute("project-version", TO_VERSION);
        try {
            saveDocument(document, projectSource);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void processDataMap(Document document) {
        try {
            Element dataMap = document.getDocumentElement();
            dataMap.setAttribute("xmlns","http://cayenne.apache.org/schema/10/modelMap");
            dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/10/modelMap http://cayenne.apache.org/schema/10/modelMap.xsd");
            dataMap.setAttribute("project-version", TO_VERSION);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private void processAdditionalDatamapFiles(Document document) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/domain/map/@name", document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node mapNode = nodes.item(i);
                Resource mapResource = projectSource.getRelativeResource(mapNode.getNodeValue() + ".map.xml");
                Document datamapDoc = readDOMDocument(mapResource);
                processDataMap(datamapDoc);
                saveDocument(datamapDoc, mapResource);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected String getToVersion() {
        return TO_VERSION;
    }

}