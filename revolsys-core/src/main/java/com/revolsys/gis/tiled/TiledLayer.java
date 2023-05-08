package com.revolsys.gis.tiled;

public interface TiledLayer {

  String getLayerName();

  double getResolution(double metresPerPixel);

}
