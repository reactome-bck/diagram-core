package org.reactome.server.diagram.converter.input.xml;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class XMLValidationEventHandler implements ValidationEventHandler{
    private StringBuilder internalBuffer = null;

    public void setInternalBuffer(StringBuilder internalBuffer){
        this.internalBuffer = internalBuffer;
    }

    public StringBuilder getInternalBuffer(){
        return internalBuffer;
    }

    @Override
    public boolean handleEvent(ValidationEvent event) {
        if(internalBuffer !=null){
            internalBuffer.append("XML Parsing Event:")
                    .append(" MESSAGE:  ").append(event.getMessage())
                    .append(" LINE: ").append(event.getLocator().getLineNumber())
                    .append(" COLUMN: ").append(event.getLocator().getColumnNumber())
                    .append(" SEVERITY:  ").append(event.getSeverity());
        }
        return true;
    }
}
