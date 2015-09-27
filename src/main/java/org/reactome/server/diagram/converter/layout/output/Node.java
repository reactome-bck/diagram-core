package org.reactome.server.diagram.converter.layout.output;

import org.reactome.server.diagram.converter.input.model.Component;
import org.reactome.server.diagram.converter.input.model.Components;
import org.reactome.server.diagram.converter.input.model.NodeAttachments;
import org.reactome.server.diagram.converter.input.model.OrgGkRenderRenderableFeature;
import org.reactome.server.diagram.converter.util.ShapeBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@XmlRootElement
public class Node extends NodeCommon {

    public Boolean hideComponents;

    public List<NodeAttachment> nodeAttachments;

    public List<SummaryItem> summaryItems;

    public List<Long> componentIds;

    public List<Connector> connectors;

    public Boolean trivial = null;

    // This is used only in case of the Genes, to hold the arrow shape
    public Shape endShape;

    public Node(Object obj) {
        super(obj);
        for (Method method : obj.getClass().getMethods()) {
            switch (method.getName()) {
                case "isHideComponents":
                    this.hideComponents = getBoolean(method, obj);
                    break;
                case "getNodeAttachments":
                    this.nodeAttachments = getNodeAttachments(method, obj);
                    break;
                case "getComponents":
                    this.componentIds = getComponents(method, obj);
                    break;
            }
        }
        connectors = new LinkedList<>();

        // Calculate the arrow shape of the Gene
        this.setEndShape();

        // Get rid of position as it simply points to the center of the node
        //position = null; //TODO Enable it
    }

    private List<NodeAttachment> getNodeAttachments(Method method, Object object) {
        List<NodeAttachment> rtn = new LinkedList<>();
        try {
            NodeAttachments nodeAttachments = (NodeAttachments) method.invoke(object);
            if (nodeAttachments != null) {
                for (OrgGkRenderRenderableFeature item : nodeAttachments.getOrgGkRenderRenderableFeature()) {
                    if (item != null) {
                        rtn.add(new NodeAttachment(item, this));
                    }
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return rtn.isEmpty() ? null : rtn;
    }

    private static List<Long> getComponents(Method method, Object object) {
        List<Long> rtn = new LinkedList<>();
        try {
            Components components = (Components) method.invoke(object);
            if (components != null && components.getComponent() != null) {
                for (Object c : components.getComponent()) {
                    Component component = (Component) c;
                    rtn.add(component.getId().longValue());
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return rtn.isEmpty() ? null : rtn;
    }

    public void setBoundaries() {
        List<Integer> xx = new ArrayList<>();
        xx.add(this.prop.x);
        xx.add(this.prop.x + this.prop.width);

        List<Integer> yy = new ArrayList<>();
        yy.add(this.prop.y);
        yy.add(this.prop.y + this.prop.height);

        // Take into account the connectors
        for (Connector connector : this.connectors) {
            for (Segment segment : connector.segments) {
                xx.add(segment.from.x);
                xx.add(segment.to.x);
                yy.add(segment.from.y);
                yy.add(segment.to.y);
            }
            if (connector.endShape != null) {
                xx.add(connector.endShape.minX);
                xx.add(connector.endShape.maxX);
                yy.add(connector.endShape.minY);
                yy.add(connector.endShape.maxY);
            }
            // Also take into account the Stoichiometry boxes
            if (connector.stoichiometry != null && connector.stoichiometry.shape != null) {
                xx.add(connector.stoichiometry.shape.minX);
                xx.add(connector.stoichiometry.shape.maxX);
                yy.add(connector.stoichiometry.shape.minY);
                yy.add(connector.stoichiometry.shape.maxY);
            }
        }

        // Also take into account the NodeAttachment boxes
        if (nodeAttachments != null) {
            for (NodeAttachment nodeAttachment : nodeAttachments) {
                xx.add(nodeAttachment.shape.a.x);
                yy.add(nodeAttachment.shape.a.y);
                xx.add(nodeAttachment.shape.b.x);
                yy.add(nodeAttachment.shape.b.y);
            }
        }
        if (summaryItems != null) {
            for (SummaryItem summaryItem : summaryItems) {
                xx.add(summaryItem.shape.minY);
                yy.add(summaryItem.shape.minY);
                xx.add(summaryItem.shape.maxX);
                yy.add(summaryItem.shape.maxY);
            }
        }
        // In case of a gene also include the arrow
        if (endShape != null) {
            xx.add(endShape.minX);
            xx.add(endShape.maxX);
            yy.add(endShape.minY);
            yy.add(endShape.maxY);
        }

        this.minX = Collections.min(xx);
        this.maxX = Collections.max(xx);
        this.minY = Collections.min(yy);
        this.maxY = Collections.max(yy);
    }

    private void setEndShape() {
        if (this.renderableClass.equals("Gene")) {
            // Calculate the arrow shape of the Gene
            List<Coordinate> points = ShapeBuilder.createArrow(
                    this.prop.x + this.prop.width,
                    this.prop.y + 2,
                    this.prop.x + this.prop.width + ShapeBuilder.ARROW_LENGTH,
                    this.prop.y + 2);
            // Shape is a filled arrow by default
            this.endShape = new Shape(points.get(0), points.get(1), points.get(2), Boolean.FALSE, Shape.Type.ARROW);
        }
    }

    public void setSummaryItems() {
        if (this.renderableClass.equals("Protein")) {// || this.renderableClass.equals("Chemical")) {
            this.summaryItems = new ArrayList<>();
            this.summaryItems.add(new SummaryItem(SummaryItem.Type.TR, this));
        }
    }
}
