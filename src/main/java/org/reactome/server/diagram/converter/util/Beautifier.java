package org.reactome.server.diagram.converter.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.reactome.server.diagram.converter.layout.output.DiagramObject;
import org.reactome.server.diagram.converter.util.report.LogEntry;
import org.reactome.server.diagram.converter.util.report.LogEntryType;
import org.reactome.server.diagram.converter.util.report.LogUtil;

import java.util.regex.Pattern;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public abstract class Beautifier {
    private static Logger logger = Logger.getLogger(Beautifier.class.getName());
    private static Pattern NAME_COPIED = Pattern.compile("(\\(name copied from entity in)[\\s\\S]*?[\\)]");
    private static Pattern COORDINATES_COPIED = Pattern.compile("(\\(the coordinates are copied over from)[\\s\\S]*?[\\)]");

    public static <T> T  processName(T input) {
        if(input instanceof DiagramObject) {
            DiagramObject object = (DiagramObject) input;
            String aux = NAME_COPIED.matcher(object.displayName).replaceFirst("");
            aux = COORDINATES_COPIED.matcher(aux).replaceFirst("");
            if(!aux.equals(object.displayName)) {
                log(object.reactomeId+ "");
            }
            object.displayName = aux.trim();
            return (T) object;
        } else if(input instanceof GKInstance) {
            GKInstance object = (GKInstance) input;
            log(object.getDBID() + "");
            String aux = NAME_COPIED.matcher(object.getDisplayName()).replaceFirst("");
            aux = COORDINATES_COPIED.matcher(aux).replaceFirst("");
            if(!aux.equals(object.getDisplayName())) {
                log(object.getDBID() + "");
            }
            object.setDisplayName(aux.trim());
            return (T) object;
        } else {
            return input;
        }
    }

    private static void log(String identifier) {
        LogUtil.log(logger, Level.WARN, new LogEntry(LogEntryType.VERY_LONG_NAMES, null, identifier));
    }
}
