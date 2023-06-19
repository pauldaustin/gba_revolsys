package com.revolsys.swing.map.layer.tile;

import java.awt.image.BufferedImage;

import com.revolsys.gis.tiled.TileLevel;
import com.revolsys.raster.GeoreferencedImageMapTile;

public class TiledMapLayerTile extends GeoreferencedImageMapTile {
  private final TiledMapLayer layer;

  private final int tileX;

  private final int tileY;

  private final TileLevel level;

  public TiledMapLayerTile(final TiledMapLayer layer, final TileLevel level, final int tileX,
    final int tileY) {
    super(level.getBoundingBox(tileX, tileY), level.getTileWidthPixels(),
      level.getTileHeightPixels());
    this.layer = layer;
    this.level = level;
    this.tileX = tileX;
    this.tileY = tileY;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof TiledMapLayerTile) {
      final TiledMapLayerTile tile = (TiledMapLayerTile)obj;
      if (tile.layer == this.layer) {
        if (tile.level == this.level) {
          if (tile.tileX == this.tileX) {
            if (tile.tileY == this.tileY) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public int getTileX() {
    return this.tileX;
  }

  public int getTileY() {
    return this.tileY;
  }

  @Override
  public int hashCode() {
    return this.level.hashCode() << 24 & this.tileX << 16 & this.tileY << 8;
  }

  @Override
  protected BufferedImage loadBuffferedImage() {
    try {
      return this.layer.getTileImage(this.level, this.tileX, this.tileY);
    } catch (final Throwable e) {
      this.layer.setError(e);
      return null;
    }
  }

  @Override
  public String toString() {
    return this.layer + " " + this.level + "/" + this.tileX + "/" + this.tileY;
  }
}
