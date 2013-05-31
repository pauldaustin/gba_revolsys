package com.revolsys.swing.map.layer;

import java.util.List;

import com.revolsys.swing.map.Viewport2D;

public abstract class AbstractTiledImageLayer extends AbstractLayer {

  public AbstractTiledImageLayer() {
    this(null, true, false, false);
  }

  public AbstractTiledImageLayer(final String name, final boolean readOnly,
    final boolean selectSupported, final boolean querySupported) {
    super(name);
    setReadOnly(readOnly);
    setSelectSupported(selectSupported);
    setQuerySupported(querySupported);
    setRenderer(new TiledImageLayerRenderer(this));
  }

  public abstract List<MapTile> getOverlappingMapTiles(
    final Viewport2D viewport);

  public TileLoaderProcess createTileLoaderProcess() {
    return new TileLoaderProcess(this);
  }

  public abstract double getResolution(final Viewport2D viewport);
}
