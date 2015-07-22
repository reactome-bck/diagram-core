package org.reactome.server.diagram.converter.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.reactome.server.diagram.converter.graph.output.Graph;
import org.reactome.server.diagram.converter.layout.output.Diagram;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class JsonWriter {

    private static ObjectMapper mapper = null;
    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//        mapper.disable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static void serialiseDiagram(Diagram diagram, String outputDirectory){
        File outJSONFile = new File(outputDirectory + File.separator + diagram.dbId + ".json");
        File outLinkedFile = new File(outputDirectory + File.separator + diagram.stableId + ".json");
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mapper.writeValue(byteArrayOutputStream, diagram);

            FileOutputStream fileOutputStream = new FileOutputStream(outJSONFile, false);
            byteArrayOutputStream.writeTo(fileOutputStream);

            //Create symbolicLink
            if (!Files.exists(Paths.get(outLinkedFile.getAbsolutePath()))) {
                Files.createSymbolicLink(Paths.get(outLinkedFile.getAbsolutePath()), Paths.get(outJSONFile.getName()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void serialiseGraph(Graph graph, String outputDirectory){
        File outGraphFile = new File(outputDirectory + File.separator + graph.getDbId() + ".graph.json");
        File outLinkedFile = new File(outputDirectory + File.separator + graph.getStId() + ".graph.json");

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mapper.writeValue(byteArrayOutputStream, graph);

            FileOutputStream fileOutputStream = new FileOutputStream(outGraphFile, false);
            byteArrayOutputStream.writeTo(fileOutputStream);

            //Create symbolicLink
            if (!Files.exists(Paths.get(outLinkedFile.getAbsolutePath()))) {
                Files.createSymbolicLink(Paths.get(outLinkedFile.getAbsolutePath()), Paths.get(outGraphFile.getName()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
