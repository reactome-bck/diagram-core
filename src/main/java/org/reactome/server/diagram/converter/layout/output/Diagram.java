package org.reactome.server.diagram.converter.layout.output;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reactome.server.diagram.converter.graph.output.SubpathwayNode;
import org.reactome.server.diagram.converter.util.Beautifier;
import org.reactome.server.diagram.converter.util.report.LogUtil;
import org.reactome.server.diagram.converter.util.MapSet;
import org.reactome.server.diagram.converter.util.shadows.ShadowsUtil;
import org.reactome.server.diagram.converter.util.ShapeBuilder;
import org.reactome.server.diagram.converter.util.report.LogEntry;
import org.reactome.server.diagram.converter.util.report.LogEntryType;

import java.util.*;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Diagram {
    private static Logger logger = Logger.getLogger(Diagram.class.getName());

    //DO NOT GET SERIALISED
    private Boolean isDisease;
    private Set<Long> normalComponents = new HashSet<>();
    private Set<Long> crossedComponents = new HashSet<>();
    private Set<Long> notFadeOut = new HashSet<>();

    private Long dbId;
    private String stableId;
    private String displayName;
    private Boolean forNormalDraw = Boolean.TRUE;
    private Long lastId = 0L;

    private transient Set<Long> diseaseComponents;
    private transient Set<Long> lofNodes;

    private Map<Long, DiagramObject> objectMap = new HashMap<>();

    private MapSet<Long, Node> nodes = new MapSet<>();
    private MapSet<Long, Edge> edges = new MapSet<>();
    private MapSet<Long, Compartment> compartments = new MapSet<>();

    private Map<Long, Note> notes = new HashMap<>();
    private Map<Long, Link> links = new HashMap<>();

    private Set<Shadow> shadows = new HashSet<>();

    private Integer minX; private Integer maxX;
    private Integer minY; private Integer maxY;

    public void createShadows(Collection<SubpathwayNode> subpathways){
        if(subpathways==null) return;
        int colorId = 0;
        List<SubpathwayNode> list = new ArrayList<>(subpathways);
        Collections.sort(list); //Ensures the same colours for the shadows across releases
        for (SubpathwayNode subpathway : list) {
            if(subpathway.level>1) continue;
            List<DiagramObject> participants = new ArrayList<>();
            for (Long event : subpathway.events) {
                Set<Edge> edges = this.edges.getElements(event);
                if(edges==null) continue; // For subpathways pointing to a process node (encapsulated pathway)
                participants.addAll(edges);
                for (Edge edge : edges) {
                    DiagramObject obj;
                    if(edge.inputs!=null)
                    for (ReactionPart part : edge.inputs) {
                        if ((obj= this.objectMap.get(part.id)) != null) participants.add(obj);
                    }

                    if(edge.outputs!=null)
                    for (ReactionPart part : edge.outputs) {
                        if ((obj= this.objectMap.get(part.id)) != null) participants.add(obj);
                    }

                    if(edge.catalysts!=null)
                    for (ReactionPart part : edge.catalysts) {
                        if ((obj= this.objectMap.get(part.id)) != null) participants.add(obj);
                    }

                    if(edge.activators!=null)
                    for (ReactionPart part : edge.activators) {
                        if ((obj= this.objectMap.get(part.id)) != null) participants.add(obj);
                    }

                    if(edge.inhibitors!=null)
                    for (ReactionPart part : edge.inhibitors) {
                        if ((obj= this.objectMap.get(part.id)) != null) participants.add(obj);
                    }
                }
            }
            if(participants.isEmpty()){
                String message = "[" + stableId + "] contains a subpathway without participants. ";
                LogUtil.log(logger, Level.WARN, new LogEntry(LogEntryType.SUBPATHWAY_WITHOUT_PARTICIPANTS, message, stableId, subpathway.stId, subpathway.displayName));
            }else {
                shadows.add(new Shadow(getUniqueId(), subpathway, participants, colorId++));
            }
        }

        if(!shadows.isEmpty()){
            //Shadows util repositions the shadows text for a better look and feel
            this.shadows = (new ShadowsUtil(shadows)).getShadows();
        }
    }

    /***
     * Iterates over all Edges and Links and identifies the isolated glyphs.
     * The method can also apply a fix by removing this glyph from the list of Nodes.
     *
     * @param applyFix If true the fix is applied
     */
    public void checkForIsolatedNodes(boolean applyFix){
        // proceed only if nodes and edges are not null
        if(nodes==null || edges==null) { return;}

        // generate a map of all nodes excluding the ProcessNodes (ugly green boxes)
        HashMap<Long, Node> nodesMap = new HashMap<>();
        for (NodeCommon node : nodes.values()) {
            if(!node.renderableClass.equals("ProcessNode") && node.isFadeOut==null){
                nodesMap.put(node.id, (Node) node);
            }
        }

        // iterate over all Edges and check their ReactionParts
        for (Edge edge : edges.values()) {
            filterNodes(nodesMap, edge.inputs);
            filterNodes(nodesMap, edge.outputs);
            filterNodes(nodesMap, edge.catalysts);
            filterNodes(nodesMap, edge.activators);
            filterNodes(nodesMap, edge.inhibitors);
        }

        // iterate over all Links and check their ReactionParts
        for (Link link : links.values()) {
            filterNodes(nodesMap, link.inputs);
            filterNodes(nodesMap, link.outputs);
        }

        if(nodesMap.size() > 0) {
            for (Long entityId : nodesMap.keySet()) {
                Node node = nodesMap.get(entityId);
                String msg = "[" + stableId + "] contains an isolated glyph: " + node.reactomeId + " | " + node.displayName;
                LogUtil.log(logger, Level.WARN, new LogEntry(LogEntryType.ISOLATED_GLYPHS, msg, stableId, node.reactomeId + "", node.displayName));
                nodes.remove(entityId);

                if(applyFix) {
                    this.nodes.remove(entityId);
                    if(isDisease!=null) {
                        if (normalComponents != null)       normalComponents.remove(entityId);
                        if (crossedComponents != null)      crossedComponents.remove(entityId);
                        if (notFadeOut != null)             notFadeOut.remove(entityId);
                        if (diseaseComponents != null)      diseaseComponents.remove(entityId);
                        if (lofNodes != null)               lofNodes.remove(entityId);
                    }
                }
            }
        }
    }

    public void setIsDisease(Boolean isDisease) {
        this.isDisease = isDisease;
    }

    public boolean isDisease() {
        return (isDisease!=null && isDisease);
    }

    public void setNormalComponents(Set<Long> normalComponents) {
        this.normalComponents = normalComponents;
    }

    public void setCrossedComponents(Set<Long> crossedComponents) {
        this.crossedComponents = crossedComponents;
    }

    public void setNotFadeOut(Set<Long> notFadeOut) {
        this.notFadeOut = notFadeOut;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public String getStableId() {
        return stableId;
    }

    public void setStableId(String stableId) {
        this.stableId = stableId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getForNormalDraw() {
        return forNormalDraw;
    }

    public void setForNormalDraw(Boolean forNormalDraw) {
        this.forNormalDraw = forNormalDraw;
    }

    public void setDiseaseComponents(Set<Long> diseaseComponents) {
        this.diseaseComponents = diseaseComponents;
    }

    public void setLofNodes(Set<Long> lofNodes) {
        this.lofNodes = lofNodes;
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public boolean addNode(Node node) {
        if(isDisease==null || !isDisease || shouldBeIncluded(node.id)){
            node = Beautifier.processName(node);
            this.nodes.add(node.reactomeId, node);
            DiagramObject duplicateEntry = this.objectMap.put(node.id, node);
            if(duplicateEntry!=null){
                LogUtil.log(logger, Level.ERROR, "[" + getStableId() + "] contains Nodes with duplicate diagram IDs >>" + node.displayName + " [" + node.id + "] <-> " + duplicateEntry.displayName + " [" + duplicateEntry.id + "]");
            }
            setLastId(node);
            return true;
        }
        return false;
    }

    public Collection<Note> getNotes() {
        return notes.values();
    }

    public boolean addNote(Note note) {
        if(!note.displayName.toLowerCase().equals("note")) {
            this.notes.put(note.id, note);
            this.objectMap.put(note.id, note);
            setLastId(note);
            return true;
        }
        return false;
    }

    public Collection<Edge> getEdges() {
        return edges.values();
    }

    public boolean addEdge(Edge edge) {
        if(isDisease==null || !isDisease || shouldBeIncluded(edge.id)){
            this.edges.add(edge.reactomeId, edge);
            DiagramObject duplicateEntry = this.objectMap.put(edge.id, edge);
            if(duplicateEntry!=null){
                LogUtil.log(logger, Level.ERROR, "[" + getStableId() + "] contains Edges with duplicate diagram IDs >>" + edge.displayName + " [" + edge.id + "] <-> " + duplicateEntry.displayName + " [" + duplicateEntry.id + "]");
            }
            setLastId(edge);
            return true;
        }
        return false;
    }

    public Collection<Link> getLinks() {
        return links.values();
    }

    /**
     * Adds a link provided that it has inputs and outputs
     */
    public boolean addLink(Link link) {
        //noinspection SimplifiableIfStatement
        if(link.inputs!=null && !link.inputs.isEmpty() && link.outputs!=null && !link.outputs.isEmpty()) {
            if(isDisease==null || !isDisease || shouldBeIncluded(link.id)){
                links.put(link.id, link);
                this.objectMap.put(link.id, link);
                setLastId(link);
                return true;
            }
        }
        return false;
    }

    public Collection<Compartment> getCompartments() {
        return compartments.values();
    }

    public Set<Shadow> getShadows() {
        return shadows;
    }

    /**
     * Adds a compartment and fixes any incomplete and empty compartment
     */
    public void addCompartment(Compartment compartment) {
        // IMPORTANT
        // If schemaClass is missing then set it to "EntityCompartment"
        if(compartment.schemaClass==null || compartment.schemaClass.isEmpty()){
            compartment.schemaClass="EntityCompartment";
        }

        // IMPORTANT
        // If displayName is missing then set it to "Unidentified Compartment"
        if(compartment.displayName==null || compartment.displayName.isEmpty()){
            compartment.displayName="Unidentified Compartment";
        }
        this.compartments.add(compartment.reactomeId, compartment);
        this.objectMap.put(compartment.id, compartment);
        setLastId(compartment);
    }

    public Integer getMinX() {
        return minX;
    }

    public Integer getMaxX() {
        return maxX;
    }

    public Integer getMinY() {
        return minY;
    }

    public Integer getMaxY() {
        return maxY;
    }

    public boolean checkIfEmpty() {
        boolean rtn = nodes.isEmpty();
        if(rtn) {
            String message = "[" + stableId + "] is empty and does not contain any entity apart from compartments";
            LogUtil.log(logger, Level.WARN, new LogEntry(LogEntryType.DIAGRAM_EMPTY, message, stableId));
        }
        return rtn;
    }

    public void fadeOutNormalComponents(){
        if(     (isDisease != null && isDisease) &&
                (forNormalDraw == false) &&
                (notFadeOut == null || notFadeOut.isEmpty())) {
            if(nodes==null || nodes.isEmpty()){
                LogUtil.logSilently(logger, Level.WARN, "The nodes have not been initialised yet");
                return;
            }else if(edges==null || edges.isEmpty()){
                LogUtil.logSilently(logger, Level.WARN, "The edges have not been initialised yet");
                return;
            }

            for (Node node : nodes.values()) {
                node.isFadeOut=Boolean.TRUE;

                if(node.connectors!=null){
                    for (Connector connector : node.connectors) {
                        connector.isFadeOut = Boolean.TRUE;
                    }
                }
            }

            for(Edge edge : edges.values()) {
                    edge.isFadeOut = Boolean.TRUE;
            }

            if(links!=null) {
                for (Link link : links.values()) {
                    link.isFadeOut = Boolean.TRUE;
                }
            }
        }
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
        for(Node node : nodes.values()) {
            // take x and x + width
            xx.add(node.minX); xx.add(node.maxX);
            yy.add(node.minY); yy.add(node.maxY);
        }
        for(Edge edge : edges.values()){
            xx.add(edge.minX); xx.add(edge.maxX);
            yy.add(edge.minY); yy.add(edge.maxY);
        }
        for(Compartment compartment : compartments.values()) {
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
            LogUtil.logSilently(logger, Level.WARN, "[" + this.stableId + "] has negative boundaries ... MinX: " + this.minX + " MinY: " + this.minY);
        }
    }

    /**
     * Calculates the points of the arrows only for those reactions
     * that do not have output connectors. In this case the arrow should
     * be positioned on the last segment of the backbone.
     */
    public void setBackboneArrows(){
        if(edges == null) return;

        for (Edge edge : edges.values()) {

            if(edge.outputs == null ) continue;

            for (ReactionPart output : edge.outputs) {
                if(output.points == null || output.points.size()==0){
                    // Use the last segment of the backbone
                    // IMPORTANT!!! Segments here start from the centre of the backbone and point to the output node
                    Segment segment = edge.segments.get(edge.segments.size() -1);
                    List<Coordinate> points = ShapeBuilder.createArrow(
                            segment.to.x,
                            segment.to.y,
                            segment.from.x,
                            segment.from.y);
                    // Shape is a filled arrow
                    edge.endShape = new Shape(points.get(0), points.get(1), points.get(2), Boolean.FALSE, Shape.Type.ARROW);

                    //Check for overlapping reaction shapes and arrows
                    if(edge.segments.size()==1 && edge.position.equals(edge.segments.get(0).to)) {
                        String message = "[" + stableId + "] contains reaction " + edge.reactomeId + " with overlapping reaction shape and arrow";
                        LogUtil.log(logger, Level.WARN, new LogEntry(LogEntryType.OVERLAPPING_REACTION_SHAPES_ON_REACTOMECURATOR, message, stableId, edge.reactomeId + ""));
                    }
                }
            }
        }
    }

    public void setLinkArrows(){
        if(links == null) return;
        for (Link link : links.values()) {
            if(link.renderableClass.equals("FlowLine")){
                if(link.outputs == null ) return;
                // Use the last segment of the backbone
                // IMPORTANT!!! Segments here start from the centre of the backbone and point to the output node
                Segment segment = link.segments.get(link.segments.size() -1);
                List<Coordinate> points = ShapeBuilder.createArrow(
                        segment.to.x,
                        segment.to.y,
                        segment.from.x,
                        segment.from.y);
                // Shape is a filled arrow
                link.endShape = new Shape(points.get(0), points.get(1), points.get(2), Boolean.FALSE, Shape.Type.ARROW);
            }else if(link.renderableClass.equals("Interaction")){
                if(link.outputs == null ) return;
                // Use the last segment of the backbone
                // IMPORTANT!!! Segments here start from the centre of the backbone and point to the output node
                Segment segment = link.segments.get(link.segments.size() -1);
                List<Coordinate> points = ShapeBuilder.createArrow(
                        segment.to.x,
                        segment.to.y,
                        segment.from.x,
                        segment.from.y);
                // Shape is an empty arrow
                link.endShape = new Shape(points.get(0), points.get(1), points.get(2), Boolean.TRUE, Shape.Type.ARROW);
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
        for (NodeCommon node : nodes.values()) {
            nodesMap.put(node.id, (Node) node);
        }

        // iterate over all Edges and ReactionParts and then create the connectors
        for (Edge edge : edges.values()) {
            createAndAddConnector(nodesMap, edge.inputs, edge, Connector.Type.INPUT);
            createAndAddConnector(nodesMap, edge.outputs, edge, Connector.Type.OUTPUT);
            createAndAddConnector(nodesMap, edge.catalysts, edge, Connector.Type.CATALYST);
            createAndAddConnector(nodesMap, edge.activators, edge, Connector.Type.ACTIVATOR);
            createAndAddConnector(nodesMap, edge.inhibitors, edge, Connector.Type.INHIBITOR);
        }
    }

    public void setNodesBoundaries(){
        //Once the nodes connectors setup is finished, the boundaries need to be set
        for (Node node : nodes.values()) {
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
            for (Node node : nodes.values()) {
                if (crossedComponents.contains(node.id)) {
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
                LogUtil.logSilently(logger, Level.WARN, "The nodes have not been initialised yet");
                return;
            }else if(edges==null || edges.isEmpty()){
                LogUtil.logSilently(logger, Level.WARN, "The edges have not been initialised yet");
                return;
            }

            for (Node node : nodes.values()) {
                if (diseaseComponents.contains(node.id)) {
                    node.isFadeOut = null;
                }
                if(node.connectors!=null){
                    for (Connector connector : node.connectors) {
                        if(diseaseComponents.contains(connector.edgeId)){
                            connector.isFadeOut = null;
                        }
                    }
                }
            }

            for(Edge edge : edges.values()) {
                if (diseaseComponents.contains(edge.id)) {
                    edge.isFadeOut = null;
                }
            }

            if(links!=null && !links.isEmpty()) {
                for (Link link : links.values()) {
                    if (diseaseComponents.contains(link.id)) {
                        link.isFadeOut = null;
                    }
                }
            }
        }
    }

    public void setEntitySummaries(){
        if(nodes!=null) {
            for (Node node : nodes.values()) {
                if (node.isFadeOut != null) continue;
                node.setSummaryItems();
            }
        }
    }

    public void setOverlaidObjects(){
        if(notFadeOut!=null && !notFadeOut.isEmpty()) {
            if(nodes==null || nodes.isEmpty()){
                LogUtil.logSilently(logger, Level.WARN, "The nodes have not been initialised yet");
                return;
            }
            for (Node node : nodes.values()) {
                if (!notFadeOut.contains(node.id)) {
                    node.isFadeOut = Boolean.TRUE;
                }
                if(node.connectors!=null){
                    for (Connector connector : node.connectors) {
                        if(!notFadeOut.contains(connector.edgeId)){
                            connector.isFadeOut = Boolean.TRUE;
                        }
                    }
                }
            }

            if(edges==null || edges.isEmpty()){
                LogUtil.logSilently(logger, Level.WARN, "The edges have not been initialised yet");
                return;
            }
            for (Edge edge : edges.values()) {
                if (!notFadeOut.contains(edge.id)){
                    edge.isFadeOut = Boolean.TRUE;
                }
            }

            if(links!=null){
                for (Link link : links.values()) {
                    link.isFadeOut = Boolean.TRUE;
                }
            }
        }
    }

    private void filterNodes(HashMap<Long, Node> nodesMap, List<ReactionPart> reactionPartList){
        if(reactionPartList!=null){
            for(ReactionPart reactionPart : reactionPartList){
                nodesMap.remove(reactionPart.id);
            }
        }
    }

    private void setLastId(DiagramObject item){
        if(item.id>this.lastId) this.lastId = item.id;
    }

    private Long getUniqueId(){
        return ++this.lastId;
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
     * Checks if the particular diagram object is in any of the 5 lists (disease diagrams)
     */
    private boolean shouldBeIncluded(Long diagramId){
        return (normalComponents != null && normalComponents.contains(diagramId)) ||
                (crossedComponents != null && crossedComponents.contains(diagramId)) ||
                (diseaseComponents != null && diseaseComponents.contains(diagramId)) ||
                (lofNodes != null && lofNodes.contains(diagramId)) ||
                (notFadeOut != null && notFadeOut.contains(diagramId));
    }
}
