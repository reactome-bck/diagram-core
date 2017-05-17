package org.reactome.server.diagram.converter.util;

import org.apache.log4j.Logger;
import org.reactome.server.diagram.converter.graph.model.PhysicalEntityNode;
import org.reactome.server.diagram.converter.layout.output.*;

import java.io.IOException;
import java.util.*;

/**
 * This class is responsible for annotating the trivial molecules in every diagram.
 * The list of trivial molecules is specified in a tsv file (trivialChemicals.txt)
 * with 2 columns. The first column is mandatory and has the identifier of the molecule
 * (e.g. chEBI Id) while the second column is optional and contains the name of the molecule.
 *
 * For example:
 *      15377	H2O
 *      16761	ADP
 *      15422	ATP
 *
 * NOTE: To function properly, the class requires a map of physical entities produced by the
 * {@link org.reactome.server.diagram.converter.graph.DiagramGraphFactory DiagramGraphFactory}
 *
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class TrivialChemicals {
    private static Logger logger = Logger.getLogger(TrivialChemicals.class.getName());
    private Map<String, String> trivialMap = new HashMap<>();

    public TrivialChemicals(String trivialMoleculesFile) {
        initialise(trivialMoleculesFile);
    }

    public Diagram annotateTrivialChemicals(Diagram diagram, Map<Long, PhysicalEntityNode> physicalEntitiesMap){
        if(diagram.getNodes() == null || diagram.getNodes().isEmpty())  return diagram;

        Map<Long, Node> trivialMolecules = new HashMap<>();
        for (Node node : diagram.getNodes()) {
            if(node.reactomeId!=null){
                PhysicalEntityNode pe  = physicalEntitiesMap.get(node.reactomeId);
                if(pe!=null) {
                    String schemaClass = pe.getSchemaClass();
                    if (schemaClass != null && schemaClass.equals("SimpleEntity")) {  // Check only chemicals
                        if (isTrivial(pe.getIdentifier())) {
                            node.trivial = true;
                            trivialMolecules.put(node.id, node);
                        }
                    }
                }
            }
        }

        if (!trivialMolecules.isEmpty()) {
            // However, some molecules marked as trivial
            // play an important role in certain reactions
            Set<Node> noTrivial = new HashSet<>();
            for (Edge edge : diagram.getEdges()) {
                noTrivial.addAll(getNotTrivialMolecules(trivialMolecules, edge.inputs));
                noTrivial.addAll(getNotTrivialMolecules(trivialMolecules, edge.outputs));
            }

            // Trivial molecules that are connected to
            // flow lines are always visible
            for (Link link : diagram.getLinks()) {
                noTrivial.addAll(getNotTrivialMolecules(trivialMolecules, link.inputs));
                noTrivial.addAll(getNotTrivialMolecules(trivialMolecules, link.outputs));
            }

            for (Node node : noTrivial) {
                node.trivial = null;
            }
        }
        return diagram;
    }

    /**
     * It removes the trivial flag to those nodes that even though are trivial,
     * play an important role in the reaction.
     *
     * @param trivialMolecules map of the glyphs id to molecules nodes
     * @param parts the target parts of a reaction
     */
    private Set<Node> getNotTrivialMolecules(Map<Long, Node> trivialMolecules, List<ReactionPart> parts){
        Set<Node> rtn = new HashSet<>();
        if (parts != null) {
            boolean allTrivial = true;
            for (ReactionPart input : parts) {
                Node aux = trivialMolecules.get(input.id);
                allTrivial &= (aux != null && aux.trivial != null);
            }
            if (allTrivial) {
                for (ReactionPart input : parts) {
                    Node trivial = trivialMolecules.get(input.id);
                    if (trivial != null) rtn.add(trivial);
                }
            }
        }
        return rtn;
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

    private boolean isTrivial(String identifier){
        return trivialMap.containsKey(identifier);
    }
}
