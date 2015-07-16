package org.reactome.server.diagram.converter.graph.output;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.server.diagram.converter.graph.model.PhysicalEntityNode;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class GraphNode {

    public Long dbId;
    public String stId;
    public String displayName;
    public String schemaClass;

    public GraphNode(PhysicalEntityNode node) {
        this.dbId = node.getDbId();
        this.stId = node.getStId();
        this.displayName = node.getDisplayName();
        this.schemaClass = node.getSchemaClass();
    }

    public GraphNode(GKInstance event) {
        this.dbId = event.getDBID();
        this.setStableIdentifier(event);
        this.displayName = event.getDisplayName();
        this.schemaClass = event.getSchemClass().getName();
    }

    private void setStableIdentifier(GKInstance event){
        try {
            GKInstance stableIdentifier = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            this.stId = (String) stableIdentifier.getAttributeValue(ReactomeJavaConstants.identifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
