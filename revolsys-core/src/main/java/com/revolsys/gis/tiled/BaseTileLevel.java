package com.revolsys.gis.tiled;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jeometry.common.data.identifier.Identifier;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.Point;

public class BaseTileLevel implements TileLevel {

  public static List<TileLevel> create(final Consumer<BaseTileLevel> configurer,
    final double... resoluttions) {
    final List<TileLevel> levels = new ArrayList<>();
    for (final double resolution : resoluttions) {
      final BaseTileLevel tileLevel = new BaseTileLevel().setPixelSize(resolution);
      configurer.accept(tileLevel);
      levels.add(tileLevel);
    }
    return levels;
  }

  protected GeometryFactory geometryFactory;

  protected BoundingBox boundingBox;

  protected Identifier level;

  protected double originX;

  protected double originY;

  protected Point origin;

  protected int tileHeightPixels;

  protected int tileWidthPixels;

  protected double pixelWidth;

  protected double pixelHeight;

  protected int tileXCount;

  protected int tileYCount;

  private double tileWidth;

  private double tileHeight;

  private BaseTileLevel calculateFields() {
    this.origin = this.geometryFactory.point(this.originX, this.originY);
    this.tileWidth = this.tileWidthPixels * this.pixelWidth;
    this.tileHeight = this.tileHeightPixels * this.pixelHeight;
    final double minX = this.originX;
    final double maxY = this.originY;
    final double minY = maxY - this.tileHeight * this.tileYCount;
    final double maxX = minX + this.tileWidth * this.tileXCount;
    this.boundingBox = this.geometryFactory.newBoundingBox(minX, minY, maxX, maxY);
    return this;
  }

  @Override
  public BoundingBox getBoundingBox() {
    return this.boundingBox;
  }

  @Override
  public BoundingBox getBoundingBox(int tileX, int tileY) {
    final double minX = this.originX + this.tileWidth * tileX;
    final double maxY = this.originY - this.tileHeight * tileY;
    final double maxX = minX + this.tileWidth;
    final double minY = maxY - this.tileHeight;
    return this.geometryFactory.newBoundingBox(minX, minY, maxX, maxY);
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  @Override
  public Identifier getLevel() {
    return this.level;
  }

  public Point getOrigin() {
    return this.origin;
  }

  public double getOriginX() {
    return this.originX;
  }

  public double getOriginY() {
    return this.originY;
  }

  @Override
  public double getPixelHeight() {
    return this.pixelHeight;
  }

  @Override
  public double getPixelWidth() {
    return this.pixelWidth;
  }

  @Override
  public int getTileHeightPixels() {
    return this.tileHeightPixels;
  }

  @Override
  public int getTileWidthPixels() {
    return this.tileWidthPixels;
  }

  @Override
  public int getTileX(final double x) {
    final double deltaX = x - this.originX;
    final double ratio = deltaX / this.tileWidth;
    int tileX = (int)Math.floor(ratio);
    if (tileX > 0 && ratio - tileX < 0.0001) {
      tileX--;
    }
    return tileX;
  }

  @Override
  public int getTileXCount() {
    return this.tileXCount;
  }

  @Override
  public int getTileY(final double y) {
    final double deltaY = this.originY - y;
    final double ratio = deltaY / this.tileHeight;
    final int tileY = (int)Math.floor(ratio);
    return tileY;
  }

  @Override
  public int getTileYCount() {
    return this.tileYCount;
  }

  public BaseTileLevel setOrigin(double x, double y) {
    this.originX = x;
    this.originY = y;
    return calculateFields();
  }

  public BaseTileLevel setPixelSize(double pixelSize) {
    this.pixelWidth = pixelSize;
    this.pixelHeight = pixelSize;
    return calculateFields();
  }

  public BaseTileLevel setTileSizePixels(int tileSizePixels) {
    this.tileWidthPixels = tileSizePixels;
    this.tileHeightPixels = tileSizePixels;
    return calculateFields();
  }

  @Override
  public String toString() {
    return this.level.toString();
  }
}
