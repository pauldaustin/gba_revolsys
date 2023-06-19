package com.revolsys.gis.wmts.capabilities;

import org.jeometry.coordinatesystem.model.systems.EpsgCoordinateSystems;
import org.w3c.dom.Element;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.record.io.format.xml.XmlUtil;

public class OwsCapabilities {

  public static BoundingBox toBoundingBox(final Element element) {
    final String crs = element.getAttribute("crs");
    final GeometryFactory geometryFactory = GeometryFactory.floating2d(EpsgCoordinateSystems::parse,
      crs);
    final String lowerCorner = XmlUtil.getFirstElementText(element, "LowerCorner");
    final String[] lower = lowerCorner.split(" ");
    final String upperCorner = XmlUtil.getFirstElementText(element, "UpperCorner");
    final String[] upper = upperCorner.split(" ");
    final double minX = Double.parseDouble(lower[0]);
    final double minY = Double.parseDouble(lower[1]);
    final double maxX = Double.parseDouble(upper[0]);
    final double maxY = Double.parseDouble(upper[1]);
    return geometryFactory.newBoundingBox(minX, minY, maxX, maxY);
  }

  private final String updateSequence;

  private final String version;

  public OwsCapabilities(final Element element) {
    this.version = element.getAttribute("version");
    this.updateSequence = element.getAttribute("updateSequence");

  }

  public String getUpdateSequence() {
    return this.updateSequence;
  }

  public String getVersion() {
    return this.version;
  }

}
