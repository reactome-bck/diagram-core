package org.reactome.server.diagram.converter.util;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.DiagramGeneratorFromDB;
import org.gk.pathwaylayout.PathwayDiagramXMLGenerator;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.core.model.Pathway;
import org.reactome.core.model.StableIdentifier;

import java.util.Collection;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class DiagramFetcher {

    public MySQLAdaptor dba;

    public DiagramFetcher(MySQLAdaptor dba) {
        this.dba = dba;
    }

    public GKInstance getInstance(String identifier) throws Exception {
        identifier = identifier.trim().split("\\.")[0];
        if (identifier.startsWith("REACT")){
            return getInstance(dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, "oldIdentifier", "=", identifier));
        }else if (identifier.startsWith("R-")) {
            return getInstance(dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, ReactomeJavaConstants.identifier, "=", identifier));
        } else {
            return dba.fetchInstance(Long.parseLong(identifier));
        }
    }

    private GKInstance getInstance(Collection<GKInstance> target) throws Exception {
        if(target==null || target.size()!=1) throw new Exception("Many options have been found fot the specified identifier");
        GKInstance stId = target.iterator().next();
        return (GKInstance) dba.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject, ReactomeJavaConstants.stableIdentifier, "=", stId).iterator().next();
    }

    public String getPathwayStableId(GKInstance pathway) throws Exception{
        Pathway p = DatabaseObjectFactory.getDatabaseObject(pathway);
        StableIdentifier stableIdentifier = p.getStableIdentifier().load();
        //System.out.println("Pathway Stable Identifier: " + stableIdentifier.getIdentifier());
        return stableIdentifier.getIdentifier();
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
