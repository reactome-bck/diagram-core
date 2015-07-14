package org.reactome.server.diagram.converter.layout.output;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Segment {

    public Position from;
    public Position to;

    public Segment(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public double length(){
        Position diff = this.from.minus(this.to);
        return Math.sqrt(diff.x*diff.x + diff.y*diff.y);
    }

    @JsonIgnore
    public boolean isPoint(){
        return this.from.equals(this.to);
    }
}
