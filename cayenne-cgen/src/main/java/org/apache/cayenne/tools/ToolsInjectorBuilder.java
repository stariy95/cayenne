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
package org.apache.cayenne.tools;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.ModuleLoader;

/**
 * @since 4.2
 */
public class ToolsInjectorBuilder {

    private Collection<Module> modules;

    public ToolsInjectorBuilder() {
        this.modules = new ArrayList<>();
    }

    public ToolsInjectorBuilder addModule(Module module) {
        modules.add(module);
        return this;
    }

    public ToolsInjectorBuilder addModules(Collection<Module> modules) {
        this.modules.addAll(modules);
        return this;
    }

    private Collection<? extends Module> autoLoadedModules() {
        return new ModuleLoader().load(CayenneToolsModuleProvider.class, getClass().getClassLoader());
    }

    public Injector create() {
        Collection<Module> allModules = new ArrayList<>(autoLoadedModules());
        allModules.addAll(modules);
        return DIBootstrap.createInjector(allModules);
    }
}
