package org.reactome.server.diagram.converter.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.reactome.server.diagram.converter.graph.output.Graph;
import org.reactome.server.diagram.converter.layout.output.Diagram;
import org.reactome.server.diagram.converter.layout.output.Node;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class JSONReader {
    private static ObjectMapper mapper = null;
    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    private static<T> T deserialise(File file, Class<T> target) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        return mapper.readValue(bufferedInputStream, target);
    }

    public static void main(String[] args) throws Exception {
        File f = new File("/Users/ksidis/diagram/static/15869.json");
        Diagram d = mapper.readValue(f, Diagram.class);
        System.out.println(d.getNodes() );
//        String jsontest = "{\n" +
//                "    \"id\" : 3075,\n" +
//                "    \"reactomeId\" : 6788801,\n" +
//                "    \"displayName\" : \"(d)NDPs\",\n" +
//                "    \"schemaClass\" : \"DefinedSet\",\n" +
//                "    \"renderableClass\" : \"EntitySet\",\n" +
//                "    \"position\" : {\n" +
//                "      \"x\" : 2447,\n" +
//                "      \"y\" : 1596\n" +
//                "    },\n" +
//                "    \"minX\" : 2362,\n" +
//                "    \"maxX\" : 2477,\n" +
//                "    \"minY\" : 1585,\n" +
//                "    \"maxY\" : 1607,\n" +
//                "    \"prop\" : {\n" +
//                "      \"x\" : 2418,\n" +
//                "      \"y\" : 1585,\n" +
//                "      \"width\" : 59,\n" +
//                "      \"height\" : 22\n" +
//                "    },\n" +
//                "    \"textPosition\" : {\n" +
//                "      \"x\" : 2420,\n" +
//                "      \"y\" : 1585\n" +
//                "    },\n" +
//                "    \"connectors\" : [ {\n" +
//                "      \"edgeId\" : 3069,\n" +
//                "      \"type\" : \"OUTPUT\",\n" +
//                "      \"segments\" : [ {\n" +
//                "        \"from\" : {\n" +
//                "          \"x\" : 2412,\n" +
//                "          \"y\" : 1595\n" +
//                "        },\n" +
//                "        \"to\" : {\n" +
//                "          \"x\" : 2362,\n" +
//                "          \"y\" : 1595\n" +
//                "        }\n" +
//                "      } ],\n" +
//                "      \"stoichiometry\" : {\n" +
//                "        \"value\" : 1\n" +
//                "      },\n" +
//                "      \"endShape\" : {\n" +
//                "        \"a\" : {\n" +
//                "          \"x\" : 2405,\n" +
//                "          \"y\" : 1599\n" +
//                "        },\n" +
//                "        \"b\" : {\n" +
//                "          \"x\" : 2412,\n" +
//                "          \"y\" : 1595\n" +
//                "        },\n" +
//                "        \"c\" : {\n" +
//                "          \"x\" : 2405,\n" +
//                "          \"y\" : 1591\n" +
//                "        },\n" +
//                "        \"type\" : \"ARROW\"\n" +
//                "      }\n" +
//                "    } ]\n" +
//                "  }";
//        Node d = mapper.readValue(jsontest, Node.class);

    }
}
