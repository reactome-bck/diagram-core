package org.reactome.server.diagram.converter.layout.output;

import org.reactome.server.diagram.converter.graph.output.SubpathwayNode;
import org.reactome.server.diagram.converter.util.ShadowColours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class Shadow extends DiagramObject {

    public List<Coordinate> points = new ArrayList<>();
    public String colour;

    public Shadow(SubpathwayNode subpathway, List<DiagramObject> participants, Long id) {
        super(subpathway);  //Please note the super constructor won't do anything since SubpathwayNode doesn't have the expected methods

        this.id = id;
        this.colour = ShadowColours.getShadow(id.intValue());
        this.reactomeId = subpathway.dbId;
        this.displayName = subpathway.displayName;
        this.schemaClass = "Pathway";
        this.renderableClass = "Shadow";

        setPoints(participants);
        setBoundaries();
        setPosition();
    }

    private void setPoints(List<DiagramObject> participants) {
        List<Integer> xx = new ArrayList<>();
        List<Integer> yy = new ArrayList<>();
        for (DiagramObject participant : participants) {
            xx.add(participant.minX);
            xx.add(participant.maxX);
            yy.add(participant.minY);
            yy.add(participant.maxY);
        }

        try {
            Integer minX = Collections.min(xx);
            Integer maxX = Collections.max(xx);
            Integer minY = Collections.min(yy);
            Integer maxY = Collections.max(yy);

            this.points.add(new Coordinate(minX, minY));
            this.points.add(new Coordinate(maxX, minY));
            this.points.add(new Coordinate(maxX, maxY));
            this.points.add(new Coordinate(minX, maxY));
        }catch (NoSuchElementException e){
            e.printStackTrace();
        }
    }

    private void setPosition() {
        position = new Coordinate((minX + maxX) / 2, (minY + maxY) / 2);
    }

    private void setBoundaries() {
        List<Integer> xx = new ArrayList<>();
        List<Integer> yy = new ArrayList<>();

        for (Coordinate point : points) {
            xx.add(point.x);
            yy.add(point.y);
        }

        this.minX = Collections.min(xx);
        this.maxX = Collections.max(xx);
        this.minY = Collections.min(yy);
        this.maxY = Collections.max(yy);
    }
}
