package org.reactome.server.diagram.converter.input.xml;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reactome.server.diagram.converter.input.model.Process;
import org.reactome.server.diagram.converter.util.LogUtil;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

/**
 * This class is responsible for deserializing the diagram from its XML
 * format into a{@link org.reactome.server.diagram.converter.input.model.Process Process} object.
 *
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */

public class ProcessFactory {
    private static Logger logger = Logger.getLogger(ProcessFactory.class.getName());

    private Unmarshaller jaxbUnmarshaller = null;
    private XMLValidationEventHandler xmlValidationEventHandler = null;

    public ProcessFactory(String schemaLocation) {
        try {
            xmlValidationEventHandler = new XMLValidationEventHandler();
            JAXBContext jaxbContext = JAXBContext.newInstance(Process.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            jaxbUnmarshaller.setSchema(SchemaProvider.getSchema(schemaLocation));
            jaxbUnmarshaller.setEventHandler(xmlValidationEventHandler);
        } catch (JAXBException e) {
            LogUtil.logError(logger, "Error instantiating ProcessFactory:", e);
        } catch (SAXException e) {
            LogUtil.logError(logger, "Error instantiating ProcessFactory schema:", e);
        }
    }

    public Process createProcess(String xmlContent, String stId){
        Process process = null;
        try {
            process = deserializeXML(xmlContent);

            //Check for events during deserialization
            if (!xmlValidationEventHandler.getEvents().isEmpty()) {
                LogUtil.log(logger, Level.ERROR, "[" + stId + "] Error(s) in XML deserialisation of [" + process.getProperties().getIsChangedOrDisplayName().get(0) + "]:");
                LogUtil.log(logger, Level.ERROR, " >> " + xmlValidationEventHandler.getEvents().toString() + " << \n");
            }
        } catch (JAXBException e) {
            LogUtil.logError(logger, "Error creating Process:", e);
        }
        return process;
    }

    private Process deserializeXML(String inputXMLString) throws JAXBException {
        // Reset the events buffer before every deserialization
        xmlValidationEventHandler.clearEvents();
        return deserialize(inputXMLString);
    }

    private Process deserialize(String inputXMLContent) throws JAXBException {
        StringReader stringReader = new StringReader(inputXMLContent);
        return (Process) jaxbUnmarshaller.unmarshal(stringReader);
    }
}
