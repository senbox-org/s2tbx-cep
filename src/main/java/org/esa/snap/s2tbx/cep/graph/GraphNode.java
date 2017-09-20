package org.esa.snap.s2tbx.cep.graph;

import org.esa.snap.s2tbx.cep.util.XmlConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kraftek on 9/6/2016.
 */
public class GraphNode {

    private String operator;
    private Map<String, Object> parameters;
    private GraphNode previous;

    GraphNode(String operator, String arguments, GraphNode previous) {
        this.operator = operator;
        this.parameters = parseArguments(arguments);
        this.previous = previous;
    }

    public String getOperator() {
        return this.operator;
    }

    public void setPrevious(GraphNode previous) {
        this.previous = previous;
    }

    public void setArgument(String name, String value) {
        this.parameters.put(name, value);
    }

    public String parametersToString() {
        StringBuilder builder = new StringBuilder();
        builder.append(XmlConstants.LEVEL_3).append("<parameters class=\"com.bc.ceres.binding.dom.XppDomElement\">\n");
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                builder.append(XmlConstants.LEVEL_4).append("<").append(entry.getKey()).append(">");
                Object value = entry.getValue();
                if (value instanceof CollectionElement) {
                    builder.append("\n");
                }
                builder.append(value.toString()).append("</").append(entry.getKey()).append(">\n");
            }
        }
        builder.append(XmlConstants.LEVEL_3).append("</parameters>\n");
        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(XmlConstants.LEVEL_2).append("<node id=\"").append(this.operator).append("\">\n");
        builder.append(XmlConstants.LEVEL_3).append("<operator>").append(this.operator).append("</operator>\n");
        builder.append(XmlConstants.LEVEL_3).append("<sources>\n");
        if (previous != null) {
            builder.append(XmlConstants.LEVEL_4).append("<sourceProduct refid=\"").append(previous.operator).append("\"/>\n");
        }
        builder.append(XmlConstants.LEVEL_3).append("</sources>\n");
        builder.append(XmlConstants.LEVEL_3).append("<parameters class=\"com.bc.ceres.binding.dom.XppDomElement\">\n");
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                builder.append(XmlConstants.LEVEL_4).append("<").append(entry.getKey()).append(">");
                Object value = entry.getValue();
                if (value instanceof CollectionElement) {
                    builder.append("\n");
                }
                builder.append(value.toString()).append("</").append(entry.getKey()).append(">\n");
            }
        }
        builder.append(XmlConstants.LEVEL_3).append("</parameters>\n");
        builder.append(XmlConstants.LEVEL_2).append("</node>\n");
        return builder.toString();
    }

    private Map<String, Object> parseArguments(String arguments) {
        Map<String, Object> args = new HashMap<>();
        if (arguments != null) {
            if (arguments.startsWith("\"") && arguments.endsWith("\"")) {
                arguments = arguments.substring(1, arguments.length() - 1);
            }
            arguments = arguments.replace("'", "\"");
            String[] tokens = arguments.split("-P");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    token = token.trim();
                    String key = token.substring(0, token.indexOf("="));
                    String value = token.substring(token.indexOf("=") + 1);
                    if (value.startsWith("[")) {
                        args.put(key, parseCollection(value));
                    } else {
                        args.put(key, value);
                    }
                }
            }
        }
        return args;
    }

    private CollectionElement parseCollection(String contents) {
        CollectionElement collection = new CollectionElement();
        if (contents.startsWith("[") && contents.endsWith("]")) {
            contents = contents.substring(1, contents.length() - 1);
            String[] stringElements = contents.split(";");
            collection.setElementName(contents.substring(0, contents.indexOf("[")));
            Map<String, String> elementContents;
            for (String stringElement : stringElements) {
                elementContents = new LinkedHashMap<>();
                stringElement = stringElement.substring(stringElement.indexOf("[") + 1, stringElement.length() - 1);
                String[] elements = stringElement.split(",");
                for (String element : elements) {
                    String[] tokens = element.split("=");
                    elementContents.put(tokens[0], tokens[1]);
                }
                collection.addElement(elementContents);
            }
        }
        return collection;
    }

    private class CollectionElement {

        private String elementName;
        private List<Map<String, String>> elements;

        CollectionElement() {
            this.elements = new ArrayList<>();
        }

        void setElementName(String elementName) {
            this.elementName = elementName;
        }

        void addElement(Map<String, String> pairs) {
            this.elements.add(pairs);
        }

        @Override
        public java.lang.String toString() {
            StringBuilder builder = new StringBuilder();
            for (Map<String, String> element : this.elements) {
                builder.append("<").append(elementName).append(">\n");
                for (Map.Entry<String, String> entry : element.entrySet()) {
                    builder.append("<").append(entry.getKey()).append(">")
                            .append(entry.getValue()).append("</").append(entry.getKey()).append(">\n");
                }
                builder.append("</").append(elementName).append(">\n");
            }
            return builder.toString();
        }
    }
}
