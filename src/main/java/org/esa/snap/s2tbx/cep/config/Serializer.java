package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;

/**
 * @author Cosmin Cara
 */
public class Serializer {
    private final JAXBContext context;
    private final static Serializer instance = new Serializer();

    private Serializer() {
        try {
            this.context = JAXBContext.newInstance(Execution.class, Configuration.class, Node.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static Execution deserialize(StreamSource source) {
        try {
            Unmarshaller unmarshaller = instance.context.createUnmarshaller();
            JAXBElement<Execution> result = unmarshaller.unmarshal(source, Execution.class);
            return result.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serialize(Execution obj) {
        try {
            Marshaller marshaller = instance.context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(obj, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
