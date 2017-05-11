package org.reactome.server.diagram.converter.util.report.csv;

import com.opencsv.CSVWriter;
import org.apache.commons.lang.ArrayUtils;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.diagram.converter.util.MapSet;
import org.reactome.server.diagram.converter.util.report.LogEntry;
import org.reactome.server.diagram.converter.util.report.LogEntryType;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class CSVFileWriter {

    private static final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void writeFile(MySQLAdaptor dba, String filename, char separator, LogEntryType logEntryType, MapSet<String, LogEntry> mapSet) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(filename, false), separator);

        //Add header
        writeHeader(writer, logEntryType);

        //Sort by diagramID
        SortedSet<String> keys = new TreeSet<>(mapSet.keySet());

        for (String id : keys) {
            for (LogEntry entry : mapSet.getElements(id)) {
                String[] line;
                if (entry.getSecondaryIds() != null) {
                    line = new String[entry.getSecondaryIds().length + 1];
                    line[0] = id;
                    //Add the rest of the secondary Ids in different columns
                    for (int i = 0; i < entry.getSecondaryIds().length; i++) {
                        line[i + 1] = entry.getSecondaryIds()[i];
                    }
                } else {
                    line = new String[1];
                    line[0] = id;
                }

                String mainId = entry.getMainIdentifier(logEntryType.getId());
                line = (String[]) ArrayUtils.addAll(line, getExtraColumns(mainId, dba));

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

    private static String[] getExtraColumns(String identifier, MySQLAdaptor dba) {
        GKInstance obj = getInstance(dba, identifier);
        String c = "null";
        try {
            GKInstance created = (GKInstance) obj.getAttributeValue(ReactomeJavaConstants.created);
            c = created.getDisplayName();
        } catch (Exception e) { /* Nothing here */ }

        String m = "null";
        try {
            List objects = obj.getAttributeValuesList(ReactomeJavaConstants.modified);
            GKInstance modified = (GKInstance) objects.iterator().next();
            Date latestDate = formatter.parse((String) modified.getAttributeValue(ReactomeJavaConstants.dateTime));
            for (Object object : objects) {
                GKInstance gkInstance = (GKInstance) object;
                Date date = formatter.parse((String) gkInstance.getAttributeValue(ReactomeJavaConstants.dateTime));
                if (latestDate.before(date)) {
                    latestDate = date;
                    modified = gkInstance;
                }
            }
            m = modified.getDisplayName();
        } catch (Exception e) { /* Nothing here */ }

        return new String[]{c, m};
    }

    private static GKInstance getInstance(MySQLAdaptor dba, String identifier) {
        identifier = identifier.trim().split("\\.")[0];
        try {
            GKInstance obj;
            if (identifier.startsWith("REACT")) {
                obj = getInstance(dba, dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, "oldIdentifier", "=", identifier));
            } else if (identifier.startsWith("R-")) {
                obj = getInstance(dba, dba.fetchInstanceByAttribute(ReactomeJavaConstants.StableIdentifier, ReactomeJavaConstants.identifier, "=", identifier));
            } else {
                obj = dba.fetchInstance(Long.parseLong(identifier));
            }
            return obj;
        } catch (Exception e) {
            return null;
        }
    }

    private static GKInstance getInstance(MySQLAdaptor dba, Collection<GKInstance> target) throws Exception {
        if (target == null || target.size() != 1)
            throw new Exception("Many options have been found fot the specified identifier");
        GKInstance stId = target.iterator().next();
        return (GKInstance) dba.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject, ReactomeJavaConstants.stableIdentifier, "=", stId).iterator().next();
    }
}
