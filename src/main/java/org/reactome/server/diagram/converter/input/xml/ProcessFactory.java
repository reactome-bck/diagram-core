package org.reactome.server.diagram.converter.input.xml;

import org.apache.log4j.Logger;
import org.reactome.server.diagram.converter.input.model.Process;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import java.net.URL;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */

public class ProcessFactory {
    //TODO: Refactor the getLogger method to use this one :)
    private static Logger logger = Logger.getLogger(ProcessFactory.class.getName());


    private JAXBContext jaxbContext = null;
    private Unmarshaller jaxbUnmarshaller = null;
    private XMLValidationEventHandler xmlValidationEventHandler = null;

    public ProcessFactory(String schemaLocation) {

        try {
            jaxbContext = JAXBContext.newInstance(Process.class);

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

            //TODO Remove dependency through setter or constructor parameter
            xmlValidationEventHandler = new XMLValidationEventHandler();
            jaxbUnmarshaller.setEventHandler(xmlValidationEventHandler);
        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }
    }

    public Process createProcess(String xmlContent, String stId){
        Process process = null;
        try {
            ///TODO: store the path of the schema file as a propertie or as an input argument???
            process = deserializeXML(xmlContent, new StringBuilder());

            //Show the derialisation log
            if (!getLogger().toString().isEmpty()) {
                logger.error(" >> ERROR: in XML Deserialisation of [" + stId + "] " + process.getProperties().getIsChangedOrDisplayName().get(0));
                logger.error(" >> " + getLogger().toString() + " << ");
            }

        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }

        return process;
    }

    public Process deserializeXML(File inputFile, StringBuilder logger ) throws JAXBException {

        if(logger!=null){ xmlValidationEventHandler.setInternalLogger( logger ); }
        return deserializeXML(inputFile);
    }

    public Process deserializeXML(String inputXMLString, StringBuilder logger ) throws JAXBException {

        if(logger!=null){ xmlValidationEventHandler.setInternalLogger( logger ); }
        return deserializeXML(inputXMLString);
    }


    private Process deserializeXML(String inputXMLContent) throws JAXBException {

        StringReader stringReader = new StringReader(inputXMLContent);
        Process process = (Process) jaxbUnmarshaller.unmarshal(stringReader);
        return process;
    }

    private Process deserializeXML(File inputFile ) throws JAXBException {

        Process process = (Process) jaxbUnmarshaller.unmarshal(inputFile);
        return process;
    }


    public void setLogger(StringBuilder logger){
        if(logger!=null && xmlValidationEventHandler!=null) {
            xmlValidationEventHandler.setInternalLogger( logger );
        }
    }

    public StringBuilder getLogger(){
        if(xmlValidationEventHandler!=null) {
            return xmlValidationEventHandler.getInternalLogger();
        }else{
            return null;
        }
    }
    public void clearLogger(){
        if(xmlValidationEventHandler!=null && xmlValidationEventHandler.getInternalLogger()!=null) {
            xmlValidationEventHandler.getInternalLogger().setLength(0);
        }
    }


}
