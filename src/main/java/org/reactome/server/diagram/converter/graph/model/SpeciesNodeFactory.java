package org.reactome.server.diagram.converter.graph.model;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class SpeciesNodeFactory {
    private static Logger logger = Logger.getLogger(SpeciesNodeFactory.class.getName());


    public static long HUMAN_DB_ID = 48887L;
    public static String HUMAN_STR = "Homo sapiens";

    private static Map<Long, SpeciesNode> speciesMap = new HashMap<Long, SpeciesNode>();

    public static SpeciesNode getSpeciesNode(GKInstance physicalEntity) {
        if (!physicalEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
            return null;
        }
        List species;
        try {
            species = physicalEntity.getAttributeValuesList(ReactomeJavaConstants.species);
            if (species.isEmpty()) return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

        GKInstance aux = (GKInstance) species.get(0);
        long speciesID = aux.getDBID();
        String name = aux.getDisplayName();

        SpeciesNode speciesNode = speciesMap.get(speciesID);
        if (speciesNode == null) {
            speciesNode = new SpeciesNode(speciesID, name);
            speciesMap.put(speciesID, speciesNode);
        }
        return speciesNode;
    }

    public static SpeciesNode getHumanNode() {
        return new SpeciesNode(HUMAN_DB_ID, HUMAN_STR);
    }
}
