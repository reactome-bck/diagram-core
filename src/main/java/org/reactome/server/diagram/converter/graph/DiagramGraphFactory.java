package org.reactome.server.diagram.converter.graph;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.reactome.server.diagram.converter.graph.model.PhysicalEntityNode;
import org.reactome.server.diagram.converter.graph.output.*;
import org.reactome.server.diagram.converter.layout.output.Diagram;
import org.reactome.server.diagram.converter.layout.output.Edge;
import org.reactome.server.diagram.converter.layout.output.Node;
import org.reactome.server.diagram.converter.util.MapSet;
import org.reactome.server.diagram.converter.util.report.LogEntry;
import org.reactome.server.diagram.converter.util.report.LogEntryType;
import org.reactome.server.diagram.converter.util.report.LogUtil;

import java.util.*;

/**
 * For a given list of nodes contained in a diagram, this class produces a graph with
 * the underlying physical entities and their children. This information will be sent
 * to the client in a second batch so the graph can be kept
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class DiagramGraphFactory {

    private static Logger logger = Logger.getLogger(DiagramGraphFactory.class.getName());

    public MySQLAdaptor dba;

    public DiagramGraphFactory(MySQLAdaptor dba) {
        this.dba = dba;
    }

    public Graph getGraph(Diagram diagram) {
        return new Graph(diagram.getDbId(),
                diagram.getStableId(),
                getGraphNodes(diagram),
                getGraphEdges(diagram),
                getSubpathways(diagram));
    }

    //##############################  GRAPH NODES  ##############################

    //The buffer is used in building time to avoid querying/decomposition of those previously processed
    private Map<Long, PhysicalEntityNode> physicalEntityBuffer;

    private Set<EntityNode> getGraphNodes(Diagram diagram) {
        Set<EntityNode> rtn = new HashSet<>();
        for (PhysicalEntityNode pe : getPhysicalEntityNodes(diagram.getNodes())) {
            rtn.add(new EntityNode(pe));
        }
        return rtn;
    }

    private Collection<PhysicalEntityNode> getPhysicalEntityNodes(Collection<Node> nodes) {
        this.physicalEntityBuffer = new HashMap<>();
        for (Node node : nodes) {
            if (node.isFadeOut != null) continue;
            try {
                GKInstance instance = dba.fetchInstance(node.reactomeId);
                if (instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity) ||
                        instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                    PhysicalEntityNode peNode = process(instance);
                    peNode.addDiagramId(node.id);
                } else {
                    logger.error(node.displayName + " is not a PhysicalEntity");
                }
            } catch (Exception e) {
                logger.error(node.displayName + " is not in the database", e);
            }
        }
        return physicalEntityBuffer.values();
    }

    private PhysicalEntityNode process(GKInstance physicalEntity) {
        //When organically growing, we do not want previous processed entities to be processed again
        //This a recursive algorithm STOP CONDITION (DO NOT REMOVE)
        PhysicalEntityNode node = physicalEntityBuffer.get(physicalEntity.getDBID());
        if (node != null) {
            return node;
        }
        node = new PhysicalEntityNode(physicalEntity);
        physicalEntityBuffer.put(physicalEntity.getDBID(), node);

        for (GKInstance pE : getContainedPhysicalEntities(physicalEntity)) {
            PhysicalEntityNode child = process(pE);
            node.addChild(child);
        }
        return node;
    }

    private Set<GKInstance> getContainedPhysicalEntities(GKInstance physicalEntity) {
        Set<GKInstance> rtn = new HashSet<>();
        SchemaClass schemaClass = physicalEntity.getSchemClass();
        if (schemaClass.isa(ReactomeJavaConstants.Complex) || schemaClass.isa(ReactomeJavaConstants.EntitySet)) {
            rtn.addAll(getPhysicalEntityAttr(physicalEntity, ReactomeJavaConstants.hasComponent));
            rtn.addAll(getPhysicalEntityAttr(physicalEntity, ReactomeJavaConstants.hasMember));
            rtn.addAll(getPhysicalEntityAttr(physicalEntity, ReactomeJavaConstants.hasCandidate));
        }
        return rtn;
    }

    private Set<GKInstance> getPhysicalEntityAttr(GKInstance pE, String attr) {
        Set<GKInstance> rtn = new HashSet<>();
        try {
            List components = pE.getAttributeValuesList(attr);
            for (Object component : components) {
                rtn.add((GKInstance) component);
            }
        } catch (Exception e) {
            //Nothing here
        }
        return rtn;
    }

    //#############################  /GRAPH NODES/  #############################


    //##############################  GRAPH EDGES  ##############################

    //The buffer is used in building time to avoid querying/decomposition of those previously processed
    private Map<Long, EventNode> eventBuffer;

    private Collection<EventNode> getGraphEdges(Diagram diagram) {
        for (EventNode edge : getEventNodes(diagram.getEdges())) {
            if (edge.getPreceding() != null) {
                for (Long p : edge.getPreceding()) {
                    EventNode preceding = eventBuffer.get(p);
                    if (preceding != null) { //Preceding could be somewhere outside this diagram
                        preceding.addFollowingEvent(p);
                    }
                }
            }
        }

        //Previous one checks whether the events in the diagram exist in the database but now we need
        //to do it the other way around; see whether all the reactions in the model are in the diagram
        try {
            GKInstance pathway = dba.fetchInstance(diagram.getDbId());
            Collection<Long> reactions = new HashSet<>();
            Collection events = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (events != null) {
                for (Object e : pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent)) {
                    if (e != null) reactions.addAll(getContainedReactions((GKInstance) e));
                }
            }

            String createdBy = "";
            try {
                createdBy = ((GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.created)).getDisplayName();
            } catch (Exception e){ /*Nothing here*/}

            for (Long reaction : reactions) {
                if (!eventBuffer.keySet().contains(reaction)) {
                    String message = "[" + diagram.getStableId() + "] is missing reaction " + reaction + " | created by: " + createdBy;
                    LogUtil.log(logger, Level.WARN, new LogEntry(LogEntryType.MISSING_REACTION, diagram.getStableId(), reaction + "", message));
                }
            }

        } catch (Exception e) {
            //TODO Handdle this exception
        }

        return eventBuffer.values();
    }

    private Collection<Long> getContainedReactions(GKInstance event) throws Exception {
        Collection<Long> rtn = new HashSet<>();
        if (event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
            rtn.add(event.getDBID());
        } else if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway) && !hasDiagram(event)) {
            Collection events = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (events != null) {
                for (Object aux : events) {
                    rtn.addAll(getContainedReactions((GKInstance) aux));
                }
            }
        }
        return rtn;
    }

    private Collection<EventNode> getEventNodes(Collection<Edge> edges) {
        this.eventBuffer = new HashMap<>();
        if (edges != null) {
            for (Edge edge : edges) {
                if (edge.isFadeOut != null) continue;
                EventNode eNode = getOrCreate(edge);
                eNode.addDiagramId(edge.id);
            }
        }
        return this.eventBuffer.values();
    }

    private EventNode getOrCreate(Edge edge) {
        EventNode eventNode = this.eventBuffer.get(edge.reactomeId);
        if (eventNode == null) {
            try {
                GKInstance event = dba.fetchInstance(edge.reactomeId);
                if (event.getSchemClass().isa(ReactomeJavaConstants.Event)) {
                    eventNode = new EventNode(event);
                    this.eventBuffer.put(eventNode.getDbId(), eventNode);
                } else {
                    logger.error(edge.reactomeId + " >> " + edge.displayName + " is not a Event");
                }
            } catch (Exception e) {
                logger.error(edge.reactomeId + " >> " + edge.displayName + " is not in the database", e);
            }
        }
        return eventNode;
    }

    //#############################  /GRAPH EDGES/  #############################


    //#############################  GRAPH SUBPATHWAYS  #########################

    private Collection<SubpathwayNode> getSubpathways(Diagram diagram) {
        GKInstance pathway;
        try {
            pathway = dba.fetchInstance(diagram.getDbId());
        } catch (Exception e) {
            logger.error(diagram.getDbId() + " is not in the database", e);
            return null;
        }

        Set<SubpathwayNode> rtn = new HashSet<>();
        MapSet<Integer, GKInstance> subpathways = getSubpathways(pathway, 1);
        for (Integer level : subpathways.keySet()) {
            for (GKInstance subpathway : subpathways.getElements(level)) {
                GraphNode node = new GraphNode(subpathway);
                rtn.add(new SubpathwayNode(node, getContainedEvents(subpathway), level));
            }
        }
        if (rtn.isEmpty()) return null;
        return rtn;
    }

    private MapSet<Integer, GKInstance> getSubpathways(GKInstance pathway, int level){
        MapSet<Integer, GKInstance> subpathways = new MapSet<>();
        if (!pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) return subpathways;
        try {
            List hasEvents = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if(hasEvents!=null){
                for (Object aux : hasEvents) {
                    GKInstance event = (GKInstance) aux;
                    if(event.getSchemClass().isa(ReactomeJavaConstants.Pathway)){
                        if(!hasDiagram(event)) {
                            subpathways.add(level, event);
                            subpathways.addAll(getSubpathways(event, level + 1));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("getAttributeValuesList with hasEvent has failed for " + pathway.getDBID());
        }
        return subpathways;
    }

    private Set<Long> getContainedEvents(GKInstance pathway) {
        Set<Long> events = new HashSet<>();
        if (!pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) return events;
        try {
            List hasEvents = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (hasEvents != null) {
                for (Object aux : hasEvents) {
                    GKInstance event = (GKInstance) aux;
                    if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                        if (hasDiagram(event)) {
                            events.add(event.getDBID());
                        } else {
                            events.addAll(getContainedEvents(event));
                        }
                    } else {
                        events.add(event.getDBID());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("getAttributeValuesList with hasEvent has failed for " + pathway.getDBID());
        }
        return events;
    }

    private boolean hasDiagram(GKInstance pathway) {
        Collection<?> diagrams = null;
        try {
            diagrams = pathway.getReferers(ReactomeJavaConstants.representedPathway);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (diagrams != null && diagrams.size() > 0) {
            for (Object item : diagrams) {
                GKInstance diagram = (GKInstance) item;
                if (diagram.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
                    return true;
                }
            }
        }
        return false;
    }

    //#############################  /GRAPH SUBPATHWAYS/  #######################


    public Map<Long, PhysicalEntityNode> getPhysicalEntityMap() {
        return physicalEntityBuffer;
    }
}
