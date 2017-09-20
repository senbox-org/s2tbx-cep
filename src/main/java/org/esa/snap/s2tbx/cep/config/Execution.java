package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "execution")
public class Execution {
    @XmlElement(name = "search")
    private Search search;
    @XmlElement(name = "config")
    private Configuration config;

    public Search getSearch() { return search; }
    public void setSearch(Search search) { this.search = search; }
    public Configuration getConfiguration () { return config; }
    public void setConfiguration (Configuration config) { this.config = config; }

    @Override
    public String toString() {
        return "Execution [search = " + search + ", config = " + config + "]";
    }
}
