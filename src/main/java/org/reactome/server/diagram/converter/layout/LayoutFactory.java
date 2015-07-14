package org.reactome.server.diagram.converter.layout;

import org.reactome.server.diagram.converter.input.model.*;
import org.reactome.server.diagram.converter.input.model.Process;
import org.reactome.server.diagram.converter.input.model.Properties;
import org.reactome.server.diagram.converter.layout.output.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class LayoutFactory {

    private static Logger logger = Logger.getLogger(LayoutFactory.class.getName());

    public static Diagram getDiagramFromProcess(Process inputProcess, Long dbId, String stId) {
        Diagram outputDiagram = null;
        if(inputProcess!=null) {
            outputDiagram = new Diagram();

            //Parse General fields
            outputDiagram.nextId = inputProcess.getNextId().longValue();
            outputDiagram.reactomeDiagramId = inputProcess.getReactomeDiagramId().longValue();

            outputDiagram.dbId = dbId;
            outputDiagram.stableId = stId;

            outputDiagram.isDisease = inputProcess.isIsDisease();
            outputDiagram.forNormalDraw = inputProcess.isForNormalDraw();
            outputDiagram.hideCompartmentInName = inputProcess.isHideCompartmentInName();

            //Parse properties
            Map<String, Serializable> properties =  extractProperties(inputProcess.getProperties());
            outputDiagram.displayName = (properties.get("displayName")!=null) ? properties.get("displayName").toString() : null;
            outputDiagram.isChanged =  (properties.get("isChanged") != null) ? Boolean.valueOf( properties.get("isChanged").toString() ): null;

            //Parse General list-fields
            outputDiagram.normalComponents = ListToSet(extractIntegerListFromString(inputProcess.getNormalComponents(), ","));
            outputDiagram.diseaseComponents = ListToSet(extractIntegerListFromString(inputProcess.getDiseaseComponents(), ","));
            outputDiagram.crossedComponents = ListToSet(extractIntegerListFromString(inputProcess.getCrossedComponents(), ","));
            outputDiagram.notFadeOut = ListToSet(extractIntegerListFromString(inputProcess.getOverlaidComponents(), ","));
            outputDiagram.lofNodes = ListToSet(extractIntegerListFromString(inputProcess.getLofNodes(), ","));

            //Parse Nodes
            for (NodeCommon nodeCommon : extractNodesList(inputProcess.getNodes())) {
                if(nodeCommon instanceof Node) {
                    if(outputDiagram.isDisease==null || outputDiagram.isDisease==false){
                        outputDiagram.nodes.add((Node) nodeCommon);
                    }else if(outputDiagram.shouldBeIncluded(((Node) nodeCommon).id.intValue())) {
                        //If it is a disease diagram then only the nodes of the 5 lists are kept
                        outputDiagram.nodes.add((Node) nodeCommon);
                    }
                }else if(nodeCommon instanceof Note){
                    outputDiagram.notes.add((Note) nodeCommon);
                }else if(nodeCommon instanceof Compartment){
                    outputDiagram.compartments.add((Compartment) nodeCommon);
                }
            }

            //Parse Edges
            for (EdgeCommon edgeCommon : extractEdgesList(inputProcess.getEdges())) {
                if(edgeCommon instanceof Link){
                    outputDiagram.links.add((Link) edgeCommon);
                }else if(edgeCommon instanceof Edge){
                    if(outputDiagram.isDisease==null || outputDiagram.isDisease==false){
                        //If it is a disease diagram then only the nodes of the 5 lists are kept
                        outputDiagram.edges.add((Edge) edgeCommon);
                    }else if(outputDiagram.shouldBeIncluded(((Edge) edgeCommon).id.intValue())) {
                        outputDiagram.edges.add((Edge) edgeCommon);
                    }
                }
            }

            //Clean up links - delete those with no inputs/outputs
            outputDiagram.cleanUpOrphanLinks();

            //Parse Pathways
            outputDiagram.pathways = inputProcess.getPathways().toString();

            //Set universal min max
            outputDiagram.setUniversalBoundaries();

            //Generate node connectors
            outputDiagram.setConnectors();

            //Calculate node boundaries
            outputDiagram.setNodesBoundaries();

            //Generate the arrow at the backbones if needed
            outputDiagram.setBackboneArrows();

            //Generate the arrow of links
            outputDiagram.setLinkArrows();

            //Process the crossed components
            outputDiagram.setCrossedComponents();

            //Set the faded out components
            outputDiagram.setOverlaidObjects(outputDiagram.notFadeOut);

            //Remove the reactions overlaid by disease reactions
            outputDiagram.normalEventsOverlaidByDiseaseEventsCleanup();

            //Process disease components
            outputDiagram.setDiseaseComponents();

            //Clean up links - delete those with no inputs/outputs
            outputDiagram.cleanUpOrphanLinks();

            //Annotate and fix empty or incomplete compartments
            outputDiagram.fixCompartments();

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
                    rtn.add(new Node(inputNode));
                }else if(clazz.equals(OrgGkRenderNote.class) ){
                    Note note = new Note(inputNode);
                    if(!note.displayName.equals("Note")){rtn.add(note);}
                }else if (clazz.equals(OrgGkRenderRenderableCompartment.class) ){
                    rtn.add(new Compartment(inputNode));
                }else{
                    ///TODO enable it
                    System.err.println(" - NOT RECOGNISED NODE TYPE - " + clazz.getName() + " [" + clazz.getSimpleName() + "]" );
                    logger.warning(" - NOT RECOGNISED NODE TYPE - " + clazz.getName() + " [" + clazz.getSimpleName() + "]");
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
                }else if(item instanceof Boolean){
                    props.put("isChanged", item);
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
                    ///TODO enable it
                    System.err.println(" - NOT RECOGNISED EDGE TYPE - " + clazz.getName() + " [" + clazz.getSimpleName() + "]" );
                    logger.warning(" - NOT RECOGNISED NODE TYPE - " + clazz.getName() + " [" + clazz.getSimpleName() + "]");
                }
            }
        }
        return rtn;
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
     * Convert a string of values into a list of integers
     * 123 45 23 77 90 54
     */
    private static List<Integer> extractIntegerListFromString(String inputString, String separator){
        List<Integer> outputList = null;
        if(inputString!=null) {
            //Split inputString using the separator
            String[] tempStrArray = inputString.split(separator);
            if (tempStrArray.length > 0) {
                outputList = new ArrayList<>();
                for (String tempStr : tempStrArray) {
                    if (tempStr != null && !tempStr.isEmpty()) {
                        //convert String to Integer
                        outputList.add(Integer.parseInt(tempStr.trim()));
                    }
                }//end for every string
            }//end if
        }
        return outputList;
    }
}