package org.esa.snap.s2tbx.cep.graph;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Cosmin Cara
 */
public class GraphParser {

    public static GraphDescriptor parse(InputStream inputStream)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        Handler handler = new Handler();
        parser.parse(inputStream, handler);

        return handler.getResult();
    }

    private static class Handler extends DefaultHandler {
        private GraphDescriptor result;
        private GraphNode currentNode;
        private StringBuilder buffer = new StringBuilder(512);
        private boolean isInParameterCollection;
        private String currentName;
        private String currentValue;

        public GraphDescriptor getResult() { return this.result; }

        @Override
        public void startDocument() throws SAXException {
            result = new GraphDescriptor();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.indexOf(":") > 0) {
                qName = qName.substring(qName.indexOf(":") + 1);
            }
            buffer.setLength(0);
            switch (qName) {
                case "graph":
                case "version":
                case "operator":
                case "sources":
                    break;
                case "node":
                    String id = attributes.getValue("id");
                    currentNode = new GraphNode(id);
                    break;
                case "source":
                case "sourceProduct":
                    String refId = attributes.getValue("refid");
                    if (refId != null) {
                        GraphNode previous = result.getNode(refId);
                        currentNode.setPrevious(previous);
                    }
                    break;
                case "parameters":
                    isInParameterCollection = true;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.indexOf(":") > 0) {
                qName = qName.substring(qName.indexOf(":") + 1);
            }
            switch (qName) {
                case "node":
                    result.addNode(currentNode);
                    break;
                case "parameters":
                    isInParameterCollection = false;
                    break;
                case "name":
                    currentName = buffer.toString();
                    break;
                case "expression":
                    currentValue = buffer.toString();
                    break;
                case "variable":
                    currentNode.setVariable(currentName, currentValue);
                    break;
                case "condition":
                    currentNode.setCondition(currentName, currentValue);
                    break;
                case "variables":
                case "conditions":
                    break;
                default:
                    if (isInParameterCollection) {
                        currentNode.setArgument(qName, buffer.toString());
                    }
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            buffer.append(ch, start, length);
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            e.printStackTrace();
        }
    }
}
