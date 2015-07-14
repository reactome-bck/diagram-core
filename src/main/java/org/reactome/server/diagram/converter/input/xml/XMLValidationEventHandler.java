package org.reactome.server.diagram.converter.input.xml;

import org.apache.log4j.Logger;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class XMLValidationEventHandler implements ValidationEventHandler{

    private static Logger logger = Logger.getLogger(ProcessFactory.class.getName());

    private StringBuilder internalLogger = null;

    public void setInternalLogger(StringBuilder internalLogger){
        this.internalLogger = internalLogger;
    }

    public StringBuilder getInternalLogger(){
        return internalLogger;
    }


    @Override
    public boolean handleEvent(ValidationEvent event) {
        //TODO use mail API to send an email with the report
        //TODO CLEAN-UP
        if(internalLogger!=null){
            internalLogger.append("XML Parsing Event.")
                    .append(" MESSAGE:  " + event.getMessage())
                    .append(" LINE: " + event.getLocator().getLineNumber())
                    .append(" COLUMN: " + event.getLocator().getColumnNumber())
                    .append(" SEVERITY:  " + event.getSeverity());
        }

//            logger.warn("   XML Parsing Event");
//            logger.warn("      LINE: " + event.getLocator().getLineNumber());
//            logger.warn("      COLUMN: " + event.getLocator().getColumnNumber());
//            logger.warn("      SEVERITY:  " + event.getSeverity());
//            logger.warn("      MESSAGE:  " + event.getMessage());
//            logger.warn("      URL:  " + event.getLocator().getURL());
//            logger.trace(System.lineSeparator());

        return true;
    }
}
