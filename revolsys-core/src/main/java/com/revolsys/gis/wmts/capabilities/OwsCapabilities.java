package com.revolsys.gis.wmts.capabilities;

import org.jeometry.coordinatesystem.model.systems.EpsgCoordinateSystems;
import org.w3c.dom.Element;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.record.io.format.xml.XmlUtil;

public class OwsCapabilities {

  private final String updateSequence;

  private final String version;

  public OwsCapabilities(final Element element) {
    this.version = element.getAttribute("version");
    this.updateSequence = element.getAttribute("updateSequence");

  }

  public String getUpdateSequence() {
    return updateSequence;
  }

  public String getVersion() {
    return version;
  }

  public static BoundingBox toBoundingBox(Element element) {
    String crs = element.getAttribute("crs");
    GeometryFactory geometryFactory = GeometryFactory.floating2d(EpsgCoordinateSystems::parse, crs);
    String lowerCorner = XmlUtil.getFirstElementText(element, "LowerCorner");
    String[] lower = lowerCorner.split(" ");
    String upperCorner = XmlUtil.getFirstElementText(element, "UpperCorner");
    String[] upper = upperCorner.split(" ");
    double minX = Double.parseDouble(lower[0]);
    double minY = Double.parseDouble(lower[1]);
    double maxX = Double.parseDouble(upper[0]);
    double maxY = Double.parseDouble(upper[1]);
    return geometryFactory.newBoundingBox(minX, minY, maxX, maxY);
  }

}
