package org.reactome.server.diagram.converter.layout.output;

import org.reactome.server.diagram.converter.input.model.OrgGkRenderRenderableFeature;
import org.reactome.server.diagram.converter.util.ShapeBuilder;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
@XmlRootElement
public class NodeAttachment {
    private Float relativeX;
    private Float relativeY;
    public Long reactomeId;
    public String label;
    public String description;
    public Shape shape;

    @Deprecated
    public String renderableClass;
    @Deprecated
    public Long trackId;

    public NodeAttachment(OrgGkRenderRenderableFeature obj, Node node){
        this.relativeX = obj.getRelativeX().floatValue();
        this.relativeY = obj.getRelativeY().floatValue();
        this.label = obj.getLabel();
        this.description = obj.getDescription();
        this.trackId = obj.getTrackId().longValue();
        if (obj.getReactomeId() != null) {
            this.reactomeId = obj.getReactomeId().longValue();
        }
        this.renderableClass = obj.getClass().getSimpleName();

        Coordinate boxCentre = new Coordinate(
                Math.round( node.prop.x + node.prop.width * relativeX ),
                Math.round( node.prop.y + node.prop.height * relativeY )
        );
        this.shape = ShapeBuilder.createNodeAttachmentBox(boxCentre, label);
    }
}
