package com.revolsys.gis.wmts.capabilities;

import org.w3c.dom.Element;

import com.revolsys.record.io.format.xml.XmlUtil;

public class TileMatrixSetLink {

  private WmtsTileMatrixSet tileMatrixSet;

  private final String tileMatrixSetName;

  private final WmtsContents contents;

  public TileMatrixSetLink(final WmtsContents contents, final Element element) {
    this.contents = contents;
    this.tileMatrixSetName = XmlUtil.getFirstElementText(element, "TileMatrixSet");
    // TODO TileMatrixSetLimits
  }

  public WmtsTileMatrixSet getTileMatrixSet() {
    if (this.tileMatrixSet == null) {
      this.tileMatrixSet = this.contents.getTileMatrixSet(this.tileMatrixSetName);
    }
    return this.tileMatrixSet;
  }

  @Override
  public String toString() {
    return this.tileMatrixSetName;
  }
}
