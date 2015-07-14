package org.reactome.server.diagram.converter.graph.output;

import org.reactome.server.diagram.converter.graph.model.PhysicalEntityNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Projecting PhysicalEntityNodes to GraphNodes
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class EntityNode {

    public Long dbId;
    public String stId;
    public String displayName;
    public String schemaClass;
    public Long speciesID;

    public List<Long> diagramIds;

    //Next variable will NOT contain value for Complexes and EntitySets because they
    //do not have main resources (members or components are treated separately).
    public String identifier = null;

    public Set<Long> parents = null;
    public Set<Long> children = null;

    public EntityNode(PhysicalEntityNode node) {
        this.dbId = node.getDbId();
        this.stId = node.getStId();
        this.displayName = node.getDisplayName();
        this.schemaClass = node.getSchemaClass();
        this.identifier = node.getIdentifier();

        if(node.getSpecies()!=null) {
            this.speciesID = node.getSpecies().getSpeciesID();
        }
        this.setDiagramIds(node.getDiagramIds());
        this.setChildren(node.getChildren());
        this.setParents(node.getParents());
    }

    private void setChildren(Set<PhysicalEntityNode> children){
        if(!children.isEmpty()) this.children = new HashSet<>();
        for (PhysicalEntityNode child : children) {
            this.children.add(child.getDbId());
        }
    }

    private void setDiagramIds(List<Long> diagramIds){
        this.diagramIds = !diagramIds.isEmpty() ? diagramIds : null;
    }

    private void setParents(Set<PhysicalEntityNode> parents){
        if(!parents.isEmpty()) this.parents = new HashSet<>();
        for (PhysicalEntityNode parent : parents) {
            this.parents.add(parent.getDbId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityNode entityNode = (EntityNode) o;

        return !(dbId != null ? !dbId.equals(entityNode.dbId) : entityNode.dbId != null);
    }

    @Override
    public int hashCode() {
        return dbId != null ? dbId.hashCode() : 0;
    }
}
