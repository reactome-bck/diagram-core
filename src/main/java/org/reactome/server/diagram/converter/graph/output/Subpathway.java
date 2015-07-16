package org.reactome.server.diagram.converter.graph.output;

import java.util.Collection;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Subpathway {

    public Long dbId;
    public String stId;
    public String displayName;
    public Collection<Long> events; //These are events dbid

    public Subpathway(GraphNode node, Collection<Long> events) {
        this.dbId = node.dbId;
        this.stId = node.stId;
        this.displayName = node.displayName;
        this.events = events;
    }
}
