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
package org.apache.cayenne.configuration.rop.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.configuration.server.CayenneServerModuleProvider;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.web.WebModule;
import org.apache.cayenne.di.Module;

/**
 * @since 4.2
 */
public class ROPServerModuleProvider implements CayenneServerModuleProvider {

    @Override
    public Module module() {
        return new ROPServerModule();
    }

    @Override
    public Class<? extends Module> moduleType() {
        return ROPServerModule.class;
    }

    @Override
    public Collection<Class<? extends Module>> overrides() {
        List<Class<? extends Module>> modules = new ArrayList<>();
        modules.add(ServerModule.class);
        modules.add(WebModule.class);
        return modules;
    }
}
