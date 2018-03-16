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

package org.apache.cayenne.diagrams;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 4.1
 */
public class Node {

    private int x;
    private int y;
    private int width;
    private int height;
    private List<Node> children;
    private Object userObject;

    public Node() {
        children = new ArrayList<>();
    }

    public void addChild(Node node) {
        children.add(node);
    }

    public int getChildrenCount() {
        return children.size();
    }

    public List<Node> getChildren() {
        return children;
    }

    public Node getChild(int idx) {
        return children.get(idx);
    }

    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    @SuppressWarnings("unchecked")
    public <T> T getUserObject() {
        return (T)userObject;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
