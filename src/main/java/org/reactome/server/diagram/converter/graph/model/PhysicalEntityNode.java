package org.reactome.server.diagram.converter.graph.model;

import org.apache.log4j.Level;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.server.diagram.converter.util.report.LogUtil;

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

    private Set<PhysicalEntityNode> parents = new LinkedHashSet<>();
    private Set<PhysicalEntityNode> children = new LinkedHashSet<>();

    //Next variable will NOT contain value for Complexes and EntitySets because they
    //do not have main resources (members or components are treated separately).
    private String identifier = null;
    private List<String> geneNames = new LinkedList<>();

    public PhysicalEntityNode(GKInstance physicalEntity) {
        this.dbId = physicalEntity.getDBID();
        this.displayName = physicalEntity.getDisplayName();
        this.schemaClass = physicalEntity.getSchemClass().getName();
        this.species = SpeciesNodeFactory.getSpeciesNode(physicalEntity);
        this.setStableIdentifier(physicalEntity);
        this.diagramIds = new LinkedList<>();
        this.setResourceIdentifiers(physicalEntity);
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
        Set<PhysicalEntityNode> rtn = new HashSet<>();
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
            return new HashSet<>();
        }
        return children;
    }

    public Set<PhysicalEntityNode> getParents() {
        if(parents==null){
            return new HashSet<>();
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

    public List<String> getGeneNames() {
        return geneNames;
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
            this.stId = "" + physicalEntity.getDBID();
            // Important
            // This is because the stable identifiers are not working properly
            LogUtil.log(logger, Level.ERROR, "No stable identifier found for PhysicalEntity " + physicalEntity.getDBID());
        }
    }

    protected void setResourceIdentifiers(GKInstance physicalEntity){
        try {
            GKInstance rE = (GKInstance) physicalEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if(rE.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
                this.identifier = (String) rE.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
            }
            if(this.identifier==null && rE.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier)){
                this.identifier = (String) rE.getAttributeValue(ReactomeJavaConstants.identifier);
            }
            if(rE.getSchemClass().isValidAttribute(ReactomeJavaConstants.geneName)){
                List names = rE.getAttributeValuesList(ReactomeJavaConstants.geneName);
                if(names!=null) {
                    for (Object name : names) {
                        this.geneNames.add((String) name);
                    }
                }
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
