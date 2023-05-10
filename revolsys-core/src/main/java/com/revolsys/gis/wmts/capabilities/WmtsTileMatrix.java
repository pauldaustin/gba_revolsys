package com.revolsys.gis.wmts.capabilities;

import java.util.List;
import java.util.Objects;

import org.jeometry.common.data.identifier.Identifier;
import org.jeometry.coordinatesystem.model.Axis;
import org.jeometry.coordinatesystem.model.CoordinateSystem;
import org.jeometry.coordinatesystem.model.GeographicCoordinateSystem;
import org.jeometry.coordinatesystem.model.unit.LinearUnit;
import org.w3c.dom.Element;

import com.revolsys.gis.tiled.BaseTileLevel;
import com.revolsys.record.io.format.xml.XmlUtil;

public class WmtsTileMatrix extends BaseTileLevel {

  private final double scaleDenominator;

  private WmtsTileMatrixSet tileMatrixSet;

  public WmtsTileMatrix(WmtsTileMatrixSet tileMatrixSet, final Element element) {
    this.tileMatrixSet = tileMatrixSet;
    this.level = Identifier.newIdentifier(XmlUtil.getFirstElementText(element, "Identifier"));
    this.scaleDenominator = XmlUtil.getFirstElementDouble(element, "ScaleDenominator");
    final String[] topLeftCorner = XmlUtil.getFirstElementText(element, "TopLeftCorner").split(" ");

    this.geometryFactory = tileMatrixSet.getGeometryFactory();

    this.tileWidthPixels = XmlUtil.getFirstElementInt(element, "TileWidth");
    this.tileHeightPixels = XmlUtil.getFirstElementInt(element, "TileHeight");
    this.tileXCount = XmlUtil.getFirstElementInt(element, "MatrixWidth");
    this.tileYCount = XmlUtil.getFirstElementInt(element, "MatrixHeight");
    final CoordinateSystem coordinateSystem = this.geometryFactory.getCoordinateSystem();
    final LinearUnit lengthUnit = coordinateSystem.getLinearUnit();
    final double metresPerUnit = lengthUnit.toMetres(1);
    final double pixelSize = this.scaleDenominator * 0.00028 / metresPerUnit;

    setPixelSize(pixelSize);
    final List<Axis> axis = coordinateSystem.getAxis();
    final double c1 = Double.parseDouble(topLeftCorner[0]);
    final double c2 = Double.parseDouble(topLeftCorner[1]);
    if (axis.isEmpty() && coordinateSystem instanceof GeographicCoordinateSystem
      || "north".equalsIgnoreCase(axis.get(0).getOrientation())) {
      setOrigin(c2, c1);
    } else {
      setOrigin(c1, c2);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WmtsTileMatrix other = (WmtsTileMatrix)obj;
    return Double.doubleToLongBits(scaleDenominator) == Double
      .doubleToLongBits(other.scaleDenominator)
      && Objects.equals(tileMatrixSet, other.tileMatrixSet);
  }

  public double getScaleDenominator() {
    return this.scaleDenominator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scaleDenominator, tileMatrixSet);
  }

}
