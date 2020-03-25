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

package org.apache.cayenne.remote.hessian;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.util.Util;

import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.SerializerFactory;

/**
 * A utility class that configures Hessian serialization properties using reflection.
 * 
 * @since 1.2
 */
public class HessianConfig {

    /**
     * Creates a Hessian SerializerFactory configured with zero or more
     * AbstractSerializerFactory extensions. Extensions are specified as class names. This
     * method can inject EntityResolver if an extension factory class defines
     * <em>setEntityResolver(EntityResolver)</em> method.
     * 
     * @param factoryNames an array of factory class names. Each class must be a concrete
     *            subclass of <em>com.caucho.hessian.io.AbstractSerializerFactory</em>
     *            and have a default constructor.
     * @param resolver if not null, EntityResolver will be injected into all factories
     *            that implement <em>setEntityResolver(EntityResolver)</em> method.
     * @see #createFactory(String[], EntityResolver, Collection)
     */
    public static SerializerFactory createFactory(
            String[] factoryNames,
            EntityResolver resolver) {
        return createFactory(factoryNames, resolver, Collections.emptyList());
    }

    /**
     * Creates a Hessian SerializerFactory configured with zero or more
     * AbstractSerializerFactory extensions. Extensions are specified as class names. This
     * method can inject EntityResolver if an extension factory class defines
     * <em>setEntityResolver(EntityResolver)</em> method.
     *
     * @param factoryNames an array of factory class names. Each class must be a concrete
     *            subclass of <em>com.caucho.hessian.io.AbstractSerializerFactory</em>
     *            and have a default constructor.
     * @param resolver if not null, EntityResolver will be injected into all factories
     *            that implement <em>setEntityResolver(EntityResolver)</em> method.
     * @param additionalWhitelist serialization whitelist, examples: "java.util.*", "com.foo.io.Bean"
     * @see #createFactory(String[], EntityResolver)
     *
     * @since 4.2
     */
    public static SerializerFactory createFactory(
            String[] factoryNames,
            EntityResolver resolver,
            Collection<String> additionalWhitelist) {

        SerializerFactory factory = new CayenneSerializerFactory();

        List<String> whitelist = new ArrayList<>(additionalWhitelist);
        if(resolver != null) {
            whitelist.add("org.apache.cayenne.*");
            for (DataMap dataMap : resolver.getDataMaps()) {
                whitelist.add(dataMap.getDefaultPackage() + ".*");
            }
        }

        if(!whitelist.isEmpty()) {
            for (String pattern : whitelist) {
                factory.getClassFactory().allow(pattern);
            }
            factory.getClassFactory().setWhitelist(true);
        }

        if (factoryNames != null && factoryNames.length > 0) {
            for (String factoryName : factoryNames) {
                try {
                    factory.addFactory(loadFactory(factoryName, resolver));
                } catch (Exception e) {
                    throw new CayenneRuntimeException("Error configuring factory class "
                            + factoryName, e);
                }
            }
        }

        return factory;
    }

    static AbstractSerializerFactory loadFactory(
            String factoryName,
            EntityResolver resolver) throws Exception {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> factoryClass = Class.forName(factoryName, true, loader);

        if (!AbstractSerializerFactory.class.isAssignableFrom(factoryClass)) {
            throw new IllegalArgumentException(factoryClass
                    + " is not a AbstractSerializerFactory");
        }

        Constructor<?> c = factoryClass.getDeclaredConstructor();
        if (!Util.isAccessible(c)) {
            c.setAccessible(true);
        }

        AbstractSerializerFactory object = (AbstractSerializerFactory) c.newInstance();
        if (resolver != null) {
            try {
                Method setter = factoryClass.getDeclaredMethod(
                        "setEntityResolver",
                        EntityResolver.class);

                if (!Util.isAccessible(setter)) {
                    setter.setAccessible(true);
                }

                setter.invoke(object, resolver);
            } catch (Exception ignore) {
                // ignore injection exception
            }
        }

        return object;
    }

}
