package org.reactome.server.diagram.converter.util;

import org.reactome.server.diagram.converter.graph.model.PhysicalEntityNode;
import org.reactome.server.diagram.converter.layout.output.Diagram;
import org.reactome.server.diagram.converter.layout.output.Node;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if(diagram.getNodes()==null)
            return diagram;
        for (Node node : diagram.getNodes()) {
            if(node.reactomeId!=null){
                PhysicalEntityNode pe  = physicalEntitiesMap.get(node.reactomeId);
                if(pe==null) {
                    continue;
                }
                String schemaClass = pe.getSchemaClass();
                if(schemaClass!=null && schemaClass.equals("SimpleEntity") ) {
                    String chebiId = pe.getIdentifier();
                    if (isTrivial(chebiId)) {
                        node.trivial = true;
                    }
                }
            }
        }
        
        return diagram;
    }
    
    private void initialise(String fileLocation){
        try {
            List<String>  lines = FileUtil.readTextFile(fileLocation);
            for (String line : lines) {
                if(line==null || line.isEmpty()) { continue; }
                String[] aux = line.split("\\t");
                if(aux!=null && aux.length>=2){
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
