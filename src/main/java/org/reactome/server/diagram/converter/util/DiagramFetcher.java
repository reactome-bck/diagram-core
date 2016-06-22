package org.reactome.server.diagram.converter.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.DiagramGeneratorFromDB;
import org.gk.pathwaylayout.PathwayDiagramXMLGenerator;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.core.model.Pathway;
import org.reactome.core.model.StableIdentifier;
import org.reactome.server.diagram.converter.exception.DiagramNotFoundException;
import org.reactome.server.diagram.converter.tools.DiagramConverter;

import java.util.Collection;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class DiagramFetcher {

    private static Logger logger = Logger.getLogger(DiagramFetcher.class.getName());
    public MySQLAdaptor dba;

    public DiagramFetcher(MySQLAdaptor dba) {
        this.dba = dba;
    }

    public GKInstance getInstance(String identifier) throws DiagramNotFoundException {
        identifier = identifier.trim().split("\\.")[0];
        try {
            if (identifier.startsWith("REACT")) {
                return getInstance(dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, "oldIdentifier", "=", identifier));
            } else if (identifier.startsWith("R-")) {
                return getInstance(dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, ReactomeJavaConstants.identifier, "=", identifier));
            } else {
                return dba.fetchInstance(Long.parseLong(identifier));
            }
        } catch (Exception e){
            throw new DiagramNotFoundException("No diagram found for " + identifier);
        }
    }

    private GKInstance getInstance(Collection<GKInstance> target) throws Exception {
        if(target==null || target.size()!=1) throw new Exception("Many options have been found fot the specified identifier");
        GKInstance stId = target.iterator().next();
        return (GKInstance) dba.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject, ReactomeJavaConstants.stableIdentifier, "=", stId).iterator().next();
    }

    public String getPathwayStableId(GKInstance pathway) throws Exception {
        Pathway p = DatabaseObjectFactory.getDatabaseObject(pathway);
        try {
            StableIdentifier stableIdentifier = p.getStableIdentifier().load();
            return stableIdentifier.getIdentifier();
        } catch (Exception e) {
            if(DiagramConverter.RECOVER) {
                return "" + p.getDbId();
            }
            String msg = "No stable identifier found for pathway " + p.getDbId();
            LogUtil.log(logger, Level.ERROR, msg);
            throw new Exception(msg);
        }
    }

    @SuppressWarnings("unused")
    public String getPathwayDiagramXML(Long pathwayId) throws Exception {
        return getPathwayDiagramXML(dba.fetchInstance(pathwayId));
    }

    public String getPathwayDiagramXML(GKInstance pathway) throws Exception {
        DiagramGeneratorFromDB diagramHelper = new DiagramGeneratorFromDB();
        diagramHelper.setMySQLAdaptor(dba);
        // Find PathwayDiagram
        GKInstance diagram = diagramHelper.getPathwayDiagram(pathway);
        if(diagram!=null){
            PathwayDiagramXMLGenerator xmlGenerator = new PathwayDiagramXMLGenerator();
            return xmlGenerator.generateXMLForPathwayDiagram(diagram, pathway);
        }
        return null;
    }
}
