package org.esa.snap.s2tbx.cep.config;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "search")
public class Search {
    private String startDate;
    private String tiles;
    private boolean lookForFolders;
    private String endDate;
    private String productType;

    public String getStartDate () { return startDate; }
    public void setStartDate (String startDate) { this.startDate = startDate; }
    public String getTiles () { return tiles; }
    public void setTiles (String tiles) { this.tiles = tiles; }
    public boolean getLookForFolders () { return lookForFolders; }
    public void setLookForFolders (boolean lookForFolders) { this.lookForFolders = lookForFolders; }
    public String getEndDate () { return endDate; }
    public void setEndDate (String endDate) { this.endDate = endDate; }
    public String getProductType () { return productType; }
    public void setProductType (String productType) { this.productType = productType; }

    @Override
    public String toString() {
        return "Search [startDate = " + startDate + ", tiles = " + String.join(",", tiles) + ", lookForFolders = "
                + lookForFolders + ", endDate = " + endDate + ", productType = " + productType + "]";
    }
}
