package com.revolsys.swing.map.layer.ogc.wmts;

import java.awt.image.BufferedImage;
import java.util.Objects;

import com.revolsys.gis.wmts.capabilities.WmtsTileMatrix;
import com.revolsys.raster.GeoreferencedImageMapTile;

public class WmtsMapTile extends GeoreferencedImageMapTile {
  private final OgcWmtsTiledImageLayer layer;

  private final WmtsTileMatrix tileMatrix;

  private final int tileX;

  private final int tileY;

  public WmtsMapTile(final OgcWmtsTiledImageLayer layer, final WmtsTileMatrix tileMatrix,
    final int tileX, final int tileY) {
    super(tileMatrix.getBoundingBox(tileX, tileY), tileMatrix.getTileWidthPixels(),
      tileMatrix.getTileHeightPixels());
    this.layer = layer;
    this.tileMatrix = tileMatrix;
    this.tileX = tileX;
    this.tileY = tileY;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof WmtsMapTile) {
      final WmtsMapTile tile = (WmtsMapTile)obj;
      if (tile.getTileMatrix() == getTileMatrix()) {
        if (tile.getTileX() == getTileX()) {
          if (tile.getTileY() == getTileY()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public WmtsTileMatrix getTileMatrix() {
    return this.tileMatrix;
  }

  public int getTileX() {
    return this.tileX;
  }

  public int getTileY() {
    return this.tileY;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.layer, this.tileMatrix, this.tileX, this.tileY);
  }

  @Override
  protected BufferedImage loadBuffferedImage() {
    try {
      return this.layer.getTileImage(this.tileMatrix, this.tileX, this.tileY);
    } catch (final Throwable e) {
      this.layer.setError(e);
      return null;
    }
  }

  @Override
  public String toString() {
    return this.layer.getName() + " " + this.tileMatrix + "/" + this.tileX + "/" + this.tileY;
  }
}
