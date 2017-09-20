package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "slave")
public class Slave {
    @XmlElement(name = "shareMound")
    private String shareMount;
    @XmlElement(name = "inputFolder")
    private String inputFolder;
    @XmlList
    private List<String> nodes;
    @XmlElement(name = "outputFolder")
    private String outputFolder;
    @XmlElement(name = "user")
    private String user;
    @XmlElement(name = "password")
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
    public List<String> getNodes ()
    {
        return nodes;
    }
    public void setNodes (List<String> nodes)
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
                + String.join(",", nodes) + ", outputFolder = " + outputFolder
                + ", user = " + user + ", password = " + password + "]";
    }
}
