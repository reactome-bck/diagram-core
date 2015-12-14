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
        if(internalLogger!=null){
            internalLogger.append("XML Parsing Event.")
                    .append(" MESSAGE:  ").append(event.getMessage())
                    .append(" LINE: ").append(event.getLocator().getLineNumber())
                    .append(" COLUMN: ").append(event.getLocator().getColumnNumber())
                    .append(" SEVERITY:  ").append(event.getSeverity());
        }
        return true;
    }
}
