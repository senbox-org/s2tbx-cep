package org.esa.snap.s2tbx.cep.graph;

import org.esa.snap.s2tbx.cep.util.XmlConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kraftek on 9/6/2016.
 */
public class GraphDescriptor {
    private List<GraphNode> nodes;

    public GraphDescriptor() {
        this.nodes = new ArrayList<>();
    }

    public void addNode(String operator, String arguments) {
        GraphNode previous = null;
        if (this.nodes.size() > 0) {
            previous = this.nodes.get(this.nodes.size() - 1);
        }
        GraphNode newNode = new GraphNode(operator, arguments, previous);
        this.nodes.add(newNode);
    }

    public GraphNode getNode(int index) {
        return index >= 0 && index < this.nodes.size() ? this.nodes.get(index) : null;
    }

    public int getNodeCount() {
        return this.nodes.size();
    }

    public void insertNode(int index, String operator, String arguments) {
        if (index >= 0 && index < this.nodes.size()) {
            GraphNode previous = index > 0 ? this.nodes.get(index - 1) : null;
            GraphNode newNode = new GraphNode(operator, arguments, previous);
            this.nodes.add(index, newNode);
            this.nodes.get(index + 1).setPrevious(newNode);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<graph id=\"Graph\">\n");
        builder.append(XmlConstants.LEVEL_1).append("<version>1.0</version>\n");
        for (GraphNode node : this.nodes) {
            builder.append(node.toString());
        }
        builder.append("</graph>");
        return builder.toString();
    }
}
