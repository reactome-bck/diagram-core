package org.reactome.server.diagram.converter.tools;

import com.martiansoftware.jsap.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.controller.DatabaseObjectHelper;
import org.reactome.core.controller.GKInstance2ModelObject;
import org.reactome.core.factory.DatabaseObjectFactory;
import org.reactome.core.model.Pathway;
import org.reactome.core.model.Species;
import org.reactome.server.diagram.converter.Main;
import org.reactome.server.diagram.converter.graph.DiagramGraphFactory;
import org.reactome.server.diagram.converter.graph.output.Graph;
import org.reactome.server.diagram.converter.input.model.Process;
import org.reactome.server.diagram.converter.input.xml.ProcessFactory;
import org.reactome.server.diagram.converter.layout.LayoutFactory;
import org.reactome.server.diagram.converter.layout.output.Diagram;
import org.reactome.server.diagram.converter.util.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collection;

/**
 * This tool converts XML files containing pathway diagrams into JSON files
 *
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class Convertor2JsonTool {

    private static Logger logger = Logger.getLogger(Convertor2JsonTool.class.getName());

    private static DiagramFetcher diagramFetcher;
    private static ProcessFactory processFactory;
    private static DiagramGraphFactory graphFactory;

    private static TrivialChemicals trivialChemicals;

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                Convertor2JsonTool.class.getName(),
                "A tool to convert the pathway diagrams from XML to JSON format and produce the accompanying graphs",
                new Parameter[]{
                        new UnflaggedOption("tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                                "The tool to use. Options:" + Main.Tool.getOptions())
                        , new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                        "The database host")
                        , new FlaggedOption("database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                        "The reactome database name to connect to")
                        , new FlaggedOption("username", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "username",
                        "The database username")
                        , new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password",
                        "The password to connect to the database")
                        , new FlaggedOption("output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                        "The directory where the converted files are written to.")
                        , new FlaggedOption("trivial", JSAP.STRING_PARSER, "trivialchemicals.txt", JSAP.NOT_REQUIRED, 'r', "trivial",
                        "A file containing the ids and the names of the trivial molecules.")
                        , new QualifiedSwitch("target", JSAP.STRING_PARSER, "ALL", JSAP.NOT_REQUIRED, 't', "target",
                        "Target pathways to convert. Use either comma separated IDs, 'human' for human pathways or 'all' for everything ").setList(true).setListSeparator(',')
                        , new QualifiedSwitch("verbose", JSAP.BOOLEAN_PARSER, "false", JSAP.NOT_REQUIRED, 'v', "verbose",
                        "Requests verbose output.")
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) {
            System.out.println("Usage: java " + Convertor2JsonTool.class.getName());
            System.out.println("\t\t\t\t" + jsap.getUsage() + "\n");
            System.out.println(jsap.getHelp()); // shows full help as well
            LogUtil.log(logger, Level.FATAL, "Diagram conversion failed. Error in the input parameters.");
            System.exit( 1 );
        }

        LogUtil.setVerbose(config.getBoolean("verbose"));

        MySQLAdaptor dba = new MySQLAdaptor(
                config.getString("host"),
                config.getString("database"),
                config.getString("username"),
                config.getString("password")
        );
        dba.setUseCache(false);

        ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");

        GKInstance2ModelObject converter = context.getBean(GKInstance2ModelObject.class);
        DatabaseObjectFactory.initializeFactory(dba, converter);

        DatabaseObjectHelper helper = context.getBean(DatabaseObjectHelper.class);
        helper.setDba(dba);
        diagramFetcher = new DiagramFetcher(dba);
        graphFactory = new DiagramGraphFactory(dba);
        processFactory = new ProcessFactory("/process_schema.xsd");

        //Check if output directory exists
        final String output = FileUtil.checkFolderName(config.getString("output"));
        //Check if target pathways are specified
        String[] target = config.getStringArray("target");

        //Initialise TrivialChemicals Map
        String trivialChemicalsFile = config.getString("trivial");
        if(FileUtil.fileExists(trivialChemicalsFile)){
            LogUtil.log(logger, Level.INFO, "Using [" + trivialChemicalsFile + "] to annotate trivial chemicals...");
            trivialChemicals = new TrivialChemicals(trivialChemicalsFile);
        }else{
            LogUtil.log(logger, Level.WARN, "Trivial chemicals file was not found at [" + trivialChemicalsFile + "]. Skipping annotation...");
        }

        //1. Query for all XML diagrams if required
        if (target[0].toUpperCase().equals("ALL")) {
            //Convert ALL pathways
            LogUtil.log(logger, Level.TRACE, " >> Retrieving Pathway Diagrams ...");

            Collection pathways = dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
            int examinedPathways = 0;
            int pathwaysWithDiagrams = 0;
            LogUtil.log(logger, Level.TRACE, " >> " + pathways.size() + " Pathways Diagrams retrieved... \n");
            for (Object p : pathways) {
                examinedPathways++;
                if(LogUtil.isVerbose()) {
                    if (examinedPathways % 500 == 0) {
                        System.out.format(" >> Processing: %.1f%% - Examined: [%d/%d] - Converted: %d \n",
                                examinedPathways * 100 / (float) pathways.size(),
                                examinedPathways,
                                pathways.size(),
                                pathwaysWithDiagrams);
                    }
                }
                if(convert((GKInstance) p, output)){
                    pathwaysWithDiagrams++;
                }
            }
            LogUtil.log(logger, Level.TRACE, " >> Done");
        } else if(target[0].toUpperCase().equals("HUMAN")){
            //Convert ONLY Human  pathways
            LogUtil.log(logger, Level.TRACE, " >> Retrieving Only HUMAN Pathway Diagrams ...");
            Collection pathways = dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
            int examinedPathways = 0;
            int pathwaysWithDiagrams = 0;
            for (Object p : pathways) {
                Pathway pathway = DatabaseObjectFactory.getDatabaseObject((GKInstance) p);
                pathway.loadDetails();
                Species species = pathway.getSpecies().get(0);

                if(species.getDbId().equals(48887L)){
                    examinedPathways++;
                    if (LogUtil.isVerbose()) {
                        if (examinedPathways % 200 == 0) {
                            System.out.format(" >> Processing: %.1f%% - Examined: [%d/%d] - Converted: %d \n",
                                    examinedPathways * 100 / (float) pathways.size(),
                                    examinedPathways,
                                    pathways.size(),
                                    pathwaysWithDiagrams);
                        }
                    }
                    if (convert((GKInstance) p, output)) {
                        pathwaysWithDiagrams++;
                    }
                }
            }
            LogUtil.log(logger, Level.TRACE, " >> Done");
        }else {
            //Convert only the list of pathways provided by the argument "source"
            LogUtil.log(logger, Level.TRACE, " >> Converting Selected Pathway Diagrams[" + target.length + "]  ...");

            for (String id : target) {
                try {
                    GKInstance pathway = diagramFetcher.getInstance(id);
                    String pathwayStableId = diagramFetcher.getPathwayStableId(pathway);
                    LogUtil.log(logger, Level.TRACE, " >> Pathway Diagram ID: " + pathway.getDBID() + " Stable ID: " + pathwayStableId);

                    if(convert(pathway, output)){
                        LogUtil.log(logger, Level.TRACE, " >> Done");
                    }
                }catch (NullPointerException e){
                    LogUtil.logError(logger, "[" + id + "] conversion failed. The following error occurred while processing pathway diagram:", e);
                }
            }
        }
        // send all email reports
        LogUtil.sendEmailReports();
    }

    private static boolean convert(GKInstance pathway, String outputDir) {
        Diagram diagram = getDiagram(pathway);
        if (diagram != null) {
            Graph graph = graphFactory.getGraph(diagram);
            JsonWriter.serialiseGraph(graph, outputDir);

            diagram.createShadows(graph.getSubpathways());
            if (trivialChemicals != null) {
                diagram = trivialChemicals.annotateTrivialChemicals(diagram, graphFactory.getPhysicalEntityMap());
            }
            JsonWriter.serialiseDiagram(diagram, outputDir);
            return true;
        }
        return false;
    }

    private static Diagram getDiagram(GKInstance pathway) {
        String stId = null;
        try {
            stId = diagramFetcher.getPathwayStableId(pathway);
            String xml = diagramFetcher.getPathwayDiagramXML(pathway);
            if (xml != null) {
                Process process = processFactory.createProcess(xml, stId);
                return LayoutFactory.getDiagramFromProcess(process, pathway.getDBID(), stId);
            }
        } catch (Exception e) {
            LogUtil.logError(logger,"[" + stId + "] conversion failed. The following error occurred while converting pathway diagram:", e);
        }
        return null;
    }
}
