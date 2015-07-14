package org.reactome.server.diagram.converter.graph.model;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class PhysicalEntityNode {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PhysicalEntityNode.class.getName());

    private Long dbId;
    private String stId;
    private String displayName;
    private String schemaClass;
    private SpeciesNode species;

    private List<Long> diagramIds;

    private Set<PhysicalEntityNode> parents = new LinkedHashSet<PhysicalEntityNode>();
    private Set<PhysicalEntityNode> children = new LinkedHashSet<PhysicalEntityNode>();

    //Next variable will NOT contain value for Complexes and EntitySets because they
    //do not have main resources (members or components are treated separately).
    private String identifier = null;

    public PhysicalEntityNode(GKInstance physicalEntity) {
        this.dbId = physicalEntity.getDBID();
        this.displayName = physicalEntity.getDisplayName();
        this.schemaClass = physicalEntity.getSchemClass().getName();
        this.species = SpeciesNodeFactory.getSpeciesNode(physicalEntity);
        this.setStableIdentifier(physicalEntity);
        this.diagramIds = new LinkedList<>();
        this.setResourceIdentifier(physicalEntity);
    }

    public void addChild(PhysicalEntityNode child){
        this.children.add(child);
        //During the building process we need to keep the graph well formed
        //NOTE: PARENTS need to be removed before the data structure is serialised
        child.addParent(this);
    }

    protected void addParent(PhysicalEntityNode parent){
        this.parents.add(parent);
    }

    public Set<PhysicalEntityNode> getAllNodes(){
        Set<PhysicalEntityNode> rtn = new HashSet<PhysicalEntityNode>();
        rtn.add(this);
        if(this.children!=null){
            for (PhysicalEntityNode child : this.children) {
                rtn.addAll(child.getAllNodes());
            }
        }
        return rtn;
    }

    public Set<PhysicalEntityNode> getChildren() {
        if(children==null){
            return new HashSet<PhysicalEntityNode>();
        }
        return children;
    }

    public Set<PhysicalEntityNode> getParents() {
        if(parents==null){
            return new HashSet<PhysicalEntityNode>();
        }
        return parents;
    }

    public Long getDbId() {
        return dbId;
    }

    public String getStId() {
        return stId;
    }

    public List<Long> getDiagramIds() {
        return diagramIds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSchemaClass() {
        return schemaClass;
    }

    public SpeciesNode getSpecies() {
        return species;
    }

    public void addDiagramId(Long diagramId) {
        this.diagramIds.add(diagramId);
    }

    protected void removeLinkToParent(){
        this.parents = null;
        if(this.children!=null){
            for (PhysicalEntityNode child : this.children) {
                child.removeLinkToParent();
            }
        }
    }

    protected void setStableIdentifier(GKInstance physicalEntity){
        try {
            GKInstance stableIdentifier = (GKInstance) physicalEntity.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            this.stId = (String) stableIdentifier.getAttributeValue(ReactomeJavaConstants.identifier);
        } catch (Exception e) {
            // Important
            // This is because the stable identifiers are not working properly
            logger.error("Error setting stable Identifier for " + this.toString() + e.getStackTrace());
        }
    }

    protected void setResourceIdentifier(GKInstance physicalEntity){
        try {
            GKInstance rE = (GKInstance) physicalEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if(rE.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
                this.identifier = (String) rE.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
            }
            if(this.identifier==null && rE.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier)){
                this.identifier = (String) rE.getAttributeValue(ReactomeJavaConstants.identifier);
            }
        } catch (Exception e) {
            //Nothing here
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhysicalEntityNode that = (PhysicalEntityNode) o;

        //noinspection RedundantIfStatement
        if (!dbId.equals(that.dbId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if(dbId !=null)
            return dbId.hashCode();
        else
            return 0;
    }

    @Override
    public String toString() {
        return "PhysicalEntityNode{" +
                "dbId=" + dbId +
                ", displayName='" + displayName + '\'' +
                ", schemaClass='" + schemaClass + '\'' +
                ", species=" + species +
                '}';
    }
}
