package org.apache.cayenne.modeler.editor.dbimport.tree;

import java.awt.Color;

public enum Status {
    INCLUDED    (new Color(60,179,113)),
    EXCLUDED    (new Color(178, 0, 0)),
    UNKNOWN     (Color.BLUE), // TODO: BLUE for debug only, change to LIGHT_GRAY
    NONE        (Color.LIGHT_GRAY);

    private final Color color;

    Status(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
