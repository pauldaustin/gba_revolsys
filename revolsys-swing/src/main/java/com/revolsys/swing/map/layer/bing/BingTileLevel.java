package com.revolsys.swing.map.layer.bing;

import org.jeometry.common.data.identifier.Identifier;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.gis.tiled.TileLevel;

public class BingTileLevel implements TileLevel {

  private final int level;

  private final Identifier identifier;

  private final int mapSizePixels;

  private final double resolution;

  public BingTileLevel(final int level, final double resolution) {
    this.level = level;
    this.identifier = Identifier.newIdentifier(level);
    this.mapSizePixels = BingClient.TILE_SIZE << level;
    this.resolution = resolution;
  }

  @Override
  public BoundingBox getBoundingBox() {
    return BingClient.WORLD_MERCATOR.getAreaBoundingBox();
  }

  @Override
  public BoundingBox getBoundingBox(final int tileX, final int tileY) {
    final double y1 = getLatitude(tileY);
    final double y2 = getLatitude(tileY + 1);
    final double x1 = getLongitude(tileX);
    final double x2 = getLongitude(tileX + 1);
    return BingClient.WGS84.newBoundingBox(x1, y1, x2, y2)//
      .bboxToCs(BingClient.WORLD_MERCATOR);
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return BingClient.WORLD_MERCATOR;
  }

  public double getLatitude(final int tileY) {
    final double mapSize = this.mapSizePixels;
    final double y = 0.5 - tileY * BingClient.TILE_SIZE / mapSize;
    return 90 - 360 * Math.atan(Math.exp(-y * 2 * Math.PI)) / Math.PI;
  }

  @Override
  public Identifier getLevel() {
    return this.identifier;
  }

  public double getLongitude(final int tileX) {
    final double mapSize = this.mapSizePixels;
    final double x = tileX * BingClient.TILE_SIZE / mapSize - 0.5;
    return 360 * x;
  }

  @Override
  public double getPixelHeight() {
    return this.resolution;
  }

  @Override
  public double getPixelWidth() {
    return this.resolution;
  }

  @Override
  public int getTileHeightPixels() {
    return BingClient.TILE_SIZE;
  }

  @Override
  public int getTileWidthPixels() {
    return BingClient.TILE_SIZE;
  }

  @Override
  public int getTileX(final double longitude) {
    final double ratio = (longitude + 180) / 360;
    int tileX = (int)Math.floor(ratio * (1 << this.level));

    if (ratio >= 1) {
      tileX--;
    }
    return tileX;
  }

  @Override
  public int getTileXCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getTileY(final double latitude) {
    final double sinLatitude = Math.sin(latitude * Math.PI / 180);
    final int tileY = (int)Math
      .floor((0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI))
        * Math.pow(2, this.level));
    return tileY;
  }

  @Override
  public int getTileYCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String toString() {
    return this.identifier.toString();
  }

}
