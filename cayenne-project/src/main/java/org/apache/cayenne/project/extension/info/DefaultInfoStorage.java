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

package org.apache.cayenne.project.extension.info;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Info storage that uses Map to store all values and Objects as keys.
 *
 * @since 4.1
 */
public class DefaultInfoStorage implements InfoStorage {

    private Map<Object, ObjectInfo> objectInfoMap = new ConcurrentHashMap<>();

    @Override
    public String putInfo(Object object, String infoType, String info) {
        ObjectInfo infoData = objectInfoMap.get(Objects.requireNonNull(object));
        if(infoData == null) {
            infoData = new ObjectInfo();
            objectInfoMap.put(object, infoData);
        }

        return infoData.put(infoType, info);
    }

    @Override
    public String getInfo(Object object, String infoType) {
        ObjectInfo info = objectInfoMap.get(object);
        if(info == null) {
            return "";
        }
        return info.get(infoType);
    }

    private static class ObjectInfo {
        private Map<String, String> infoByType = new ConcurrentHashMap<>();

        String put(String type, String info) {
            return infoByType.put(type, info);
        }

        String get(String type) {
            String value = infoByType.get(type);
            if(value == null) {
                return "";
            }
            return value;
        }
    }
}
