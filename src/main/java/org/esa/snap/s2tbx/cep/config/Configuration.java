package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "config")
public class Configuration {
    @XmlElement(name = "slave")
    private Slave slave;
    @XmlElement(name = "master")
    private Master master;

    public Slave getSlave ()
    {
        return slave;
    }
    public void setSlave (Slave slave)
    {
        this.slave = slave;
    }
    public Master getMaster ()
    {
        return master;
    }
    public void setMaster (Master master)
    {
        this.master = master;
    }

    @Override
    public String toString() {
        return "Configuration [slave = " + slave + ", master = " + master + "]";
    }
}
