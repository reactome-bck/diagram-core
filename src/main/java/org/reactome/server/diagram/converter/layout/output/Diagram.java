package org.reactome.server.diagram.converter.layout.output;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.core.model.CatalystActivity;
import org.reactome.core.model.EntityFunctionalStatus;
import org.reactome.core.model.PhysicalEntity;
import org.reactome.core.model.ReactionlikeEvent;
import org.reactome.server.diagram.converter.util.ShapeBuilder;

import java.util.*;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class Diagram {
    //DO NOT GET SERIALISED
    @Deprecated public transient Long reactomeDiagramId;
    @Deprecated public transient String pathways;
    @Deprecated public transient Long nextId;
    @Deprecated public transient Boolean hideCompartmentInName;
    @Deprecated public transient Boolean isChanged;
    public transient Boolean isDisease;
    public transient Set<Integer> normalComponents = new HashSet<>();
    public transient Set<Integer> crossedComponents = new HashSet<>();
    public transient Set<Integer> notFadeOut = new HashSet<>();

    public Long dbId;
    public String stableId;
    public String displayName;
    public Boolean forNormalDraw = Boolean.TRUE;

    public Set<Integer> diseaseComponents;
    public Set<Integer> lofNodes;

    public List<Node> nodes = new LinkedList<>();
    public List<Note> notes = new LinkedList<>();
    public List<Edge> edges = new LinkedList<>();
    public List<Link> links = new LinkedList<>();
    public List<Compartment> compartments = new LinkedList<>();

    public Integer minX; public Integer maxX;
    public Integer minY; public Integer maxY;


    /**
     * Deletes links that do not have inputs or outputs OR
     * those that their inputs or outputs have been deleted
     * because they were not in the normalComponents (Disease Pathways)
     */
    public void cleanUpOrphanLinks(){
        List<Link> linksList = new LinkedList<>();
        for (Link link : links) {
            //Only keep those links going from an entity to another :)
            if(     link.inputs!=null && !link.inputs.isEmpty() &&
                    link.outputs!=null && !link.outputs.isEmpty()) {
                linksList.add(link);
            }
        }
        this.links = linksList;
    }

    /**
     * Calculates the diagram's universal boundaries and embeds it into the json
     * in order to avoid any client-side processing. Nodes, compartments and edges
     * are all calculated
     */
    public void setUniversalBoundaries(){
        //Iterate over all Nodes and edges and find the minX-maxX, minY-maxY
        List<Integer> xx = new LinkedList<>();
        List<Integer> yy = new LinkedList<>();
        for(Node node : nodes) {
            // take x and x + width
            xx.add(node.minX); xx.add(node.maxX);
            yy.add(node.minY); yy.add(node.maxY);
        }
        for(Edge edge : edges){
            xx.add(edge.minX); xx.add(edge.maxX);
            yy.add(edge.minY); yy.add(edge.maxY);
        }
        for(Compartment compartment : compartments) {
            // take x and x + width
            xx.add(compartment.minX); xx.add(compartment.maxX);
            yy.add(compartment.minY); yy.add(compartment.maxY);
        }
        this.minX = Collections.min(xx);
        this.maxX = Collections.max(xx);
        this.minY = Collections.min(yy);
        this.maxY = Collections.max(yy);

        // Detect negative coordinates and print a warning message
        if(this.minX < 0 || this.minY < 0){
            System.err.println(" >> WARNING: DiagramID: " + this.dbId + " Negative boundaries detected ... MinX: " + this.minX + "MinY: " + this.minY);
        }
    }

    /**
     * Calculates the points of the arrows only for those reactions
     * that do not have output connectors. In this case the arrow should
     * be positioned on the last segment of the backbone.
     */
    public void setBackboneArrows(){
        if(edges == null) return;

        for (Edge edge : edges) {

            if(edge.outputs == null ) continue;

            for (ReactionPart output : edge.outputs) {
                if(output.points == null || output.points.size()==0){
                    // Use the last segment of the backbone
                    // IMPORTANT!!! Segments here start from the centre of the backbone and point to the output node
                    Segment segment = edge.segments.get(edge.segments.size() -1);
                    List<Position> points = ShapeBuilder.createArrow(
                            segment.to.x,
                            segment.to.y,
                            segment.from.x,
                            segment.from.y);
                    // Shape is a filled arrow
                    edge.endShape = new Shape(points.get(0), points.get(1), points.get(2), null, Boolean.FALSE, Shape.Type.ARROW);
                }
            }
        }
    }

    public void setLinkArrows(){
        if(links == null) return;
        for (Link link : links) {
            if(link.renderableClass.equals("FlowLine")){
                if(link.outputs == null ) return;
                // Use the last segment of the backbone
                // IMPORTANT!!! Segments here start from the centre of the backbone and point to the output node
                Segment segment = link.segments.get(link.segments.size() -1);
                List<Position> points = ShapeBuilder.createArrow(
                        segment.to.x,
                        segment.to.y,
                        segment.from.x,
                        segment.from.y);
                // Shape is a filled arrow
                link.endShape = new Shape(points.get(0), points.get(1), points.get(2), null, Boolean.FALSE, Shape.Type.ARROW);
            }else if(link.renderableClass.equals("Interaction")){
                if(link.outputs == null ) return;
                // Use the last segment of the backbone
                // IMPORTANT!!! Segments here start from the centre of the backbone and point to the output node
                Segment segment = link.segments.get(link.segments.size() -1);
                List<Position> points = ShapeBuilder.createArrow(
                        segment.to.x,
                        segment.to.y,
                        segment.from.x,
                        segment.from.y);
                // Shape is an empty arrow
                link.endShape = new Shape(points.get(0), points.get(1), points.get(2), null, Boolean.TRUE, Shape.Type.ARROW);
            }
        }
    }

    /**
     * Iterates over all Edges, creates the connectors (with their segments)
     * and attaches them to their respective node
     */
    public void setConnectors(){
        // proceed only if nodes and edges are not null
        if(nodes==null || edges==null) { return;}

        // generate a map of all nodes
        HashMap<Long, Node> nodesMap = new HashMap<>();
        for (NodeCommon node : nodes) {
            nodesMap.put(node.id, (Node) node);
        }

        // iterate over all Edges and ReactionParts and then create the connectors
        for (Edge edge : edges) {
            createAndAddConnector(nodesMap, edge.inputs, edge, Connector.Type.INPUT);
            createAndAddConnector(nodesMap, edge.outputs, edge, Connector.Type.OUTPUT);
            createAndAddConnector(nodesMap, edge.catalysts, edge, Connector.Type.CATALYST);
            createAndAddConnector(nodesMap, edge.activators, edge, Connector.Type.ACTIVATOR);
            createAndAddConnector(nodesMap, edge.inhibitors, edge, Connector.Type.INHIBITOR);
        }
    }

    public void setNodesBoundaries(){
        //Once the nodes connectors setup is finished, the boundaries need to be set
        for (Node node : nodes) {
            node.setBoundaries();
        }
    }

    /**
     * Processes all nodes in the crossedComponents list
     */
    public void setCrossedComponents(){
        if(crossedComponents!=null && !crossedComponents.isEmpty()){
            if(nodes==null || nodes.isEmpty()){
                throw new RuntimeException("The nodes have not been initialised yet");
            }
            for (Node node : nodes) {
                if (crossedComponents.contains(node.id.intValue())) {
                    node.isCrossed = Boolean.TRUE;
                }
            }
        }
    }

    /**
     * Processes all diagram objects in the diseaseComponents list
     */
    public void setDiseaseComponents(){
        if(diseaseComponents!=null && !diseaseComponents.isEmpty()){
            if(nodes==null || nodes.isEmpty()){
                throw new RuntimeException("The nodes have not been initialised yet");
            }else if(edges==null || edges.isEmpty()){
                throw new RuntimeException("The edges have not been initialised yet");
            }

            for (Node node : nodes) {
                if (diseaseComponents.contains(node.id.intValue())) {
                    node.isFadeOut = null;
                }
                if(node.connectors!=null){
                    for (Connector connector : node.connectors) {
                        if(diseaseComponents.contains(connector.edgeId.intValue())){
                            connector.isFadeOut = null;
                        }
                    }
                }
            }

            for(Edge edge : edges) {
                if (diseaseComponents.contains(edge.id.intValue())) {
                    edge.isFadeOut = null;
                }
            }
        }
    }

    public void setOverlaidObjects(Set<Integer> shown){
        if(shown!=null && !shown.isEmpty()) {
            if(nodes==null || nodes.isEmpty()){
                throw new RuntimeException("The nodes have not been initialised yet");
            }
            for (Node node : nodes) {
                if (!shown.contains(node.id.intValue())) {
                    node.isFadeOut = Boolean.TRUE;
                }
                if(node.connectors!=null){
                    for (Connector connector : node.connectors) {
                        if(!shown.contains(connector.edgeId.intValue())){
                            connector.isFadeOut = Boolean.TRUE;
                        }
                    }
                }
            }

            if(edges==null || edges.isEmpty()){
                throw new RuntimeException("The edges have not been initialised yet");
            }
            for (Edge edge : edges) {
                if (!shown.contains(edge.id.intValue())){
                    edge.isFadeOut = Boolean.TRUE;
                }
            }

            if(links!=null){
                for (Link link : links) {
                    link.isFadeOut = Boolean.TRUE;
                }
            }
        }
    }

    private void createAndAddConnector(HashMap<Long, Node> nodesMap, List<ReactionPart> reactionPartList, Edge edge, Connector.Type type){
        if(reactionPartList!=null){
            for(ReactionPart reactionPart : reactionPartList){
                Node node = nodesMap.get(reactionPart.id);
                if(node!=null){
                    node.connectors.add( new Connector(edge, reactionPart, type) );
                }
            }
        }
    }

    /**
     * Identify and remove the normal counterparts of any disease reaction
     */
    public void normalEventsOverlaidByDiseaseEventsCleanup(){
        Set<Node> nodesToBeKept = new HashSet<>();
        Set<Long> reactionsToBeDeleted = new HashSet<>();
        //Find the disease reactions and identify the normal counterparts
        for (GKInstance event : InstanceUtilities.getContainedEvents(DatabaseObjectFactory.fetchInstance(dbId))) {
            if(event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)){
                ReactionlikeEvent rle = DatabaseObjectFactory.getDatabaseObject(event).load();
                List<ReactionlikeEvent> normalEvents = rle.getNormalReaction();
                if(normalEvents!=null && !normalEvents.isEmpty()){
                    //Here we can assume rle is a disease reaction ;)
                    //Identify the normal counterpart and its participants in order to remove them
                    List<PhysicalEntity> catalysts = getCatalystActivityPEs(rle.getCatalystActivity());
                    List<PhysicalEntity> functionalStatus = getFunctionalStatusPEs(rle.getEntityFunctionalStatus());
                    nodesToBeKept.addAll(getNodesToBeKept(rle.getInput()));
                    nodesToBeKept.addAll(getNodesToBeKept(rle.getOutput()));
                    nodesToBeKept.addAll(getNodesToBeKept(catalysts));
                    nodesToBeKept.addAll(getNodesToBeKept(functionalStatus));
                    for (ReactionlikeEvent normalEvent : normalEvents) {
                        reactionsToBeDeleted.add(normalEvent.getDbId());
                    }
                }
            }
        }

        Set<Long> entitiesToBeDeleted = new HashSet<>();
        Set<Long> edgesToBeDeleted = new HashSet<>();
        Set<Edge> edgesToBeKept = new HashSet<>();
        for (Edge edge : edges) {
            if(!reactionsToBeDeleted.contains(edge.reactomeId)){
                edgesToBeKept.add(edge);
            }else{
                // Remove only those edges and nodes that are not in
                // the overlaidComponents and crossedComponents lists
                edgesToBeDeleted.add(edge.id);
                entitiesToBeDeleted.addAll(getEntitiesToDelete(edge.inputs));
                entitiesToBeDeleted.addAll(getEntitiesToDelete(edge.outputs));
                entitiesToBeDeleted.addAll(getEntitiesToDelete(edge.catalysts));
                entitiesToBeDeleted.addAll(getEntitiesToDelete(edge.activators));
                entitiesToBeDeleted.addAll(getEntitiesToDelete(edge.inhibitors));
            }
        }
        edges = new LinkedList<>(edgesToBeKept);

        for (Node node : nodes) {
            if(!entitiesToBeDeleted.contains(node.id)) {
                nodesToBeKept.add(node);
            }
            //There might have connectors belonging to deleted edges
            if (node.connectors != null) {
                List<Connector> keepConnectorsSafe = new LinkedList<>();
                for (Connector connector : node.connectors) {
                    if (!edgesToBeDeleted.contains(connector.edgeId)) {
                        keepConnectorsSafe.add(connector);
                    }
                }
                node.connectors = keepConnectorsSafe;
            }
        }
        nodes = new LinkedList<>(nodesToBeKept);
    }

    /**
     * Fixes incomplete and empty compartments
     */
    public void fixCompartments() {
        if (compartments == null || compartments.isEmpty()){
            return;
        }
        for (Compartment compartment : compartments) {
            // IMPORTANT
            // If schemaClass is missing then set it to "EntityCompartment"
            if(compartment.schemaClass==null || compartment.schemaClass.isEmpty()){
                compartment.schemaClass="EntityCompartment";
            }

            // IMPORTANT
            // If displayName is missing then set it to "Unidentified Compartment"
            if(compartment.displayName==null || compartment.displayName.isEmpty()){
                compartment.schemaClass="Unidentified Compartment";
            }
        }

    }

    /**
     * Return diagram entities that are NOT in the overlaidComponents(notFadedOut) list and
     * NOT in the crossedComponents list
     */
    private List<Long> getEntitiesToDelete(List<ReactionPart> participants){
        List<Long> rtn = new LinkedList<>();
        if(participants!=null){
            for (ReactionPart participant : participants) {
                if (
                        (crossedComponents==null || !crossedComponents.contains(participant.id.intValue())) &&
                        (notFadeOut==null || !notFadeOut.contains(participant.id.intValue()))
                   ){
                    rtn.add(participant.id);
                }
            }
        }
        return rtn;
    }

    private List<Node> getNodesToBeKept(List<PhysicalEntity> participants){
        List<Node> rtn = new LinkedList<>();
        if(participants!=null) {
            for (PhysicalEntity entity : participants) {
                for (Node node : nodes) {
                    if (node.reactomeId.equals(entity.getDbId())) {
                        rtn.add(node);
                    }
                }
            }
        }
        return rtn;
    }

    private List<PhysicalEntity> getCatalystActivityPEs(List<CatalystActivity> catalystActivities){
        List<PhysicalEntity> rtn = new LinkedList<>();
        if(catalystActivities!=null) {
            for (CatalystActivity catalystActivity : catalystActivities) {
                catalystActivity.load();
                rtn.add(catalystActivity.getPhysicalEntity());
            }
        }
        return rtn;
    }

    private List<PhysicalEntity> getFunctionalStatusPEs(List<EntityFunctionalStatus> efss){
        List<PhysicalEntity> rtn = new LinkedList<>();
        if(efss!=null) {
            for (EntityFunctionalStatus efs : efss) {
                efs.load();
                rtn.add(efs.getPhysicalEntity());
            }
        }
        return rtn;
    }

    /**
     * Checks if the particular diagram object is in any of the 5 lists (disease diagrams)
     */
    public boolean shouldBeIncluded(Integer diagramId){
        if(     (normalComponents!=null && normalComponents.contains(diagramId))   ||
                (crossedComponents!=null && crossedComponents.contains(diagramId)) ||
                (diseaseComponents!=null && diseaseComponents.contains(diagramId)) ||
                (lofNodes!=null && lofNodes.contains(diagramId))                   ||
                (notFadeOut!=null && notFadeOut.contains(diagramId))               ){
            return true;
        }else{
            return false;
        }
    }
}
