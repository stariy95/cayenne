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

package org.apache.cayenne.modeler.editor.fasteditor;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.fasteditor.SyncModelAction;

/**
 * @since 4.2
 */
public class FastEditorView extends JPanel {

    private final ProjectController mediator;

    private final JFXPanel jfxPanel = new JFXPanel();
    private final JToolBar toolBar = new JToolBar();

    public FastEditorView(ProjectController mediator) {
        this.mediator = mediator;

        initView();
    }

    private void initView() {
        this.setLayout(new BorderLayout());
        initJfxPanel();
        initToolBar();
    }

    private void initJfxPanel() {
        Platform.runLater(() -> {
            jfxPanel.setScene(new FastEditorScene());
        });

        add(new JScrollPane(jfxPanel), BorderLayout.CENTER);
    }


    private void initToolBar() {
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        ActionManager actionManager = Application.getInstance().getActionManager();

        toolBar.add(actionManager.getAction(CreateObjEntityAction.class).buildButton(1));
        toolBar.add(actionManager.getAction(CreateRelationshipAction.class).buildButton(3));
        toolBar.addSeparator();

        toolBar.add(actionManager.getAction(SyncModelAction.class).buildButton());

        add(toolBar, BorderLayout.NORTH);
    }
}
