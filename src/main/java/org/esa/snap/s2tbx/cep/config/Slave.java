package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement(name = "slave")
public class Slave {
    private String shareMount;
    private String inputFolder;
    private List<Node> nodes;
    private String outputFolder;
    private String user;
    private String password;

    public String getShareMount ()
    {
        return shareMount;
    }
    public void setShareMount (String shareMount)
    {
        this.shareMount = shareMount;
    }
    public String getInputFolder ()
    {
        return inputFolder;
    }
    public void setInputFolder (String inputFolder)
    {
        this.inputFolder = inputFolder;
    }
    @XmlElementWrapper(name = "nodes")
    @XmlElement(name = "node")
    public List<Node> getNodes ()
    {
        return nodes;
    }
    public void setNodes (List<Node> nodes)
    {
        this.nodes = nodes;
    }
    public String getOutputFolder ()
    {
        return outputFolder;
    }
    public void setOutputFolder (String outputFolder) { this.outputFolder = outputFolder; }
    public String getUser ()
    {
        return user;
    }
    public void setUser (String user)
    {
        this.user = user;
    }
    public String getPassword ()
    {
        return password;
    }
    public void setPassword (String password)
    {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Slave [shareMount = " + shareMount + ", inputFolder = " + inputFolder + ", nodes = "
                + String.join(",", nodes.stream().map(Node::getName).collect(Collectors.toList())) + ", outputFolder = " + outputFolder
                + ", user = " + user + ", password = " + password + "]";
    }
}
