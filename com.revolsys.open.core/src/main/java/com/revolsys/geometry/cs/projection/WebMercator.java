package com.revolsys.geometry.cs.projection;

import com.revolsys.geometry.cs.ProjectedCoordinateSystem;

public class WebMercator extends AbstractCoordinatesProjection {

  public WebMercator(final ProjectedCoordinateSystem cs) {
  }

  @Override
  public void inverse(final double x, final double y, final double[] targetCoordinates,
    final int targetOffset) {
    final double lon = x / 20037508.34 * 180;
    double lat = y / 20037508.34 * 180;

    lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI / 2);

    targetCoordinates[targetOffset] = lon;
    targetCoordinates[targetOffset + 1] = lat;
  }

  @Override
  public void project(final double lon, final double lat, final double[] targetCoordinates,
    final int targetOffset) {

    final double x = lon * 20037508.34 / 180;
    double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
    y = y * 20037508.34 / 180;
    targetCoordinates[targetOffset] = x;
    targetCoordinates[targetOffset + 1] = y;
  }

}
