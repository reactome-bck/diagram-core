package org.reactome.server.diagram.converter.input.xml;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reactome.server.diagram.converter.input.model.Process;
import org.reactome.server.diagram.converter.util.report.LogUtil;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import java.net.URL;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */

public class ProcessFactory {
    //TODO: Refactor the getBuffer method to use this one :)
    private static Logger logger = Logger.getLogger(ProcessFactory.class.getName());

    private Unmarshaller jaxbUnmarshaller = null;
    private XMLValidationEventHandler xmlValidationEventHandler = null;

    public ProcessFactory(String schemaLocation) {

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Process.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = null;
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                URL url = classLoader.getResource(schemaLocation);
                if(url!=null) {
                    schema = sf.newSchema(new File(url.getFile()));
                }
            } catch (SAXException e) {
                e.printStackTrace();
            }
            jaxbUnmarshaller.setSchema(schema);

            xmlValidationEventHandler = new XMLValidationEventHandler();
            jaxbUnmarshaller.setEventHandler(xmlValidationEventHandler);
        } catch (JAXBException e) {
            LogUtil.logError(logger, "Error instantiating ProcessFactory:", e);
        }
    }

    public Process createProcess(String xmlContent, String stId){
        Process process = null;
        try {
            process = deserializeXML(xmlContent, new StringBuilder());

            //Show the derialisation log
            if (!getBuffer().toString().isEmpty()) {
                LogUtil.log(logger, Level.ERROR, "[" + stId + "] Error(s) in XML deserialisation of [" + process.getProperties().getIsChangedOrDisplayName().get(0) + "]:");
                LogUtil.log(logger, Level.ERROR, " >> " + getBuffer().toString() + " << \n");
            }
        } catch (JAXBException e) {
            LogUtil.logError(logger, "Error creating Process:", e);
        }
        return process;
    }

    public Process deserializeXML(File inputFile, StringBuilder buffer ) throws JAXBException {
        if(buffer!=null){ xmlValidationEventHandler.setInternalBuffer(buffer); }
        return deserializeXML(inputFile);
    }

    public Process deserializeXML(String inputXMLString, StringBuilder buffer ) throws JAXBException {
        if(buffer!=null){ xmlValidationEventHandler.setInternalBuffer(buffer); }
        return deserializeXML(inputXMLString);
    }


    private Process deserializeXML(String inputXMLContent) throws JAXBException {
        StringReader stringReader = new StringReader(inputXMLContent);
        return (Process) jaxbUnmarshaller.unmarshal(stringReader);
    }

    private Process deserializeXML(File inputFile ) throws JAXBException {
        return (Process) jaxbUnmarshaller.unmarshal(inputFile);
    }


    public void setBuffer(StringBuilder buffer){
        if(buffer!=null && xmlValidationEventHandler!=null) {
            xmlValidationEventHandler.setInternalBuffer(buffer);
        }
    }

    public StringBuilder getBuffer(){
        if(xmlValidationEventHandler!=null) {
            return xmlValidationEventHandler.getInternalBuffer();
        }else{
            return null;
        }
    }
    public void clearBuffer(){
        if(xmlValidationEventHandler!=null && xmlValidationEventHandler.getInternalBuffer()!=null) {
            xmlValidationEventHandler.getInternalBuffer().setLength(0);
        }
    }
}
