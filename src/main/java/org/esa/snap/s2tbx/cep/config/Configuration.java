package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "configuration")
public class Configuration {
    private Slave slave;
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
