package org.reactome.server.diagram.converter.layout.output;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class NodeCommon extends DiagramObject {

    public NodeProperties prop = new NodeProperties();
    public Coordinate textPosition;
    public Bound insets;

    public Color bgColor;
    public Color fgColor;

    public Boolean isCrossed = null;

    public Boolean needDashedBorder;

    public NodeCommon(Object obj) {
        super(obj);
        for (Method method : obj.getClass().getMethods()) {
            switch (method.getName()){
                case "getBounds":
                    String bound = getString(method, obj);
                    setProp(extractIntegerListFromString(bound, " "));
                    break;
                case "getTextPosition":
                    String textPosition = getString(method, obj);
                    this.textPosition = extractPositionFromString(textPosition, " ");
                    break;
                case "getInsets":
                    String insets = getString(method, obj);
                    this.insets = extractBoundFromString(insets, " ");
                    break;
                case "getBgColor":
                    String bgColor = getString(method, obj);
                    this.bgColor = extractColorFromString(bgColor, " ");
                    break;
                case "getFgColor":
                    String fgColor = getString(method, obj);
                    this.fgColor = extractColorFromString(fgColor, " ");
                    break;
                case "isNeedDashedBorder":
                    this.needDashedBorder = getBoolean(method, obj);
                    break;
            }
        }
        setBoundaries();
    }

    private void setProp(List<Integer> bounds){
        this.prop.x = bounds.get(0);
        this.prop.y = bounds.get(1);
        this.prop.width = bounds.get(2);
        this.prop.height = bounds.get(3);
    }

    private void setBoundaries(){
        this.minX = this.prop.x;
        this.minY = this.prop.y;

        this.maxX = this.prop.x + this.prop.width;
        this.maxY = this.prop.y + this.prop.height;
    }
}