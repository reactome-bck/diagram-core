package org.reactome.server.diagram.converter.graph.output;

import java.util.Collection;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SubpathwayNode implements Comparable<SubpathwayNode> {

    public Long dbId;
    public String stId;
    public String displayName;
    public Collection<Long> events; //These are events dbid

    //Level will be used for the shadowing calculation ONLY on the server side
    //That's the reason why it doesn't get serialised to json but is kept here
    public transient int level;

    public SubpathwayNode(GraphNode node, Collection<Long> events, int level) {
        this.dbId = node.dbId;
        this.stId = node.stId;
        this.displayName = node.displayName;
        this.events = events;
        this.level = level;
    }

    @Override
    public int compareTo(SubpathwayNode o) {
        return this.dbId.compareTo(o.dbId);
    }
}
