package org.reactome.server.diagram.converter.tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.controller.GKInstance2ModelObject;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.server.diagram.converter.exception.DiagramNotFoundException;
import org.reactome.server.diagram.converter.exception.MissingStableIdentifierException;
import org.reactome.server.diagram.converter.graph.DiagramGraphFactory;
import org.reactome.server.diagram.converter.graph.output.Graph;
import org.reactome.server.diagram.converter.input.model.Process;
import org.reactome.server.diagram.converter.input.ProcessFactory;
import org.reactome.server.diagram.converter.layout.LayoutFactory;
import org.reactome.server.diagram.converter.layout.output.Diagram;
import org.reactome.server.diagram.converter.util.DiagramFetcher;
import org.reactome.server.diagram.converter.util.FileUtil;
import org.reactome.server.diagram.converter.util.LogUtil;
import org.reactome.server.diagram.converter.util.TrivialChemicals;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class DiagramConverter {

    public static final boolean RECOVER = false;

    private static Logger logger = Logger.getLogger(DiagramConverter.class.getName());

    private DiagramFetcher diagramFetcher;
    private ProcessFactory processFactory;
    private DiagramGraphFactory graphFactory;
    private TrivialChemicals trivialChemicals;

    public DiagramConverter(MySQLAdaptor dba, GKInstance2ModelObject converter, String trivialChemicalsFile) {
        DatabaseObjectFactory.initializeFactory(dba, converter);
        diagramFetcher = new DiagramFetcher(dba);
        processFactory = new ProcessFactory("/process_schema.xsd");
        graphFactory = new DiagramGraphFactory(dba);

        //Initialise TrivialChemicals Map
        if(FileUtil.fileExists(trivialChemicalsFile)){
            LogUtil.log(logger, Level.INFO, "Using [" + trivialChemicalsFile + "] to annotate trivial chemicals...");
            trivialChemicals = new TrivialChemicals(trivialChemicalsFile);
        }else{
            LogUtil.log(logger, Level.WARN, "Trivial chemicals file was not found at [" + trivialChemicalsFile + "]. Skipping annotation...");
        }
    }

    public Diagram getDiagram(String identifier) throws DiagramNotFoundException, MissingStableIdentifierException {
        GKInstance pathway = diagramFetcher.getInstance(identifier);
        Diagram diagram = getDiagram(pathway);
        if (diagram != null) {
            Graph graph = graphFactory.getGraph(diagram);
            diagram.createShadows(graph.getSubpathways());
            if (trivialChemicals != null) {
                diagram = trivialChemicals.annotateTrivialChemicals(diagram, graphFactory.getPhysicalEntityMap());
            }
        }
        return diagram;
    }

    public Graph getGraph(String identifier) throws Exception {
        return getGraph(getDiagram(identifier));
    }

    public Graph getGraph(Diagram diagram){
        Graph graph = null;
        if (diagram != null) {
            graph = graphFactory.getGraph(diagram);
        }
        return graph;
    }

    @SuppressWarnings("Duplicates")
    private Diagram getDiagram(GKInstance pathway) throws MissingStableIdentifierException, DiagramNotFoundException {
        try {
            String stId  = diagramFetcher.getPathwayStableId(pathway);
            String xml = diagramFetcher.getPathwayDiagramXML(pathway);
            if (xml != null) {
                Process process = processFactory.createProcess(xml, stId);
                return LayoutFactory.getDiagramFromProcess(process, pathway.getDBID(), stId);
            }
        } catch (Exception e) {
            LogUtil.logError(logger,"Conversion failed. The following error occurred while converting the diagram for " + pathway.getDisplayName(), e);
            throw new MissingStableIdentifierException("Conversion failed for the diagram of " + pathway.getDisplayName());
        }
        throw new DiagramNotFoundException("Diagram for " + pathway.getDisplayName() + " couldn't be found");
    }
}
