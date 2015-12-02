package org.reactome.server.diagram.converter.tools;

import com.martiansoftware.jsap.*;
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
import org.reactome.server.diagram.converter.util.DiagramFetcher;
import org.reactome.server.diagram.converter.util.FileUtil;
import org.reactome.server.diagram.converter.util.JsonWriter;
import org.reactome.server.diagram.converter.util.TrivialChemicals;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

/**
 * This tool converts XML files containing pathway diagrams into JSON files
 *
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class Convertor2JsonTool {

    private static Logger logger = Logger.getLogger(Convertor2JsonTool.class.getName());
    public static boolean VERBOSE = false;

    private static DiagramFetcher diagramFetcher;
    private static ProcessFactory processFactory;
    private static DiagramGraphFactory graphFactory;

    private static TrivialChemicals trivialChemicals;

    private static final String RESTFUL_API = "http://reactomedev.oicr.on.ca/ReactomeRESTfulAPI/RESTfulWS/pathwayDiagram/";

    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = new SimpleJSAP(
                Convertor2JsonTool.class.getName(),
                "A set of tools to convert the pathway diagrams from XML to JSON format",
                new Parameter[]{
                        new UnflaggedOption("tool", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
                                "The tool to use. Options:" + Main.Tool.getOptions())
                        , new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.NOT_REQUIRED, 'h', "host",
                        "The database host")
                        , new FlaggedOption("database", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'd', "database",
                        "The reactome database name to connect to")
                        , new FlaggedOption("username", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'u', "username",
                        "The database user")
                        , new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password",
                        "The password to connect to the database")
                        , new FlaggedOption("output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output",
                        "The directory where the converted files are written to.")
                        , new QualifiedSwitch("target", JSAP.STRING_PARSER, "ALL", JSAP.NOT_REQUIRED, 't', "target",
                        "Source pathways to convert").setList(true).setListSeparator(',')
                        , new QualifiedSwitch("verbose", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'v', "verbose",
                        "Requests verbose output.")
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) {
            System.out.println("Usage: java " + Convertor2JsonTool.class.getName());
            System.out.println("\t\t\t\t" + jsap.getUsage() + "\n");
            System.out.println(jsap.getHelp()); // shows full help as well
            System.exit( 1 );
        }

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
        processFactory = new ProcessFactory("src/main/resources/process_schema.xsd");

        //Check if output directory exists
        final String output = FileUtil.checkFolderName(config.getString("output"));
        //Check if target pathways are specified
        String[] target = config.getStringArray("target");
        VERBOSE = config.getBoolean("verbose");

        //Initialise TrivialChemicals Map
        trivialChemicals = new TrivialChemicals("src/main/resources/trivialchemicals.txt");

        //1. Query for all XML diagrams if required
        if (target[0].toUpperCase().equals("ALL")) {
            //Convert ALL pathways
            printMessage(" >> Retrieving Pathway Diagrams ...");

            Collection pathways = dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
            int examinedPathways = 0;
            int pathwaysWithDiagrams = 0;
            printMessage(" >> " + pathways.size() + " Pathways Diagrams retrieved... \n");
            for (Object p : pathways) {
                examinedPathways++;
                if(VERBOSE) {
                    if (examinedPathways % 500 == 0) {
                        System.out.format("\r >> Processing : %.1f%% - Converted: [%d / %d] ",
                                examinedPathways * 100 / (float) pathways.size(),
                                pathwaysWithDiagrams,
                                examinedPathways);
                    }
                }
                if(convert((GKInstance) p, output)){
                    pathwaysWithDiagrams++;
                }
            }
            printMessage(" >> Done");
        } else if(target[0].toUpperCase().equals("HUMAN")){
            //Convert ONLY Human  pathways
            printMessage(" >> Retrieving Only HUMAN Pathway Diagrams ...");
            Collection pathways = dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
            int examinedPathways = 0;
            int pathwaysWithDiagrams = 0;
            for (Object p : pathways) {
                Pathway pathway = DatabaseObjectFactory.getDatabaseObject((GKInstance) p);
                pathway.loadDetails();
                Species species = pathway.getSpecies().get(0);

                if(species.getDbId().equals(48887L)){
                    examinedPathways++;
                    if (VERBOSE) {
                        if (examinedPathways % 200 == 0) {
                            System.out.format("\r >> Processing : %.1f%% - Converted: [%d / %d] ",
                                    examinedPathways * 100 / (float) pathways.size(),
                                    pathwaysWithDiagrams,
                                    examinedPathways);
                        }
                    }
                    if (convert((GKInstance) p, output)) {
                        pathwaysWithDiagrams++;
                    }
                }
            }
            printMessage(" >> Done");
        }else {
            //Convert only the list of pathways provided by the argument "source"
            printMessage(" >> Converting Selected Pathway Diagrams[" + target.length + "]  ...");

            for (String id : target) {
                try {
                    GKInstance pathway = diagramFetcher.getPathwayInstance(Long.parseLong(id));
                    String pathwayStableId = diagramFetcher.getPathwayStableId(pathway);
                    printMessage(" >> Pathway Diagram ID: " + pathway.getDBID() + " Stable ID: " + pathwayStableId);

                    if(convert(pathway, output)){
                        printMessage(" >> Done");
                    }
                }catch (NullPointerException e){
                    e.printStackTrace();
                    printError("WTH " + e.getMessage());
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private static boolean convert(GKInstance pathway, String outputDir){
        Diagram diagram = getDiagram(pathway);
        if(diagram!=null) {
            Graph graph = graphFactory.getGraph(diagram);
            JsonWriter.serialiseGraph(graph, outputDir);

            diagram.createShadows(graph.getSubpathways());
            diagram = trivialChemicals.annotateTrivialChemicals(diagram, graphFactory.getPhysicalEntityMap());
            JsonWriter.serialiseDiagram(diagram, outputDir);
            return true;
        }
        return false;
    }

    private static Diagram getDiagram(GKInstance pathway){
        try {
            String stId = diagramFetcher.getPathwayStableId(pathway);
            String xml; //diagramFetcher.getPathwayDiagramXML(pathway);
            // For the time being we use the RESTFul API to retrieve the XML as
            // DiagramFetcher does not provide the same diagram ids
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(
                    RESTFUL_API + pathway.getDBID() + "/XML",
                    String.class);
            xml = response.getBody();
            if(xml!=null) {
                Process process = processFactory.createProcess(xml, stId);
                return LayoutFactory.getDiagramFromProcess(process, pathway.getDBID(), stId);
            }
        } catch (Exception e) {
            printError(e);
        }
        return null;
    }

    private static void printMessage(String message){
        if(VERBOSE){
            System.out.println(message);
        }
        logger.trace(message);
    }

    private static void printError(Throwable error){
        if(VERBOSE){
            System.err.println(error.getMessage());
            error.printStackTrace();
        }
        logger.error(error.getMessage(), error);
    }

    private static void printError(String message){
        if(VERBOSE){
            System.err.println(message);
        }
        logger.error(message);
    }
}
