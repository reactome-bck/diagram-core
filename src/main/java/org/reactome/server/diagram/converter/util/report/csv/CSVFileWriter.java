package org.reactome.server.diagram.converter.util.report.csv;

import com.opencsv.CSVWriter;
import org.reactome.server.diagram.converter.util.MapSet;
import org.reactome.server.diagram.converter.util.report.LogEntry;
import org.reactome.server.diagram.converter.util.report.LogEntryType;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class CSVFileWriter {

    public static void writeFile(String filename, char separator, LogEntryType logEntryType, MapSet<String, LogEntry> mapSet) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(filename, false), separator);

        //Add header
        writeHeader(writer, logEntryType);

        //Sort by diagramID
        SortedSet<String> keys = new TreeSet<>(mapSet.keySet());

        for (String id : keys) {
            for (LogEntry entry : mapSet.getElements(id)) {
                String[] line;
                if(entry.getSecondaryIds()!=null) {
                    line = new String[entry.getSecondaryIds().length + 1];
                    line[0] = id;
                    //Add the rest of the secondary Ids in different columns
                    for(int i = 0; i<entry.getSecondaryIds().length; i++){
                        line[i+1] = entry.getSecondaryIds()[i];
                    }
                } else {
                    line = new String[1];
                    line[0] = id;
                }
                writer.writeNext(line, false);
                writer.flush();
            }
        }
        writer.close();
    }


    private static void writeHeader(CSVWriter writer, LogEntryType logEntryType) {
        String[] header = logEntryType.getColumns();
        writer.writeNext(header, false);
    }
}
