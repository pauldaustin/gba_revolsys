package com.revolsys.swing.map.layer.arcgisrest;

import java.awt.image.BufferedImage;

import com.revolsys.io.esri.map.rest.MapServer;
import com.revolsys.swing.map.layer.MapTile;

public class ArcGisServerRestMapTile extends MapTile {

  private final MapServer mapServer;

  private final int zoomLevel;

  private final int tileX;

  private final int tileY;

  public ArcGisServerRestMapTile(final MapServer mapServer,
    final int zoomLevel, final double resolution, final int tileX,
    final int tileY) {
    super(mapServer.getBoundingBox(zoomLevel, tileX, tileY),
      mapServer.getTileInfo().getWidth(), mapServer.getTileInfo().getHeight(),
      resolution);
    this.mapServer = mapServer;
    this.zoomLevel = zoomLevel;
    this.tileX = tileX;
    this.tileY = tileY;
  }

  public MapServer getMapServer() {
    return mapServer;
  }

  public int getTileX() {
    return tileX;
  }

  public int getTileY() {
    return tileY;
  }

  public int getZoomLevel() {
    return zoomLevel;
  }

  protected BufferedImage loadBuffferedImage() {
    return mapServer.getTileImage(zoomLevel, tileX, tileY);
  }

  @Override
  public String toString() {
    return mapServer.getMapName() + " " + zoomLevel + "/" + tileX + "/" + tileY;
  }
}
