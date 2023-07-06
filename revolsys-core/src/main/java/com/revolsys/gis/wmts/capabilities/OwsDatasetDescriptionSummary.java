package com.revolsys.gis.wmts.capabilities;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.record.io.format.xml.XmlUtil;

public class OwsDatasetDescriptionSummary extends OwsDescription {

  private List<BoundingBox> wgs84BoundingBoxes = new ArrayList<>();

  private List<BoundingBox> boundingBoxes = new ArrayList<>();

  private final String identifier;

  public OwsDatasetDescriptionSummary(final Element element) {
    super(element);
    this.identifier = XmlUtil.getFirstElementText(element, "Identifier");
    this.wgs84BoundingBoxes = XmlUtil.getList(element, "WGS84BoundingBox",
      OwsCapabilities::toBoundingBox);
    this.boundingBoxes = XmlUtil.getList(element, "BoundingBox", OwsCapabilities::toBoundingBox);
    // TODO metadata
    // TODO DataSetDescriptionSummary
  }

  public List<BoundingBox> getBoundingBoxes() {
    return this.boundingBoxes;
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public List<BoundingBox> getWgs84BoundingBoxes() {
    return this.wgs84BoundingBoxes;
  }
}
