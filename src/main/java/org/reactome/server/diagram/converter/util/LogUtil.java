package org.reactome.server.diagram.converter.util;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SMTPAppender;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class LogUtil{
    private static boolean VERBOSE = false;

    public static boolean isVerbose() {
        return LogUtil.VERBOSE;
    }

    public static void setVerbose(boolean VERBOSE) {
        LogUtil.VERBOSE = VERBOSE;
    }

    public static void log(Logger logger, Level level, String message){
        if(VERBOSE) {
            if(level.isGreaterOrEqual(Level.WARN)){
                System.err.println(message);
            }else{
                System.out.println(message);
            }
        }
        logger.log(level, message);
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
            //TODO put timestamp on subject
            smtpAppender.setSendOnClose(true);
            smtpAppender.close();
        }
    }
}
