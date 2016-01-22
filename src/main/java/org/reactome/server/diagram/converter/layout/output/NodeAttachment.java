package org.reactome.server.diagram.converter.layout.output;

import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.core.model.DatabaseObject;
import org.reactome.server.diagram.converter.input.model.OrgGkRenderRenderableFeature;
import org.reactome.server.diagram.converter.util.ShapeBuilder;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@XmlRootElement
public class NodeAttachment {
    public Long reactomeId;
    public String label;
    public String description;
    public Shape shape;

    public NodeAttachment(OrgGkRenderRenderableFeature obj, Node node) {
        this.label = obj.getLabel();
        this.description = obj.getDescription();

        if (obj.getReactomeId() != null) {
            this.reactomeId = obj.getReactomeId().longValue();
            DatabaseObject object = DatabaseObjectFactory.getDatabaseObject(reactomeId);
            if(object!=null) {
                this.description = object.getDisplayName();
            }
        }

        Float relativeX = obj.getRelativeX().floatValue();
        Float relativeY = obj.getRelativeY().floatValue();
        Coordinate boxCentre = new Coordinate(
                Math.round(node.prop.x + node.prop.width * relativeX),
                Math.round(node.prop.y + node.prop.height * relativeY)
        );
        this.shape = ShapeBuilder.createNodeAttachmentBox(boxCentre, label);
    }
}
