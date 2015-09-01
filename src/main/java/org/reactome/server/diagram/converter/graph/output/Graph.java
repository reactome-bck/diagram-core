package org.reactome.server.diagram.converter.graph.output;

import java.util.Collection;

/**
 * Just a container in order to serialise an object containing a collection rather
 * than directly the collection
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Graph {

    private Long dbId;
    private String stId;

    private Collection<EntityNode> nodes;
    private Collection<EventNode> edges;

    private Collection<SubpathwayNode> subpathways;

    public Graph(Long dbId, String stId, Collection<EntityNode> nodes, Collection<EventNode> edges, Collection<SubpathwayNode> subpathways) {
        this.dbId = dbId;
        this.stId = stId;
        this.nodes = nodes;
        this.edges = edges;
        this.subpathways = subpathways;
    }

    public Long getDbId() {
        return dbId;
    }

    public String getStId() {
        return stId;
    }

    public Collection<EntityNode> getNodes() {
        return nodes;
    }

    public Collection<EventNode> getEdges() {
        return edges;
    }

    public Collection<SubpathwayNode> getSubpathways() {
        return subpathways;
    }
}
