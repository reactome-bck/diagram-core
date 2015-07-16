package org.reactome.server.diagram.converter.graph;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.reactome.server.diagram.converter.graph.model.PhysicalEntityNode;
import org.reactome.server.diagram.converter.graph.output.*;
import org.reactome.server.diagram.converter.layout.output.Diagram;
import org.reactome.server.diagram.converter.layout.output.Edge;
import org.reactome.server.diagram.converter.layout.output.Node;

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
        return new Graph(diagram.dbId,
                diagram.stableId,
                getGraphNodes(diagram),
                getGraphEdges(diagram),
                getSubpathways(diagram));
    }

    //##############################  GRAPH NODES  ##############################

    //The buffer is used in building time to avoid querying/decomposition of those previously processed
    private Map<Long, PhysicalEntityNode> physicalEntityBuffer;

    private Set<EntityNode> getGraphNodes(Diagram diagram) {
        Set<EntityNode> rtn = new HashSet<>();
        for (PhysicalEntityNode pe : getPhysicalEntityNodes(diagram.nodes)) {
            rtn.add(new EntityNode(pe));
        }
        return rtn;
    }

    private Collection<PhysicalEntityNode> getPhysicalEntityNodes(List<Node> nodes) {
        this.physicalEntityBuffer = new HashMap<>();
        for (Node node : nodes) {
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
        for (EventNode edge : getEventNodes(diagram.edges)) {
            if (edge.getPreceding() != null) {
                for (Long p : edge.getPreceding()) {
                    EventNode preceding = eventBuffer.get(p);
                    if (preceding != null) { //Preceding could be somewhere outside this diagram
                        preceding.addFollowingEvent(p);
                    }
                }
            }
        }
        return eventBuffer.values();
    }

    private Collection<EventNode> getEventNodes(List<Edge> edges) {
        this.eventBuffer = new HashMap<>();
        if (edges != null) {
            for (Edge edge : edges) {
                try {
                    GKInstance event = dba.fetchInstance(edge.reactomeId);
                    if (event.getSchemClass().isa(ReactomeJavaConstants.Event)) {
                        EventNode eNode = new EventNode(event, edge.id);
                        this.eventBuffer.put(eNode.getDbId(), eNode);
                    } else {
                        logger.error(edge.displayName + " is not a Event");
                    }
                } catch (Exception e) {
                    logger.error(edge.displayName + " is not in the database", e);
                }
            }
        }
        return this.eventBuffer.values();
    }

    //#############################  /GRAPH EDGES/  #############################


    //#############################  GRAPH SUBPATHWAYS  #########################

    private Collection<Subpathway> getSubpathways(Diagram diagram) {
        GKInstance pathway;
        try {
            pathway = dba.fetchInstance(diagram.dbId);
        } catch (Exception e) {
            logger.error(diagram.dbId + " is not in the database", e);
            return null;
        }

        Set<Subpathway> rtn = new HashSet<>();
        Set<GKInstance> events = InstanceUtilities.getContainedEvents(pathway);
        if (events != null) {
            for (GKInstance event : events) {
                if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                    if (!hasDiagram(event)) { //Do not go deeper when not needed :)
                        GraphNode node = new GraphNode(event);
                        Set<GKInstance> containedEvents = InstanceUtilities.getContainedEvents(event);
                        if (containedEvents != null) {
                            Set<Long> dbIds = new HashSet<>();
                            for (GKInstance instance : containedEvents) {
                                if(instance.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                                    dbIds.add(instance.getDBID());
                                }
                            }
                            rtn.add(new Subpathway(node, dbIds));
                        }
                    }
                }
            }
        }

        if(rtn.isEmpty()) return null;
        return rtn;
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
