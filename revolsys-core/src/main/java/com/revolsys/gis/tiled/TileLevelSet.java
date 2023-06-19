package com.revolsys.gis.tiled;

public interface TileLevelSet {

  TileLevel getTileLevel(double metresPerPixel);

  default double getTileLevelResolution(final double metresPerPixel) {
    final TileLevel tileLevel = getTileLevel(metresPerPixel);
    if (tileLevel != null) {
      return tileLevel.getTileWidthPixels();
    }
    return Double.NaN;
  }

}
