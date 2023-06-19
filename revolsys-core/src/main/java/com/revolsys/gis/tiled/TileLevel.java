package com.revolsys.gis.tiled;

import org.jeometry.common.data.identifier.Identifier;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;

public interface TileLevel {

  BoundingBox getBoundingBox();

  BoundingBox getBoundingBox(int tileX, int tileY);

  GeometryFactory getGeometryFactory();

  Identifier getLevel();

  double getPixelHeight();

  double getPixelWidth();

  int getTileHeightPixels();

  int getTileWidthPixels();

  int getTileX(double minX);

  int getTileXCount();

  int getTileY(double minY);

  int getTileYCount();
}
