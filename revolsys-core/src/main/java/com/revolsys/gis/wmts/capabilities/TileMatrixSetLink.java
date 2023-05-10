package com.revolsys.gis.wmts.capabilities;

import org.w3c.dom.Element;

import com.revolsys.record.io.format.xml.XmlUtil;

public class TileMatrixSetLink {

  private WmtsTileMatrixSet tileMatrixSet;

  private String tileMatrixSetName;

  private WmtsContents contents;

  public TileMatrixSetLink(WmtsContents contents, final Element element) {
    this.contents = contents;
    this.tileMatrixSetName = XmlUtil.getFirstElementText(element, "TileMatrixSet");
    // TODO TileMatrixSetLimits
  }

  public WmtsTileMatrixSet getTileMatrixSet() {
    if (tileMatrixSet == null) {
      this.tileMatrixSet = contents.getTileMatrixSet(tileMatrixSetName);
    }
    return tileMatrixSet;
  }

  @Override
  public String toString() {
    return tileMatrixSetName;
  }
}
