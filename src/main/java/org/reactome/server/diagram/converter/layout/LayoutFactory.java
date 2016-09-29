package org.reactome.server.diagram.converter.layout;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reactome.server.diagram.converter.input.model.*;
import org.reactome.server.diagram.converter.input.model.Process;
import org.reactome.server.diagram.converter.input.model.Properties;
import org.reactome.server.diagram.converter.layout.output.*;
import org.reactome.server.diagram.converter.util.report.LogUtil;
import org.reactome.server.diagram.converter.util.report.LogEntry;
import org.reactome.server.diagram.converter.util.report.LogEntryType;

import java.io.Serializable;
import java.util.*;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class LayoutFactory {

    private static Logger logger = Logger.getLogger(LayoutFactory.class.getName());
    private static Diagram outputDiagram = null;

    public static Diagram getDiagramFromProcess(Process inputProcess, Long dbId, String stId) {

        if(inputProcess!=null) {
            outputDiagram = new Diagram();

            //Parse General fields
            outputDiagram.setDbId(dbId);
            outputDiagram.setStableId(stId);

            outputDiagram.setIsDisease(inputProcess.isIsDisease());
            outputDiagram.setForNormalDraw(inputProcess.isForNormalDraw());

            //Parse properties
            Map<String, Serializable> properties =  extractProperties(inputProcess.getProperties());
            outputDiagram.setDisplayName((properties.get("displayName")!=null) ? properties.get("displayName").toString() : null);

            //Parse General list-fields
            outputDiagram.setNormalComponents(ListToSet(extractListFromString(inputProcess.getNormalComponents(), ",")));
            outputDiagram.setDiseaseComponents(ListToSet(extractListFromString(inputProcess.getDiseaseComponents(), ",")));
            outputDiagram.setCrossedComponents(ListToSet(extractListFromString(inputProcess.getCrossedComponents(), ",")));
            outputDiagram.setNotFadeOut(ListToSet(extractListFromString(inputProcess.getOverlaidComponents(), ",")));
            outputDiagram.setLofNodes(ListToSet(extractListFromString(inputProcess.getLofNodes(), ",")));

            //Parse Nodes
            for (NodeCommon nodeCommon : extractNodesList(inputProcess.getNodes())) {
                if(nodeCommon instanceof Node) {
                    outputDiagram.addNode((Node) nodeCommon);
                }else if(nodeCommon instanceof Note){
                    outputDiagram.addNote((Note) nodeCommon);
                }else if(nodeCommon instanceof Compartment){
                    outputDiagram.addCompartment((Compartment) nodeCommon);
                }
            }

            //Check for empty diagrams and log them
            outputDiagram.checkIfEmpty();

            //Parse Edges
            for (EdgeCommon edgeCommon : extractEdgesList(inputProcess.getEdges())) {
                if(edgeCommon instanceof Link){
                    outputDiagram.addLink((Link) edgeCommon);
                }else if(edgeCommon instanceof Edge){
                    outputDiagram.addEdge((Edge) edgeCommon);
                }
            }

            //Generate node connectors
            outputDiagram.setConnectors();

            //Generate the arrow at the backbones if needed
            outputDiagram.setBackboneArrows();

            //Generate the arrow of links
            outputDiagram.setLinkArrows();

            //In case of a disease pathway normal nodes should be faded out
            outputDiagram.fadeOutNormalComponents();

            //Process the crossed components
            outputDiagram.setCrossedComponents();

            //Set the faded out components
            outputDiagram.setOverlaidObjects();

            //Process disease components
            outputDiagram.setDiseaseComponents();

            //Sets the entities visual summary
            outputDiagram.setEntitySummaries();

            //Calculate node boundaries >> IMPORTANT: Needs to be done after outputDiagram.setEntitySummaries()
            outputDiagram.setNodesBoundaries();

            //Set universal min max >> IMPORTANT: This has to always be done at the very end
            outputDiagram.setUniversalBoundaries();
        }
        return outputDiagram;
    }

    private static List<NodeCommon> extractNodesList(Nodes inputNodes){
        List<NodeCommon> rtn = new LinkedList<>();

        if(inputNodes==null){
            return null;
        }

        List<Object> inputNodesList = inputNodes.getOrgGkRenderProcessNodeOrOrgGkRenderRenderableChemicalOrOrgGkRenderRenderableCompartment();
        for(Object inputNode : inputNodesList) {
            if(inputNode!=null) {
                Class clazz = inputNode.getClass();
                if (    clazz.equals(OrgGkRenderRenderableComplex.class)    ||
                        clazz.equals(OrgGkRenderRenderableEntitySet.class)  ||
                        clazz.equals(OrgGkRenderRenderableChemical.class)   ||
                        clazz.equals(OrgGkRenderRenderableProtein.class)    ||
                        clazz.equals(OrgGkRenderRenderableRNA.class)        ||
                        clazz.equals(OrgGkRenderProcessNode.class)          ||
                        clazz.equals(OrgGkRenderRenderableEntity.class)     ||
                        clazz.equals(OrgGkRenderRenderableGene.class) ){
                    Node node = new Node(inputNode);
                    // Sometimes a node does not have a schemaClass
                    if(node.schemaClass==null) {
                        LogUtil.log(logger, Level.WARN, "[" + outputDiagram.getStableId() + "] contains entity [dbid:" + node.reactomeId + "] without a schemaClass");
                        continue;
                    }
                    fixBrokenRenderableClass(node);
                    rtn.add(node);
                }else if(clazz.equals(OrgGkRenderNote.class) ){
                    Note note = new Note(inputNode);
                    if(!note.displayName.equals("Note")){rtn.add(note);}
                }else if (clazz.equals(OrgGkRenderRenderableCompartment.class) ){
                    rtn.add(new Compartment(inputNode));
                }else{
                    LogUtil.log(logger, Level.WARN, "[" + outputDiagram.getStableId() + "] contains a not recognised NODE type - " + clazz.getName() + " [" + clazz.getSimpleName() + "]");
                }
            }
        }
        return rtn;
    }


    private static Map<String, Serializable> extractProperties(Properties inputProps){
        Map<String, Serializable> props = new HashMap<>();
        if(inputProps!=null && inputProps.getIsChangedOrDisplayName()!=null){
            for (Serializable item : inputProps.getIsChangedOrDisplayName()) {
                if(item instanceof String){
                    props.put("displayName", item);
                }
            }
        }
        return props;
    }

    private static List<EdgeCommon> extractEdgesList(Edges inputEdges){
        if(inputEdges==null){
            return null;
        }

        List<EdgeCommon> rtn = new ArrayList<>();
        List<Object> inputEdgesList = inputEdges.getOrgGkRenderFlowLineOrOrgGkRenderEntitySetAndMemberLinkOrOrgGkRenderRenderableReaction();
        for(Object item : inputEdgesList) {
            if(item!=null) {
                Class clazz = item.getClass();
                if (        clazz.equals(OrgGkRenderRenderableReaction.class) ){
                    rtn.add(new Edge(item));
                }else if (  clazz.equals(OrgGkRenderFlowLine.class)                  ||
                            clazz.equals(OrgGkRenderEntitySetAndMemberLink.class)    ||
                            clazz.equals(OrgGkRenderEntitySetAndEntitySetLink.class) ||
                            clazz.equals(OrgGkRenderRenderableInteraction.class)     ){
                    rtn.add(new Link(item));
                }else{
                    LogUtil.log(logger, Level.WARN, "[" + outputDiagram.getStableId() + "] contains a not recognised EDGE type - " + clazz.getName() + " [" + clazz.getSimpleName() + "]");
                }
            }
        }
        return rtn;
    }

    private static final Set<String> SETS_TYPES = new HashSet<>(Arrays.asList("OpenSet", "CandidateSet", "DefineSet", "DefinedSet", "EntitySet"));

    private static void fixBrokenRenderableClass(DiagramObject obj){
        String correction = "";
        if (obj.schemaClass.equals("SimpleEntity") && !obj.renderableClass.equals("Chemical")) {
            correction = "Chemical";
        } else if (obj.schemaClass.equals("OtherEntity") && !obj.renderableClass.equals("Entity")) {
            correction = "Entity";
        } else if (obj.schemaClass.equals("Complex") && !obj.renderableClass.equals("Complex")) {
            correction = "Complex";
        } else if (obj.schemaClass.equals("GenomeEncodedEntity") && !obj.renderableClass.equals("Entity")) {
            correction = "Entity";
        } else if (SETS_TYPES.contains(obj.schemaClass) && !obj.renderableClass.equals("EntitySet")) {
            correction = "EntitySet";
        }
        if (!correction.isEmpty()) {
            String message = "[" + outputDiagram.getStableId() + "] contains [" + obj.reactomeId + "] with RenderableClass: [" + obj.renderableClass + "] but this object has schemaClass [" + obj.schemaClass + "]. RenderableClass corrected to [" + correction + "]";
            LogUtil.log(logger, Level.WARN, new LogEntry(LogEntryType.RENDERABLECLASS_MISSMATCH, outputDiagram.getStableId(), "" + obj.reactomeId, message));
            obj.renderableClass = correction;
        }
    }

    /*
    * Convert a string of values into a Set
    * 123 45 23 77 90 54
    */
    private static <E> Set<E> ListToSet( List<E> inputList){
        Set<E> rtn = null;
        if(inputList!=null && !inputList.isEmpty()){
            rtn = new HashSet<>(inputList);
        }
        return rtn;
    }

    /*
     * Convert a string of values into a list of longs
     */
    private static List<Long> extractListFromString(String inputString, String separator){
        List<Long> outputList = null;
        if(inputString!=null) {
            //Split inputString using the separator
            String[] tempStrArray = inputString.split(separator);
            if (tempStrArray.length > 0) {
                outputList = new ArrayList<>();
                for (String tempStr : tempStrArray) {
                    if (tempStr != null && !tempStr.isEmpty()) {
                        //convert String to Integer
                        outputList.add(Long.parseLong(tempStr.trim()));
                    }
                }//end for every string
            }//end if
        }
        return outputList;
    }
}