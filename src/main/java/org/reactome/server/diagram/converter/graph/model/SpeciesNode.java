package org.reactome.server.diagram.converter.graph.model;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SpeciesNode {
    private Long speciesID;
    private String name;

    protected SpeciesNode(Long speciesID, String name) {
        this.speciesID = speciesID;
        this.name = name;
    }

    public Long getSpeciesID() {
        return speciesID;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpeciesNode that = (SpeciesNode) o;

        if (speciesID != null ? !speciesID.equals(that.speciesID) : that.speciesID != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return speciesID != null ? speciesID.hashCode() : 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
