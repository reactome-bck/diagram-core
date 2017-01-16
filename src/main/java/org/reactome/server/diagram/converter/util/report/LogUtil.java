package org.reactome.server.diagram.converter.util.report;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SMTPAppender;
import org.reactome.server.diagram.converter.util.MapSet;
import org.reactome.server.diagram.converter.util.report.csv.CSVFileWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class LogUtil{
    private static boolean VERBOSE = false;

    private static Map<LogEntryType, MapSet<String, LogEntry>> entriesMap = new HashMap<>();

    public static boolean isVerbose() {
        return LogUtil.VERBOSE;
    }

    public static void setVerbose(boolean VERBOSE) {
        LogUtil.VERBOSE = VERBOSE;
    }

    public static void log(Logger logger, Level level, LogEntry entry){
        logger.log(level, entry.getMessage());
        processEntry(entry);
    }

    public static void log(Logger logger, Level level, String message){
        if(message!=null && !message.isEmpty()) {
            logger.log(level, message);
            if(VERBOSE) {
                System.out.println(message);
            }
        }
    }

    public static void logSilently(Logger logger, Level level, String message){
        if(message!=null && !message.isEmpty()) {
            logger.log(level, message);
        }
    }

    public static void logError(Logger logger, LogEntry entry, Throwable error){
        logError(logger, entry.getMessage(), error);
        processEntry(entry);
    }

    public static void logError(Logger logger, String message, Throwable error) {
        if (VERBOSE) {
            System.err.println(message);
            System.err.println(error.getMessage());
            error.printStackTrace();
            System.err.println();//an empty line
        }
        logger.error(message);
        logger.error(error.getMessage(), error);
        logger.error(""); //an empty line
    }

    public static void sendEmailReports() {
        Appender appender = Logger.getRootLogger().getAppender("MAIL_REPORT");
        if (appender != null) {
            SMTPAppender smtpAppender = (SMTPAppender) appender;
            String curDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            smtpAppender.setSubject("Diagrams Conversion - Report - " + curDate);
            smtpAppender.setSendOnClose(true);
            smtpAppender.close();
        }

    }

    private static void processEntry(LogEntry entry) {
        MapSet<String, LogEntry> mapSet = entriesMap.get(entry.getType());
        if(mapSet == null) {
            mapSet = new MapSet<>();
            entriesMap.put(entry.getType(), mapSet);
        }
        mapSet.add(entry.getId(), entry);
    }

    public static void writeCSVFiles() {
        Set<LogEntryType> types = entriesMap.keySet();
        if(types!=null && !types.isEmpty()) {
            String curDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            for (LogEntryType type : types) {
                MapSet<String, LogEntry> mapSet = entriesMap.get(type);
                try {

                    CSVFileWriter.writeFile(type.name() + "_" + curDate + ".csv", ',', type, mapSet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
