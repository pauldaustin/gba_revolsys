package com.revolsys.gis.wmts.capabilities;

import java.util.List;

import org.jeometry.coordinatesystem.model.systems.EpsgCoordinateSystems;
import org.w3c.dom.Element;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.BoundingBoxProxy;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.gis.tiled.BaseTileLevelSet;
import com.revolsys.record.io.format.xml.XmlUtil;

public class WmtsTileMatrixSet extends BaseTileLevelSet implements BoundingBoxProxy {

  private final BoundingBox boundingBox;

  private final GeometryFactory geometryFactory;

  private final String identifier;

  private final List<WmtsTileMatrix> tileMatrices;

  private final String wellKnownScaleSet;

  public WmtsTileMatrixSet(final Element element) {
    this.identifier = XmlUtil.getFirstElementText(element, "Identifier");
    final String supportedCRS = XmlUtil.getFirstElementText(element, "SupportedCRS");
    this.geometryFactory = GeometryFactory.floating2d(EpsgCoordinateSystems::parse, supportedCRS);
    this.boundingBox = XmlUtil.getFirstElement(element, "BoundingBox",
      OwsCapabilities::toBoundingBox);
    this.wellKnownScaleSet = XmlUtil.getFirstElementText(element, "WellKnownScaleSet");
    this.tileMatrices = XmlUtil.getList(element, "TileMatrix", e -> new WmtsTileMatrix(this, e));
    this.tileLevels.addAll(this.tileMatrices);
  }

  @Override
  public BoundingBox getBoundingBox() {
    return this.boundingBox;
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  public String getIdentifier() {
    return this.identifier;
  }

  @Override
  public WmtsTileMatrix getTileLevel(double metresPerPixel) {
    return (WmtsTileMatrix)super.getTileLevel(metresPerPixel);
  }

  public List<WmtsTileMatrix> getTileMatrices() {
    return this.tileMatrices;
  }

  public String getWellKnownScaleSet() {
    return this.wellKnownScaleSet;
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public String toString() {
    return this.identifier;
  }
}
