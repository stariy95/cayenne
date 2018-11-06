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

package org.apache.cayenne.modeler.editor.fasteditor.render.node;

public class NodeState {

    public static final long STATE_NORMAL         = 1 << 0;
    public static final long STATE_SELECTED       = 1 << 1;
    public static final long STATE_MULTI_SELECTED = 1 << 2;
    public static final long STATE_DRAG           = 1 << 3;

    private long state = STATE_NORMAL;

    public boolean haveState(long state) {
        return (this.state & state) > 0;
    }

    public NodeState addState(long state) {
        this.state |= state;
        return this;
    }

    public NodeState removeState(long state) {
        this.state &= this.state ^ state;
        return this;
    }

}
