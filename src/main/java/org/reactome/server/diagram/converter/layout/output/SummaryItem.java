package org.reactome.server.diagram.converter.layout.output;

import org.reactome.server.diagram.converter.util.ShapeBuilder;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@XmlRootElement
public class SummaryItem {
    public enum Type {
        TL(0F, 0F),
        TR(1F, 0F),
        BR(1F, 1F),
        BL(0F, 1F);

        float relativeX;
        float relativeY;

        Type(float relativeX, float relativeY) {
            this.relativeX = relativeX;
            this.relativeY = relativeY;
        }
    }

    public Type type;
    public Shape shape;

    public SummaryItem(Type type, Node node) {
        this.type = type;

        Coordinate boxCentre = new Coordinate(
                Math.round(node.prop.x + node.prop.width * type.relativeX),
                Math.round(node.prop.y + node.prop.height * type.relativeY)
        );
        this.shape = ShapeBuilder.createNodeSummaryItem(boxCentre, "1");
    }
}
