package org.reactome.server.diagram.converter.layout.output;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Position {

    public Integer x;
    public Integer y;

    public Position(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Position(List<Integer> points) {
        this.x = points.get(0);
        this.y = points.get(1);
    }

    public Position add(Position value){
        return new Position(x+value.x, y+value.y);
    }

    public Position minus(Position value){
        return new Position(x-value.x, y-value.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (x != null ? !x.equals(position.x) : position.x != null) return false;
        return !(y != null ? !y.equals(position.y) : position.y != null);

    }

    @Override
    public int hashCode() {
        int result = x != null ? x.hashCode() : 0;
        result = 31 * result + (y != null ? y.hashCode() : 0);
        return result;
    }
}
