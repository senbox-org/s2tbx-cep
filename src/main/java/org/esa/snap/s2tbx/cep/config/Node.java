package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Cosmin Cara
 */
@XmlRootElement(name = "node")
public class Node {
    private String name;
    private String os;
    private String gptLocation;
    private String outputFormat;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getOs() {
        return os;
    }
    public void setOs(String os) {
        this.os = os;
    }
    public String getGptLocation() { return gptLocation; }
    public void setGptLocation(String gptLocation) { this.gptLocation = gptLocation; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    @Override
    public String toString() {
        return "Node [name=" + name + ", os=" + os + ", gptLocation=" + gptLocation + ", format=" + outputFormat + "]";
    }
}
