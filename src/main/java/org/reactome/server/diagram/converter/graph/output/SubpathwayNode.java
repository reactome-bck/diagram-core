package org.reactome.server.diagram.converter.graph.output;

import java.util.Collection;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SubpathwayNode {

    public Long dbId;
    public String stId;
    public String displayName;
    public Collection<Long> events; //These are events dbid

    public SubpathwayNode(GraphNode node, Collection<Long> events) {
        this.dbId = node.dbId;
        this.stId = node.stId;
        this.displayName = node.displayName;
        this.events = events;
    }
}
