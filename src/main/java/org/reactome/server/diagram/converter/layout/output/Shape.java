package org.reactome.server.diagram.converter.layout.output;

import org.reactome.core.model.DatabaseObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class Shape {
    public enum Type { ARROW, BOX, CIRCLE, DOUBLE_CIRCLE, STOP }

    public Position a;
    public Position b;
    public Position c;
    public Integer r;
    public Integer r1;
    public String s;        //symbol e.g. ?, \\, 0-9
    public Type type;
    public Boolean empty = null;

    public Shape(Position a, Position b, Position c, Integer r, Boolean empty, Type type) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.r = r;
        if(empty==true){
            this.empty = true;
        }
        this.type = type;
        setBoundaries();
    }

//    @Deprecated
//    public static String getShapeFill(DiagramObject item, boolean isFill){
//        if(isFill) {
//            if (item.isDisease != null && item.isDisease) {
//                return "#FF0000";
//            } else {
//                return "#000000";
//            }
//        }else{
//            return "#FFFFFF";
//        }
//    }

    protected Integer minX; protected Integer maxX;
    protected Integer minY; protected Integer maxY;
    protected void setBoundaries(){
        List<Integer> xx =  new ArrayList<>();
        List<Integer> yy = new ArrayList<>();
        switch (type){
            case CIRCLE:
            case DOUBLE_CIRCLE:
                xx.add(c.x); yy.add(c.y);
                xx.add(c.x+r); yy.add(c.y+r);
                xx.add(c.x-r); yy.add(c.y-r);
                break;
            case ARROW:
                xx.add(c.x); yy.add(c.y);
            default:
                xx.add(a.x); yy.add(a.y);
                xx.add(b.x); yy.add(b.y);
        }

        this.minX = Collections.min(xx);
        this.maxX = Collections.max(xx);
        this.minY = Collections.min(yy);
        this.maxY = Collections.max(yy);
    }
}
