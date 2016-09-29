package org.reactome.server.diagram.converter.util.report.csv;

import com.opencsv.CSVWriter;
import org.reactome.server.diagram.converter.util.MapSet;
import org.reactome.server.diagram.converter.util.report.LogEntry;

import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class CSVFileWriter {
    private static String[] HEADER_COLUMN = new String[] {"#DiagramID", "#Details"};

    public static void writeFile(String filename, char separator, MapSet<String, LogEntry> mapSet) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(filename, false), separator);

        //Add header
        writeHeader(mapSet, writer);

        for (String id : mapSet.keySet()) {
            String[] line = new String[2];
            line[0] = id;
            line[1] = "";
            for (LogEntry entry : mapSet.getElements(id)) {
                if(entry.getSecondaryId()!=null) {
                    line[1] += entry.getSecondaryId() + " ";
                }
            }

            writer.writeNext(line, false);
            writer.flush();
        }
        writer.close();
    }


    private static void writeHeader(MapSet<String, LogEntry> mapSet, CSVWriter writer) {
        String[] header = new String[] {HEADER_COLUMN[0], HEADER_COLUMN[1]};
        writer.writeNext(header, false);
    }
}
