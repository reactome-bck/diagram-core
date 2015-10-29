package org.reactome.server.diagram.converter.util;

import org.reactome.server.diagram.converter.graph.model.PhysicalEntityNode;
import org.reactome.server.diagram.converter.layout.output.Diagram;
import org.reactome.server.diagram.converter.layout.output.Edge;
import org.reactome.server.diagram.converter.layout.output.Node;
import org.reactome.server.diagram.converter.layout.output.ReactionPart;

import java.io.IOException;
import java.util.*;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class TrivialChemicals {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TrivialChemicals.class.getName());

    private Map<String, String> trivialMap = new HashMap<>();

    public TrivialChemicals(String trivialMoleculesFile) {
        initialise(trivialMoleculesFile);
    }

    public Diagram annotateTrivialChemicals(Diagram diagram, Map<Long, PhysicalEntityNode> physicalEntitiesMap){
        if(diagram.getNodes()==null)  return diagram;

        Map<Long, Node> trivialMolecules = new HashMap<>();
        for (Node node : diagram.getNodes()) {
            if(node.reactomeId!=null){
                PhysicalEntityNode pe  = physicalEntitiesMap.get(node.reactomeId);
                if(pe!=null) {
                    String schemaClass = pe.getSchemaClass();
                    if (schemaClass != null && schemaClass.equals("SimpleEntity")) {
                        String chebiId = pe.getIdentifier();
                        if (isTrivial(chebiId)) {
                            node.trivial = true;
                            trivialMolecules.put(node.id, node);
                        }
                    }
                }
            }
        }

        for (Edge edge : diagram.getEdges()) {
            checkingTrivialMolecules(trivialMolecules, edge.inputs);
            checkingTrivialMolecules(trivialMolecules, edge.outputs);
//            checkingTrivialMolecules(trivialMolecules, edge.catalysts);
        }

        return diagram;
    }

    /**
     * It removes the trivial flag to those nodes that even though are trivial, play an important role in the reaction.
     * @param trivialMolecules map of the glyphs id to molecules nodes
     * @param parts the target parts of a reaction
     */
    private void checkingTrivialMolecules(Map<Long, Node> trivialMolecules, List<ReactionPart> parts){
        boolean allTrivial = true;
        for (ReactionPart input : parts) {
            Node aux = trivialMolecules.get(input.id);
            allTrivial &= (aux!=null && aux.trivial!=null);
        }
        if(allTrivial){
            for (ReactionPart input : parts) {
                Node trivial = trivialMolecules.get(input.id);
                if(trivial!=null) trivial.trivial=null;
            }
        }
    }
    
    private void initialise(String fileLocation){
        try {
            List<String>  lines = FileUtil.readTextFile(fileLocation);
            for (String line : lines) {
                if(line==null || line.isEmpty()) { continue; }
                String[] aux = line.split("\\t");
                if(aux.length >= 2){
                    trivialMap.put(aux[0].trim(), aux[1].trim());
                }
            }
        } catch (IOException e) {
            logger.error("Error reading trivial chemicals' file: " + fileLocation);
            System.err.println("Error reading trivial chemicals' file: " + fileLocation);
            e.printStackTrace();
        }
    }

    public boolean isTrivial(String identifier){
        return trivialMap.containsKey(identifier);
    }

}
